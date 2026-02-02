package com.ej2.mapper;

import com.ej2.dto.ActivityDTO;
import com.ej2.dto.BoardPostStatsDTO;
import com.ej2.dto.DashboardStatsDTO;
import com.ej2.dto.WeeklyStatDTO;
import com.ej2.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {

    /**
     * 대시보드 기본 통계 조회
     * @return 통계 데이터 (사용자수, 관리자수, 게시판수, 게시물수, 댓글수, 주간증가량)
     */
    DashboardStatsDTO selectDashboardStats();

    /**
     * 주간 트렌드 조회 (최근 7일)
     * @return 일별 가입자수, 게시물수, 댓글수
     */
    List<WeeklyStatDTO> selectWeeklyStats();

    /**
     * 최근 활동 피드 조회
     * @param limit 조회 개수
     * @return 최근 활동 목록 (가입, 게시물, 댓글)
     */
    List<ActivityDTO> selectRecentActivity(@Param("limit") int limit);

    /**
     * 사용자 목록 조회 (페이지네이션)
     * @param offset 시작 위치
     * @param limit 조회 개수
     * @return 사용자 목록
     */
    List<User> selectUsers(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 전체 사용자 수 조회
     * @return 사용자 수
     */
    int countUsers();

    /**
     * 사용자 역할 변경
     * @param userId 사용자 ID
     * @param role 새 역할 (ADMIN or USER)
     * @return 영향받은 행 수
     */
    int updateUserRole(@Param("userId") Long userId, @Param("role") String role);

    /**
     * 사용자 삭제
     * @param userId 사용자 ID
     * @return 영향받은 행 수
     */
    int deleteUser(@Param("userId") Long userId);

    /**
     * 게시판별 게시글 통계 조회
     * @return 게시판별 총 게시글 수와 1주간 증가수
     */
    List<BoardPostStatsDTO> selectBoardPostStats();
}
