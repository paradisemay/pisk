package ru.ifmo.se.pisk.service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpRange;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.ifmo.se.pisk.config.AppProperties;
import ru.ifmo.se.pisk.domain.Album;
import ru.ifmo.se.pisk.domain.Artist;
import ru.ifmo.se.pisk.domain.Comment;
import ru.ifmo.se.pisk.domain.Genre;
import ru.ifmo.se.pisk.domain.Track;
import ru.ifmo.se.pisk.domain.User;
import ru.ifmo.se.pisk.domain.UserRating;
import ru.ifmo.se.pisk.domain.UserTrackKey;
import ru.ifmo.se.pisk.repository.AlbumRepository;
import ru.ifmo.se.pisk.repository.ArtistRepository;
import ru.ifmo.se.pisk.repository.CommentRepository;
import ru.ifmo.se.pisk.repository.GenreRepository;
import ru.ifmo.se.pisk.repository.TrackRepository;
import ru.ifmo.se.pisk.repository.UserRatingRepository;

@Service
public class TrackService {

    private final TrackRepository trackRepository;
    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final UserRatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final AppProperties appProperties;

    public TrackService(TrackRepository trackRepository, GenreRepository genreRepository, ArtistRepository artistRepository,
                        AlbumRepository albumRepository, UserRatingRepository ratingRepository,
                        CommentRepository commentRepository, AppProperties appProperties) {
        this.trackRepository = trackRepository;
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public Track uploadTrack(User author, MultipartFile file, Long albumId, Long artistId, Long genreId,
                             String name, int numberOnAlbum, int duration, boolean explicit, boolean inoagent) throws IOException {
        Genre genre = genreRepository.findById(genreId).orElseThrow();
        Artist artist = artistRepository.findById(artistId).orElseThrow();
        Album album = albumRepository.findById(albumId).orElseThrow();
        Path storage = Path.of(appProperties.getStoragePath());
        Files.createDirectories(storage);
        Path path = storage.resolve(file.getOriginalFilename());
        Files.write(path, file.getBytes());

        Track track = new Track();
        track.setAuthor(author);
        track.setAlbum(album);
        track.setArtist(artist);
        track.setGenre(genre);
        track.setName(name);
        track.setNumberOnAlbum(numberOnAlbum);
        track.setDuration(duration);
        track.setFilesize(file.getSize());
        track.setFilepath(path.toAbsolutePath().toString());
        track.setExplicit(explicit);
        track.setInoagent(inoagent);
        track.setUploadDate(Instant.now());
        return trackRepository.save(track);
    }

    @Transactional(readOnly = true)
    public List<Track> search(String query) {
        return trackRepository.findByNameContainingIgnoreCase(query);
    }

    @Transactional
    public UserRating rate(User user, Long trackId, short rate) {
        Track track = trackRepository.findById(trackId).orElseThrow();
        Optional<UserRating> existing = ratingRepository.findByUserIdAndTrackId(user.getId(), trackId);
        if (existing.isPresent()) {
            UserRating rating = existing.get();
            rating.setRate(rate);
            return ratingRepository.save(rating);
        }
        UserRating rating = new UserRating();
        rating.setId(new UserTrackKey());
        rating.getId().setUserId(user.getId());
        rating.getId().setTrackId(trackId);
        rating.setUser(user);
        rating.setTrack(track);
        rating.setRate(rate);
        track.setRating(track.getRating() + rate);
        track.setRateCount(track.getRateCount() + 1);
        ratingRepository.save(rating);
        return rating;
    }

    @Transactional
    public Comment complain(User user, Long trackId, String body, int trackTime) {
        Track track = trackRepository.findById(trackId).orElseThrow();
        Comment comment = new Comment();
        comment.setId(new UserTrackKey());
        comment.getId().setUserId(user.getId());
        comment.getId().setTrackId(trackId);
        comment.setUser(user);
        comment.setTrack(track);
        comment.setBody(body);
        comment.setTrackTime(trackTime);
        comment.setCreatedAt(Instant.now());
        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> stream(Long trackId, String rangeHeader) throws IOException {
        Track track = trackRepository.findById(trackId).orElseThrow();
        File file = new File(track.getFilepath());
        long contentLength = file.length();
        if (rangeHeader == null) {
            return ResponseEntity.ok()
                    .contentLength(contentLength)
                    .body(new FileSystemResource(file));
        }
        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        if (ranges.isEmpty()) {
            return ResponseEntity.status(416).build();
        }
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(contentLength);
        long end = range.getRangeEnd(contentLength);
        long rangeLength = end - start + 1;
        byte[] data = readRange(file, start, rangeLength);
        return ResponseEntity.status(206)
                .header("Content-Range", "bytes " + start + "-" + end + "/" + contentLength)
                .contentLength(rangeLength)
                .body(new ByteArrayResource(data));
    }

    private byte[] readRange(File file, long start, long length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            byte[] buffer = new byte[(int) length];
            int read = raf.read(buffer);
            if (read < length) {
                byte[] actual = new byte[read];
                System.arraycopy(buffer, 0, actual, 0, read);
                return actual;
            }
            return buffer;
        }
    }
}
