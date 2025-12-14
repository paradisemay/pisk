package ru.ifmo.se.pisk.controller.view;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.ifmo.se.pisk.controller.dto.AdminSettingsForm;
import ru.ifmo.se.pisk.controller.dto.TrackUploadForm;
import ru.ifmo.se.pisk.model.Track;
import ru.ifmo.se.pisk.service.TrackService;

@Controller
@RequestMapping
public class PageController {

    private final TrackService trackService;

    public PageController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/catalog";
    }


    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("trackUploadForm", new TrackUploadForm());
        model.addAttribute("genres", trackService.groupByGenre().keySet());
        return "upload";
    }

    @PostMapping("/upload")
    public String upload(@Valid @ModelAttribute TrackUploadForm trackUploadForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", trackService.groupByGenre().keySet());
            return "upload";
        }
        trackService.addTrack(
                Track.builder()
                        .id(System.currentTimeMillis())
                        .title(trackUploadForm.getTitle())
                        .description(trackUploadForm.getDescription())
                        .genre(trackUploadForm.getGenre())
                        .audioUrl(trackUploadForm.getAudioUrl())
                        .coverUrl(trackUploadForm.getCoverUrl())
                        .tags(trackUploadForm.getTags())
                        .author(trackService.findById(1L).orElseThrow(() -> new IllegalArgumentException("Трек с ID 1 не найден")).getAuthor())
                        .rating(5.0)
                        .plays(0)
                        .publishedAt(java.time.LocalDate.now())
                        .duration(java.time.Duration.ofMinutes(3))
                        .build()
        );
        return "redirect:/catalog?sort=date";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("settings", new AdminSettingsForm());
        model.addAttribute("genres", trackService.groupByGenre());
        return "admin";
    }

    @PostMapping("/admin")
    public String saveAdmin(@Valid @ModelAttribute("settings") AdminSettingsForm form,
                            BindingResult bindingResult,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", trackService.groupByGenre());
            return "admin";
        }
        model.addAttribute("saved", true);
        model.addAttribute("genres", trackService.groupByGenre());
        return "admin";
    }
}
