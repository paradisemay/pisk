package ru.ifmo.se.pisk.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.domain.entity.ArtistEntity;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.service.ArtistService;
import ru.ifmo.se.pisk.service.CommentService;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrackPageController.class)
public class TrackPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackService trackService;

    @MockBean
    private UserService userService;

    @MockBean
    private GenreService genreService;

    @MockBean
    private ArtistService artistService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private CommentService commentService;

    @Test
    @WithMockUser
    public void trackPageShouldDisplayArtistName() throws Exception {
        Long trackId = 1L;
        Long authorId = 10L;
        Long genreId = 20L;
        Long artistId = 30L;
        String trackName = "Test Track";
        String authorName = "Test Author";
        String genreName = "Test Genre";
        String artistName = "Test Artist";

        TrackInfo trackInfo = new TrackInfo(
                trackId,
                trackName,
                authorId,
                null, // albumId
                artistId,
                genreId,
                null,
                "path/to/file",
                "audio/mpeg",
                "file.mp3",
                1,
                false,
                false,
                120,
                1024,
                Instant.now(),
                4.5,
                10,
                5,
                2
        );

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(authorName);

        GenreEntity genreEntity = new GenreEntity();
        genreEntity.setName(genreName);

        ArtistEntity artistEntity = new ArtistEntity();
        artistEntity.setName(artistName);

        given(trackService.getTrackInfo(trackId)).willReturn(trackInfo);
        given(userService.requireUser(authorId)).willReturn(userEntity);
        given(genreService.requireGenre(genreId)).willReturn(genreEntity);
        given(artistService.requireArtist(artistId)).willReturn(artistEntity);

        // Mock current user lookup
        given(userService.findByUsername(org.mockito.ArgumentMatchers.anyString()))
                .willReturn(java.util.Optional.of(userEntity));

        mockMvc.perform(get("/tracks/{id}", trackId))
                .andExpect(status().isOk())
                .andExpect(view().name("track"))
                .andExpect(model().attributeExists("track"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(artistName)));
    }

    @Test
    @WithMockUser(username = "tester")
    public void rateTrackShouldCallServiceAndRedirect() throws Exception {
        Long trackId = 1L;
        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setUsername("tester");

        given(userService.findByUsername("tester")).willReturn(java.util.Optional.of(user));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tracks/{id}/rate", trackId)
                        .param("rating", "5")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tracks/" + trackId));

        org.mockito.Mockito.verify(trackService).rateTrack(org.mockito.ArgumentMatchers.argThat(cmd ->
            cmd.trackId().equals(trackId) && cmd.userId().equals(100L) && cmd.value() == 5
        ));
    }
}
