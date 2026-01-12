package ru.ifmo.se.pisk.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
public class SystemSettingsEntity {
    @Id
    private Integer id;

    @Column(name = "deletion_threshold", nullable = false, precision = 3, scale = 2)
    private BigDecimal deletionThreshold;

    @Column(name = "deletion_vote_threshold", nullable = false)
    private Integer deletionVoteThreshold;
}
