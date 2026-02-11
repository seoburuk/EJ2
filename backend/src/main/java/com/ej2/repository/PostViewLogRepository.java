package com.ej2.repository;

import com.ej2.model.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostViewLogRepository extends JpaRepository<PostViewLog, Long> {

    // 로그인한 사용자가 최근(24시간 이내) 이 게시글을 조회했는지 확인
    // List로 반환하여 중복 레코드 발생 시 NonUniqueResultException 방지
    List<PostViewLog> findByPostIdAndUserIdAndViewedAtAfter(
        Long postId,
        Long userId,
        LocalDateTime after
    );

    // IP 주소 기반으로 비로그인 사용자가 최근 이 게시글을 조회했는지 확인
    List<PostViewLog> findByPostIdAndIpAddressAndViewedAtAfter(
        Long postId,
        String ipAddress,
        LocalDateTime after
    );
}
