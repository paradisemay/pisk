package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.entity.SystemSettingsEntity;

public interface SystemSettingsRepository extends JpaRepository<SystemSettingsEntity, Integer> {
}
