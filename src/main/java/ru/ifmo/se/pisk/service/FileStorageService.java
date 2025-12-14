package ru.ifmo.se.pisk.service;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FileStorageService {
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
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );
    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024; // 30 MB
    private static final long MAX_AVATAR_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public FileStorageService() { init(); }

    public synchronized void init() {
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve("tracks"));
            Files.createDirectories(root.resolve("covers"));
            Files.createDirectories(root.resolve("avatars"));
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось инициализировать файловое хранилище", ex);
        }
    }

    public void validateTrack(String originalFilename, String contentType, long fileSize) {
        validate(originalFilename, contentType, fileSize, ALLOWED_CONTENT_TYPES, MAX_FILE_SIZE);
    }

    public void validateAvatar(String originalFilename, String contentType, long fileSize) {
        validate(originalFilename, contentType, fileSize, ALLOWED_AVATAR_CONTENT_TYPES, MAX_AVATAR_FILE_SIZE);
    }

    private void validate(String originalFilename, String contentType, long fileSize, Set<String> allowedTypes, long maxSize) {
        if (!StringUtils.hasText(originalFilename)) throw new IllegalArgumentException("Пустое имя файла");
        if (!allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Формат файла не поддерживается: " + contentType);
        }
        if (fileSize <= 0 || fileSize > maxSize) {
            throw new IllegalArgumentException("Размер файла превышает допустимый лимит: " + (fileSize / (1024 * 1024)) + "MB");
        }
    }

    public String storeTrack(Long artistId, String originalFilename, InputStream data) {
        String sanitized = sanitizeFilename(originalFilename);
        String filename = Instant.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + "-" + sanitized;
        Path artistDir = root.resolve("tracks").resolve(String.valueOf(artistId));
        try (InputStream input = data) {
            Files.createDirectories(artistDir);
            Path destination = artistDir.resolve(filename);
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            return root.relativize(destination).toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Ошибка сохранения файла трека", ex);
        }
    }

    public String storeAvatar(Long userId, String originalFilename, InputStream data) {
        String sanitized = sanitizeFilename(originalFilename);
        String filename = "avatar-" + userId + "-" + UUID.randomUUID() + "-" + sanitized;
        Path userDir = root.resolve("avatars").resolve(String.valueOf(userId));
        try (InputStream input = data) {
            Files.createDirectories(userDir);
            Path destination = userDir.resolve(filename);
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            return root.relativize(destination).toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Ошибка сохранения аватара", ex);
        }
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

    public String toPublicUrl(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }
        return "/storage/" + relativePath.replace('\\', '/');
    }
}
