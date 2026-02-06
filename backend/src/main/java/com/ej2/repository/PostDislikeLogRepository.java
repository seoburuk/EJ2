package com.ej2.repository;

import com.ej2.model.PostDislikeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostDislikeLogRepository extends JpaRepository<PostDislikeLog, Long> {

    // 로그인한 사용자가 최근 이 게시글에 싫어요를 눌렀는지 확인
    // List로 반환하여 중복 레코드 발생 시 NonUniqueResultException 방지
    List<PostDislikeLog> findByPostIdAndUserIdAndDislikedAtAfter(
        Long postId,
        Long userId,
        LocalDateTime after
    );

    // IP 주소 기반으로 비로그인 사용자가 최근 싫어요를 눌렀는지 확인
    List<PostDislikeLog> findByPostIdAndIpAddressAndDislikedAtAfter(
        Long postId,
        String ipAddress,
        LocalDateTime after
    );
}