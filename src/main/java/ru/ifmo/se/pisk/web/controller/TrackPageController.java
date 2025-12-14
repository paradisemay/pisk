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

    public TrackPageController(TrackService trackService, UserService userService, GenreService genreService) {
        this.trackService = trackService;
        this.userService = userService;
        this.genreService = genreService;
    }

    @GetMapping("/{id}")
    public String track(@PathVariable Long id, Model model, HttpServletRequest request) {
        try {
            TrackInfo info = trackService.getTrackInfo(id);
            String authorName = info.authorId() != null ? userService.requireUser(info.authorId()).getUsername() : "-";
            String genreTitle = info.genreId() != null ? genreService.requireGenre(info.genreId()).getName() : "-";
            String coverUrl = info.coverPath() != null ? "/api/v1/tracks/" + info.id() + "/cover" : null;
            TrackDetailsView view = new TrackDetailsView(
                    info.id(),
                    info.name(),
                    info.authorId(),
                    authorName,
                    genreTitle,
                    coverUrl,
                    info.explicit(),
                    info.inoagent(),
                    info.averageRating(),
                    info.rateCount(),
                    info.durationSeconds(),
                    info.filesize(),
                    info.uploadDate(),
                    "/api/v1/tracks/" + info.id() + "/file"
            );
            model.addAttribute("track", view);
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "track";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}
