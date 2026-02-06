package com.ej2.repository;

import com.ej2.model.PostDislikeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostDislikeLogRepository extends JpaRepository<PostDislikeLog, Long> {
    
    // Check if a user (logged in) has disliked this post recently
    Optional<PostDislikeLog> findByPostIdAndUserIdAndDislikedAtAfter(
        Long postId,
        Long userId,
        LocalDateTime after
    );

    // Check if an IP address has disliked this post recently
    Optional<PostDislikeLog> findByPostIdAndIpAddressAndDislikedAtAfter(
        Long postId,
        String ipAddress,
        LocalDateTime after
    );
}