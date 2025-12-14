package ru.ifmo.se.pisk.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
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
import ru.ifmo.se.pisk.repository.specification.TrackSpecifications;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;

@Service
public class TrackService {

    private final TrackRepository trackRepository;
    private final TrackStatusHistoryRepository trackStatusHistoryRepository;
    private final UserService userService;
    private final GenreService genreService;
    private final AlbumService albumService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final RatingRepository ratingRepository;
    private final RateLimitService rateLimitService;

    public TrackService(TrackRepository trackRepository,
                        TrackStatusHistoryRepository trackStatusHistoryRepository,
                        UserService userService,
                        GenreService genreService,
                        AlbumService albumService,
                        FileStorageService fileStorageService,
                        AuditService auditService,
                        RatingRepository ratingRepository,
                        RateLimitService rateLimitService) {
        this.trackRepository = trackRepository;
        this.trackStatusHistoryRepository = trackStatusHistoryRepository;
        this.userService = userService;
        this.genreService = genreService;
        this.albumService = albumService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.ratingRepository = ratingRepository;
        this.rateLimitService = rateLimitService;
    }


    @Transactional(readOnly = true)
    public TrackEntity requireTrack(Long trackId) {
        return trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Трек не найден: " + trackId));
    }

    @Transactional
    public TrackInfo changeStatus(Long trackId, TrackStatus status, Long moderatorId) {
        TrackEntity track = requireTrack(trackId);
        track.setStatus(status);
        trackRepository.save(track);
        return getTrackInfo(trackId);
    }

    @Transactional
    public TrackInfo uploadTrack(Long artistId, TrackUploadCommand command) {
        if (command.durationSeconds() <= 0) {
            throw new IllegalArgumentException("Длительность должна быть положительной");
        }
        rateLimitService.checkRateLimit("track-upload:" + artistId);
        fileStorageService.validateTrack(command.originalFilename(), command.contentType(), command.fileSize());
        UserEntity artist = userService.requireUser(artistId);
        GenreEntity genre = command.genreId() != null ? genreService.requireGenre(command.genreId()) : null;
        AlbumEntity album = command.albumId() != null ? albumService.requireAlbum(command.albumId()) : null;
        String storagePath = fileStorageService.storeTrack(artistId, command.originalFilename(), command.dataSupplier().get());
        TrackEntity entity = new TrackEntity();
        entity.setName(command.name());
        entity.setAuthor(artist);
        entity.setGenre(genre);
        entity.setAlbum(album);
        // Note: ArtistEntity is separate from UserEntity, setting to null for now
        entity.setArtist(null);
        entity.setStatus(TrackStatus.DRAFT);
        entity.setDurationSeconds(command.durationSeconds());
        entity.setFileSize(command.fileSize());
        entity.setStoragePath(storagePath);
        entity.setContentType(command.contentType());
        entity.setOriginalFilename(command.originalFilename());
        entity.setNumberOnAlbum(command.numberOnAlbum());
        entity.setExplicit(command.explicit());
        entity.setInoagent(command.inoagent());
        entity.setUploadedAt(java.time.Instant.now());
        TrackEntity saved = trackRepository.save(entity);
        TrackStatusHistoryEntity history = new TrackStatusHistoryEntity();
        history.setTrack(saved);
        history.setFromStatus(null);
        history.setToStatus(TrackStatus.DRAFT);
        history.setChangedAt(Instant.now());
        history.setActor(artist);
        trackStatusHistoryRepository.save(history);
        auditService.record(AuditAction.TRACK_UPLOADED, "Track", saved.getId().toString(), artist, Map.of());
        return DomainMapper.toTrackInfo(saved);
    }

    @Transactional
    public RatingInfo rateTrack(RatingCommand command) {
        TrackEntity track = requireTrack(command.trackId());
        UserEntity user = userService.requireUser(command.userId());
        RatingEntity rating = ratingRepository.findByTrackAndUser(track, user)
                .orElseGet(() -> {
                    RatingEntity entity = new RatingEntity();
                    entity.setTrack(track);
                    entity.setUser(user);
                    entity.setCreatedAt(Instant.now());
                    return entity;
                });
        rating.setValue(command.value());
        rating.setComment(command.comment());
        RatingEntity saved = ratingRepository.save(rating);
        double average = Optional.ofNullable(ratingRepository.calculateAverageForTrack(track)).orElse(0.0);
        long count = Optional.ofNullable(ratingRepository.countByTrack(track)).orElse(0L);
        track.setRateCount((int) count);
        track.setRating(Math.round(average * count));
        trackRepository.save(track);
        auditService.record(AuditAction.RATING_CREATED, "Track", track.getId().toString(), user, Map.of());
        return new RatingInfo(saved.getId(), track.getId(), user.getId(), saved.getValue(), saved.getComment(), saved.getCreatedAt());
    }

    @Transactional
    public TrackInfo incrementPlayCount(Long trackId) {
        TrackEntity track = requireTrack(trackId);
        track.setPlayCount(track.getPlayCount() + 1);
        trackRepository.save(track);
        return DomainMapper.toTrackInfo(track);
    }

    @Transactional(readOnly = true)
    public TrackInfo getTrackInfo(Long trackId) {
        TrackEntity track = requireTrack(trackId);
        return DomainMapper.toTrackInfo(track);
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

    @Transactional(readOnly = true)
    public Page<TrackInfo> findByAuthor(Long authorId, Pageable pageable) {
        UserEntity author = userService.requireUser(authorId);
        return trackRepository.findByAuthor(author, pageable).map(DomainMapper::toTrackInfo);
    }
}
