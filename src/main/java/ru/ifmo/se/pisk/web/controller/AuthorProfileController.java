package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.view.TrackView;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping("/authors")
public class AuthorProfileController {

    private final UserService userService;
    private final TrackService trackService;
    private final GenreService genreService;

    public AuthorProfileController(UserService userService, TrackService trackService, GenreService genreService) {
        this.userService = userService;
        this.trackService = trackService;
        this.genreService = genreService;
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Long id,
                          @PageableDefault(size = 6) Pageable pageable,
                          Model model,
                          HttpServletRequest request) {
        try {
            UserEntity author = userService.requireUser(id);
            Page<TrackInfo> tracks = trackService.findByAuthor(id, pageable);
            Page<TrackView> view = tracks.map(info -> new TrackView(
                    info.id(),
                    info.name(),
                    info.authorId(),
                    author.getUsername(),
                    info.genreId() != null ? genreService.requireGenre(info.genreId()).getName() : "-",
                    info.explicit(),
                    info.inoagent(),
                    info.averageRating(),
                    info.durationSeconds(),
                    info.uploadDate()
            ));
            model.addAttribute("author", author);
            model.addAttribute("tracks", view);
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "artist";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}
