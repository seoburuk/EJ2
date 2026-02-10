package com.ej2.repository;

import com.ej2.model.PostLikeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostLikeLogRepository extends JpaRepository<PostLikeLog, Long> {

    // 로그인한 사용자가 최근 이 게시글에 좋아요를 눌렀는지 확인
    // List로 반환하여 중복 레코드 발생 시 NonUniqueResultException 방지
    List<PostLikeLog> findByPostIdAndUserIdAndLikedAtAfter(
        Long postId,
        Long userId,
        LocalDateTime after
    );

    // IP 주소 기반으로 비로그인 사용자가 최근 좋아요를 눌렀는지 확인
    List<PostLikeLog> findByPostIdAndIpAddressAndLikedAtAfter(
        Long postId,
        String ipAddress,
        LocalDateTime after
    );

    // 로그인한 사용자가 이 게시글에 좋아요를 눌렀는지 확인 (시간 무관)
    List<PostLikeLog> findByPostIdAndUserId(Long postId, Long userId);

    // IP 주소 기반으로 비로그인 사용자가 좋아요를 눌렀는지 확인 (시간 무관)
    List<PostLikeLog> findByPostIdAndIpAddress(Long postId, String ipAddress);
}
