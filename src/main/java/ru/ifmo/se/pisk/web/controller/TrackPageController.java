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

    public TrackPageController(TrackService trackService, UserService userService, GenreService genreService, ArtistService artistService) {
        this.trackService = trackService;
        this.userService = userService;
        this.genreService = genreService;
        this.artistService = artistService;
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
