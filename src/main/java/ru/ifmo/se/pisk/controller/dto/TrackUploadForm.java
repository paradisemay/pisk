package ru.ifmo.se.pisk.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TrackUploadForm {
    @NotBlank(message = "{validation.title.required}")
    @Size(max = 100, message = "{validation.title.length}")
    private String title;

    @Size(max = 500, message = "{validation.description.length}")
    private String description;

    @NotBlank(message = "{validation.genre.required}")
    private String genre;

    @NotBlank(message = "{validation.audioUrl.required}")
    @Pattern(regexp = "https?://.+|/.+", message = "{validation.audioUrl.format}")
    private String audioUrl;

    @Pattern(regexp = "https?://.+|/.+", message = "{validation.coverUrl.format}")
    private String coverUrl;

    private List<String> tags;
}
