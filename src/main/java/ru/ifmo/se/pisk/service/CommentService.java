package ru.ifmo.se.pisk.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.entity.CommentEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.repository.CommentRepository;
import ru.ifmo.se.pisk.repository.TrackRepository;
import ru.ifmo.se.pisk.repository.UserRepository;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, TrackRepository trackRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentEntity> getCommentsForTrack(Long trackId) {
        TrackEntity track = trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found"));







        List<CommentEntity> comments = commentRepository.findByTrackOrderByCreatedAtAsc(track);
        comments.sort((c1, c2) -> {
            int t1 = c1.getTrackTime() == null ? 0 : c1.getTrackTime();
            int t2 = c2.getTrackTime() == null ? 0 : c2.getTrackTime();
            return Integer.compare(t1, t2);
        });
        return comments;
    }

    @Transactional
    public CommentEntity addComment(Long trackId, Long userId, String body, Integer trackTime) {
        TrackEntity track = trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found"));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CommentEntity comment = new CommentEntity();
        comment.setTrack(track);
        comment.setAuthor(user);
        comment.setBody(body);
        comment.setTrackTime(trackTime);

        return commentRepository.save(comment);
    }
}
