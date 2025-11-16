package ru.ifmo.se.pisk.service;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
        UserEntity artist = userService.requireUser(artistId);
        AlbumEntity album = command.albumId() != null ? albumService.requireAlbum(command.albumId()) : null;
        Set<GenreEntity> genres = command.genreIds() == null ? new HashSet<>() : command.genreIds().stream()
                .map(genreService::requireGenre)
                .collect(Collectors.toSet());
        String storagePath;
        try (InputStream inputStream = command.dataSupplier().get()) {
            storagePath = fileStorageService.storeTrack(artistId, command.originalFilename(), inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось сохранить файл трека", ex);
        }
        TrackEntity entity = new TrackEntity();
        entity.setArtist(artist);
        entity.setAlbum(album);
        entity.setGenres(genres);
        entity.setTitle(command.title());
        entity.setDurationSeconds(command.durationSeconds());
        entity.setStatus(TrackStatus.PENDING_REVIEW);
        entity.setStoragePath(storagePath);
        entity.setContentType(command.contentType());
        entity.setFileSize(command.fileSize());
        entity.setUploadedAt(Instant.now());
        TrackEntity saved = trackRepository.save(entity);
        TrackStatusHistoryEntity history = new TrackStatusHistoryEntity();
        history.setTrack(saved);
        history.setToStatus(saved.getStatus());
        history.setActor(artist);
        trackStatusHistoryRepository.save(history);
        auditService.record(AuditAction.TRACK_UPLOADED, "Track", saved.getId().toString(), artist,
                Map.of("status", saved.getStatus().name()));
        return DomainMapper.toTrackInfo(saved);
    }

    @Transactional
    public TrackInfo changeStatus(Long trackId, TrackStatus status, Long moderatorId) {
        TrackEntity entity = requireTrack(trackId);
        UserEntity moderator = userService.requireUser(moderatorId);
        TrackStatus previous = entity.getStatus();
        entity.setStatus(status);
        if (status == TrackStatus.PUBLISHED) {
            entity.setPublishedAt(Instant.now());
        }
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
                Map.of("value", Integer.toString(command.value())));
        return DomainMapper.toRatingInfo(saved);
    }

    private void recalculateTrackRating(TrackEntity track) {
        Double average = ratingRepository.calculateAverageForTrack(track);
        Long count = ratingRepository.countByTrack(track);
        if (average == null || count == null || count == 0) {
            track.setAverageRating(0);
            track.setRatingsCount(0);
            return;
        }
        track.setAverageRating(average);
        track.setRatingsCount(count);
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
    public Page<TrackInfo> findTracks(TrackStatus status, Pageable pageable) {
        if (status != null) {
            return trackRepository.findByStatus(status, pageable).map(DomainMapper::toTrackInfo);
        }
        return trackRepository.findAll(pageable).map(DomainMapper::toTrackInfo);
    }

    @Transactional(readOnly = true)
    public Page<TrackInfo> searchTracks(String title, Set<Long> genreIds, Pageable pageable) {
        Specification<TrackEntity> spec = Specification.where((root, query, builder) -> builder.conjunction());
        if (title != null && !title.isBlank()) {
            spec = spec.and((root, query, builder) -> builder.like(builder.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
        }
        if (genreIds != null && !genreIds.isEmpty()) {
            spec = spec.and((root, query, builder) -> {
                query.distinct(true);
                return root.join("genres").get("id").in(genreIds);
            });
        }
        return trackRepository.findAll(spec, pageable).map(DomainMapper::toTrackInfo);
    }
}
