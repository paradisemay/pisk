package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.repository.projection.TrackSearchResult;

public interface TrackRepository extends JpaRepository<TrackEntity, Long>, JpaSpecificationExecutor<TrackEntity> {

    @Query(value = "SELECT * FROM fn_search_tracks(" +
            "cast(:query as text), " +
            "cast(:genreIds as bigint[]), " +
            "cast(:artistIds as bigint[]), " +
            "cast(:labelIds as bigint[]), " +
            "cast(:explicit as boolean), " +
            "cast(:sort as text), " +
            ":limit, " +
            ":offset)", nativeQuery = true)
    List<TrackSearchResult> searchTracks(
            @Param("query") String query,
            @Param("genreIds") Long[] genreIds,
            @Param("artistIds") Long[] artistIds,
            @Param("labelIds") Long[] labelIds,
            @Param("explicit") Boolean explicit,
            @Param("sort") String sort,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    Page<TrackEntity> findByStatus(TrackStatus status, Pageable pageable);
    Page<TrackEntity> findByAuthor(UserEntity author, Pageable pageable);
    Optional<TrackEntity> findByIdAndAuthor(Long id, UserEntity author);

    Page<TrackEntity> findByGenre_Id(Long genreId, Pageable pageable);

    java.util.List<TrackEntity> findByAlbum(AlbumEntity album);

    @Query("select t from TrackEntity t where lower(t.name) like lower(concat('%', :query, '%'))")
    Page<TrackEntity> searchByName(@Param("query") String query, Pageable pageable);

    @Query("select new ru.ifmo.se.pisk.repository.projection.ArtistRatingAggregate(t.author.id, "
            + "case when sum(t.rateCount) = 0 then 0.0 else (sum(t.rating) * 1.0) / sum(t.rateCount) end, "
            + "sum(t.rateCount)) from TrackEntity t group by t.author.id order by "
            + "case when sum(t.rateCount) = 0 then 0.0 else (sum(t.rating) * 1.0) / sum(t.rateCount) end desc")
    List<ru.ifmo.se.pisk.repository.projection.ArtistRatingAggregate> findTopArtistsByAverageRating(Pageable pageable);

    @Query(value = "SELECT * FROM fn_track_rate(:userId, :trackId, CAST(:rate AS SMALLINT))", nativeQuery = true)
    List<Object[]> rateTrack(@Param("userId") Long userId, @Param("trackId") Long trackId, @Param("rate") Integer rate);
}
