package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ifmo.se.pisk.domain.UserRole;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.form.RegisterUserForm;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model,
                            HttpServletRequest request) {
        model.addAttribute("langLinks", LanguageLinks.build(request));
        model.addAttribute("loginError", error != null);
        model.addAttribute("logoutSuccess", logout != null);
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model, HttpServletRequest request) {
        if (!model.containsAttribute("registerForm")) {
            RegisterUserForm form = new RegisterUserForm();
            form.setRole(UserRole.USER);
            model.addAttribute("registerForm", form);
        }
        model.addAttribute("roles", List.of(UserRole.USER, UserRole.ARTIST));
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterUserForm form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", List.of(UserRole.USER, UserRole.ARTIST));
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "register";
        }
        try {
            userService.registerUser(form.getUsername(), form.getEmail(), form.getPassword(), form.getRole());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("roles", List.of(UserRole.USER, UserRole.ARTIST));
            model.addAttribute("langLinks", LanguageLinks.build(request));
            model.addAttribute("registrationError", ex.getMessage());
            return "register";
        }
        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}
