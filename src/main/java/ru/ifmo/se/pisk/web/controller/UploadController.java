package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.repository.UserRepository;
import ru.ifmo.se.pisk.service.AudioMetadataService;
import ru.ifmo.se.pisk.service.AudioMetadataService.AudioMetadata;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.LabelService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.web.form.UploadTrackForm;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping({"/upload", "/newupload"})
public class UploadController {

    private final TrackService trackService;
    private final GenreService genreService;
    private final LabelService labelService;
    private final AudioMetadataService audioMetadataService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public UploadController(TrackService trackService,
                            GenreService genreService,
                            LabelService labelService,
                            AudioMetadataService audioMetadataService,
                            FileStorageService fileStorageService,
                            UserRepository userRepository) {
        this.trackService = trackService;
        this.genreService = genreService;
        this.labelService = labelService;
        this.audioMetadataService = audioMetadataService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String form(Model model, HttpServletRequest request, Authentication authentication) {
        if (!model.containsAttribute("uploadForm")) {
            UploadTrackForm form = new UploadTrackForm();
            if (authentication != null && authentication.isAuthenticated()) {
                userRepository.findByUsernameIgnoreCase(authentication.getName()).ifPresent(user -> {
                    if (user.getRole() == ru.ifmo.se.pisk.domain.UserRole.ARTIST) {
                        form.setArtistName(user.getUsername());
                    }
                });
            }
            model.addAttribute("uploadForm", form);
        }
        model.addAttribute("genres", genreService.listAll());
        model.addAttribute("labels", labelService.listAll());
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "upload";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("uploadForm") UploadTrackForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {

        UserEntity user = requireCurrentUser(authentication);
        System.out.println("user " + user.getUsername() + " id is " + user.getId().toString());

        if (bindingResult.hasErrors()) {
            prepareModel(model, request);
            return "upload";
        }

        if (form.getFile() == null || form.getFile().isEmpty()) {
            bindingResult.rejectValue("file", "error.file", "Выберите файл");
            prepareModel(model, request);
            return "upload";
        }

        try {
            fileStorageService.validate(form.getFile().getOriginalFilename(),
                    form.getFile().getContentType(),
                    form.getFile().getSize());

            byte[] fileBytes;
            try {
                fileBytes = form.getFile().getBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось прочитать файл", e);
            }

            AudioMetadata metadata = audioMetadataService.extract(fileBytes,
                    form.getFile().getOriginalFilename(),
                    form.getFile().getContentType());

            String title = StringUtils.hasText(form.getName())
                    ? form.getName()
                    : (metadata.title() != null ? metadata.title() : form.getFile().getOriginalFilename());

            int trackNumber = metadata.trackNumber() != null ? metadata.trackNumber() : 1;

            TrackUploadCommand command = new TrackUploadCommand(
                    title,
                    form.getArtistName(),
                    form.getGenreName(),
                    form.getLabelName(),
                    form.getAlbumName(),
                    metadata.durationSeconds(),
                    form.isExplicit(),
                    form.isInoagent(),
                    trackNumber,
                    form.getFile().getContentType(),
                    fileBytes.length,
                    form.getFile().getOriginalFilename(),
                    () -> new ByteArrayInputStream(fileBytes),
                    metadata.coverData(),
                    metadata.coverContentType()
            );

            var info = trackService.uploadTrack(user.getId(), command);

            redirectAttributes.addFlashAttribute("uploadSuccess", true);
            return "redirect:/tracks/" + info.id();

        } catch (IllegalArgumentException | IllegalStateException e) {
            bindingResult.rejectValue("file", "error.file", e.getMessage());
            prepareModel(model, request);
            return "upload";
        }
    }

    private void prepareModel(Model model, HttpServletRequest request) {
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
}
