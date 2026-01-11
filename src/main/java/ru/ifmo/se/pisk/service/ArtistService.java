package ru.ifmo.se.pisk.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.ifmo.se.pisk.domain.entity.ArtistEntity;
import ru.ifmo.se.pisk.repository.ArtistRepository;

@Service
public class ArtistService {

    private final ArtistRepository artistRepository;

    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @Transactional
    public ArtistEntity findOrCreateByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Имя исполнителя обязательно");
        }
        return artistRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> {
                    ArtistEntity entity = new ArtistEntity();
                    entity.setName(name.trim());
                    entity.setDescription("");
                    return artistRepository.save(entity);
                });
    }

    @Transactional(readOnly = true)
    public List<ArtistEntity> searchByName(String namePart) {
        if (!StringUtils.hasText(namePart)) {
            return List.of();
        }
        return artistRepository.searchByName(namePart.trim());
    }

    public ArtistEntity findById(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artist with id " + id + " not found"));
    }
}
