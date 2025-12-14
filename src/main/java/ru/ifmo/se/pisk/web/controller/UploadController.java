package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.web.form.UploadTrackForm;

@Controller
@RequestMapping("/newupload")
public class UploadController {

    private final TrackService trackService;
    private final GenreService genreService;

    public UploadController(TrackService trackService, GenreService genreService) {
        this.trackService = trackService;
        this.genreService = genreService;
    }

    @GetMapping
    public String form(Model model, HttpServletRequest request) {
        if (!model.containsAttribute("uploadForm")) {
            model.addAttribute("uploadForm", new UploadTrackForm());
        }
        model.addAttribute("genres", genreService.listAll());
        model.addAttribute("langLinks", languageLinks(request));
        return "upload";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("uploadForm") UploadTrackForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", genreService.listAll());
            model.addAttribute("langLinks", languageLinks(request));
            return "upload";
        }

        Supplier<InputStream> dataSupplier = () -> {
            try {
                return form.getFile().getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось прочитать файл", e);
            }
        };
        TrackUploadCommand command = new TrackUploadCommand(
                form.getName(),
                form.getDurationSeconds(),
                form.getAlbumId(),
                form.getGenreId(),
                form.isExplicit(),
                form.isInoagent(),
                form.getNumberOnAlbum(),
                form.getFile().getContentType(),
                form.getFile().getSize(),
                form.getFile().getOriginalFilename(),
                dataSupplier
        );
        var info = trackService.uploadTrack(form.getArtistId(), command);
        redirectAttributes.addFlashAttribute("uploadSuccess", true);
        return "redirect:/tracks/" + info.id();
    }

    private Map<String, String> languageLinks(HttpServletRequest request) {
        String base = ServletUriComponentsBuilder.fromRequest(request).replaceQueryParam("lang").toUriString();
        Map<String, String> links = new HashMap<>();
        links.put("ru", ServletUriComponentsBuilder.fromUriString(base).replaceQueryParam("lang", "ru").toUriString());
        links.put("en", ServletUriComponentsBuilder.fromUriString(base).replaceQueryParam("lang", "en").toUriString());
        return links;
    }
}
