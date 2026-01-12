package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.repository.UserRepository;
import ru.ifmo.se.pisk.service.AlbumService;
import ru.ifmo.se.pisk.service.AudioMetadataService;
import ru.ifmo.se.pisk.service.AudioMetadataService.AudioMetadata;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.LabelService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.web.form.UploadAlbumMetaForm;
import ru.ifmo.se.pisk.web.form.UploadAlbumTracksForm;
import ru.ifmo.se.pisk.web.form.UploadTrackItemForm;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

/**
 * Двухшаговая загрузка альбома:
 * 1) /upload — метаданные альбома (исполнитель, название альбома, жанр, лейбл, обложка, кол-во треков)
 * 2) /upload/tracks — список треков (название + файл для каждого)
 */
@Controller
@RequestMapping({"/upload", "/newupload"})
@SessionAttributes("albumDraft")
public class UploadController {

    private final TrackService trackService;
    private final GenreService genreService;
    private final LabelService labelService;
    private final AlbumService albumService;
    private final AudioMetadataService audioMetadataService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public UploadController(TrackService trackService,
                            GenreService genreService,
                            LabelService labelService,
                            AlbumService albumService,
                            AudioMetadataService audioMetadataService,
                            FileStorageService fileStorageService,
                            UserRepository userRepository) {
        this.trackService = trackService;
        this.genreService = genreService;
        this.labelService = labelService;
        this.albumService = albumService;
        this.audioMetadataService = audioMetadataService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @ModelAttribute("albumDraft")
    public AlbumDraft albumDraft() {
        return new AlbumDraft();
    }

    @GetMapping
    public String form(Model model, HttpServletRequest request, Authentication authentication) {
        if (!model.containsAttribute("albumMetaForm")) {
            UploadAlbumMetaForm form = new UploadAlbumMetaForm();
            if (authentication != null && authentication.getName() != null) {
                userRepository.findByUsernameIgnoreCase(authentication.getName()).ifPresent(user -> {
                    if (user.getRole() == ru.ifmo.se.pisk.domain.UserRole.ARTIST) {
                        form.setArtistName(user.getUsername());
                        model.addAttribute("isArtist", true);
                    }
                });
            }
            model.addAttribute("albumMetaForm", form);
        } else {

            if (authentication != null && authentication.getName() != null) {
                userRepository.findByUsernameIgnoreCase(authentication.getName()).ifPresent(user -> {
                    if (user.getRole() == ru.ifmo.se.pisk.domain.UserRole.ARTIST) {
                        model.addAttribute("isArtist", true);

                        ((UploadAlbumMetaForm) model.getAttribute("albumMetaForm")).setArtistName(user.getUsername());
                    }
                });
            }
        }
        prepareMetaModel(model, request);
        return "upload";
    }

    @PostMapping("/meta")
    public String submitMeta(@Valid @ModelAttribute("albumMetaForm") UploadAlbumMetaForm form,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model,
                             @ModelAttribute("albumDraft") AlbumDraft draft,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {

        UserEntity user = requireCurrentUser(authentication);
        if (user.getRole() == ru.ifmo.se.pisk.domain.UserRole.ARTIST) {
             form.setArtistName(user.getUsername());
             model.addAttribute("isArtist", true);
        }

        if (bindingResult.hasErrors()) {
            prepareMetaModel(model, request);
            return "upload";
        }

        try {
            String coverPath = null;
            MultipartFile cover = form.getCover();
            if (cover != null && !cover.isEmpty()) {

                fileStorageService.validateAvatar(cover.getOriginalFilename(), cover.getContentType(), cover.getSize());
                byte[] coverBytes = cover.getBytes();
                coverPath = fileStorageService.storeCover(cover.getOriginalFilename(), coverBytes);
            }

            GenreEntity genre = genreService.findOrCreateByName(form.getGenreName());
            LabelEntity label = labelService.findOrCreateByName(form.getLabelName());


            albumService.findOrCreateSimple(
                    form.getAlbumName(),
                    genre,
                    label,
                    form.isExplicit(),
                    form.isInoagent(),
                    form.getTrackCount(),
                    coverPath
            );

            draft.setArtistName(form.getArtistName());
            draft.setAlbumName(form.getAlbumName());
            draft.setGenreName(form.getGenreName());
            draft.setLabelName(form.getLabelName());
            draft.setExplicit(form.isExplicit());
            draft.setInoagent(form.isInoagent());
            draft.setTrackCount(form.getTrackCount());
            draft.setCoverProvided(coverPath != null);

            return "redirect:/upload/tracks";

        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            bindingResult.reject("error.albumMeta", e.getMessage());
            prepareMetaModel(model, request);
            return "upload";
        }
    }

    @GetMapping("/tracks")
    public String tracksForm(@SessionAttribute(value = "albumDraft", required = false) AlbumDraft draft,
                             Model model,
                             HttpServletRequest request) {
        if (draft == null || !StringUtils.hasText(draft.getAlbumName()) || draft.getTrackCount() <= 0) {
            return "redirect:/upload";
        }

        if (!model.containsAttribute("uploadTracksForm")) {
            UploadAlbumTracksForm form = new UploadAlbumTracksForm();
            List<UploadTrackItemForm> items = new ArrayList<>();
            for (int i = 0; i < draft.getTrackCount(); i++) {
                items.add(new UploadTrackItemForm());
            }
            form.setTracks(items);
            model.addAttribute("uploadTracksForm", form);
        }

        model.addAttribute("draft", draft);
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "upload_tracks";
    }

    @PostMapping("/tracks")
    public String submitTracks(@Valid @ModelAttribute("uploadTracksForm") UploadAlbumTracksForm form,
                               BindingResult bindingResult,
                               Authentication authentication,
                               @SessionAttribute("albumDraft") AlbumDraft draft,
                               SessionStatus sessionStatus,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               HttpServletRequest request) {

        UserEntity user = requireCurrentUser(authentication);

        if (draft == null || !StringUtils.hasText(draft.getAlbumName()) || draft.getTrackCount() <= 0) {
            return "redirect:/upload";
        }


        if (form.getTracks() == null || form.getTracks().size() != draft.getTrackCount()) {
            bindingResult.reject("error.tracks", "Неверное количество треков");
        } else {
            for (int i = 0; i < form.getTracks().size(); i++) {
                UploadTrackItemForm item = form.getTracks().get(i);
                if (!StringUtils.hasText(item.getName())) {
                    bindingResult.rejectValue("tracks[" + i + "].name", "error.trackName", "Введите название трека");
                }
                if (item.getFile() == null || item.getFile().isEmpty()) {
                    bindingResult.rejectValue("tracks[" + i + "].file", "error.trackFile", "Выберите файл");
                }
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("draft", draft);
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "upload_tracks";
        }

        try {
            Long firstTrackId = null;
            Long albumId = null;
            for (int i = 0; i < form.getTracks().size(); i++) {
                UploadTrackItemForm item = form.getTracks().get(i);
                MultipartFile file = item.getFile();

                fileStorageService.validateTrack(file.getOriginalFilename(), file.getContentType(), file.getSize());
                byte[] fileBytes = file.getBytes();

                AudioMetadata metadata = audioMetadataService.extract(
                        fileBytes,
                        file.getOriginalFilename(),
                        file.getContentType()
                );

                String title = StringUtils.hasText(item.getName())
                        ? item.getName().trim()
                        : (metadata.title() != null ? metadata.title() : file.getOriginalFilename());

                int numberOnAlbum = i + 1;


                byte[] coverData = (!draft.isCoverProvided() && i == 0) ? metadata.coverData() : null;
                String coverContentType = (!draft.isCoverProvided() && i == 0) ? metadata.coverContentType() : null;

                TrackUploadCommand command = new TrackUploadCommand(
                        title,
                        draft.getArtistName(),
                        draft.getGenreName(),
                        draft.getLabelName(),
                        draft.getAlbumName(),
                        metadata.durationSeconds(),
                        draft.isExplicit(),
                        draft.isInoagent(),
                        numberOnAlbum,
                        file.getContentType(),
                        fileBytes.length,
                        file.getOriginalFilename(),
                        () -> new ByteArrayInputStream(fileBytes),
                        coverData,
                        coverContentType
                );

                var info = trackService.uploadTrack(user.getId(), command);
                if (firstTrackId == null) {
                    firstTrackId = info.id();
                    albumId = info.albumId();
                }
            }


            sessionStatus.setComplete();

            redirectAttributes.addFlashAttribute("uploadSuccess", true);
            if (albumId != null) {
                return "redirect:/albums/" + albumId;
            }
            if (firstTrackId != null) {
                return "redirect:/tracks/" + firstTrackId;
            }
            return "redirect:/catalog";

        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            bindingResult.reject("error.upload", e.getMessage());
            model.addAttribute("draft", draft);
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "upload_tracks";
        }
    }

    private void prepareMetaModel(Model model, HttpServletRequest request) {
        model.addAttribute("genres", genreService.listAll());
        model.addAttribute("labels", labelService.listAll());
        model.addAttribute("langLinks", LanguageLinks.build(request));
    }

    private UserEntity requireCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }

    /** Данные шага 1, сохранённые в сессии. */
    public static class AlbumDraft {
        private String artistName;
        private String albumName;
        private String genreName;
        private String labelName;
        private boolean explicit;
        private boolean inoagent;
        private int trackCount;
        private boolean coverProvided;

        public String getArtistName() {
            return artistName;
        }

        public void setArtistName(String artistName) {
            this.artistName = artistName;
        }

        public String getAlbumName() {
            return albumName;
        }

        public void setAlbumName(String albumName) {
            this.albumName = albumName;
        }

        public String getGenreName() {
            return genreName;
        }

        public void setGenreName(String genreName) {
            this.genreName = genreName;
        }

        public String getLabelName() {
            return labelName;
        }

        public void setLabelName(String labelName) {
            this.labelName = labelName;
        }

        public boolean isExplicit() {
            return explicit;
        }

        public void setExplicit(boolean explicit) {
            this.explicit = explicit;
        }

        public boolean isInoagent() {
            return inoagent;
        }

        public void setInoagent(boolean inoagent) {
            this.inoagent = inoagent;
        }

        public int getTrackCount() {
            return trackCount;
        }

        public void setTrackCount(int trackCount) {
            this.trackCount = trackCount;
        }

        public boolean isCoverProvided() {
            return coverProvided;
        }

        public void setCoverProvided(boolean coverProvided) {
            this.coverProvided = coverProvided;
        }
    }
}
