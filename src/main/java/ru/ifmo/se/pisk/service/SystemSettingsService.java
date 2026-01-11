package ru.ifmo.se.pisk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.entity.SystemSettingsEntity;
import ru.ifmo.se.pisk.repository.SystemSettingsRepository;

@Service
public class SystemSettingsService {

    private final SystemSettingsRepository repository;

    public SystemSettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public double getDeletionThreshold() {
        return repository.findById(1)
                .map(SystemSettingsEntity::getDeletionThreshold)
                .orElse(1.0);
    }

    @Transactional
    public void updateDeletionThreshold(double threshold) {
        SystemSettingsEntity settings = repository.findById(1).orElseGet(() -> {
            SystemSettingsEntity s = new SystemSettingsEntity();
            s.setId(1);
            return s;
        });
        settings.setDeletionThreshold(threshold);
        repository.save(settings);
    }
}
