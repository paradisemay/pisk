package ru.ifmo.se.pisk.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileStorageService {

    private final Path root = Paths.get("storage");

    public FileStorageService() {
        init();
    }

    public synchronized void init() {
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve("tracks"));
            Files.createDirectories(root.resolve("covers"));
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось инициализировать файловое хранилище", ex);
        }
    }

    public String storeTrack(Long artistId, String originalFilename, InputStream data) {
        String sanitized = sanitizeFilename(originalFilename);
        String filename = Instant.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + "-" + sanitized;
        Path artistDir = root.resolve("tracks").resolve(String.valueOf(artistId));
        try {
            Files.createDirectories(artistDir);
            Path destination = artistDir.resolve(filename);
            Files.copy(data, destination, StandardCopyOption.REPLACE_EXISTING);
            return root.relativize(destination).toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Ошибка сохранения файла трека", ex);
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
}
