package ru.ifmo.se.pisk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ifmo.se.pisk.domain.RatingInfo;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.RatingEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.TrackStatusHistoryEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.repository.RatingRepository;
import ru.ifmo.se.pisk.repository.TrackRepository;
import ru.ifmo.se.pisk.repository.TrackStatusHistoryRepository;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    private TrackRepository trackRepository;
    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private UserService userService;
    @Mock
    private AlbumService albumService;
    @Mock
    private GenreService genreService;
    @Mock
    private ArtistService artistService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AuditService auditService;
    @Mock
    private TrackStatusHistoryRepository trackStatusHistoryRepository;
    @Mock
    private LabelService labelService;

    @InjectMocks
    private TrackService trackService;

    private UserEntity artist;
    private AlbumEntity album;

    @BeforeEach
    void setUp() {
        artist = new UserEntity();
        artist.setId(42L);
        album = new AlbumEntity();
        album.setId(10L);
        album.setTrackNumber(5);
    }

    @Test
    void uploadTrackValidatesAndPersists() {
        when(userService.requireUser(42L)).thenReturn(artist);
        GenreEntity genre = new GenreEntity();
        genre.setId(5L);
        when(genreService.findOrCreateByName("Rock")).thenReturn(genre);
        when(labelService.findOrCreateByName("Sony")).thenReturn(new ru.ifmo.se.pisk.domain.entity.LabelEntity());
        when(albumService.findOrCreateSimple(eq("Album"), eq(genre), any(), eq(false), eq(false), eq(1), any())).thenReturn(album);
        when(artistService.findOrCreateByName("Tester")).thenReturn(new ru.ifmo.se.pisk.domain.entity.ArtistEntity());
        when(fileStorageService.storeTrack(eq(42L), any(), any())).thenReturn("tracks/42/file.mp3");
        TrackEntity saved = new TrackEntity();
        saved.setId(100L);
        saved.setAuthor(artist);
        saved.setAlbum(album);
        saved.setGenre(genre);
        saved.setStoragePath("tracks/42/file.mp3");
        saved.setUploadedAt(Instant.now());
        when(trackRepository.save(any(TrackEntity.class))).thenReturn(saved);

        TrackUploadCommand command = new TrackUploadCommand(
                "Test",
                "Tester",
                "Rock",
                "Sony",
                "Album",
                120,
                false,
                false,
                1,
                "audio/mpeg",
                1024L,
                "test.mp3",
                () -> new ByteArrayInputStream("data".getBytes()),
                null,
                null
        );

        var result = trackService.uploadTrack(42L, command);

        assertThat(result.id()).isEqualTo(100L);
    }

    @Test
    void uploadTrackRejectsNonPositiveDuration() {
        TrackUploadCommand command = new TrackUploadCommand(
                "Bad",
                "Tester",
                "Rock",
                null,
                null,
                0,
                false,
                false,
                0,
                "audio/mpeg",
                10L,
                "bad.mp3",
                () -> new ByteArrayInputStream(new byte[0]),
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> trackService.uploadTrack(42L, command));
    }

    @Test
    void rateTrackRecalculatesAverage() {
        TrackEntity track = new TrackEntity();
        track.setId(15L);
        track.setAlbum(album);
        when(trackRepository.findById(15L)).thenReturn(java.util.Optional.of(track));
        UserEntity user = new UserEntity();
        user.setId(7L);
        when(userService.requireUser(7L)).thenReturn(user);

        RatingEntity persisted = new RatingEntity();
        persisted.setId(1L);
        persisted.setTrack(track);
        persisted.setUser(user);
        persisted.setValue(5);
        persisted.setComment("Great");
        persisted.setCreatedAt(Instant.now());

        // Return the entity simulating it exists after the stored proc call
        when(ratingRepository.findByTrackAndUser(track, user)).thenReturn(java.util.Optional.of(persisted));

        RatingCommand command = new RatingCommand(15L, 7L, 5, "Great");
        RatingInfo info = trackService.rateTrack(command);

        assertThat(info.value()).isEqualTo(5);

        verify(trackRepository).rateTrack(eq(7L), eq(15L), eq(5));
        verify(auditService).record(eq(ru.ifmo.se.pisk.domain.AuditAction.RATING_CREATED), eq("Track"), eq("15"), eq(user), anyMap());
    }
}
