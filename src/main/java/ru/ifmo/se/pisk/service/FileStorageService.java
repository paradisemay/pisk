package ru.ifmo.se.pisk.service;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final Path root = Paths.get("storage");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/mpeg", // mp3
            "audio/wav",
            "audio/x-wav",
            "audio/flac",
            "audio/x-flac",
            "audio/ogg",
            "audio/mp4",   // m4a
            "audio/aac"
    );
    private static final Set<String> ALLOWED_COVER_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024; // 30 MB
    private static final long MAX_COVER_SIZE = 5 * 1024 * 1024; // 5 MB

    public FileStorageService() { init(); }

    public synchronized void init() {
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve("tracks"));
            Files.createDirectories(root.resolve("covers"));
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось инициализировать файловое хранилище", ex);
        }
    }

    public void validate(String originalFilename, String contentType, long fileSize) {
        if (!StringUtils.hasText(originalFilename)) throw new IllegalArgumentException("Пустое имя файла");
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Формат файла не поддерживается: " + contentType);
        }
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Размер файла превышает допустимый лимит: " + (fileSize / (1024 * 1024)) + "MB");
        }
    }

    public String storeTrack(Long artistId, String originalFilename, InputStream data) {
        String sanitized = sanitizeFilename(originalFilename);
        String filename = Instant.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + "-" + sanitized;
        Path artistDir = root.resolve("tracks").resolve(String.valueOf(artistId));
        try (InputStream stream = data) {
            Files.createDirectories(artistDir);
            Path destination = artistDir.resolve(filename);
            Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
            return root.relativize(destination).toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Ошибка сохранения файла трека", ex);
        }
    }

    public void validateCover(String originalFilename, String contentType, Long fileSize) {
        if (fileSize == null || fileSize <= 0 || fileSize > MAX_COVER_SIZE) {
            throw new IllegalArgumentException("Размер обложки превышает допустимый лимит: " + MAX_COVER_SIZE / (1024 * 1024) + "MB");
        }
        String resolvedContentType = StringUtils.hasText(contentType) ? contentType : MediaType.IMAGE_JPEG_VALUE;
        if (!ALLOWED_COVER_TYPES.contains(resolvedContentType)) {
            throw new IllegalArgumentException("Формат обложки не поддерживается: " + resolvedContentType);
        }
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("Пустое имя файла обложки");
        }
    }

    public Optional<String> storeCover(Long trackId, String originalFilename, String contentType, InputStream data) {
        String resolvedContentType = StringUtils.hasText(contentType) ? contentType : MediaType.IMAGE_JPEG_VALUE;
        String extension = resolveExtension(resolvedContentType, originalFilename);
        Path destination = root.resolve("covers").resolve(trackId + extension);
        try (InputStream stream = data) {
            Files.createDirectories(destination.getParent());
            Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(root.relativize(destination).toString().replace('\\', '/'));
        } catch (IOException ex) {
            throw new IllegalStateException("Ошибка сохранения обложки", ex);
        }
    }

    public Optional<String> storeCover(Long trackId, CoverData coverData) {
        if (coverData == null || coverData.bytes() == null || coverData.bytes().length == 0) {
            return Optional.empty();
        }
        if (coverData.bytes().length > MAX_COVER_SIZE) {
            logger.warn("Embedded cover is too large: {} bytes", coverData.bytes().length);
            return Optional.empty();
        }
        String contentType = StringUtils.hasText(coverData.contentType()) ? coverData.contentType() : MediaType.IMAGE_JPEG_VALUE;
        if (!ALLOWED_COVER_TYPES.contains(contentType)) {
            logger.warn("Embedded cover has unsupported content type: {}", coverData.contentType());
            return Optional.empty();
        }
        String extension = resolveExtension(contentType, "cover");
        Path destination = root.resolve("covers").resolve(trackId + extension);
        try (InputStream stream = new ByteArrayInputStream(coverData.bytes())) {
            Files.createDirectories(destination.getParent());
            Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(root.relativize(destination).toString().replace('\\', '/'));
        } catch (IOException ex) {
            logger.warn("Failed to store embedded cover for track {}", trackId, ex);
            return Optional.empty();
        }
    }

    public Optional<CoverData> extractEmbeddedCover(Path audioPath) {
        try {
            AudioFile audioFile = AudioFileIO.read(audioPath.toFile());
            Tag tag = audioFile.getTag();
            if (tag == null) {
                return Optional.empty();
            }
            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null || artwork.getBinaryData() == null) {
                return Optional.empty();
            }
            return Optional.of(new CoverData(artwork.getBinaryData(), artwork.getMimeType()));
        } catch (Exception ex) {
            logger.warn("Не удалось извлечь обложку из {}", audioPath, ex);
            return Optional.empty();
        }
    }

    public Path resolve(String relativePath) {
        return root.resolve(relativePath);
    }

    public Resource load(String relativePath) {
        Path file = root.resolve(relativePath);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Файл не найден: " + relativePath);
        }
        return new FileSystemResource(file);
    }

    public String sanitizeFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "file";
        }
        String cleaned = originalFilename.replaceAll("[^A-Za-z0-9_.-]", "_");
        if (!StringUtils.hasText(cleaned)) {
            cleaned = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        }
        return cleaned;
    }

    private String resolveExtension(String contentType, String originalFilename) {
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            return ".png";
        }
        if (StringUtils.hasText(originalFilename) && originalFilename.toLowerCase(Locale.ROOT).endsWith(".png")) {
            return ".png";
        }
        return ".jpg";
    }

    public record CoverData(byte[] bytes, String contentType) {}
}
