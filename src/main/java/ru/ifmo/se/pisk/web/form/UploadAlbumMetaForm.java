package ru.ifmo.se.pisk.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

/**
 * Шаг 1: метаданные альбома.
 */
public class UploadAlbumMetaForm {

    @NotBlank
    private String artistName;

    @NotBlank
    private String albumName;

    @NotBlank
    private String genreName;

    private String labelName;

    private boolean explicit;

    private boolean inoagent;

    @Min(1)
    @Max(50)
    private int trackCount = 1;

    /** Опциональная обложка альбома (png/jpeg/webp). */
    private MultipartFile cover;

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
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

    public int getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    public MultipartFile getCover() {
        return cover;
    }

    public void setCover(MultipartFile cover) {
        this.cover = cover;
    }
}
