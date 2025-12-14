package ru.ifmo.se.pisk.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.ifmo.se.pisk.repository.UserRepository;

@ControllerAdvice
public class CurrentUserAdvice {

    private final ObjectProvider<UserRepository> userRepositoryProvider;

    public CurrentUserAdvice(ObjectProvider<UserRepository> userRepositoryProvider) {
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @ModelAttribute("currentUser")
    public CurrentUser enrichModelWithUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        UserRepository userRepository = userRepositoryProvider.getIfAvailable();
        if (userRepository == null) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsernameIgnoreCase(username)
                .map(user -> new CurrentUser(user.getId(), user.getUsername(), null))
                .orElse(null);
    }
}
