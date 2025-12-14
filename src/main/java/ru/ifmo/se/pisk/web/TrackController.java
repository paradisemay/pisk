package ru.ifmo.se.pisk.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.ifmo.se.pisk.domain.Comment;
import ru.ifmo.se.pisk.domain.Track;
import ru.ifmo.se.pisk.domain.User;
import ru.ifmo.se.pisk.domain.UserRating;
import ru.ifmo.se.pisk.service.AppUserDetails;
import ru.ifmo.se.pisk.service.TrackService;

@RestController
@RequestMapping("/api/v1/tracks")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Track upload(@AuthenticationPrincipal AppUserDetails userDetails,
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("albumId") Long albumId,
                        @RequestParam("artistId") Long artistId,
                        @RequestParam("genreId") Long genreId,
                        @RequestParam("name") String name,
                        @RequestParam("numberOnAlbum") int numberOnAlbum,
                        @RequestParam("duration") int duration,
                        @RequestParam(value = "explicit", defaultValue = "false") boolean explicit,
                        @RequestParam(value = "inoagent", defaultValue = "false") boolean inoagent) throws IOException {
        User author = userDetails.getUser();
        return trackService.uploadTrack(author, file, albumId, artistId, genreId, name, numberOnAlbum, duration, explicit, inoagent);
    }

    @GetMapping
    public List<Track> search(@RequestParam("q") String query) {
        return trackService.search(query);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping("/{id}/rating")
    public UserRating rate(@AuthenticationPrincipal AppUserDetails userDetails,
                           @PathVariable Long id,
                           @RequestParam @Min(1) @Max(5) short rate) {
        return trackService.rate(userDetails.getUser(), id, rate);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping("/{id}/complaints")
    public Comment complain(@AuthenticationPrincipal AppUserDetails userDetails,
                            @PathVariable Long id,
                            @RequestParam @NotBlank String body,
                            @RequestParam("trackTime") int trackTime) {
        return trackService.complain(userDetails.getUser(), id, body, trackTime);
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> stream(@PathVariable Long id, @RequestHeader(value = "Range", required = false) String range) throws IOException {
        return trackService.stream(id, range);
    }
}
