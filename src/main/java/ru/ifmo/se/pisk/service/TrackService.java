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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.RatingInfo;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
import ru.ifmo.se.pisk.repository.TrackRepository;
import ru.ifmo.se.pisk.repository.TrackStatusHistoryRepository;
import ru.ifmo.se.pisk.repository.specification.TrackSpecifications;

@Service
public class TrackService {

    private final TrackRepository trackRepository;
    private final TrackStatusHistoryRepository trackStatusHistoryRepository;
    private final UserService userService;
    private final GenreService genreService;
    private final AlbumService albumService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    public TrackService(TrackRepository trackRepository,
                        TrackStatusHistoryRepository trackStatusHistoryRepository,
                        UserService userService,
                        GenreService genreService,
                        AlbumService albumService,
                        FileStorageService fileStorageService,
                        AuditService auditService) {
        this.trackRepository = trackRepository;
        this.trackStatusHistoryRepository = trackStatusHistoryRepository;
        this.userService = userService;
        this.genreService = genreService;
        this.albumService = albumService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
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
        entity.setUploadedAt(java.time.Instant.now());
        TrackEntity saved = trackRepository.save(entity);

        Optional<String> extractedCover = fileStorageService.extractEmbeddedCover(fileStorageService.resolve(storagePath))
                .flatMap(data -> fileStorageService.storeCover(saved.getId(), data));

        if (extractedCover.isEmpty() && command.coverSupplier() != null) {
            fileStorageService.validateCover(command.coverOriginalFilename(), command.coverContentType(), command.coverSize());
            extractedCover = fileStorageService.storeCover(saved.getId(), command.coverOriginalFilename(), command.coverContentType(), command.coverSupplier().get());
        }

        extractedCover.ifPresent(saved::setCoverPath);
        if (extractedCover.isPresent()) {
            trackRepository.save(saved);
        }
        return DomainMapper.toTrackInfo(saved);
    }

    @Transactional
    public RatingInfo rateTrack(RatingCommand command) {
        TrackEntity track = requireTrack(command.trackId());
        // Implementation would update rating in repository
        return new RatingInfo(System.currentTimeMillis(), command.trackId(), command.userId(), command.value(), command.comment(), java.time.Instant.now());
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
