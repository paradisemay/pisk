package ru.ifmo.se.pisk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "album")
@Getter
@Setter
@NoArgsConstructor
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "upload_date", nullable = false)
    private Instant uploadDate = Instant.now();

    @Column(name = "cover_url")
    private String coverUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre", nullable = false)
    private Genre genre;

    @Column(name = "track_number", nullable = false)
    private Integer trackNumber;

    @Column(nullable = false)
    private boolean explicit;

    @Column(nullable = false)
    private boolean inoagent;

    @Column(name = "avg_rating", nullable = false)
    private float avgRating = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    @Column(name = "publication_date", nullable = false)
    private Instant publicationDate;
}
