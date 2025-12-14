package ru.ifmo.se.pisk.controller.mapper;

import ru.ifmo.se.pisk.controller.dto.AlbumResponse;
import ru.ifmo.se.pisk.controller.dto.AuditResponse;
import ru.ifmo.se.pisk.controller.dto.ComplaintResponse;
import ru.ifmo.se.pisk.controller.dto.GenreResponse;
import ru.ifmo.se.pisk.controller.dto.RatingResponse;
import ru.ifmo.se.pisk.controller.dto.SubscriptionResponse;
import ru.ifmo.se.pisk.controller.dto.TrackResponse;
import ru.ifmo.se.pisk.controller.dto.UserResponse;
import ru.ifmo.se.pisk.domain.AlbumInfo;
import ru.ifmo.se.pisk.domain.AuditRecord;
import ru.ifmo.se.pisk.domain.ComplaintInfo;
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
                profile.avatarPath(),
                profile.role(),
                profile.status(),
                profile.createdAt(),
                profile.lastLoginAt(),
                profile.subscriptionArtistIds()
        );
    }

    public static GenreResponse toGenreResponse(GenreInfo info) {
        return new GenreResponse(info.id(), info.title(), info.description(), info.color(), info.averageRating());
    }

    public static AlbumResponse toAlbumResponse(AlbumInfo info) {
        return new AlbumResponse(
                info.id(),
                info.name(),
                info.description(),
                info.uploadDate(),
                info.coverUrl(),
                info.genreId(),
                info.trackNumber(),
                info.explicit(),
                info.inoagent(),
                info.averageRating(),
                info.labelId(),
                info.publicationDate()
        );
    }

    public static TrackResponse toTrackResponse(TrackInfo info) {
        return new TrackResponse(
                info.id(),
                info.name(),
                info.authorId(),
                info.albumId(),
                info.artistId(),
                info.genreId(),
                info.filepath(),
                info.numberOnAlbum(),
                info.explicit(),
                info.inoagent(),
                info.durationSeconds(),
                info.filesize(),
                info.uploadDate(),
                info.averageRating(),
                info.rateCount(),
                info.likeCount(),
                info.commentCount()
        );
    }

    public static RatingResponse toRatingResponse(RatingInfo info) {
        return new RatingResponse(info.id(), info.trackId(), info.userId(), info.value(), info.comment(), info.createdAt());
    }

    public static SubscriptionResponse toSubscriptionResponse(SubscriptionInfo info) {
        return new SubscriptionResponse(info.id(), info.followerId(), info.artistId(), info.status(), info.createdAt());
    }

    public static ComplaintResponse toComplaintResponse(ComplaintInfo info) {
        return new ComplaintResponse(
                info.id(),
                info.trackId(),
                info.complainantId(),
                info.reason(),
                info.status(),
                info.createdAt(),
                info.resolvedAt(),
                info.moderatorNote()
        );
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
