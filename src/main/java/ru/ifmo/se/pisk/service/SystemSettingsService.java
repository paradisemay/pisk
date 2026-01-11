package ru.ifmo.se.pisk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.controller.dto.AdminSettingsForm;
import ru.ifmo.se.pisk.domain.entity.SystemSettingsEntity;
import ru.ifmo.se.pisk.repository.SystemSettingsRepository;

@Service
@Transactional
public class SystemSettingsService {

    private final SystemSettingsRepository repository;

    public SystemSettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    public AdminSettingsForm getSettings() {
        SystemSettingsEntity entity = repository.findById(1).orElseGet(() -> {
            SystemSettingsEntity newSettings = new SystemSettingsEntity();
            newSettings.setId(1);
            newSettings.setModerationEnabled(true);
            newSettings.setDefaultPageSize(10);
            newSettings.setTrackDeletionThreshold(0.0);
            return repository.save(newSettings);
        });

        AdminSettingsForm form = new AdminSettingsForm();
        form.setModerationEnabled(entity.getModerationEnabled());
        form.setDefaultPageSize(entity.getDefaultPageSize());
        form.setTrackDeletionThreshold(entity.getTrackDeletionThreshold());
        return form;
    }

    public void updateSettings(AdminSettingsForm form) {
        SystemSettingsEntity entity = repository.findById(1).orElseGet(() -> {
            SystemSettingsEntity newSettings = new SystemSettingsEntity();
            newSettings.setId(1);
            return newSettings;
        });

        entity.setModerationEnabled(form.getModerationEnabled());
        entity.setDefaultPageSize(form.getDefaultPageSize());
        entity.setTrackDeletionThreshold(form.getTrackDeletionThreshold());

        repository.save(entity);
    }
}
