package ru.ifmo.se.pisk.domain.mapper;

import java.util.Map;
import java.util.stream.Collectors;
import ru.ifmo.se.pisk.domain.AlbumInfo;
import ru.ifmo.se.pisk.domain.AuditRecord;
import ru.ifmo.se.pisk.domain.ComplaintInfo;
import ru.ifmo.se.pisk.domain.GenreInfo;
import ru.ifmo.se.pisk.domain.RatingInfo;
import ru.ifmo.se.pisk.domain.SubscriptionInfo;
import ru.ifmo.se.pisk.domain.SubscriptionStatus;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.UserProfile;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.AuditEntity;
import ru.ifmo.se.pisk.domain.entity.ComplaintEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.RatingEntity;
import ru.ifmo.se.pisk.domain.entity.SubscriptionEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public final class DomainMapper {

    private DomainMapper() {
    }

    public static UserProfile toUserProfile(UserEntity entity) {
        return new UserProfile(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getRole(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getLastLoginAt(),
                entity.getSubscriptionsAsFollower().stream()
                        .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                        .map(subscription -> subscription.getArtist().getId())
                        .collect(Collectors.toUnmodifiableSet())
        );
    }

    public static GenreInfo toGenreInfo(GenreEntity entity) {
        return new GenreInfo(entity.getId(), entity.getName(), entity.getDescription(), null, 0);
    }

    public static AlbumInfo toAlbumInfo(AlbumEntity entity) {
        return new AlbumInfo(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getReleaseDate() != null ? entity.getReleaseDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC) : null,
                entity.getCoverUrl(),
                entity.getGenres().stream().findFirst().map(GenreEntity::getId).orElse(null),
                entity.getTracks() != null ? entity.getTracks().size() : 0,
                false,
                false,
                0,
                entity.getOwner() != null ? entity.getOwner().getId() : null,
                entity.getReleaseDate() != null ? entity.getReleaseDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC) : null
        );
    }

    public static TrackInfo toTrackInfo(TrackEntity entity) {
        return new TrackInfo(
                entity.getId(),
                entity.getTitle(),
                entity.getArtist() != null ? entity.getArtist().getId() : null,
                entity.getAlbum() != null ? entity.getAlbum().getId() : null,
                null,
                entity.getGenres().stream().findFirst().map(GenreEntity::getId).orElse(null),
                entity.getStoragePath(),
                0,
                false,
                false,
                entity.getDurationSeconds(),
                entity.getFileSize(),
                entity.getUploadedAt(),
                entity.getAverageRating(),
                (int) entity.getRatingsCount()
        );
    }

    public static RatingInfo toRatingInfo(RatingEntity entity) {
        return new RatingInfo(
                entity.getId(),
                entity.getTrack().getId(),
                entity.getUser().getId(),
                entity.getValue(),
                entity.getComment(),
                entity.getCreatedAt()
        );
    }

    public static SubscriptionInfo toSubscriptionInfo(SubscriptionEntity entity) {
        return new SubscriptionInfo(
                entity.getId(),
                entity.getFollower().getId(),
                entity.getArtist().getId(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public static ComplaintInfo toComplaintInfo(ComplaintEntity entity) {
        return new ComplaintInfo(
                entity.getId(),
                entity.getTrack().getId(),
                entity.getComplainant().getId(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getResolvedAt(),
                entity.getModeratorNote()
        );
    }

    public static AuditRecord toAuditRecord(AuditEntity entity) {
        Map<String, String> metadata = entity.getMetadata() == null
                ? Map.of()
                : Map.copyOf(entity.getMetadata());
        return new AuditRecord(
                entity.getId(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getActor().getId(),
                entity.getOccurredAt(),
                metadata
        );
    }
}
