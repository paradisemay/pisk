package ru.ifmo.se.pisk.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.model.Track;
import ru.ifmo.se.pisk.service.TrackService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/authors")
@Tag(name = "Authors", description = "Профили авторов и их треки")
public class AuthorApiController {

    private final TrackService trackService;

    public AuthorApiController(TrackService trackService) {
        this.trackService = trackService;
    }

    @Operation(summary = "Треки автора")
    @GetMapping("/{authorId}/tracks")
    public ResponseEntity<List<Track>> byAuthor(@PathVariable long authorId) {
        List<Track> tracks = trackService.findByAuthor(authorId);
        if (tracks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tracks);
    }
}
