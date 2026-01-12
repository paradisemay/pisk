package ru.ifmo.se.pisk.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.model.Author;
import ru.ifmo.se.pisk.model.Track;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.entity.TrackStatusHistoryEntity;
import ru.ifmo.se.pisk.domain.RatingInfo;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.ArtistEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.RatingEntity;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
import ru.ifmo.se.pisk.service.AuditService;
import ru.ifmo.se.pisk.repository.TrackRepository;
import ru.ifmo.se.pisk.repository.TrackStatusHistoryRepository;
import ru.ifmo.se.pisk.repository.RatingRepository;
import ru.ifmo.se.pisk.repository.specification.TrackSpecifications;

import java.time.Instant;
import java.util.Map;

@Service
public class TrackService {

    private final TrackRepository trackRepository;
    private final TrackStatusHistoryRepository trackStatusHistoryRepository;
    private final UserService userService;
    private final GenreService genreService;
    private final AlbumService albumService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final ArtistService artistService;
    private final RatingRepository ratingRepository;
    private final LabelService labelService;
    private final ru.ifmo.se.pisk.repository.TrackLikeRepository trackLikeRepository;
    private final SystemSettingsService systemSettingsService;
    private final EntityManager entityManager;

    public TrackService(TrackRepository trackRepository,
                        TrackStatusHistoryRepository trackStatusHistoryRepository,
                        UserService userService,
                        GenreService genreService,
                        AlbumService albumService,
                        FileStorageService fileStorageService,
                        AuditService auditService,
                        ArtistService artistService,
                        RatingRepository ratingRepository,
                        LabelService labelService,
                        ru.ifmo.se.pisk.repository.TrackLikeRepository trackLikeRepository,
                        SystemSettingsService systemSettingsService,
                        EntityManager entityManager) {
        this.trackRepository = trackRepository;
        this.trackStatusHistoryRepository = trackStatusHistoryRepository;
        this.userService = userService;
        this.genreService = genreService;
        this.albumService = albumService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.artistService = artistService;
        this.ratingRepository = ratingRepository;
        this.labelService = labelService;
        this.trackLikeRepository = trackLikeRepository;
        this.systemSettingsService = systemSettingsService;
        this.entityManager = entityManager;
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
    public TrackInfo uploadTrack(Long authorId, TrackUploadCommand command) {
        if (command.durationSeconds() <= 0) {
            throw new IllegalArgumentException("Длительность должна быть положительной");
        }
        UserEntity author = userService.requireUser(authorId);

        ArtistEntity artist = artistService.findOrCreateByName(command.artistName());
        GenreEntity genre = genreService.findOrCreateByName(command.genreName());
        ru.ifmo.se.pisk.domain.entity.LabelEntity label = labelService.findOrCreateByName(command.labelName());
        AlbumEntity album = null;
        if (org.springframework.util.StringUtils.hasText(command.albumName())) {
            String coverPath = command.coverData() != null
                    ? fileStorageService.storeCover(command.originalFilename() + "-cover", command.coverData())
                    : null;
            album = albumService.findOrCreateSimple(command.albumName(), genre, label, command.explicit(), command.inoagent(),
                    command.numberOnAlbum(), coverPath);
        }
        String storagePath = fileStorageService.storeTrack(authorId, command.originalFilename(), command.dataSupplier().get());
        TrackEntity entity = new TrackEntity();
        entity.setName(command.name());
        entity.setAuthor(author);
        entity.setGenre(genre);
        entity.setAlbum(album);
        entity.setArtist(artist);
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
        entity.setNumberOnAlbum(command.numberOnAlbum());
        entity.setExplicit(command.explicit());
        entity.setInoagent(command.inoagent());
        entity.setContentType(command.contentType());
        entity.setOriginalFilename(command.originalFilename());
        TrackEntity saved = trackRepository.save(entity);
        TrackStatusHistoryEntity history = new TrackStatusHistoryEntity();
        history.setTrack(saved);
        history.setFromStatus(null);
        history.setToStatus(TrackStatus.DRAFT);
        history.setChangedAt(Instant.now());
        history.setActor(author);
        trackStatusHistoryRepository.save(history);
        auditService.record(AuditAction.TRACK_UPLOADED, "Track", saved.getId().toString(), artist, Map.of());
        return DomainMapper.toTrackInfo(saved);
    }

    @Transactional
    public RatingInfo rateTrack(RatingCommand command) {
        TrackEntity track = requireTrack(command.trackId());
        UserEntity user = userService.requireUser(command.userId());

        // Use database function to rate
        List<Object[]> results = trackRepository.rateTrack(command.userId(), command.trackId(), command.value());

        // Analyze result to check for deletion
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            // row content from fn_track_rate: track_id, rate_count, rating_sum, rating_avg
            // rate_count is Integer, rating_sum is BigInt, rating_avg is Numeric (BigDecimal)
            if (row.length >= 4 && row[3] != null) {
                java.math.BigDecimal avgRating = (java.math.BigDecimal) row[3];
                java.math.BigDecimal threshold = systemSettingsService.getDeletionThreshold();

                if (avgRating.compareTo(threshold) < 0) {
                     // Need to find author ID for deletion.
                     // The track entity "track" variable (fetched at start of method) still has author info.
                     // Even if L1 cache is stale on ratings, the author ID is immutable/unlikely to change concurrently.
                     trackRepository.callTrackDelete(command.trackId(), track.getAuthor().getId());
                }
            }
        }

        // We still fetch the rating entity to return consistent info, or just construct it.
        // If track was deleted, this might fail if CASCADE deleted rating?
        // fn_track_delete manually deletes user_rating.
        // So if deleted, findByTrackAndUser will be empty.
        Optional<RatingEntity> ratingOpt = ratingRepository.findByTrackAndUser(track, user);
        if (ratingOpt.isEmpty()) {
            // Track was deleted, or rating failed.
            // If track deleted, we return a dummy info or exception.
            entityManager.clear();
            if (!trackRepository.existsById(command.trackId())) {
                 // Track deleted
                 return new RatingInfo(0L, command.trackId(), command.userId(), (short)command.value(), command.comment(), Instant.now());
            }
            throw new IllegalStateException("Rating should exist after fn_track_rate call");
        }
        RatingEntity rating = ratingOpt.get();

        Map<String, String> auditData = new HashMap<>();
        auditData.put("value", String.valueOf(command.value()));
        auditData.put("comment", command.comment());
        auditService.record(ru.ifmo.se.pisk.domain.AuditAction.RATING_CREATED, "Track", String.valueOf(track.getId()), user,
                auditData);
        return new RatingInfo(rating.getId(), command.trackId(), command.userId(), rating.getValue(), rating.getComment(), rating.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Optional<Integer> getUserTrackRating(Long trackId, Long userId) {
        TrackEntity track = requireTrack(trackId);
        UserEntity user = userService.requireUser(userId);
        return ratingRepository.findByTrackAndUser(track, user)
                .map(RatingEntity::getValue);
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
        Specification<TrackEntity> spec = TrackSpecifications.fullTextSearch(criteria.query())
                .and(TrackSpecifications.hasGenres(criteria.genreIds()))
                .and(TrackSpecifications.hasAuthor(criteria.authorId()))
                .and(TrackSpecifications.hasStatus(criteria.status()))
                .and(TrackSpecifications.isExplicit(criteria.explicit()))
                .and(TrackSpecifications.isInoagent(criteria.inoagent()));

        return trackRepository.findAll(spec, pageable)
                .map(DomainMapper::toTrackInfo);
    }


    @Transactional(readOnly = true)
    public Page<TrackInfo> findByAuthor(Long authorId, Pageable pageable) {
        UserEntity author = userService.requireUser(authorId);
        return trackRepository.findByAuthor(author, pageable).map(DomainMapper::toTrackInfo);
    }

    @Transactional(readOnly = true)
    public List<TrackInfo> listAlbumTracks(Long albumId) {
        AlbumEntity album = albumService.requireAlbum(albumId);
        return trackRepository.findByAlbum(album).stream()
                .sorted(Comparator.comparingInt(TrackEntity::getNumberOnAlbum))
                .map(DomainMapper::toTrackInfo)
                .collect(Collectors.toList());
    }

    @Transactional
    public void likeTrack(Long trackId, Long userId) {
        // Validation (optional, as DB function checks FKs, but good for application logic/exceptions)
        requireTrack(trackId);
        userService.requireUser(userId);
        trackLikeRepository.likeTrack(userId, trackId);
    }

    @Transactional
    public void unlikeTrack(Long trackId, Long userId) {
        requireTrack(trackId);
        userService.requireUser(userId);
        trackLikeRepository.unlikeTrack(userId, trackId);
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long trackId, Long userId) {
        TrackEntity track = requireTrack(trackId);
        UserEntity user = userService.requireUser(userId);
        return trackLikeRepository.findByTrackAndUser(track, user).isPresent();
    }

    @Transactional(readOnly = true)
    public Page<ru.ifmo.se.pisk.web.view.TrackView> getLikedTracksView(Long userId, Pageable pageable) {
        UserEntity user = userService.requireUser(userId);
        return trackLikeRepository.findByUser(user, pageable)
                .map(like -> {
                    TrackEntity t = like.getTrack();
                    String coverPublicUrl = t.getAlbum() != null && t.getAlbum().getCoverUrl() != null
                         ? fileStorageService.toPublicUrl(t.getAlbum().getCoverUrl())
                         : null;
                    return new ru.ifmo.se.pisk.web.view.TrackView(
                            t.getId(),
                            t.getName(),
                            t.getAuthor().getId(),
                            t.getAuthor().getUsername(),
                            t.getArtist().getName(),
                            t.getGenre().getName(),
                            t.isExplicit(),
                            t.isInoagent(),
                            t.getRateCount() > 0 ? (double) t.getRating() / t.getRateCount() : 0.0,
                            t.getDurationSeconds(),
                            t.getUploadedAt(),
                            coverPublicUrl
                    );
                });
    }
}
