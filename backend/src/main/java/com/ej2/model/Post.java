package com.ej2.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "anonymous_id", length = 50)
    private String anonymousId;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "dislike_count")
    private Integer dislikeCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    @Column(name = "scrap_count")
    private Integer scrapCount = 0;

    @Column(name = "is_notice")
    private Boolean isNotice = false;

    @Column(name = "is_blinded")
    private Boolean isBlinded = false;

    @Column(name = "blind_reason", columnDefinition = "TEXT")
    private String blindReason;

    @Column(name = "reported_count")
    private Integer reportedCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 업데이트 시간 갱신 필요없는 필드 변경시 사용
    @Transient
    private boolean refreshUpdatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (refreshUpdatedAt) {
            updatedAt = LocalDateTime.now();
        }

        refreshUpdatedAt = true;
    }

    // Constructors
    public Post() {
    }

    public Post(Long boardId, Long userId, String title, String content) {
        this.boardId = boardId;
        this.userId = userId;
        this.title = title;
        this.content = content;
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
        refreshUpdatedAt = false;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
        refreshUpdatedAt = false;
    }

    public Integer getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(Integer dislikeCount) {
        this.dislikeCount = dislikeCount;
        refreshUpdatedAt = false;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
        refreshUpdatedAt = false;
    }

    public Integer getScrapCount() {
        return scrapCount;
    }

    public void setScrapCount(Integer scrapCount) {
        this.scrapCount = scrapCount;
        refreshUpdatedAt = false;
    }

    public Boolean getIsNotice() {
        return isNotice;
    }

    public void setIsNotice(Boolean isNotice) {
        this.isNotice = isNotice;
        refreshUpdatedAt = false;
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
        refreshUpdatedAt = false;
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
