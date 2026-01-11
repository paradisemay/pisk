package ru.ifmo.se.pisk.web.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Шаг 2: список треков альбома.
 */
public class UploadAlbumTracksForm {

    @Valid
    @NotNull
    @Size(min = 1)
    private List<UploadTrackItemForm> tracks = new ArrayList<>();

    public List<UploadTrackItemForm> getTracks() {
        return tracks;
    }

    public void setTracks(List<UploadTrackItemForm> tracks) {
        this.tracks = tracks;
    }
}
