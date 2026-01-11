package ru.ifmo.se.pisk.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
public class SystemSettingsEntity {
    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Column(name = "moderation_enabled", nullable = false)
    private Boolean moderationEnabled;

    @Column(name = "default_page_size", nullable = false)
    private Integer defaultPageSize;

    @Column(name = "track_deletion_threshold")
    private Double trackDeletionThreshold;
}
