package ru.ifmo.se.pisk.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
import ru.ifmo.se.pisk.controller.util.PageRequestFactory;

@RestController
@RequestMapping("/api/v1/tracks")
public class TrackController {

    /**
     * Поля сортировки, доступные через query-параметры sort/direction.
     */
    private static final Set<String> SORTABLE_FIELDS = Set.of("uploadedAt", "rating", "rateCount", "playCount", "name");

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
                                          @RequestParam(required = false, name = "query") String search,
                                          @RequestParam(required = false) Set<Long> genreIds,
                                          @RequestParam(required = false) Long authorId,
                                          @RequestParam(required = false) TrackStatus status,
                                          @RequestParam(defaultValue = "uploadedAt") String sort,
                                          @RequestParam(defaultValue = "DESC") Sort.Direction direction,
                                          @RequestParam(defaultValue = "0") Integer page,
                                          @RequestParam(required = false) Integer size) {
        TrackSearchCriteria criteria = new TrackSearchCriteria(search, genreIds, authorId, status, explicit, inoagent);
        Pageable pageable = PageRequestFactory.build(page, size, sort, direction, SORTABLE_FIELDS);
        return trackService.searchTracks(criteria, pageable).map(ApiMapper::toTrackResponse);
    }

    @GetMapping("/{trackId}")
    public TrackResponse getTrack(@PathVariable Long trackId) {
        return ApiMapper.toTrackResponse(trackService.getTrackInfo(trackId));
    }

    @GetMapping(value = "/{trackId}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> download(@PathVariable Long trackId) {
        var info = trackService.getTrackInfo(trackId);
        Resource resource = fileStorageService.load(info.filepath());
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentLength(info.filesize())
                .contentType(mediaType)
                .header("Content-Disposition", "attachment; filename=" + info.id() + ".bin")
                .body(resource);
    }

}
