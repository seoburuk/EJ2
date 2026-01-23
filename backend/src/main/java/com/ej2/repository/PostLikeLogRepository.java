package com.ej2.repository;

import com.ej2.model.PostLikeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostLikeLogRepository extends JpaRepository<PostLikeLog, Long> {

    // Check if a user (logged in) has liked this post recently
    Optional<PostLikeLog> findByPostIdAndUserIdAndLikedAtAfter(
        Long postId,
        Long userId,
        LocalDateTime after
    );

    // Check if an IP address has liked this post recently
    Optional<PostLikeLog> findByPostIdAndIpAddressAndLikedAtAfter(
        Long postId,
        String ipAddress,
        LocalDateTime after
    );
}
