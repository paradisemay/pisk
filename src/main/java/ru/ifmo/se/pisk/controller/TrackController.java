package ru.ifmo.se.pisk.controller;

import jakarta.validation.Valid;
import java.io.InputStream;
import java.io.IOException;
import java.util.Set;
import java.util.function.Supplier;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.ifmo.se.pisk.controller.dto.CreateRatingRequest;
import ru.ifmo.se.pisk.controller.dto.RatingResponse;
import ru.ifmo.se.pisk.controller.dto.TrackResponse;
import ru.ifmo.se.pisk.controller.dto.TrackStatusUpdateRequest;
import ru.ifmo.se.pisk.controller.mapper.ApiMapper;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import org.springframework.core.io.InputStreamResource;

@RestController
@RequestMapping("/api/v1/tracks")
public class TrackController {

    private final TrackService trackService;
    private final FileStorageService fileStorageService;

    public TrackController(TrackService trackService, FileStorageService fileStorageService) {
        this.trackService = trackService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrackResponse> upload(@RequestParam Long artistId,
                                                @RequestParam String name,
                                                @RequestParam long durationSeconds,
                                                @RequestParam(required = false) Long albumId,
                                                @RequestParam(required = false) Long genreId,
                                                @RequestParam(defaultValue = "false") boolean explicit,
                                                @RequestParam(defaultValue = "false") boolean inoagent,
                                                @RequestParam(defaultValue = "0") int numberOnAlbum,
                                                @RequestParam("file") MultipartFile file) {
        fileStorageService.validate(file.getOriginalFilename(), file.getContentType(), file.getSize());
        Supplier<InputStream> supplier = () -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось прочитать загруженный файл", e);
            }
        };
        TrackUploadCommand command = new TrackUploadCommand(
                name,
                durationSeconds,
                albumId,
                genreId,
                explicit,
                inoagent,
                numberOnAlbum,
                file.getContentType(),
                file.getSize(),
                file.getOriginalFilename(),
                supplier
        );
        var info = trackService.uploadTrack(artistId, command);
        return ResponseEntity.ok(ApiMapper.toTrackResponse(info));
    }

    @GetMapping("/{trackId}/stream")
    public ResponseEntity<Resource> streamTrack(
            @PathVariable Long trackId,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        TrackEntity track = trackService.requireTrack(trackId);
        if (track.getStatus() != TrackStatus.PUBLISHED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Resource resource = fileStorageService.load(track.getStoragePath());
        long fileSize = track.getFileSize();
        long start = 0, end = fileSize - 1;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            try {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isBlank()) {
                    end = Long.parseLong(ranges[1]);
                }
            } catch (Exception ignore) {}
        }
        if (end >= fileSize) end = fileSize - 1;
        long contentLength = end - start + 1;
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, track.getContentType());
        responseHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
        responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(track.getOriginalFilename()).build().toString());
        if (rangeHeader != null) {
            responseHeaders.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
        }
        ResponseEntity.BodyBuilder builder = (rangeHeader != null)
                ? ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                : ResponseEntity.ok();
        try {
            if (start == 0) trackService.incrementPlayCount(trackId);
            InputStream raw = resource.getInputStream();
            raw.skip(start);
            return builder.headers(responseHeaders)
                    .body(new InputStreamResource(raw));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{trackId}/status")
    public TrackResponse updateStatus(@PathVariable Long trackId, @Valid @RequestBody TrackStatusUpdateRequest request) {
        return ApiMapper.toTrackResponse(trackService.changeStatus(trackId, request.status(), request.moderatorId()));
    }

    @PostMapping("/{trackId}/ratings")
    public RatingResponse rateTrack(@PathVariable Long trackId, @Valid @RequestBody CreateRatingRequest request) {
        RatingCommand command = new RatingCommand(trackId, request.userId(), request.value(), request.comment());
        return ApiMapper.toRatingResponse(trackService.rateTrack(command));
    }

    @PostMapping("/{trackId}/plays")
    public TrackResponse registerPlay(@PathVariable Long trackId) {
        return ApiMapper.toTrackResponse(trackService.incrementPlayCount(trackId));
    }

    @GetMapping
    public Page<TrackResponse> findTracks(@RequestParam(required = false) Boolean explicit,
                                          @RequestParam(required = false) Boolean inoagent,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) Set<Long> genreIds,
                                          Pageable pageable) {
        if (title != null || (genreIds != null && !genreIds.isEmpty())) {
            return trackService.searchTracks(title, genreIds, pageable).map(ApiMapper::toTrackResponse);
        }
        return trackService.findTracks(explicit, inoagent, pageable).map(ApiMapper::toTrackResponse);
    }
    // ... остальной код контроллера без изменений ...
}
