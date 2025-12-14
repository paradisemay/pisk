package ru.ifmo.se.pisk.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.GenreInfo;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.GenreRepository;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Transactional
    public GenreInfo createGenre(String name, String description) {
        genreRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new IllegalArgumentException("Жанр уже существует");
        });
        GenreEntity entity = new GenreEntity();
        entity.setName(name);
        entity.setDescription(description);
        return DomainMapper.toGenreInfo(genreRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public GenreEntity requireGenre(Long id) {
        return genreRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Жанр не найден"));
    }

    @Transactional(readOnly = true)
    public List<GenreInfo> listAll() {
        return genreRepository.findAll().stream().map(DomainMapper::toGenreInfo).toList();
    }

    @Transactional
    public GenreEntity findOrCreateByName(String name) {
        if (!org.springframework.util.StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Жанр обязателен");
        }
        return genreRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> {
                    GenreEntity entity = new GenreEntity();
                    entity.setName(name.trim());
                    entity.setDescription("");
                    return genreRepository.save(entity);
                });
    }

    @Transactional(readOnly = true)
    public List<GenreEntity> search(String namePart) {
        if (!org.springframework.util.StringUtils.hasText(namePart)) {
            return List.of();
        }
        return genreRepository.searchByName(namePart.trim());
    }
}
