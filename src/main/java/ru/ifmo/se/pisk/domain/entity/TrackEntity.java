package ru.ifmo.se.pisk.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import ru.ifmo.se.pisk.domain.TrackStatus;

@Entity
@Table(name = "track")
public class TrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private AlbumEntity album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    private GenreEntity genre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private ArtistEntity artist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrackStatus status = TrackStatus.DRAFT;

    @Column(name = "duration", nullable = false)
    private long durationSeconds;

    @Column(name = "filesize", nullable = false)
    private long fileSize;

    @Column(name = "rating", nullable = false)
    private long rating;

    @Column(name = "rate_count", nullable = false)
    private int rateCount;

    @Column(name = "play_count")
    private long playCount;

    @Column(name = "upload_date", nullable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "filepath", nullable = false, length = 255)
    private String storagePath;

    @Column(name = "number_on_album")
    private int numberOnAlbum;

    @Column(name = "explicit", nullable = false)
    private boolean explicit;

    @Column(name = "inoagent", nullable = false)
    private boolean inoagent;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RatingEntity> ratings = new HashSet<>();

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CommentEntity> comments = new HashSet<>();

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TrackLikeEntity> likes = new HashSet<>();

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ComplaintEntity> complaints = new HashSet<>();

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TrackStatusHistoryEntity> statusHistory = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public TrackStatus getStatus() {
        return status;
    }

    public void setStatus(TrackStatus status) {
        this.status = status;
    }

    public AlbumEntity getAlbum() {
        return album;
    }

    public void setAlbum(AlbumEntity album) {
        this.album = album;
    }

    public UserEntity getAuthor() {
        return author;
    }

    public void setAuthor(UserEntity author) {
        this.author = author;
    }

    public GenreEntity getGenre() {
        return genre;
    }

    public void setGenre(GenreEntity genre) {
        this.genre = genre;
    }

    public ArtistEntity getArtist() {
        return artist;
    }

    public void setArtist(ArtistEntity artist) {
        this.artist = artist;
    }

    public Set<RatingEntity> getRatings() {
        return ratings;
    }

    public void setRatings(Set<RatingEntity> ratings) {
        this.ratings = ratings;
    }

    public Set<CommentEntity> getComments() {
        return comments;
    }

    public void setComments(Set<CommentEntity> comments) {
        this.comments = comments;
    }

    public Set<TrackLikeEntity> getLikes() {
        return likes;
    }

    public void setLikes(Set<TrackLikeEntity> likes) {
        this.likes = likes;
    }

    public Set<ComplaintEntity> getComplaints() {
        return complaints;
    }

    public void setComplaints(Set<ComplaintEntity> complaints) {
        this.complaints = complaints;
    }

    public Set<TrackStatusHistoryEntity> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(Set<TrackStatusHistoryEntity> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getRating() {
        return rating;
    }

    public void setRating(long rating) {
        this.rating = rating;
    }

    public int getRateCount() {
        return rateCount;
    }

    public void setRateCount(int rateCount) {
        this.rateCount = rateCount;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public int getNumberOnAlbum() {
        return numberOnAlbum;
    }

    public void setNumberOnAlbum(int numberOnAlbum) {
        this.numberOnAlbum = numberOnAlbum;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    public boolean isInoagent() {
        return inoagent;
    }

    public void setInoagent(boolean inoagent) {
        this.inoagent = inoagent;
    }

    public long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(long playCount) {
        this.playCount = playCount;
    }
}
