package ru.ifmo.se.pisk.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.controller.mapper.ApiMapper;
import ru.ifmo.se.pisk.controller.dto.TrackResponse;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.service.TrackService;

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
    public ResponseEntity<Page<TrackResponse>> byAuthor(@PathVariable Long authorId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TrackInfo> tracks = trackService.findByAuthor(authorId, pageable);
        if (tracks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tracks.map(ApiMapper::toTrackResponse));
    }
}
