package ru.ifmo.se.pisk.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AlbumInfo;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.AlbumRepository;
import ru.ifmo.se.pisk.repository.LabelRepository;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final UserService userService;
    private final GenreService genreService;
    private final LabelRepository labelRepository;

    public AlbumService(AlbumRepository albumRepository, UserService userService, GenreService genreService,
                        LabelRepository labelRepository) {
        this.albumRepository = albumRepository;
        this.userService = userService;
        this.genreService = genreService;
        this.labelRepository = labelRepository;
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
}
