package ru.ifmo.se.pisk.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;
import ru.ifmo.se.pisk.repository.projection.AlbumCardAggregate;

public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {
    List<AlbumEntity> findByLabel(LabelEntity label);

    @Query("select a from AlbumEntity a where a.publicationDate between :start and :end")
    List<AlbumEntity> findReleasedBetween(@Param("start") Instant start, @Param("end") Instant end);

    Optional<AlbumEntity> findByNameIgnoreCase(String name);

    @Query("select a from AlbumEntity a where lower(a.name) like lower(concat('%', :namePart, '%'))")
    List<AlbumEntity> searchByName(@Param("namePart") String namePart);

    @Query("select count(t) from TrackEntity t where t.album.id = :albumId")
    int countTracksInAlbum(@Param("albumId") Long albumId);

    @Query("""
            select new ru.ifmo.se.pisk.repository.projection.AlbumCardAggregate(
                a.id,
                a.name,
                a.coverUrl,
                a.uploadDate,
                a.trackNumber,
                a.explicit,
                a.inoagent,
                a.averageRating,
                g.id,
                g.name,
                u.id,
                u.username,
                ar.id,
                ar.name
            )
            from AlbumEntity a
            left join a.genre g
            left join TrackEntity t on t.album = a and t.numberOnAlbum = 1
            left join t.author u
            left join t.artist ar
            where (:title is null or :title = '' or lower(a.name) like lower(concat('%', :title, '%')))
              and (:explicit is null or a.explicit = :explicit)
              and (:inoagent is null or a.inoagent = :inoagent)
              and (:genreIdsEmpty = true or g.id in :genreIds)
            """)
    Page<AlbumCardAggregate> searchAlbumCards(
            @Param("title") String title,
            @Param("explicit") Boolean explicit,
            @Param("inoagent") Boolean inoagent,
            @Param("genreIds") java.util.Set<Long> genreIds,
            @Param("genreIdsEmpty") boolean genreIdsEmpty,
            Pageable pageable
    );
}
