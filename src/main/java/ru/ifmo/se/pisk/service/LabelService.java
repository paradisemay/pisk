package ru.ifmo.se.pisk.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;
import ru.ifmo.se.pisk.repository.LabelRepository;

@Service
public class LabelService {

    private final LabelRepository labelRepository;

    public LabelService(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @Transactional(readOnly = true)
    public List<LabelEntity> listAll() {
        return labelRepository.findAll();
    }

    @Transactional
    public LabelEntity findOrCreateByName(String name) {
        if (!org.springframework.util.StringUtils.hasText(name)) {




            return null;
        }
        return labelRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> {
                    LabelEntity entity = new LabelEntity();
                    entity.setName(name.trim());
                    entity.setDescription("");
                    entity.setAverageRating(0.0);
                    return labelRepository.save(entity);
                });
    }
}
