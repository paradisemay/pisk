package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.Comment;
import ru.ifmo.se.pisk.domain.UserTrackKey;

public interface CommentRepository extends JpaRepository<Comment, UserTrackKey> {
}
