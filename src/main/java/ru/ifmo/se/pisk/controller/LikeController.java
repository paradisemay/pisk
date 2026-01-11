package ru.ifmo.se.pisk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;

@RestController
@RequestMapping("/api/v1/tracks")
public class LikeController {

    private final TrackService trackService;
    private final UserService userService;

    public LikeController(TrackService trackService, UserService userService) {
        this.trackService = trackService;
        this.userService = userService;
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> likeTrack(@PathVariable Long id, java.security.Principal principal) {
        ru.ifmo.se.pisk.domain.entity.UserEntity user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        trackService.likeTrack(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlikeTrack(@PathVariable Long id, java.security.Principal principal) {
        ru.ifmo.se.pisk.domain.entity.UserEntity user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        trackService.unlikeTrack(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
