package ru.ifmo.se.pisk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
import java.util.stream.Stream;

import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.RatingInfo;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
import ru.ifmo.se.pisk.repository.specification.TrackSpecifications;

@Service
public class TrackService {

    private final List<Track> tracks;
    private final Map<Long, TrackStatus> statuses;

    public TrackService() {
        Author alice = Author.builder()
                .id(1L)
                .name("Alice Elektron")
                .bio("Любит экспериментальную электронику.")
                .avatarUrl("/img/author1.png")
                .registeredAt(LocalDate.now().minusMonths(5))
                .followers(1820)
                .build();
        Author bob = Author.builder()
                .id(2L)
                .name("DJ Bob")
                .bio("Битмейкер и саунд-дизайнер.")
                .avatarUrl("/img/author2.png")
                .registeredAt(LocalDate.now().minusYears(1))
                .followers(2400)
                .build();

        tracks = new ArrayList<>(List.of(
                Track.builder()
                        .id(1L)
                        .title("Northern Lights")
                        .description("Спокойный эмбиент под северное сияние.")
                        .audioUrl("/audio/northern-lights.mp3")
                        .coverUrl("/img/cover1.jpg")
                        .genre("ambient")
                        .tags(List.of("chill", "sleep"))
                        .rating(4.8)
                        .plays(12500)
                        .publishedAt(LocalDate.now().minusDays(10))
                        .author(alice)
                        .duration(Duration.ofMinutes(4).plusSeconds(12))
                        .build(),
                Track.builder()
                        .id(2L)
                        .title("City Pulse")
                        .description("Уличный шум превращается в техно.")
                        .audioUrl("/audio/city-pulse.mp3")
                        .coverUrl("/img/cover2.jpg")
                        .genre("techno")
                        .tags(List.of("dance", "urban"))
                        .rating(4.6)
                        .plays(8700)
                        .publishedAt(LocalDate.now().minusDays(3))
                        .author(bob)
                        .duration(Duration.ofMinutes(5).plusSeconds(3))
                        .build(),
                Track.builder()
                        .id(3L)
                        .title("Morning Run")
                        .description("Энергичный трек для пробежки.")
                        .audioUrl("/audio/morning-run.mp3")
                        .coverUrl("/img/cover3.jpg")
                        .genre("house")
                        .tags(List.of("sport", "motivation"))
                        .rating(4.2)
                        .plays(3200)
                        .publishedAt(LocalDate.now().minusWeeks(2))
                        .author(alice)
                        .duration(Duration.ofMinutes(3).plusSeconds(40))
                        .build()
        ));
        statuses = tracks.stream().collect(Collectors.toMap(Track::getId, t -> TrackStatus.PUBLISHED));
    }

    public Page<Track> findTracks(String search, String genre, List<String> tags, String sort, Pageable pageable) {
        List<Track> filtered = tracks.stream()
                .filter(track -> search == null || track.getTitle().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                        || track.getDescription().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .filter(track -> genre == null || genre.isBlank() || Objects.equals(track.getGenre(), genre))
                .filter(track -> tags == null || tags.isEmpty() || track.getTags().stream().anyMatch(tags::contains))
                .sorted(resolveComparator(sort))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        if (start > end) {
            return Page.empty(pageable);
        }
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    private Comparator<Track> resolveComparator(String sort) {
        if ("plays".equalsIgnoreCase(sort)) {
            return Comparator.comparingInt(Track::getPlays).reversed();
        }
        if ("date".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Track::getPublishedAt).reversed();
        GenreEntity genre = genreService.requireGenre(command.genreId());
        String sanitizedName = fileStorageService.sanitizeFilename(command.originalFilename());
        String storagePath;
        try (InputStream inputStream = command.dataSupplier().get()) {
            storagePath = fileStorageService.storeTrack(artistId, sanitizedName, inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось сохранить файл трека", ex);
        }
        return Comparator.comparingDouble(Track::getRating).reversed();
    }

    public Optional<Track> findById(long id) {
        return tracks.stream().filter(t -> t.getId() == id).findFirst();
    }

    public List<Track> findByAuthor(long authorId) {
        return tracks.stream().filter(t -> t.getAuthor().getId() == authorId).toList();
    }

    public Map<String, Long> groupByGenre() {
        return tracks.stream()
                .collect(Collectors.groupingBy(Track::getGenre, Collectors.counting()));
    }

    public Track addTrack(Track track) {
        tracks.add(track);
        statuses.put(track.getId(), TrackStatus.PUBLISHED);
        return track;
    }

    public TrackEntity requireTrack(Long trackId) {
        Track track = findById(trackId).orElseThrow(() -> new IllegalArgumentException("Track not found"));
        TrackEntity entity = new TrackEntity();
        entity.setAuthor(artist);
        entity.setAlbum(album);
        entity.setGenre(genre);
        entity.setName(command.name());
        entity.setDurationSeconds(command.durationSeconds());
        entity.setStoragePath(storagePath);
        entity.setFileSize(command.fileSize());
        entity.setContentType(command.contentType());
        entity.setOriginalFilename(sanitizedName);
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
        statuses.put(trackId, status);
        return getTrackInfo(trackId);
    }

    public TrackInfo uploadTrack(Long artistId, TrackUploadCommand command) {
        Track track = Track.builder()
                .id(System.currentTimeMillis())
                .title(command.name())
                .description("Upload placeholder")
                .genre("demo")
                .audioUrl("/audio/" + command.originalFilename())
                .coverUrl("/img/cover-uploaded.jpg")
                .tags(List.of())
                .author(tracks.getFirst().getAuthor())
                .rating(0)
                .plays(0)
                .publishedAt(LocalDate.now())
                .duration(Duration.ofSeconds(command.durationSeconds()))
                .build();
        addTrack(track);
        return toInfo(track);
    }

    public RatingInfo rateTrack(RatingCommand command) {
        Track track = findById(command.trackId()).orElseThrow(() -> new IllegalArgumentException("Трек не найден: " + command.trackId()));
        Track updated = Track.builder()
                .id(track.getId())
                .title(track.getTitle())
                .description(track.getDescription())
                .audioUrl(track.getAudioUrl())
                .coverUrl(track.getCoverUrl())
                .genre(track.getGenre())
                .tags(track.getTags())
                .rating(Math.min(5.0, track.getRating() + command.value() / 5.0))
                .plays(track.getPlays())
                .publishedAt(track.getPublishedAt())
                .author(track.getAuthor())
                .duration(track.getDuration())
                .build();
        tracks.replaceAll(t -> t.getId().equals(updated.getId()) ? updated : t);
        return new RatingInfo(System.currentTimeMillis(), command.trackId(), command.userId(), command.value(), command.comment(), java.time.Instant.now());
    }

    public TrackInfo incrementPlayCount(Long trackId) {
        Track track = findById(trackId).orElseThrow(() -> new IllegalArgumentException("Трек не найден: " + trackId));
        Track updated = Track.builder()
                .id(track.getId())
                .title(track.getTitle())
                .description(track.getDescription())
                .audioUrl(track.getAudioUrl())
                .coverUrl(track.getCoverUrl())
                .genre(track.getGenre())
                .tags(track.getTags())
                .rating(track.getRating())
                .plays(track.getPlays() + 1)
                .publishedAt(track.getPublishedAt())
                .author(track.getAuthor())
                .duration(track.getDuration())
                .build();
        tracks.replaceAll(t -> t.getId().equals(updated.getId()) ? updated : t);
        return toInfo(updated);
    }

    public Page<TrackInfo> searchTracks(String title, java.util.Set<Long> genreIds, Pageable pageable) {
        List<Track> filtered = tracks.stream()
                .filter(t -> title == null || t.getTitle().toLowerCase(Locale.ROOT).contains(title.toLowerCase(Locale.ROOT)))
                .toList();
        return sliceToPage(filtered, pageable);
    }

    public Page<TrackInfo> findTracks(Boolean explicit, Boolean inoagent, Pageable pageable) {
        return sliceToPage(tracks, pageable);
    }

    public TrackInfo getTrackInfo(Long trackId) {
        Track track = findById(trackId).orElseThrow(() -> new IllegalArgumentException("Трек не найден: " + trackId));
        return toInfo(track);
    }

    private Page<TrackInfo> sliceToPage(List<Track> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        if (start > end) {
            return Page.empty(pageable);
        }
        List<TrackInfo> content = list.subList(start, end).stream().map(this::toInfo).toList();
        return new PageImpl<>(content, pageable, list.size());
    }

    private TrackInfo toInfo(Track track) {
        return new TrackInfo(track.getId(), track.getTitle(),
                track.getAuthor() != null ? track.getAuthor().getId() : null,
                null, track.getAuthor() != null ? track.getAuthor().getId() : null,
                null, track.getAudioUrl(), 0, false, false,
                track.getDuration() != null ? track.getDuration().getSeconds() : 0,
                0, java.time.Instant.now(), track.getRating(), 1, track.getPlays(), 0);
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
