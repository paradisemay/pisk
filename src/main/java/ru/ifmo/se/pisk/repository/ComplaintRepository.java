package ru.ifmo.se.pisk.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.ComplaintStatus;
import ru.ifmo.se.pisk.repository.entity.ComplaintEntity;

public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {
    List<ComplaintEntity> findByStatus(ComplaintStatus status);
}
