package ru.ifmo.se.pisk.controller.mapper;

import ru.ifmo.se.pisk.controller.dto.AlbumResponse;
import ru.ifmo.se.pisk.controller.dto.AuditResponse;
import ru.ifmo.se.pisk.controller.dto.GenreResponse;
import ru.ifmo.se.pisk.controller.dto.RatingResponse;
import ru.ifmo.se.pisk.controller.dto.SubscriptionResponse;
import ru.ifmo.se.pisk.controller.dto.TrackResponse;
import ru.ifmo.se.pisk.controller.dto.UserResponse;
import ru.ifmo.se.pisk.domain.AlbumInfo;
import ru.ifmo.se.pisk.domain.AuditRecord;
import ru.ifmo.se.pisk.domain.GenreInfo;
import ru.ifmo.se.pisk.domain.RatingInfo;
import ru.ifmo.se.pisk.domain.SubscriptionInfo;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.UserProfile;

public final class ApiMapper {

    private ApiMapper() {
    }

    public static UserResponse toUserResponse(UserProfile profile) {
        return new UserResponse(
                profile.id(),
                profile.username(),
                profile.email(),
                profile.role(),
                profile.status(),
                profile.createdAt(),
                profile.lastLoginAt(),
                profile.subscriptionArtistIds()
        );
    }

    public static GenreResponse toGenreResponse(GenreInfo info) {
        return new GenreResponse(info.id(), info.name(), info.description());
    }

    public static AlbumResponse toAlbumResponse(AlbumInfo info) {
        return new AlbumResponse(
                info.id(),
                info.title(),
                info.description(),
                info.releaseDate(),
                info.ownerId(),
                info.genreIds(),
                info.coverUrl()
        );
    }

    public static TrackResponse toTrackResponse(TrackInfo info) {
        return new TrackResponse(
                info.id(),
                info.title(),
                info.duration(),
                info.status(),
                info.albumId(),
                info.artistId(),
                info.genreIds(),
                info.storagePath(),
                info.contentType(),
                info.fileSize(),
                info.uploadedAt(),
                info.publishedAt(),
                info.averageRating(),
                info.ratingsCount(),
                info.playCount()
        );
    }

    public static RatingResponse toRatingResponse(RatingInfo info) {
        return new RatingResponse(info.id(), info.trackId(), info.userId(), info.value(), info.comment(), info.createdAt());
    }

    public static SubscriptionResponse toSubscriptionResponse(SubscriptionInfo info) {
        return new SubscriptionResponse(info.id(), info.followerId(), info.artistId(), info.status(), info.createdAt());
    }

    public static AuditResponse toAuditResponse(AuditRecord record) {
        return new AuditResponse(
                record.id(),
                record.action(),
                record.entityType(),
                record.entityId(),
                record.actorId(),
                record.occurredAt(),
                record.metadata()
        );
    }
}
