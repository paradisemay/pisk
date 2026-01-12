package ru.ifmo.se.pisk.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.ifmo.se.pisk.domain.AlbumInfo;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.AlbumRepository;
import ru.ifmo.se.pisk.repository.LabelRepository;
import ru.ifmo.se.pisk.repository.projection.AlbumCardAggregate;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final UserService userService;
    private final GenreService genreService;
    private final LabelRepository labelRepository;
    private final FileStorageService fileStorageService;

    public AlbumService(AlbumRepository albumRepository, UserService userService, GenreService genreService,
                        LabelRepository labelRepository, FileStorageService fileStorageService) {
        this.albumRepository = albumRepository;
        this.userService = userService;
        this.genreService = genreService;
        this.labelRepository = labelRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public AlbumEntity findOrCreateSimple(String title, GenreEntity genre, LabelEntity label, boolean explicit, boolean inoagent,
                                          int trackNumber, String coverPath) {
        if (!org.springframework.util.StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Название альбома обязательно");
        }
        AlbumEntity existing = albumRepository.findByNameIgnoreCase(title.trim()).orElse(null);
        if (existing != null) {
            if (existing.getCoverUrl() == null && coverPath != null) {
                existing.setCoverUrl(coverPath);
                albumRepository.save(existing);
            }
            return existing;
        }

        AlbumEntity entity = new AlbumEntity();
        entity.setName(title.trim());
        entity.setDescription("Добавлен автоматически при загрузке трека");
        entity.setGenre(genre);
        entity.setLabel(label);
        entity.setTrackNumber(Math.max(trackNumber, 1));
        entity.setExplicit(explicit);
        entity.setInoagent(inoagent);
        entity.setCoverUrl(coverPath);
        entity.setAverageRating(0);
        entity.setPublicationDate(entity.getUploadDate());
        return albumRepository.save(entity);
    }

    @Transactional
    public AlbumInfo createAlbum(Long ownerId, String title, String description, LocalDate releaseDate, Long genreId,
                                 Long labelId, int trackNumber, boolean explicit, boolean inoagent, String coverUrl) {
        userService.requireUser(ownerId);
        if (releaseDate == null) {
            throw new IllegalArgumentException("Дата релиза обязательна");
        }
        if (genreId == null) {
            throw new IllegalArgumentException("Жанр обязателен");
        }
        if (labelId == null) {
            throw new IllegalArgumentException("Лейбл обязателен");
        }
        if (trackNumber < 1) {
            throw new IllegalArgumentException("Число треков должно быть положительным");
        }
        AlbumEntity entity = new AlbumEntity();
        entity.setName(title);
        entity.setDescription(description);
        entity.setCoverUrl(coverUrl);
        entity.setUploadDate(releaseDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        entity.setPublicationDate(entity.getUploadDate());
        GenreEntity genre = genreService.requireGenre(genreId);
        entity.setGenre(genre);
        LabelEntity label = labelRepository.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("Лейбл не найден"));
        entity.setLabel(label);
        entity.setTrackNumber(trackNumber);
        entity.setAverageRating(0);
        entity.setExplicit(explicit);
        entity.setInoagent(inoagent);
        return DomainMapper.toAlbumInfo(albumRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public AlbumEntity requireAlbum(Long id) {
        return albumRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Альбом не найден"));
    }

    @Transactional(readOnly = true)
    public Page<AlbumCardAggregate> searchAlbumCards(String title,
                                                     java.util.Set<Long> genreIds,
                                                     Boolean explicit,
                                                     Boolean inoagent,
                                                     Pageable pageable) {
        boolean genreIdsEmpty = genreIds == null || genreIds.isEmpty();
        return albumRepository.searchAlbumCards(title, explicit, inoagent,
                genreIdsEmpty ? java.util.Set.of(-1L) : genreIds,
                genreIdsEmpty,
                pageable);
    }

    @Transactional
    public void deleteIfEmpty(Long albumId) {
        if (albumId == null) return;
        AlbumEntity album = albumRepository.findById(albumId).orElse(null);
        if (album == null) return;








        int trackCount = albumRepository.countTracksInAlbum(albumId);
        if (trackCount == 0) {
            if (album.getCoverUrl() != null) {
                fileStorageService.delete(album.getCoverUrl());
            }
            albumRepository.delete(album);
        }
    }
}
