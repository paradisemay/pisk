package ru.ifmo.se.pisk.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.ifmo.se.pisk.service.SystemSettingsService;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final SystemSettingsService settingsService;

    public AdminSettingsController(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("threshold", settingsService.getDeletionThreshold());
        return "admin/settings";
    }

    @PostMapping
    public String updateSettings(@RequestParam("threshold") double threshold) {
        settingsService.updateDeletionThreshold(threshold);
        return "redirect:/admin/settings?success";
    }
}
