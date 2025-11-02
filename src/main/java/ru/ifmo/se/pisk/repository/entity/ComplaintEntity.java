package ru.ifmo.se.pisk.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import ru.ifmo.se.pisk.domain.ComplaintStatus;

@Entity
@Table(name = "complaints")
public class ComplaintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private TrackEntity track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_id", nullable = false)
    private UserEntity complainant;

    @Column(nullable = false, length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "moderator_note", length = 512)
    private String moderatorNote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrackEntity getTrack() {
        return track;
    }

    public void setTrack(TrackEntity track) {
        this.track = track;
    }

    public UserEntity getComplainant() {
        return complainant;
    }

    public void setComplainant(UserEntity complainant) {
        this.complainant = complainant;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getModeratorNote() {
        return moderatorNote;
    }

    public void setModeratorNote(String moderatorNote) {
        this.moderatorNote = moderatorNote;
    }
}
