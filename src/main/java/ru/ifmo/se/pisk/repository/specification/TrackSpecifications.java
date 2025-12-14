package ru.ifmo.se.pisk.repository.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;

import java.util.Set;

public final class TrackSpecifications {

    private TrackSpecifications() {
    }

    public static Specification<TrackEntity> fullTextSearch(String query) {
        return (root, criteriaQuery, builder) -> {
            criteriaQuery.distinct(true);
            if (query == null || query.isBlank()) {
                return builder.conjunction();
            }
            Join<TrackEntity, AlbumEntity> album = root.join("album", JoinType.LEFT);
            var searchableText = builder.function(
                    "concat_ws",
                    String.class,
                    builder.literal(" "),
                    root.get("name"),
                    album.get("description")
            );
            var vector = builder.function(
                    "to_tsvector",
                    String.class,
                    builder.literal("simple"),
                    searchableText
            );
            var tsQuery = builder.function(
                    "plainto_tsquery",
                    String.class,
                    builder.literal("simple"),
                    builder.literal(query)
            );
            var rank = builder.function("ts_rank", Double.class, vector, tsQuery);
            var trigramSimilarity = builder.function(
                    "similarity",
                    Double.class,
                    builder.lower(root.get("name")),
                    builder.literal(query.toLowerCase())
            );
            Predicate matchesByRank = builder.greaterThan(rank, 0d);
            Predicate matchesByTrigram = builder.greaterThan(trigramSimilarity, 0.2d);
            return builder.or(matchesByRank, matchesByTrigram);
        };
    }

    public static Specification<TrackEntity> hasGenres(Set<Long> genreIds) {
        return (root, query, builder) -> {
            if (genreIds == null || genreIds.isEmpty()) {
                return builder.conjunction();
            }
            return root.get("genre").get("id").in(genreIds);
        };
    }

    public static Specification<TrackEntity> hasAuthor(Long authorId) {
        return (root, query, builder) -> {
            if (authorId == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("author").get("id"), authorId);
        };
    }

    public static Specification<TrackEntity> hasStatus(TrackStatus status) {
        return (root, query, builder) -> {
            if (status == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("status"), status);
        };
    }

    public static Specification<TrackEntity> isExplicit(Boolean explicit) {
        return (root, query, builder) -> {
            if (explicit == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("explicit"), explicit);
        };
    }

    public static Specification<TrackEntity> isInoagent(Boolean inoagent) {
        return (root, query, builder) -> {
            if (inoagent == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("inoagent"), inoagent);
        };
    }
}
