package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public interface TrackRepository extends JpaRepository<TrackEntity, Long>, JpaSpecificationExecutor<TrackEntity> {
    Page<TrackEntity> findByStatus(TrackStatus status, Pageable pageable);
    Page<TrackEntity> findByArtist(UserEntity artist, Pageable pageable);
    Optional<TrackEntity> findByIdAndArtist(Long id, UserEntity artist);

    @Query("select t from TrackEntity t join t.genres g where g.id in :genreIds")
    Page<TrackEntity> findByGenres(@Param("genreIds") Iterable<Long> genreIds, Pageable pageable);

    @Query("select t from TrackEntity t where lower(t.title) like lower(concat('%', :query, '%'))")
    Page<TrackEntity> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query("select new ru.ifmo.se.pisk.repository.projection.ArtistRatingAggregate(t.artist.id, avg(r.value), count(r)) "
            + "from TrackEntity t join t.ratings r group by t.artist.id order by avg(r.value) desc")
    List<ru.ifmo.se.pisk.repository.projection.ArtistRatingAggregate> findTopArtistsByAverageRating(Pageable pageable);
}
