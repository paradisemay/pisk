package ru.ifmo.se.pisk.service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.RatingInfo;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.RatingEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.TrackStatusHistoryEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.RatingRepository;
import ru.ifmo.se.pisk.repository.TrackRepository;
import ru.ifmo.se.pisk.repository.TrackStatusHistoryRepository;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
import ru.ifmo.se.pisk.repository.specification.TrackSpecifications;

@Service
public class TrackService {

    private final TrackRepository trackRepository;
    private final RatingRepository ratingRepository;
    private final UserService userService;
    private final AlbumService albumService;
    private final GenreService genreService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final RateLimitService rateLimitService;
    private final TrackStatusHistoryRepository trackStatusHistoryRepository;

    public TrackService(TrackRepository trackRepository,
                        RatingRepository ratingRepository,
                        UserService userService,
                        AlbumService albumService,
                        GenreService genreService,
                        FileStorageService fileStorageService,
                        AuditService auditService,
                        RateLimitService rateLimitService,
                        TrackStatusHistoryRepository trackStatusHistoryRepository) {
        this.trackRepository = trackRepository;
        this.ratingRepository = ratingRepository;
        this.userService = userService;
        this.albumService = albumService;
        this.genreService = genreService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.rateLimitService = rateLimitService;
        this.trackStatusHistoryRepository = trackStatusHistoryRepository;
    }

    @Transactional
    public TrackInfo uploadTrack(Long artistId, TrackUploadCommand command) {
        rateLimitService.checkRateLimit("track-upload:" + artistId);
        if (command.durationSeconds() <= 0) {
            throw new IllegalArgumentException("Продолжительность должна быть положительной");
        }
        if (command.fileSize() <= 0) {
            throw new IllegalArgumentException("Размер файла должен быть положительным");
        }
        if (command.genreId() == null) {
            throw new IllegalArgumentException("Жанр обязателен");
        }
        if (command.albumId() == null) {
            throw new IllegalArgumentException("Альбом обязателен");
        }
        if (command.numberOnAlbum() < 1) {
            throw new IllegalArgumentException("Номер трека в альбоме должен быть положительным");
        }
        UserEntity artist = userService.requireUser(artistId);
        AlbumEntity album = albumService.requireAlbum(command.albumId());
        if (command.numberOnAlbum() > album.getTrackNumber()) {
            throw new IllegalArgumentException("Номер трека превышает размер альбома");
        }
        GenreEntity genre = genreService.requireGenre(command.genreId());
        String storagePath;
        try (InputStream inputStream = command.dataSupplier().get()) {
            storagePath = fileStorageService.storeTrack(artistId, command.originalFilename(), inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось сохранить файл трека", ex);
        }
        TrackEntity entity = new TrackEntity();
        entity.setAuthor(artist);
        entity.setAlbum(album);
        entity.setGenre(genre);
        entity.setName(command.name());
        entity.setDurationSeconds(command.durationSeconds());
        entity.setStoragePath(storagePath);
        entity.setFileSize(command.fileSize());
        entity.setUploadedAt(Instant.now());
        entity.setNumberOnAlbum(command.numberOnAlbum());
        entity.setExplicit(command.explicit());
        entity.setInoagent(command.inoagent());
        TrackEntity saved = trackRepository.save(entity);
        album.setTrackNumber(Math.max(album.getTrackNumber(), saved.getNumberOnAlbum()));
        if (saved.isExplicit()) {
            album.setExplicit(true);
        }
        if (saved.isInoagent()) {
            album.setInoagent(true);
        }
        TrackStatusHistoryEntity history = new TrackStatusHistoryEntity();
        history.setTrack(saved);
        history.setToStatus(saved.getStatus());
        history.setActor(artist);
        trackStatusHistoryRepository.save(history);
        auditService.record(AuditAction.TRACK_UPLOADED, "Track", saved.getId().toString(), artist,
                Map.of(
                        "genre_id", genre.getId().toString(),
                        "explicit", Boolean.toString(saved.isExplicit()),
                        "inoagent", Boolean.toString(saved.isInoagent()),
                        "number_on_album", Integer.toString(saved.getNumberOnAlbum())
                ));
        return DomainMapper.toTrackInfo(saved);
    }

    @Transactional
    public TrackInfo changeStatus(Long trackId, TrackStatus status, Long moderatorId) {
        TrackEntity entity = requireTrack(trackId);
        UserEntity moderator = userService.requireUser(moderatorId);
        TrackStatus previous = entity.getStatus();
        entity.setStatus(status);
        TrackStatusHistoryEntity history = new TrackStatusHistoryEntity();
        history.setTrack(entity);
        history.setFromStatus(previous);
        history.setToStatus(status);
        history.setActor(moderator);
        trackStatusHistoryRepository.save(history);
        auditService.record(AuditAction.TRACK_STATUS_CHANGED, "Track", trackId.toString(), moderator,
                Map.of("status", status.name()));
        return DomainMapper.toTrackInfo(entity);
    }

    @Transactional
    public RatingInfo rateTrack(RatingCommand command) {
        if (command.value() < 1 || command.value() > 5) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 5");
        }
        TrackEntity track = requireTrack(command.trackId());
        UserEntity user = userService.requireUser(command.userId());
        RatingEntity entity = ratingRepository.findByTrackAndUser(track, user)
                .orElseGet(() -> {
                    RatingEntity ratingEntity = new RatingEntity();
                    ratingEntity.setTrack(track);
                    ratingEntity.setUser(user);
                    return ratingEntity;
                });
        entity.setValue(command.value());
        entity.setComment(command.comment());
        RatingEntity saved = ratingRepository.save(entity);
        recalculateTrackRating(track);
        auditService.record(AuditAction.RATING_CREATED, "Track", track.getId().toString(), user,
                Map.of(
                        "value", Integer.toString(command.value()),
                        "rate_count", Integer.toString(track.getRateCount()),
                        "rating_cached", Long.toString(track.getRating())
                ));
        return DomainMapper.toRatingInfo(saved);
    }

    private void recalculateTrackRating(TrackEntity track) {
        Double average = ratingRepository.calculateAverageForTrack(track);
        Long count = ratingRepository.countByTrack(track);
        if (average == null || count == null || count == 0) {
            track.setRating(0);
            track.setRateCount(0);
            return;
        }
        track.setRating(Math.round(average * count));
        track.setRateCount(count.intValue());
        if (track.getAlbum() != null) {
            updateAlbumRating(track.getAlbum());
        }
    }

    private void updateAlbumRating(AlbumEntity album) {
        var tracks = trackRepository.findByAlbum(album);
        long totalRating = 0;
        long totalRates = 0;
        for (TrackEntity trackEntity : tracks) {
            totalRating += trackEntity.getRating();
            totalRates += trackEntity.getRateCount();
        }
        double average = totalRates == 0 ? 0 : ((double) totalRating) / totalRates;
        album.setAverageRating(average);
    }

    @Transactional
    public TrackInfo incrementPlayCount(Long trackId) {
        TrackEntity entity = requireTrack(trackId);
        entity.setPlayCount(entity.getPlayCount() + 1);
        return DomainMapper.toTrackInfo(entity);
    }

    @Transactional(readOnly = true)
    public TrackEntity requireTrack(Long trackId) {
        return trackRepository.findById(trackId).orElseThrow(() -> new IllegalArgumentException("Трек не найден"));
    }

    @Transactional(readOnly = true)
    public TrackInfo getTrackInfo(Long trackId) {
        return DomainMapper.toTrackInfo(requireTrack(trackId));
    }

    @Transactional(readOnly = true)
    public Page<TrackInfo> searchTracks(TrackSearchCriteria criteria, Pageable pageable) {
        Specification<TrackEntity> spec = Specification.where(TrackSpecifications.fullTextSearch(criteria.query()))
                .and(TrackSpecifications.hasGenres(criteria.genreIds()))
                .and(TrackSpecifications.hasAuthor(criteria.authorId()))
                .and(TrackSpecifications.hasStatus(criteria.status()))
                .and(TrackSpecifications.isExplicit(criteria.explicit()))
                .and(TrackSpecifications.isInoagent(criteria.inoagent()));
        return trackRepository.findAll(spec, pageable).map(DomainMapper::toTrackInfo);
    }
}
