package ru.ifmo.se.pisk.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.domain.Album;
import ru.ifmo.se.pisk.domain.Artist;
import ru.ifmo.se.pisk.domain.Genre;
import ru.ifmo.se.pisk.domain.Label;
import ru.ifmo.se.pisk.service.AdminService;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/genre")
    public Genre createGenre(@Valid @RequestBody GenreRequest request) {
        return adminService.createGenre(request.title(), request.description(), request.color());
    }

    @PostMapping("/artist")
    public Artist createArtist(@Valid @RequestBody SimpleRequest request) {
        return adminService.createArtist(request.name(), request.description());
    }

    @PostMapping("/label")
    public Label createLabel(@Valid @RequestBody SimpleRequest request) {
        return adminService.createLabel(request.name(), request.description());
    }

    @PostMapping("/album")
    public Album createAlbum(@Valid @RequestBody AlbumRequest request) {
        return adminService.createAlbum(request.name(), request.description(), request.genreId(), request.trackNumber(),
                request.explicit(), request.inoagent(), request.labelId(), request.publicationDate());
    }

    public record GenreRequest(@NotBlank String title, String description, String color) { }
    public record SimpleRequest(@NotBlank String name, String description) { }
    public record AlbumRequest(@NotBlank String name, @NotBlank String description, Long genreId, int trackNumber,
                               boolean explicit, boolean inoagent, Long labelId, Instant publicationDate) { }
}
