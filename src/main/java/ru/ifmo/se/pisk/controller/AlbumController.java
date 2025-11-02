package ru.ifmo.se.pisk.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.controller.dto.AlbumResponse;
import ru.ifmo.se.pisk.controller.dto.CreateAlbumRequest;
import ru.ifmo.se.pisk.controller.mapper.ApiMapper;
import ru.ifmo.se.pisk.service.AlbumService;

@RestController
@RequestMapping("/api/v1/albums")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @PostMapping
    public AlbumResponse create(@Valid @RequestBody CreateAlbumRequest request) {
        return ApiMapper.toAlbumResponse(
                albumService.createAlbum(
                        request.ownerId(),
                        request.title(),
                        request.description(),
                        request.releaseDate(),
                        request.genreIds(),
                        request.coverUrl()
                )
        );
    }
}
