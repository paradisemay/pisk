package ru.ifmo.se.pisk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import ru.ifmo.se.pisk.service.AudioMetadataService;
import ru.ifmo.se.pisk.service.AudioMetadataService.AudioMetadata;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.dto.RatingCommand;
import ru.ifmo.se.pisk.service.dto.TrackUploadCommand;
import org.springframework.core.io.InputStreamResource;
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
import ru.ifmo.se.pisk.controller.util.PageRequestFactory;

@RestController
@Tag(name = "Tracks", description = "Управление треками и их статусами")
@RequestMapping("/api/v1/tracks")
public class TrackController {

    /**
     * Поля сортировки, доступные через query-параметры sort/direction.
     */
    private static final Set<String> SORTABLE_FIELDS = Set.of("uploadedAt", "rating", "rateCount", "playCount", "name");

    private final TrackService trackService;
    private final FileStorageService fileStorageService;
    private final AudioMetadataService audioMetadataService;

    public TrackController(TrackService trackService, FileStorageService fileStorageService,
                           AudioMetadataService audioMetadataService) {
        this.trackService = trackService;
        this.fileStorageService = fileStorageService;
        this.audioMetadataService = audioMetadataService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить трек", description = "Загружает аудиофайл и сохраняет метаданные трека")
    public ResponseEntity<TrackResponse> upload(@RequestParam Long artistId,
                                                @RequestParam String name,
                                                @RequestParam String artistName,
                                                @RequestParam String genreName,
                                                @RequestParam(required = false) String labelName,
                                                @RequestParam(required = false) String albumName,
                                                @RequestParam(defaultValue = "false") boolean explicit,
                                                @RequestParam(defaultValue = "false") boolean inoagent,
                                                @Parameter(description = "Файл аудио") @RequestParam("file") MultipartFile file) {
        fileStorageService.validate(file.getOriginalFilename(), file.getContentType(), file.getSize());
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать загруженный файл", e);
        }
        AudioMetadata metadata = audioMetadataService.extract(content, file.getOriginalFilename(), file.getContentType());
        int trackNumber = metadata.trackNumber() != null ? metadata.trackNumber() : 1;
        TrackUploadCommand command = new TrackUploadCommand(
                name,
                artistName,
                genreName,
                labelName,
                albumName,
                metadata.durationSeconds(),
                explicit,
                inoagent,
                trackNumber,
                file.getContentType(),
                content.length,
                file.getOriginalFilename(),
                () -> new ByteArrayInputStream(content),
                metadata.coverData(),
                metadata.coverContentType()
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
    @Operation(summary = "Изменить статус", description = "Обновляет статус трека модератором")
    public TrackResponse updateStatus(@PathVariable Long trackId, @Valid @RequestBody TrackStatusUpdateRequest request) {
        return ApiMapper.toTrackResponse(trackService.changeStatus(trackId, request.status(), request.moderatorId()));
    }

    @PostMapping("/{trackId}/ratings")
    @Operation(summary = "Поставить оценку", description = "Сохраняет оценку и комментарий пользователя")
    public RatingResponse rateTrack(@PathVariable Long trackId, @Valid @RequestBody CreateRatingRequest request) {
        RatingCommand command = new RatingCommand(trackId, request.userId(), request.value(), request.comment());
        var result = trackService.rateTrack(command);


        return ApiMapper.toRatingResponse(result.ratingInfo());
    }

    @PostMapping("/{trackId}/plays")
    @Operation(summary = "Зарегистрировать прослушивание", description = "Увеличивает счётчик прослушиваний")
    public TrackResponse registerPlay(@PathVariable Long trackId) {
        return ApiMapper.toTrackResponse(trackService.incrementPlayCount(trackId));
    }

    @GetMapping
    @Operation(summary = "Список треков", description = "Возвращает треки с фильтрами и пагинацией")
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
    @Operation(summary = "Получить трек", description = "Возвращает карточку трека по идентификатору")
    public TrackResponse getTrack(@PathVariable Long trackId) {
        return ApiMapper.toTrackResponse(trackService.getTrackInfo(trackId));
    }

    @GetMapping(value = "/{trackId}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Скачать файл", description = "Возвращает бинарные данные аудиофайла")
    public ResponseEntity<?> download(@PathVariable Long trackId, @RequestHeader HttpHeaders headers) {
        var info = trackService.getTrackInfo(trackId);
        Resource resource = fileStorageService.load(info.filepath());
        MediaType mediaType = resolveMediaType(resource, info);
        try {
            byte[] data = resource.getInputStream().readAllBytes();
            long contentLength = data.length;

            List<HttpRange> ranges = headers.getRange();
            if (ranges == null || ranges.isEmpty()) {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache())
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .contentType(mediaType)
                        .contentLength(contentLength)
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(info))
                        .body(new ByteArrayResource(data));
            }

            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            byte[] slice = Arrays.copyOfRange(data, (int) start, (int) end + 1);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .cacheControl(CacheControl.noCache())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(mediaType)
                    .contentLength(slice.length)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(info))
                    .body(new ByteArrayResource(slice));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать файл", e);
        }
    }

    private MediaType resolveMediaType(Resource resource, ru.ifmo.se.pisk.domain.TrackInfo info) {
        if (info.contentType() != null) {
            try {
                return MediaType.parseMediaType(info.contentType());
            } catch (Exception ignored) {

            }
        }
        return MediaTypeFactory.getMediaType(info.originalFilename())
                .or(() -> MediaTypeFactory.getMediaType(resource))
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private String contentDisposition(ru.ifmo.se.pisk.domain.TrackInfo info) {
        String filename = info.originalFilename();
        if (filename == null) {
            filename = Path.of(info.filepath()).getFileName().toString();
        }
        return ContentDisposition.attachment().filename(filename).build().toString();
    }

}
