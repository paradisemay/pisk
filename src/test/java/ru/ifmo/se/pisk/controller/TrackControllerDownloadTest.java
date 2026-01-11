package ru.ifmo.se.pisk.controller;

import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.service.AudioMetadataService;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.TrackService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrackControllerDownloadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackService trackService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private AudioMetadataService audioMetadataService;

    @Test
    void shouldReturnFullContentWithHeadersWhenRangeNotProvided() throws Exception {
        byte[] data = "Sample audio content".getBytes();
        Resource resource = new ByteArrayResource(data);
        TrackInfo info = new TrackInfo(
                1L,
                "track",
                2L,
                3L,
                4L,
                5L,
                null,
                "tracks/1/track.mp3",
                "audio/mpeg",
                "track.mp3",
                1,
                false,
                false,
                120,
                data.length,
                Instant.now(),
                0,
                0,
                0,
                0
        );
        when(trackService.getTrackInfo(1L)).thenReturn(info);
        when(fileStorageService.load(info.filepath())).thenReturn(resource);

        mockMvc.perform(get("/api/v1/tracks/1/file"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"track.mp3\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "audio/mpeg"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, data.length))
                .andExpect(content().bytes(data));
    }

    @Test
    void shouldReturnPartialContentWhenRangeProvided() throws Exception {
        byte[] data = "Range response data".getBytes();
        Resource resource = new ByteArrayResource(data);
        TrackInfo info = new TrackInfo(
                10L,
                "range-track",
                2L,
                3L,
                4L,
                5L,
                null,
                "tracks/1/range-track.mp3",
                "audio/mpeg",
                "range-track.mp3",
                1,
                false,
                false,
                120,
                data.length,
                Instant.now(),
                0,
                0,
                0,
                0
        );
        when(trackService.getTrackInfo(10L)).thenReturn(info);
        when(fileStorageService.load(info.filepath())).thenReturn(resource);

        mockMvc.perform(get("/api/v1/tracks/10/file").header(HttpHeaders.RANGE, "bytes=0-4"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-4/" + data.length))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"range-track.mp3\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "audio/mpeg"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 5))
                .andExpect(content().bytes(Arrays.copyOfRange(data, 0, 5)));
    }
}
