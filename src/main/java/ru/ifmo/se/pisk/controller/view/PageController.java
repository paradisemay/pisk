package ru.ifmo.se.pisk.controller.view;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.ifmo.se.pisk.controller.dto.AdminSettingsForm;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping
public class PageController {

    private final GenreService genreService;

    public PageController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping("/admin")
    public String admin(Model model, HttpServletRequest request) {
        model.addAttribute("settings", new AdminSettingsForm());
        model.addAttribute("genres", genreService.listAll());
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "admin";
    }

    @PostMapping("/admin")
    public String saveAdmin(@Valid @ModelAttribute("settings") AdminSettingsForm form,
                            BindingResult bindingResult,
                            Model model,
                            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", genreService.listAll());
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "admin";
        }
        model.addAttribute("saved", true);
        model.addAttribute("genres", genreService.listAll());
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "admin";
    }
}
