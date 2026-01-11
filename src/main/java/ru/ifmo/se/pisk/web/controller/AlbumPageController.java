package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.service.AlbumService;
import ru.ifmo.se.pisk.service.ArtistService;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.util.LanguageLinks;
import ru.ifmo.se.pisk.web.view.AlbumDetailsView;
import ru.ifmo.se.pisk.web.view.AlbumTrackView;

@Controller
@RequestMapping("/albums")
public class AlbumPageController {

    private final AlbumService albumService;
    private final TrackService trackService;
    private final UserService userService;
    private final GenreService genreService;
    private final FileStorageService fileStorageService;
    private final ArtistService artistService;

    public AlbumPageController(AlbumService albumService,
                               TrackService trackService,
                               UserService userService,
                               GenreService genreService,
                               FileStorageService fileStorageService,
                               ArtistService artistService) {
        this.albumService = albumService;
        this.trackService = trackService;
        this.userService = userService;
        this.genreService = genreService;
        this.fileStorageService = fileStorageService;
        this.artistService = artistService;
    }

    @GetMapping("/{id}")
    public String album(@PathVariable Long id, Model model, HttpServletRequest request) {
        try {
            AlbumEntity album = albumService.requireAlbum(id);

            List<TrackInfo> tracks = trackService.listAlbumTracks(id);

            // artist/uploader берём с первого трека (обычно #1)
            TrackInfo first = tracks.isEmpty() ? null : tracks.get(0);

            String uploaderName = "-";
            boolean uploaderIsArtist = false;
            if (first != null && first.authorId() != null) {
                ru.ifmo.se.pisk.domain.entity.UserEntity uploader = userService.requireUser(first.authorId());
                uploaderName = uploader.getUsername();
                uploaderIsArtist = uploader.getRole() == ru.ifmo.se.pisk.domain.UserRole.ARTIST;
            }

            String genreTitle = album.getGenre() != null
                    ? album.getGenre().getName()
                    : "-";

            String coverPublicUrl = fileStorageService.toPublicUrl(album.getCoverUrl());

            String resolvedArtistName = (first != null && first.artistId() != null)
                    ? artistService.requireArtist(first.artistId()).getName()
                    : "-";

            AlbumDetailsView view = new AlbumDetailsView(
                    album.getId(),
                    album.getName(),
                    album.getDescription(),
                    coverPublicUrl,
                    genreTitle,
                    album.getTrackNumber(),
                    album.isExplicit(),
                    album.isInoagent(),
                    album.getAverageRating(),
                    resolvedArtistName,
                    first != null ? first.authorId() : null,
                    uploaderName,
                    uploaderIsArtist,
                    album.getUploadDate()
            );

            List<AlbumTrackView> trackViews = tracks.stream()
                    .sorted(java.util.Comparator.comparingInt(TrackInfo::numberOnAlbum))
                    .map(t -> new AlbumTrackView(
                            t.id(),
                            t.numberOnAlbum(),
                            t.name(),
                            t.durationSeconds(),
                            t.averageRating(),
                            t.explicit(),
                            t.inoagent(),
                            t.uploadDate()
                    ))
                    .toList();

            model.addAttribute("album", view);
            model.addAttribute("tracks", trackViews);
            model.addAttribute("langLinks", LanguageLinks.build(request));
            return "album";

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}
