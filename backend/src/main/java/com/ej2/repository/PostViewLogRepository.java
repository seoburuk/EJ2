package com.ej2.repository;

import com.ej2.model.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostViewLogRepository extends JpaRepository<PostViewLog, Long> {

    // Check if a user (logged in) has viewed this post recently (within 24 hours)
    Optional<PostViewLog> findByPostIdAndUserIdAndViewedAtAfter(
        Long postId,
        Long userId,
        LocalDateTime after
    );

    // Check if an IP address has viewed this post recently (within 24 hours)
    Optional<PostViewLog> findByPostIdAndIpAddressAndViewedAtAfter(
        Long postId,
        String ipAddress,
        LocalDateTime after
    );
}
