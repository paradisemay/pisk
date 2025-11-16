package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;

public interface LabelRepository extends JpaRepository<LabelEntity, Long> {
}
