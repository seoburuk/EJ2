package com.ej2.dto;

/**
 * 게시판별 게시글 통계 DTO
 * 게시판 이름, 총 게시글 수, 1주간 증가수를 담습니다.
 */
public class BoardPostStatsDTO {
    private Long boardId;
    private String boardName;
    private Long totalPosts;
    private Long weeklyIncrease;

    public BoardPostStatsDTO() {
    }

    public BoardPostStatsDTO(Long boardId, String boardName, Long totalPosts, Long weeklyIncrease) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.totalPosts = totalPosts;
        this.weeklyIncrease = weeklyIncrease;
    }

    // Getters and Setters
    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public Long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(Long totalPosts) {
        this.totalPosts = totalPosts;
    }

    public Long getWeeklyIncrease() {
        return weeklyIncrease;
    }

    public void setWeeklyIncrease(Long weeklyIncrease) {
        this.weeklyIncrease = weeklyIncrease;
    }
}
