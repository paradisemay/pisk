package ru.ifmo.se.pisk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.entity.SystemSettingsEntity;
import ru.ifmo.se.pisk.repository.SystemSettingsRepository;

import java.math.BigDecimal;

@Service
public class SystemSettingsService {

    private final SystemSettingsRepository repository;

    public SystemSettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BigDecimal getDeletionThreshold() {
        return getSettings().getDeletionThreshold();
    }

    @Transactional
    public void updateDeletionThreshold(BigDecimal threshold) {
        SystemSettingsEntity settings = getSettings();
        settings.setDeletionThreshold(threshold);
        repository.save(settings);
    }

    private SystemSettingsEntity getSettings() {
        return repository.findById(1)
                .orElseThrow(() -> new IllegalStateException("System settings not found"));
    }
}
