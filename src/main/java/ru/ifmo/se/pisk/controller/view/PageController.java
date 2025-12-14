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
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.TrackService;

@Controller
@RequestMapping
public class PageController {

    private final TrackService trackService;
    private final GenreService genreService;

    public PageController(TrackService trackService, GenreService genreService) {
        this.trackService = trackService;
        this.genreService = genreService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/catalog";
    }


    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("trackUploadForm", new TrackUploadForm());
        model.addAttribute("genres", genreService.listAll().stream()
                .map(g -> g.title())
                .toList());
        return "upload";
    }

    @PostMapping("/upload")
    public String upload(@Valid @ModelAttribute TrackUploadForm trackUploadForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", genreService.listAll().stream()
                    .map(g -> g.title())
                    .toList());
            return "upload";
        }
        // Redirect to the proper upload controller that uses the repository
        return "redirect:/newupload";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("settings", new AdminSettingsForm());
        model.addAttribute("genres", genreService.listAll());
        return "admin";
    }

    @PostMapping("/admin")
    public String saveAdmin(@Valid @ModelAttribute("settings") AdminSettingsForm form,
                            BindingResult bindingResult,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", genreService.listAll());
            return "admin";
        }
        model.addAttribute("saved", true);
        model.addAttribute("genres", genreService.listAll());
        return "admin";
    }
}
