package ru.ifmo.se.pisk.web.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;

public class UploadTrackForm {

    @NotNull
    private Long artistId;

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private Long durationSeconds;

    @NotNull
    private Long albumId;

    @NotNull
    private Long genreId;

    private boolean explicit;

    private boolean inoagent;

    @Min(1)
    private int numberOnAlbum = 1;

    @NotNull
    private MultipartFile file;

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    public boolean isInoagent() {
        return inoagent;
    }

    public void setInoagent(boolean inoagent) {
        this.inoagent = inoagent;
    }

    public int getNumberOnAlbum() {
        return numberOnAlbum;
    }

    public void setNumberOnAlbum(int numberOnAlbum) {
        this.numberOnAlbum = numberOnAlbum;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
