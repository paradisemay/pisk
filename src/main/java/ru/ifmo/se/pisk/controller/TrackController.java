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
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;

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

    @GetMapping("/{trackId}")
    public TrackResponse getTrack(@PathVariable Long trackId) {
        return ApiMapper.toTrackResponse(trackService.getTrackInfo(trackId));
    }

    @GetMapping("/{trackId}/file")
    public ResponseEntity<ResourceRegion> download(@PathVariable Long trackId, @RequestHeader HttpHeaders headers) {
        var info = trackService.getTrackInfo(trackId);
        Resource resource = fileStorageService.load(info.filepath());
        MediaType mediaType = resolveMediaType(resource, info);
        long contentLength = info.filesize();

        List<HttpRange> ranges = headers.getRange();
        if (ranges == null || ranges.isEmpty()) {
            ResourceRegion region = new ResourceRegion(resource, 0, contentLength);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(mediaType)
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(info))
                    .body(region);
        }

        HttpRange range = ranges.get(0);
        ResourceRegion region = createRegion(resource, contentLength, range);
        long start = region.getPosition();
        long end = start + region.getCount() - 1;
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(mediaType)
                .contentLength(region.getCount())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(info))
                .body(region);
    }

    private MediaType resolveMediaType(Resource resource, ru.ifmo.se.pisk.domain.TrackInfo info) {
        if (info.contentType() != null) {
            try {
                return MediaType.parseMediaType(info.contentType());
            } catch (Exception ignored) {
                // fallback below
            }
        }
        return MediaTypeFactory.getMediaType(info.originalFilename())
                .or(() -> MediaTypeFactory.getMediaType(resource))
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private ResourceRegion createRegion(Resource resource, long contentLength, HttpRange range) {
        long start = range.getRangeStart(contentLength);
        long end = range.getRangeEnd(contentLength);
        long rangeLength = end - start + 1;
        return new ResourceRegion(resource, start, rangeLength);
    }

    private String contentDisposition(ru.ifmo.se.pisk.domain.TrackInfo info) {
        String filename = info.originalFilename();
        if (filename == null) {
            filename = Path.of(info.filepath()).getFileName().toString();
        }
        return ContentDisposition.attachment().filename(filename).build().toString();
    }
}
