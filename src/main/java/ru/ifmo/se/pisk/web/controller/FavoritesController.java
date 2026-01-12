package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.util.LanguageLinks;
import ru.ifmo.se.pisk.web.view.TrackView;

@Controller
@RequestMapping("/favorites")
public class FavoritesController {

    private final TrackService trackService;
    private final UserService userService;

    public FavoritesController(TrackService trackService, UserService userService) {
        this.trackService = trackService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String favorites(@RequestParam(defaultValue = "0") int page,
                            Model model,
                            HttpServletRequest request) {
        ru.ifmo.se.pisk.domain.entity.UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());
        Page<TrackView> tracks = trackService.getLikedTracksView(currentUser.getId(), pageable);

        model.addAttribute("tracks", tracks);
        model.addAttribute("langLinks", LanguageLinks.build(request));


        return "favorites";
    }

    private ru.ifmo.se.pisk.domain.entity.UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
             return userService.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }
}
