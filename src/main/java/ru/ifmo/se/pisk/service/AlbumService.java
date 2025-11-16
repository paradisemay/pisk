package ru.ifmo.se.pisk.service;

import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AlbumInfo;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.AlbumRepository;

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
        userService.requireUser(ownerId);
        AlbumEntity entity = new AlbumEntity();
        entity.setName(title);
        entity.setDescription(description);
        entity.setCoverUrl(coverUrl);
        if (releaseDate != null) {
            entity.setUploadDate(releaseDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
            entity.setPublicationDate(entity.getUploadDate());
        }
        if (genreIds != null && !genreIds.isEmpty()) {
            GenreEntity genre = genreService.requireGenre(genreIds.iterator().next());
            entity.setGenre(genre);
        }
        entity.setTrackNumber(0);
        entity.setAverageRating(0);
        entity.setExplicit(false);
        entity.setInoagent(false);
        return DomainMapper.toAlbumInfo(albumRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public AlbumEntity requireAlbum(Long id) {
        return albumRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Альбом не найден"));
    }
}
