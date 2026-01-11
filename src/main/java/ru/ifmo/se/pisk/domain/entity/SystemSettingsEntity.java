package ru.ifmo.se.pisk.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "system_settings")
public class SystemSettingsEntity {
    @Id
    private Integer id = 1;

    @Column(name = "deletion_threshold", nullable = false)
    private Double deletionThreshold;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getDeletionThreshold() {
        return deletionThreshold;
    }

    public void setDeletionThreshold(Double deletionThreshold) {
        this.deletionThreshold = deletionThreshold;
    }
}
