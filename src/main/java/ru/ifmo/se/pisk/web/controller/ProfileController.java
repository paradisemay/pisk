package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.repository.UserRepository;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.form.AvatarUploadForm;
import ru.ifmo.se.pisk.web.util.LanguageLinks;
import ru.ifmo.se.pisk.web.view.ProfileView;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public ProfileController(UserRepository userRepository, UserService userService, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String profile(Model model, Authentication authentication, HttpServletRequest request) {
        UserEntity user = requireCurrentUser(authentication);
        prepareModel(model, user, request);
        return "profile";
    }

    @PostMapping
    public String uploadAvatar(@Valid @ModelAttribute("avatarForm") AvatarUploadForm form,
                               BindingResult bindingResult,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               HttpServletRequest request) {
        UserEntity user = requireCurrentUser(authentication);
        if (form.getFile() == null || form.getFile().isEmpty()) {
            bindingResult.rejectValue("file", "account.validation.file", "Выберите файл для загрузки");
        }
        if (bindingResult.hasErrors()) {
            prepareModel(model, user, request);
            return "profile";
        }
        Supplier<InputStream> supplier = () -> {
            try {
                return form.getFile().getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось прочитать файл", e);
            }
        };
        try {
            String path = userService.updateAvatar(
                    user.getId(),
                    form.getFile().getOriginalFilename(),
                    form.getFile().getContentType(),
                    form.getFile().getSize(),
                    supplier
            );
            redirectAttributes.addFlashAttribute("avatarUpdated", true);
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("account.validation.error", ex.getMessage());
            prepareModel(model, user, request);
            return "profile";
        }
    }

    private void prepareModel(Model model, UserEntity user, HttpServletRequest request) {
        String avatarUrl = fileStorageService.toPublicUrl(user.getAvatarPath());
        model.addAttribute("profile", new ProfileView(user.getId(), user.getUsername(), user.getEmail(), avatarUrl));
        if (!model.containsAttribute("avatarForm")) {
            model.addAttribute("avatarForm", new AvatarUploadForm());
        }
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
