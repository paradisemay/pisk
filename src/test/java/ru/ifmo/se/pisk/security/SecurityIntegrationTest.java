package ru.ifmo.se.pisk.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import ru.ifmo.se.pisk.config.SecurityConfig;
import ru.ifmo.se.pisk.controller.AuditController;
import ru.ifmo.se.pisk.controller.ComplaintController;
import ru.ifmo.se.pisk.controller.SubscriptionController;
import ru.ifmo.se.pisk.controller.TrackController;
import ru.ifmo.se.pisk.controller.UserController;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.UserProfile;
import ru.ifmo.se.pisk.domain.UserRole;
import ru.ifmo.se.pisk.domain.UserStatus;
import ru.ifmo.se.pisk.service.AuditService;
import ru.ifmo.se.pisk.service.ComplaintService;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.SubscriptionService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import ru.ifmo.se.pisk.repository.UserRepository;

@WebMvcTest(controllers = {
        TrackController.class,
        UserController.class,
        AuditController.class,
        ComplaintController.class,
        SubscriptionController.class
})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackService trackService;
    @MockBean
    private FileStorageService fileStorageService;
    @MockBean
    private UserService userService;
    @MockBean
    private AuditService auditService;
    @MockBean
    private ComplaintService complaintService;
    @MockBean
    private SubscriptionService subscriptionService;
    @MockBean
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setupMocks() {
        Mockito.when(trackService.findTracks(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Page.empty());
    }

    @Test
    void registrationIsPublicWithCsrf() throws Exception {
        UserProfile profile = new UserProfile(1L, "newuser", "new@example.com", UserRole.USER, UserStatus.PENDING, Instant.now(), null, Set.of());
        Mockito.when(userService.registerUser(Mockito.eq("newuser"), Mockito.eq("new@example.com"), Mockito.eq("password123"), Mockito.eq(UserRole.USER)))
                .thenReturn(profile);
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"email\":\"new@example.com\",\"password\":\"password123\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void loginIssuesSessionCookie() throws Exception {
        var entity = new ru.ifmo.se.pisk.domain.entity.UserEntity();
        entity.setUsername("user");
        entity.setEmail("user@mail");
        entity.setPasswordHash(passwordEncoder.encode("secret"));
        entity.setRole(UserRole.USER);
        entity.setStatus(UserStatus.ACTIVE);
        Mockito.when(userRepository.findByUsername("user")).thenReturn(Optional.of(entity));
        Mockito.when(userRepository.findByEmail("user")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "user")
                        .param("password", "secret"))
                .andExpect(status().isFound())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotNull(result.getRequest().getSession(false)));
    }

    @Test
    void uploadForbiddenForAnonymous() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp3", "audio/mpeg", "data".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/tracks")
                        .file(file)
                        .param("artistId", "1")
                        .param("name", "song")
                        .param("durationSeconds", "10")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadAllowedForUserRole() throws Exception {
        TrackInfo info = new TrackInfo(10L, "song", 1L, null, 1L, null, "file", 0, false, false, 10, 100, null, 0, 0, 0, 0);
        Mockito.when(trackService.uploadTrack(Mockito.eq(1L), Mockito.any(TrackUploadCommand.class))).thenReturn(info);
        MockMultipartFile file = new MockMultipartFile("file", "test.mp3", "audio/mpeg", "data".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/tracks")
                        .file(file)
                        .param("artistId", "1")
                        .param("name", "song")
                        .param("durationSeconds", "10")
                        .with(csrf())
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointDeniedForUser() throws Exception {
        Mockito.when(auditService.findLast(50)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/audit").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointAllowedForAdmin() throws Exception {
        Mockito.when(auditService.findLast(50)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/audit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
