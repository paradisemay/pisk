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
import ru.ifmo.se.pisk.repository.UserRepository;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.form.ForgotPasswordForm;
import ru.ifmo.se.pisk.web.form.RegisterUserForm;
import ru.ifmo.se.pisk.web.form.ResetPasswordForm;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
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
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Пароли не совпадают");
        }

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

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(Model model, HttpServletRequest request) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submitForgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes,
                                       HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "forgot-password";
        }

        return userRepository.findByUsernameIgnoreCaseAndEmailIgnoreCase(form.getUsername(), form.getEmail())
                .map(user -> {
                    request.getSession(true).setAttribute("passwordResetUserId", user.getId());
                    redirectAttributes.addFlashAttribute("resetAllowed", true);
                    return "redirect:/reset-password";
                })
                .orElseGet(() -> {
                    model.addAttribute("langLinks", LanguageLinks.build(request));
                    model.addAttribute("resetRequestError", "Пользователь с такими данными не найден");
                    return "forgot-password";
                });
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(Model model, HttpServletRequest request) {
        if (request.getSession(false) == null || request.getSession(false).getAttribute("passwordResetUserId") == null) {
            return "redirect:/forgot-password";
        }
        if (!model.containsAttribute("resetPasswordForm")) {
            model.addAttribute("resetPasswordForm", new ResetPasswordForm());
        }
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        Object userId = request.getSession(false) != null ? request.getSession(false).getAttribute("passwordResetUserId") : null;
        if (userId == null) {
            return "redirect:/forgot-password";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Пароли не совпадают");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "reset-password";
        }

        userService.updatePassword((Long) userId, form.getPassword());
        request.getSession(false).removeAttribute("passwordResetUserId");
        redirectAttributes.addFlashAttribute("passwordReset", true);
        return "redirect:/login";
    }
}
