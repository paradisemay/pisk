package ru.ifmo.se.pisk.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import ru.ifmo.se.pisk.domain.UserRole;
import ru.ifmo.se.pisk.domain.UserStatus;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_salt", nullable = false, length = 64)
    private String passwordSalt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @OneToMany(mappedBy = "follower", fetch = FetchType.LAZY)
    private Set<SubscriptionEntity> subscriptionsAsFollower = new HashSet<>();

    @OneToMany(mappedBy = "artist", fetch = FetchType.LAZY)
    private Set<SubscriptionEntity> subscriptionsAsArtist = new HashSet<>();

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private Set<TrackEntity> tracks = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<RatingEntity> ratings = new HashSet<>();

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private Set<CommentEntity> comments = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<TrackLikeEntity> likes = new HashSet<>();

    @OneToMany(mappedBy = "complainant", fetch = FetchType.LAZY)
    private Set<ComplaintEntity> complaints = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Set<SubscriptionEntity> getSubscriptionsAsFollower() {
        return subscriptionsAsFollower;
    }

    public void setSubscriptionsAsFollower(Set<SubscriptionEntity> subscriptionsAsFollower) {
        this.subscriptionsAsFollower = subscriptionsAsFollower;
    }

    public Set<SubscriptionEntity> getSubscriptionsAsArtist() {
        return subscriptionsAsArtist;
    }

    public void setSubscriptionsAsArtist(Set<SubscriptionEntity> subscriptionsAsArtist) {
        this.subscriptionsAsArtist = subscriptionsAsArtist;
    }

    public Set<TrackEntity> getTracks() {
        return tracks;
    }

    public void setTracks(Set<TrackEntity> tracks) {
        this.tracks = tracks;
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
}
