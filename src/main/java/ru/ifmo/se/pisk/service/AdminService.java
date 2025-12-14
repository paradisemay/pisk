package ru.ifmo.se.pisk.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.Album;
import ru.ifmo.se.pisk.domain.Artist;
import ru.ifmo.se.pisk.domain.Genre;
import ru.ifmo.se.pisk.domain.Label;
import ru.ifmo.se.pisk.repository.AlbumRepository;
import ru.ifmo.se.pisk.repository.ArtistRepository;
import ru.ifmo.se.pisk.repository.GenreRepository;
import ru.ifmo.se.pisk.repository.LabelRepository;

@Service
public class AdminService {

    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final LabelRepository labelRepository;

    public AdminService(GenreRepository genreRepository, ArtistRepository artistRepository,
                        AlbumRepository albumRepository, LabelRepository labelRepository) {
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.labelRepository = labelRepository;
    }

    @Transactional
    public Genre createGenre(String title, String description, String color) {
        Genre genre = new Genre();
        genre.setTitle(title);
        genre.setDescription(description);
        genre.setColor(color);
        return genreRepository.save(genre);
    }

    @Transactional
    public Artist createArtist(String name, String description) {
        Artist artist = new Artist();
        artist.setName(name);
        artist.setDescription(description);
        artist.setCreationDate(Instant.now());
        return artistRepository.save(artist);
    }

    @Transactional
    public Label createLabel(String name, String description) {
        Label label = new Label();
        label.setName(name);
        label.setDescription(description);
        return labelRepository.save(label);
    }

    @Transactional
    public Album createAlbum(String name, String description, Long genreId, int trackNumber,
                             boolean explicit, boolean inoagent, Long labelId, Instant publicationDate) {
        Genre genre = genreRepository.findById(genreId).orElseThrow();
        Label label = labelRepository.findById(labelId).orElseThrow();
        Album album = new Album();
        album.setName(name);
        album.setDescription(description);
        album.setGenre(genre);
        album.setTrackNumber(trackNumber);
        album.setExplicit(explicit);
        album.setInoagent(inoagent);
        album.setLabel(label);
        album.setPublicationDate(publicationDate);
        return albumRepository.save(album);
    }
}
