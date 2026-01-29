package com.ej2.repository;

import com.ej2.model.CommentLikeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeLogRepository extends JpaRepository<CommentLikeLog, Long> {

    // Check if a user has already liked this comment
    Optional<CommentLikeLog> findByCommentIdAndUserId(Long commentId, Long userId);

    // Check if an IP address has already liked this comment
    Optional<CommentLikeLog> findByCommentIdAndIpAddress(Long commentId, String ipAddress);

    // Get all likes for a comment by user IDs (for checking if current user liked)
    List<CommentLikeLog> findByCommentIdIn(List<Long> commentIds);
}
