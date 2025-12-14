package ru.ifmo.se.pisk.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.domain.UserRole;
import ru.ifmo.se.pisk.repository.specification.TrackSpecifications;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
class TrackRepositoryTest {

    private static final boolean DOCKER_AVAILABLE;

    static {
        boolean available;
        try {
            DockerClientFactory.instance().client();
            available = true;
        } catch (Throwable throwable) {
            available = false;
        }
        DOCKER_AVAILABLE = available;
    }

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "Docker недоступен для интеграционных тестов репозитория");
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserEntity author1;
    private UserEntity author2;
    private GenreEntity genreRock;
    private GenreEntity genrePop;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "Docker недоступен для интеграционных тестов репозитория");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm;");
        trackRepository.deleteAll();
        entityManager.flush();
        createReferenceData();
    }

    @Test
    void fullTextSearchMatchesNameAndDescription() {
        TrackEntity match = persistTrack("Bright Noise", "Shoegaze and dream pop textures", genreRock, author1, TrackStatus.PUBLISHED, false, false, 10, 2);
        persistTrack("Other Song", "Plain rock", genreRock, author1, TrackStatus.PUBLISHED, false, false, 5, 1);

        var spec = TrackSpecifications.fullTextSearch("shoegaze");
        var page = trackRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(TrackEntity::getId).contains(match.getId());
    }

    @Test
    void trigramSearchAllowsSubstrings() {
        TrackEntity match = persistTrack("Midnight Run", "", genrePop, author1, TrackStatus.PUBLISHED, false, false, 3, 1);
        persistTrack("Dawn Light", "", genrePop, author2, TrackStatus.PUBLISHED, false, false, 1, 1);

        var spec = TrackSpecifications.fullTextSearch("night");
        var page = trackRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(TrackEntity::getId).contains(match.getId());
    }

    @Test
    void filtersByAuthorGenreStatusAndFlags() {
        persistTrack("First", "", genreRock, author1, TrackStatus.PUBLISHED, false, false, 2, 1);
        persistTrack("Second", "", genrePop, author1, TrackStatus.PUBLISHED, true, false, 2, 1);
        persistTrack("Third", "", genreRock, author2, TrackStatus.DRAFT, false, false, 2, 1);

        Specification<TrackEntity> spec = Specification.where(TrackSpecifications.hasAuthor(author1.getId()))
                .and(TrackSpecifications.hasGenres(Set.of(genreRock.getId())))
                .and(TrackSpecifications.hasStatus(TrackStatus.PUBLISHED))
                .and(TrackSpecifications.isExplicit(false))
                .and(TrackSpecifications.isInoagent(false));

        var page = trackRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(TrackEntity::getName).containsExactly("First");
    }

    @Test
    void respectsSortingAndPagination() {
        TrackEntity high = persistTrack("High", "", genreRock, author1, TrackStatus.PUBLISHED, false, false, 100, 10);
        persistTrack("Low", "", genreRock, author1, TrackStatus.PUBLISHED, false, false, 10, 5);

        var firstPage = trackRepository.findAll(Specification.where(null), PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "rating")));
        var secondPage = trackRepository.findAll(Specification.where(null), PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "rating")));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(TrackEntity::getId).containsExactly(high.getId());
        assertThat(secondPage.getContent()).hasSize(1);
    }

    private void createReferenceData() {
        author1 = new UserEntity();
        author1.setUsername("user1");
        author1.setEmail("user1@example.com");
        author1.setPasswordHash("pass1");
        author1.setRole(UserRole.USER);
        entityManager.persist(author1);

        author2 = new UserEntity();
        author2.setUsername("user2");
        author2.setEmail("user2@example.com");
        author2.setPasswordHash("pass2");
        author2.setRole(UserRole.USER);
        entityManager.persist(author2);

        genreRock = new GenreEntity();
        genreRock.setName("Rock");
        genreRock.setDescription("Loud guitars");
        entityManager.persist(genreRock);

        genrePop = new GenreEntity();
        genrePop.setName("Pop");
        genrePop.setDescription("Pop songs");
        entityManager.persist(genrePop);
    }

    private TrackEntity persistTrack(String name,
                                     String albumDescription,
                                     GenreEntity genre,
                                     UserEntity author,
                                     TrackStatus status,
                                     boolean explicit,
                                     boolean inoagent,
                                     long rating,
                                     int rateCount) {
        AlbumEntity album = new AlbumEntity();
        album.setName(name + " album");
        album.setDescription(albumDescription == null ? "" : albumDescription);
        album.setGenre(genre);
        album.setTrackNumber(10);
        album.setExplicit(explicit);
        album.setInoagent(inoagent);
        album.setAverageRating(0);
        album.setUploadDate(Instant.now());
        entityManager.persist(album);

        TrackEntity track = new TrackEntity();
        track.setName(name);
        track.setAlbum(album);
        track.setGenre(genre);
        track.setAuthor(author);
        track.setStoragePath("/storage/" + name);
        track.setDurationSeconds(180);
        track.setFileSize(5120);
        track.setNumberOnAlbum(1);
        track.setStatus(status);
        track.setExplicit(explicit);
        track.setInoagent(inoagent);
        track.setRating(rating);
        track.setRateCount(rateCount);
        entityManager.persist(track);
        entityManager.flush();
        return track;
    }
}
