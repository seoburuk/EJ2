package com.ej2.service;

import com.ej2.dto.PopularPostDTO;
import com.ej2.mapper.RankingMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RankingServiceTest {

    @Mock
    private RankingMapper rankingMapper;

    @InjectMocks
    private RankingService rankingService;

    private PopularPostDTO createPost(Long id, String title, int likes, int dislikes,
                                       int comments, int scraps, int views, double score) {
        PopularPostDTO post = new PopularPostDTO();
        post.setId(id);
        post.setTitle(title);
        post.setLikeCount(likes);
        post.setDislikeCount(dislikes);
        post.setCommentCount(comments);
        post.setScrapCount(scraps);
        post.setViewCount(views);
        post.setPopularityScore(score);
        post.setCreatedAt(LocalDateTime.now());
        return post;
    }

    // ===== Period 검증 테스트 =====

    @Test
    public void testValidPeriods() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        for (String period : Arrays.asList("daily", "weekly", "monthly", "all")) {
            rankingService.getPopularPosts(period, 0, 20);
            verify(rankingMapper).selectPopularPosts(eq(period), eq(20), eq(0));
        }
    }

    @Test
    public void testInvalidPeriodDefaultsToWeekly() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        rankingService.getPopularPosts("invalid_period", 0, 20);
        verify(rankingMapper).selectPopularPosts(eq("weekly"), eq(20), eq(0));
    }

    @Test
    public void testNullPeriodDefaultsToWeekly() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        rankingService.getPopularPosts(null, 0, 20);
        verify(rankingMapper).selectPopularPosts(eq("weekly"), eq(20), eq(0));
    }

    @Test
    public void testEmptyPeriodDefaultsToWeekly() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        rankingService.getPopularPosts("  ", 0, 20);
        verify(rankingMapper).selectPopularPosts(eq("weekly"), eq(20), eq(0));
    }

    // ===== 페이지 크기 검증 테스트 =====

    @Test
    public void testDefaultPageSize() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        rankingService.getPopularPosts("weekly", 0, 0);
        verify(rankingMapper).selectPopularPosts(eq("weekly"), eq(20), eq(0));
    }

    @Test
    public void testNegativePageSize() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        rankingService.getPopularPosts("weekly", 0, -5);
        verify(rankingMapper).selectPopularPosts(eq("weekly"), eq(20), eq(0));
    }

    @Test
    public void testMaxPageSize() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        rankingService.getPopularPosts("weekly", 0, 999);
        verify(rankingMapper).selectPopularPosts(eq("weekly"), eq(100), eq(0));
    }

    // ===== 페이지네이션 오프셋 테스트 =====

    @Test
    public void testPaginationOffset() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        rankingService.getPopularPosts("weekly", 2, 10);
        verify(rankingMapper).selectPopularPosts(eq("weekly"), eq(10), eq(20));
    }

    @Test
    public void testNegativePageDefaultsToZero() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        rankingService.getPopularPosts("weekly", -3, 20);
        verify(rankingMapper).selectPopularPosts(eq("weekly"), eq(20), eq(0));
    }

    // ===== 응답 구조 테스트 =====

    @Test
    public void testResponseStructure() {
        List<PopularPostDTO> mockPosts = Arrays.asList(
            createPost(1L, "인기글1", 10, 1, 5, 3, 100, 15.5),
            createPost(2L, "인기글2", 8, 0, 3, 2, 80, 12.0)
        );

        when(rankingMapper.selectPopularPosts("weekly", 20, 0)).thenReturn(mockPosts);
        when(rankingMapper.countPopularPosts(null, "weekly")).thenReturn(50);

        Map<String, Object> result = rankingService.getPopularPosts("weekly", 0, 20);

        assertNotNull(result);
        assertEquals(mockPosts, result.get("posts"));
        assertEquals(0, result.get("page"));
        assertEquals(20, result.get("size"));
        assertEquals(50, result.get("totalCount"));
        assertEquals(3, result.get("totalPages")); // ceil(50/20) = 3
    }

    @Test
    public void testTotalPagesCalculation() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(21);

        Map<String, Object> result = rankingService.getPopularPosts("weekly", 0, 10);

        assertEquals(3, result.get("totalPages")); // ceil(21/10) = 3
    }

    @Test
    public void testEmptyResult() {
        when(rankingMapper.selectPopularPosts(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(rankingMapper.countPopularPosts(isNull(), anyString()))
            .thenReturn(0);

        Map<String, Object> result = rankingService.getPopularPosts("weekly", 0, 20);

        List<?> posts = (List<?>) result.get("posts");
        assertTrue(posts.isEmpty());
        assertEquals(0, result.get("totalCount"));
        assertEquals(0, result.get("totalPages"));
    }

    // ===== 게시판별 조회 테스트 =====

    @Test
    public void testGetPopularPostsByBoard() {
        Long boardId = 1L;
        List<PopularPostDTO> mockPosts = Arrays.asList(
            createPost(1L, "게시판글", 5, 0, 2, 1, 50, 8.0)
        );

        when(rankingMapper.selectPopularPostsByBoard(eq(boardId), eq("daily"), eq(20), eq(0)))
            .thenReturn(mockPosts);
        when(rankingMapper.countPopularPosts(eq(boardId), eq("daily")))
            .thenReturn(1);

        Map<String, Object> result = rankingService.getPopularPostsByBoard(boardId, "daily", 0, 20);

        assertNotNull(result);
        assertEquals(mockPosts, result.get("posts"));
        assertEquals(1, result.get("totalCount"));
    }

    // ===== 인기도 점수 공식 검증 (순수 로직) =====

    /**
     * SQL 점수 공식을 Java로 재현:
     * score = (like*3 + comment*2 + scrap*2.5 + view*0.1 - dislike*1) / (hoursOld + 2)^1.0
     */
    private double calculateScore(int likes, int comments, int scraps, int views,
                                   int dislikes, int hoursOld) {
        double rawScore = (likes * 3.0) + (comments * 2.0) + (scraps * 2.5)
                        + (views * 0.1) - (dislikes * 1.0);
        double gravity = Math.pow(Math.max(hoursOld, 0) + 2, 1.0);
        return rawScore / gravity;
    }

    @Test
    public void testScoreFormula_NewPostHighEngagement() {
        // 방금 올린 글 (0시간): 좋아요 10, 댓글 5, 스크랩 3, 조회 100, 싫어요 2
        double score = calculateScore(10, 5, 3, 100, 2, 0);
        // rawScore = (10*3) + (5*2) + (3*2.5) + (100*0.1) - (2*1) = 30+10+7.5+10-2 = 55.5
        // gravity = (0+2)^1 = 2
        // score = 55.5 / 2 = 27.75
        assertEquals(27.75, score, 0.001);
    }

    @Test
    public void testScoreFormula_TimeDecay() {
        // 같은 engagement, 시간만 다른 두 글 비교
        double scoreNew = calculateScore(10, 5, 3, 100, 2, 0);   // 0시간
        double scoreOld = calculateScore(10, 5, 3, 100, 2, 24);  // 24시간

        // 24시간 된 글: 55.5 / (24+2) = 55.5 / 26 ≈ 2.134
        assertEquals(55.5 / 26.0, scoreOld, 0.001);

        // 새 글이 항상 더 높은 점수
        assertTrue("새 글이 오래된 글보다 높은 점수여야 함",
            scoreNew > scoreOld);

        // 시간 감쇠 비율 확인: 24시간이면 약 13배 차이
        assertTrue("24시간 차이로 10배 이상 점수 감소",
            scoreNew / scoreOld > 10);
    }

    @Test
    public void testScoreFormula_NegativeScore() {
        // 싫어요만 많은 글: 좋아요 0, 댓글 0, 스크랩 0, 조회 5, 싫어요 10
        double score = calculateScore(0, 0, 0, 5, 10, 0);
        // rawScore = 0 + 0 + 0 + (5*0.1) - (10*1) = 0.5 - 10 = -9.5
        // gravity = 2
        // score = -9.5 / 2 = -4.75
        assertEquals(-4.75, score, 0.001);
        assertTrue("싫어요가 많으면 점수가 음수", score < 0);
    }

    @Test
    public void testScoreFormula_AllZeros() {
        // 모든 값이 0인 경우
        double score = calculateScore(0, 0, 0, 0, 0, 0);
        // rawScore = 0, gravity = 2, score = 0
        assertEquals(0.0, score, 0.001);
    }

    @Test
    public void testScoreFormula_WeightPriority() {
        // 가중치 우선순위: 좋아요(3.0) > 스크랩(2.5) > 댓글(2.0) > 조회(0.1)
        double likesOnly  = calculateScore(1, 0, 0, 0, 0, 0);   // 3.0 / 2 = 1.5
        double scrapsOnly = calculateScore(0, 0, 1, 0, 0, 0);   // 2.5 / 2 = 1.25
        double commentsOnly = calculateScore(0, 1, 0, 0, 0, 0); // 2.0 / 2 = 1.0
        double viewsOnly  = calculateScore(0, 0, 0, 1, 0, 0);   // 0.1 / 2 = 0.05

        assertTrue("좋아요 > 스크랩", likesOnly > scrapsOnly);
        assertTrue("스크랩 > 댓글", scrapsOnly > commentsOnly);
        assertTrue("댓글 > 조회", commentsOnly > viewsOnly);
    }

    @Test
    public void testScoreFormula_RankingOrder() {
        // 실제 랭킹 시나리오: 다양한 글들의 순위가 맞는지 검증
        // 글A: 인기글 (좋아요 많음, 방금 올림)
        double postA = calculateScore(20, 10, 5, 500, 3, 1);
        // 글B: 보통글 (적당한 반응, 3시간 전)
        double postB = calculateScore(5, 3, 1, 100, 0, 3);
        // 글C: 오래된 인기글 (좋아요 매우 많음, 48시간 전)
        double postC = calculateScore(50, 30, 20, 2000, 5, 48);

        // A가 B보다 높아야 함 (더 많은 engagement + 더 최신)
        assertTrue("인기 신규글 > 보통글", postA > postB);
        // B가 C보다 높을 수 있음 (시간 감쇠 때문에 오래된 글은 점수 낮음)
        // postC raw = 50*3+30*2+20*2.5+2000*0.1-5 = 150+60+50+200-5 = 455
        // postC gravity = 50, score = 455/50 = 9.1
        // postB raw = 5*3+3*2+1*2.5+100*0.1-0 = 15+6+2.5+10 = 33.5
        // postB gravity = 5, score = 33.5/5 = 6.7
        // 이 경우 C가 여전히 높음 (engagement 차이가 크니까)
        assertTrue("48시간 지났어도 engagement가 크면 여전히 높은 순위 가능",
            postC > postB);
    }
}
