package com.ej2.controller;

import com.ej2.service.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ranking")
@CrossOrigin(origins = "http://localhost:3000")
public class RankingController {

    @Autowired
    private RankingService rankingService;

    /**
     * 전체 인기글 조회
     * GET /api/ranking/popular?period=weekly&page=0&size=20
     *
     * @param period 기간 (daily, weekly, monthly, all) - 기본값: weekly
     * @param page 페이지 번호 (0부터 시작) - 기본값: 0
     * @param size 페이지당 개수 (최대 100) - 기본값: 20
     * @return 인기글 목록과 페이징 정보
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularPosts(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result = rankingService.getPopularPosts(period, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 게시판별 인기글 조회
     * GET /api/ranking/popular/board/{boardId}?period=weekly&page=0&size=20
     *
     * @param boardId 게시판 ID
     * @param period 기간
     * @param page 페이지 번호
     * @param size 페이지당 개수
     * @return 인기글 목록과 페이징 정보
     */
    @GetMapping("/popular/board/{boardId}")
    public ResponseEntity<Map<String, Object>> getPopularPostsByBoard(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result = rankingService.getPopularPostsByBoard(boardId, period, page, size);
        return ResponseEntity.ok(result);
    }
}
