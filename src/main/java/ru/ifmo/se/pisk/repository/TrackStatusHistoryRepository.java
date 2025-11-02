package ru.ifmo.se.pisk.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.TrackStatusHistoryEntity;

public interface TrackStatusHistoryRepository extends JpaRepository<TrackStatusHistoryEntity, Long> {
    List<TrackStatusHistoryEntity> findByTrackOrderByChangedAtAsc(TrackEntity track);
}
