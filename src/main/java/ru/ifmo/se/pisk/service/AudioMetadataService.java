package ru.ifmo.se.pisk.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AudioMetadataService {

    public AudioMetadata extract(byte[] data, String originalFilename, String contentType) {
        try {
            Path tempFile = Files.createTempFile("upload-track", getExtension(originalFilename));
            Files.write(tempFile, data);
            try {
                AudioFile audioFile = AudioFileIO.read(tempFile.toFile());
                AudioHeader header = audioFile.getAudioHeader();
                Tag tag = audioFile.getTag();
                Artwork artwork = tag != null ? (Artwork) tag.getFirstArtwork() : null;
                return new AudioMetadata(
                        tag != null ? emptyToNull(tag.getFirst(FieldKey.TITLE)) : null,
                        header != null ? header.getTrackLength() : 0,
                        parseTrackNumber(tag),
                        artwork != null ? artwork.getBinaryData() : null,
                        artwork != null ? artwork.getMimeType() : null
                );
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать метаданные аудиофайла", e);
        }
    }

    private Integer parseTrackNumber(Tag tag) {
        if (tag == null) {
            return null;
        }
        String track = emptyToNull(tag.getFirst(FieldKey.TRACK));
        if (track == null) {
            return null;
        }
        String sanitized = track.contains("/") ? track.substring(0, track.indexOf('/')) : track;
        try {
            int value = Integer.parseInt(sanitized.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return ".tmp";
        }
        String ext = filename.substring(filename.lastIndexOf('.'));
        return ext.length() > 10 ? ".tmp" : ext;
    }

    private String emptyToNull(String value) {
        return Optional.ofNullable(value).filter(StringUtils::hasText).orElse(null);
    }

    public record AudioMetadata(String title, long durationSeconds, Integer trackNumber, byte[] coverData,
                                String coverContentType) {
    }
}
