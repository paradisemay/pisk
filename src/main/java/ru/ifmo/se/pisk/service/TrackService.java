package ru.ifmo.se.pisk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.ifmo.se.pisk.domain.RatingInfo;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.ArtistEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.RatingEntity;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
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

    public TrackService(TrackRepository trackRepository,
                        TrackStatusHistoryRepository trackStatusHistoryRepository,
                        UserService userService,
                        GenreService genreService,
                        AlbumService albumService,
                        FileStorageService fileStorageService,
                        AuditService auditService,
                        ArtistService artistService,
                        RatingRepository ratingRepository) {
        this.trackRepository = trackRepository;
        this.trackStatusHistoryRepository = trackStatusHistoryRepository;
        this.userService = userService;
        this.genreService = genreService;
        this.albumService = albumService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.artistService = artistService;
        this.ratingRepository = ratingRepository;
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
        UserEntity author = userService.requireUser(artistId);
        ArtistEntity artist = artistService.findOrCreateByName(command.artistName());
        GenreEntity genre = genreService.findOrCreateByName(command.genreName());
        AlbumEntity album = null;
        if (org.springframework.util.StringUtils.hasText(command.albumName())) {
            String coverPath = command.coverData() != null
                    ? fileStorageService.storeCover(command.originalFilename() + "-cover", command.coverData())
                    : null;
            album = albumService.findOrCreateSimple(command.albumName(), genre, command.explicit(), command.inoagent(),
                    command.numberOnAlbum(), coverPath);
        }
        String storagePath = fileStorageService.storeTrack(artistId, command.originalFilename(), command.dataSupplier().get());
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
        entity.setUploadedAt(java.time.Instant.now());
        entity.setNumberOnAlbum(command.numberOnAlbum());
        entity.setExplicit(command.explicit());
        entity.setInoagent(command.inoagent());
        entity.setContentType(command.contentType());
        entity.setOriginalFilename(command.originalFilename());
        TrackEntity saved = trackRepository.save(entity);
        return DomainMapper.toTrackInfo(saved);
    }

    @Transactional
    public RatingInfo rateTrack(RatingCommand command) {
        TrackEntity track = requireTrack(command.trackId());
        UserEntity user = userService.requireUser(command.userId());
        RatingEntity rating = ratingRepository.findByTrackAndUser(track, user).orElseGet(RatingEntity::new);
        rating.setTrack(track);
        rating.setUser(user);
        rating.setValue(command.value());
        rating.setComment(command.comment());
        rating.setCreatedAt(Instant.now());
        RatingEntity persisted = ratingRepository.save(rating);
        Double average = ratingRepository.calculateAverageForTrack(track);
        Long count = ratingRepository.countByTrack(track);
        if (average != null && count != null) {
            track.setRateCount(count.intValue());
            track.setRating(Math.round(average * count));
            trackRepository.save(track);
        }
        Map<String, String> auditData = new HashMap<>();
        auditData.put("value", String.valueOf(command.value()));
        auditData.put("comment", command.comment());
        auditService.record(ru.ifmo.se.pisk.domain.AuditAction.RATING_CREATED, "Track", String.valueOf(track.getId()), user,
                auditData);
        return new RatingInfo(persisted.getId(), command.trackId(), command.userId(), command.value(), command.comment(), persisted.getCreatedAt());
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
