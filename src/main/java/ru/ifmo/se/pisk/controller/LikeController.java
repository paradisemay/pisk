package ru.ifmo.se.pisk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;

@RestController
@Tag(name = "Likes", description = "Управление лайками")
@RequestMapping("/api/v1/tracks/{trackId}/like")
public class LikeController {

    private final TrackService trackService;
    private final UserService userService;

    public LikeController(TrackService trackService, UserService userService) {
        this.trackService = trackService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Лайкнуть трек", description = "Добавляет трек в избранное")
    public ResponseEntity<Void> likeTrack(@PathVariable Long trackId, Principal principal) {
        UserEntity user = getCurrentUser(principal);
        trackService.addLike(user.getId(), trackId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "Убрать лайк", description = "Удаляет трек из избранного")
    public ResponseEntity<Void> unlikeTrack(@PathVariable Long trackId, Principal principal) {
        UserEntity user = getCurrentUser(principal);
        trackService.removeLike(user.getId(), trackId);
        return ResponseEntity.ok().build();
    }

    private UserEntity getCurrentUser(Principal principal) {
        if (principal == null) {
             throw new IllegalStateException("Authentication missing");
        }
        String username = principal.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
