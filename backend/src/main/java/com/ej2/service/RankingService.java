package com.ej2.service;

import com.ej2.dto.PopularPostDTO;
import com.ej2.mapper.RankingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RankingService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final List<String> VALID_PERIODS = Arrays.asList("daily", "weekly", "monthly", "all");

    @Autowired
    private RankingMapper rankingMapper;

    /**
     * 전체 인기글 조회
     * @param period 기간 (daily, weekly, monthly, all)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 인기글 목록과 페이징 정보
     */
    public Map<String, Object> getPopularPosts(String period, int page, int size) {
        String validPeriod = validatePeriod(period);
        int validSize = validateSize(size);
        int validPage = Math.max(0, page);
        int offset = validPage * validSize;

        List<PopularPostDTO> posts = rankingMapper.selectPopularPosts(validPeriod, validSize, offset);
        int totalCount = rankingMapper.countPopularPosts(null, validPeriod);

        return buildResponse(posts, validPage, validSize, totalCount);
    }

    /**
     * 게시판별 인기글 조회
     * @param boardId 게시판 ID
     * @param period 기간
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 인기글 목록과 페이징 정보
     */
    public Map<String, Object> getPopularPostsByBoard(Long boardId, String period, int page, int size) {
        String validPeriod = validatePeriod(period);
        int validSize = validateSize(size);
        int validPage = Math.max(0, page);
        int offset = validPage * validSize;

        List<PopularPostDTO> posts = rankingMapper.selectPopularPostsByBoard(boardId, validPeriod, validSize, offset);
        int totalCount = rankingMapper.countPopularPosts(boardId, validPeriod);

        return buildResponse(posts, validPage, validSize, totalCount);
    }

    /**
     * 기간 파라미터 검증
     */
    private String validatePeriod(String period) {
        if (period == null || period.trim().isEmpty()) {
            return "weekly";
        }
        String lowerPeriod = period.toLowerCase().trim();
        if (!VALID_PERIODS.contains(lowerPeriod)) {
            return "weekly";
        }
        return lowerPeriod;
    }

    /**
     * 페이지 크기 검증
     */
    private int validateSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return size;
    }

    /**
     * 응답 데이터 구성
     */
    private Map<String, Object> buildResponse(List<PopularPostDTO> posts, int page, int size, int totalCount) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("posts", posts);
        response.put("page", page);
        response.put("size", size);
        response.put("totalCount", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / size));
        return response;
    }
}
