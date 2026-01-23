package com.ej2.dto;

import com.ej2.model.Post;

import java.time.LocalDateTime;

public class PostDTO {
    private Long id;
    private Long boardId;
    private Long userId;
    private String authorNickname;  // 작성자 닉네임
    private String title;
    private String content;
    private String anonymousId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private Integer scrapCount;
    private Boolean isNotice;
    private Boolean isBlinded;
    private String blindReason;
    private Integer reportedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PostDTO() {
    }

    // Constructor from Post and author nickname
    public PostDTO(Post post, String authorNickname) {
        this.id = post.getId();
        this.boardId = post.getBoardId();
        this.userId = post.getUserId();
        this.authorNickname = authorNickname;
        this.title = post.getTitle();
        this.content = post.getContent();
        this.anonymousId = post.getAnonymousId();
        this.viewCount = post.getViewCount();
        this.likeCount = post.getLikeCount();
        this.dislikeCount = post.getDislikeCount();
        this.commentCount = post.getCommentCount();
        this.scrapCount = post.getScrapCount();
        this.isNotice = post.getIsNotice();
        this.isBlinded = post.getIsBlinded();
        this.blindReason = post.getBlindReason();
        this.reportedCount = post.getReportedCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAnonymousId() {
        return anonymousId;
    }

    public void setAnonymousId(String anonymousId) {
        this.anonymousId = anonymousId;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(Integer dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getScrapCount() {
        return scrapCount;
    }

    public void setScrapCount(Integer scrapCount) {
        this.scrapCount = scrapCount;
    }

    public Boolean getIsNotice() {
        return isNotice;
    }

    public void setIsNotice(Boolean isNotice) {
        this.isNotice = isNotice;
    }

    public Boolean getIsBlinded() {
        return isBlinded;
    }

    public void setIsBlinded(Boolean isBlinded) {
        this.isBlinded = isBlinded;
    }

    public String getBlindReason() {
        return blindReason;
    }

    public void setBlindReason(String blindReason) {
        this.blindReason = blindReason;
    }

    public Integer getReportedCount() {
        return reportedCount;
    }

    public void setReportedCount(Integer reportedCount) {
        this.reportedCount = reportedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
