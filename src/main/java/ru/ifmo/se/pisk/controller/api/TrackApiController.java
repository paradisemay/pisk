package ru.ifmo.se.pisk.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.controller.dto.TrackUploadForm;
import ru.ifmo.se.pisk.model.Track;
import ru.ifmo.se.pisk.service.TrackService;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tracks")
@Tag(name = "Tracks", description = "REST API для каталога треков")
public class TrackApiController {

    private final TrackService trackService;

    public TrackApiController(TrackService trackService) {
        this.trackService = trackService;
    }

    @Operation(summary = "Каталог треков", description = "Поиск с фильтрами, сортировкой и пагинацией")
    @GetMapping
    public Page<Track> list(@RequestParam(required = false) String search,
                            @RequestParam(required = false) String genre,
                            @RequestParam(required = false) List<String> tags,
                            @RequestParam(defaultValue = "rating") String sort,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return trackService.findTracks(search, genre, normalizeTags(tags), sort, pageable);
    }

    @Operation(summary = "Детали трека")
    @GetMapping("/{id}")
    public ResponseEntity<Track> get(@PathVariable long id) {
        return trackService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Создать трек", description = "Простая заглушка загрузки трека")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TrackUploadForm form, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getFieldErrors().stream()
                    .collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage())));
        }
        Track track = Track.builder()
                .id(System.currentTimeMillis())
                .title(form.getTitle())
                .description(form.getDescription())
                .genre(form.getGenre())
                .audioUrl(form.getAudioUrl())
                .coverUrl(form.getCoverUrl())
                .tags(form.getTags())
                .duration(Duration.ofMinutes(3))
                .author(trackService.findById(1L).orElseThrow(() -> new IllegalArgumentException("Трек с ID 1 не найден")).getAuthor())
                .rating(5.0)
                .plays(0)
                .publishedAt(LocalDate.now())
                .build();
        Track saved = trackService.addTrack(track);
        return ResponseEntity.created(URI.create("/api/tracks/" + saved.getId())).body(saved);
    }

    private List<String> normalizeTags(List<String> raw) {
        if (raw == null) {
            return null;
        }
        return raw.stream()
                .flatMap(tag -> Stream.of(tag.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
