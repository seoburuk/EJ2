package com.ej2.mapper;

import com.ej2.dto.PopularPostDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RankingMapper {

    /**
     * 전체 인기글 조회
     * @param period 기간 (daily, weekly, monthly, all)
     * @param limit 조회 개수
     * @param offset 페이징 오프셋
     * @return 인기글 목록
     */
    List<PopularPostDTO> selectPopularPosts(
        @Param("period") String period,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 게시판별 인기글 조회
     * @param boardId 게시판 ID
     * @param period 기간
     * @param limit 조회 개수
     * @param offset 페이징 오프셋
     * @return 인기글 목록
     */
    List<PopularPostDTO> selectPopularPostsByBoard(
        @Param("boardId") Long boardId,
        @Param("period") String period,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 인기글 총 개수 조회 (페이징용)
     * @param boardId 게시판 ID (null이면 전체)
     * @param period 기간
     * @return 총 개수
     */
    int countPopularPosts(
        @Param("boardId") Long boardId,
        @Param("period") String period
    );
}
