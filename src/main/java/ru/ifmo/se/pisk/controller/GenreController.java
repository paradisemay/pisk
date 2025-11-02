package ru.ifmo.se.pisk.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.controller.dto.CreateGenreRequest;
import ru.ifmo.se.pisk.controller.dto.GenreResponse;
import ru.ifmo.se.pisk.controller.mapper.ApiMapper;
import ru.ifmo.se.pisk.service.GenreService;

@RestController
@RequestMapping("/api/v1/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @PostMapping
    public GenreResponse create(@Valid @RequestBody CreateGenreRequest request) {
        return ApiMapper.toGenreResponse(genreService.createGenre(request.name(), request.description()));
    }

    @GetMapping
    public List<GenreResponse> listAll() {
        return genreService.listAll().stream().map(ApiMapper::toGenreResponse).toList();
    }
}
