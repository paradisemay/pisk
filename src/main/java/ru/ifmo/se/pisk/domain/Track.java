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
@Table(name = "track")
@Getter
@Setter
@NoArgsConstructor
public class Track extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false)
    private Long filesize;

    @Column(nullable = false)
    private Long rating = 0L;

    @Column(name = "rate_count", nullable = false)
    private Integer rateCount = 0;

    @Column(name = "upload_date", nullable = false)
    private Instant uploadDate = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @Column(nullable = false)
    private String filepath;

    @Column(name = "number_on_album", nullable = false)
    private Integer numberOnAlbum;

    @Column(nullable = false)
    private boolean explicit;

    @Column(nullable = false)
    private boolean inoagent;
}
