package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.Label;

public interface LabelRepository extends JpaRepository<Label, Long> {
}
