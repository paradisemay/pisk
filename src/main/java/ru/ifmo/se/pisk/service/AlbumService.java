package ru.ifmo.se.pisk.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AlbumInfo;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.AlbumRepository;
import ru.ifmo.se.pisk.repository.entity.AlbumEntity;
import ru.ifmo.se.pisk.repository.entity.GenreEntity;
import ru.ifmo.se.pisk.repository.entity.UserEntity;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final UserService userService;
    private final GenreService genreService;

    public AlbumService(AlbumRepository albumRepository, UserService userService, GenreService genreService) {
        this.albumRepository = albumRepository;
        this.userService = userService;
        this.genreService = genreService;
    }

    @Transactional
    public AlbumInfo createAlbum(Long ownerId, String title, String description, LocalDate releaseDate, Set<Long> genreIds, String coverUrl) {
        UserEntity owner = userService.requireUser(ownerId);
        AlbumEntity entity = new AlbumEntity();
        entity.setOwner(owner);
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setReleaseDate(releaseDate);
        if (genreIds != null && !genreIds.isEmpty()) {
            Set<GenreEntity> genres = genreIds.stream().map(genreService::requireGenre).collect(Collectors.toSet());
            entity.setGenres(genres);
        }
        entity.setCoverUrl(coverUrl);
        return DomainMapper.toAlbumInfo(albumRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public AlbumEntity requireAlbum(Long id) {
        return albumRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Альбом не найден"));
    }
}
