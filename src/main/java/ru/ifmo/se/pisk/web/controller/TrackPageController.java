package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.service.ArtistService;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.view.TrackDetailsView;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping("/tracks")
public class TrackPageController {

    private final TrackService trackService;
    private final UserService userService;
    private final GenreService genreService;
    private final ArtistService artistService;
    private final FileStorageService fileStorageService;

    public TrackPageController(TrackService trackService, UserService userService, GenreService genreService, ArtistService artistService, FileStorageService fileStorageService) {
        this.trackService = trackService;
        this.userService = userService;
        this.genreService = genreService;
        this.artistService = artistService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{id}")
    public String track(@PathVariable Long id, Model model, HttpServletRequest request) {
        try {
            TrackInfo info = trackService.getTrackInfo(id);
            String authorName = "-";
            boolean uploaderIsArtist = false;
            if (info.authorId() != null) {
                ru.ifmo.se.pisk.domain.entity.UserEntity author = userService.requireUser(info.authorId());
                authorName = author.getUsername();
                uploaderIsArtist = author.getRole() == ru.ifmo.se.pisk.domain.UserRole.ARTIST;
            }
            String genreTitle = info.genreId() != null ? genreService.requireGenre(info.genreId()).getName() : "-";
            String artistName = info.artistId() != null ? artistService.requireArtist(info.artistId()).getName() : "-";

            Integer currentUserRating = null;
            boolean isLiked = false;
            ru.ifmo.se.pisk.domain.entity.UserEntity currentUser = getCurrentUser();
            if (currentUser != null) {
                 currentUserRating = trackService.getUserTrackRating(id, currentUser.getId()).orElse(null);
                 isLiked = trackService.isLiked(id, currentUser.getId());
            }

            String coverPublicUrl = info.coverUrl() != null ? fileStorageService.toPublicUrl(info.coverUrl()) : null;
            TrackDetailsView view = new TrackDetailsView(
                    info.id(),
                    info.name(),
                    info.authorId(),
                    authorName,
                    uploaderIsArtist,
                    artistName,
                    genreTitle,
                    info.explicit(),
                    info.inoagent(),
                    info.averageRating(),
                    info.rateCount(),
                    info.durationSeconds(),
                    info.filesize(),
                    info.uploadDate(),
                    currentUserRating,
                    coverPublicUrl,
                    "/api/v1/tracks/" + info.id() + "/file",
                    isLiked
            );
            model.addAttribute("track", view);
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "track";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/rate")
    public String rateTrack(@PathVariable Long id, @org.springframework.web.bind.annotation.RequestParam("rating") Integer rating) {
        ru.ifmo.se.pisk.domain.entity.UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        trackService.rateTrack(new ru.ifmo.se.pisk.service.dto.RatingCommand(id, currentUser.getId(), rating, null));
        return "redirect:/tracks/" + id;
    }

    private ru.ifmo.se.pisk.domain.entity.UserEntity getCurrentUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
             return userService.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }
}
