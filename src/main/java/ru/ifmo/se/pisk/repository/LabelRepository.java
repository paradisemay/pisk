package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;

public interface LabelRepository extends JpaRepository<LabelEntity, Long> {
    Optional<LabelEntity> findByNameIgnoreCase(String name);
}
