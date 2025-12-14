package ru.ifmo.se.pisk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ifmo.se.pisk.domain.Album;
import ru.ifmo.se.pisk.domain.Artist;
import ru.ifmo.se.pisk.domain.Genre;
import ru.ifmo.se.pisk.domain.Label;
import ru.ifmo.se.pisk.repository.UserRepository;
import ru.ifmo.se.pisk.service.AdminService;
import ru.ifmo.se.pisk.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Testcontainers(disabledWithoutDocker = true)
public class ApplicationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("pisk")
            .withUsername("pisk")
            .withPassword("pisk");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.storage-path", () -> "target/test-storage");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AdminService adminService;

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ObjectMapper objectMapper;

    private Genre genre;
    private Artist artist;
    private Label label;
    private Album album;

    @BeforeEach
    void setUp() {
        if (genre == null) {
            genre = adminService.createGenre("Rock", "", "#ffffff");
        }
        if (label == null) {
            label = adminService.createLabel("Indie", "");
        }
        if (artist == null) {
            artist = adminService.createArtist("Tester", "");
        }
        if (album == null) {
            album = adminService.createAlbum("Demo", "desc", genre.getId(), 1, false, false, label.getId(), Instant.now());
        }
    }

    @Test
    void registrationAndLogin() throws Exception {
        String username = "john" + System.currentTimeMillis();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pwd\"}"))
                .andExpect(status().isOk());

        String authHeader = basic(username, "pwd");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pwd\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadSearchAndRateFlow() throws Exception {
        String username = "alice" + System.currentTimeMillis();
        String password = "pwd";
        authService.register(username, password);
        String authHeader = basic(username, password);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/tracks")
                        .file("file", "data".getBytes(StandardCharsets.UTF_8))
                        .param("albumId", album.getId().toString())
                        .param("artistId", artist.getId().toString())
                        .param("genreId", genre.getId().toString())
                        .param("name", "My Song")
                        .param("numberOnAlbum", "1")
                        .param("duration", "120")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode track = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long trackId = track.get("id").asLong();

        MvcResult search = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tracks").param("q", "My"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(search.getResponse().getContentAsString()).contains("My Song");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tracks/" + trackId + "/rating")
                        .header("Authorization", authHeader)
                        .param("rate", "5"))
                .andExpect(status().isOk());
    }

    private String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
