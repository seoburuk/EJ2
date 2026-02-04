package com.ej2.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_dislike_logs")
public class PostDislikeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id")
    private Long userId; // NULL for non-logged-in users

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "disliked_at")
    private LocalDateTime dislikedAt;

    @PrePersist
    protected void onCreate() {
        dislikedAt = LocalDateTime.now();
    }

    // Constructors
    public PostDislikeLog() {
    }

    public PostDislikeLog(Long postId, Long userId, String ipAddress) {
        this.postId = postId;
        this.userId = userId;
        this.ipAddress = ipAddress;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getDislikedAt() {
        return dislikedAt;
    }

    public void setDislikedAt(LocalDateTime dislikedAt) {
        this.dislikedAt = dislikedAt;
    }
}