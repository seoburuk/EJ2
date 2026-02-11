package com.ej2.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an image attached to a post.
 * Supports multiple images per post with ordering.
 */
@Entity
@Table(name = "post_images")
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;  // S3 object key: posts/{postId}/{uuid}.ext

    @Column(name = "s3_url", nullable = false, length = 1000)
    private String s3Url;  // Full S3 URL for direct access

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "file_size")
    private Long fileSize;  // Size in bytes

    @Column(name = "content_type", length = 50)
    private String contentType;  // image/jpeg, image/png, etc.

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;  // 0-4 for ordering images

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // Default constructor
    public PostImage() {
    }

    // Constructor with all fields
    public PostImage(Long postId, String s3Key, String s3Url, String originalFilename,
                     Long fileSize, String contentType, Integer displayOrder, LocalDateTime uploadedAt) {
        this.postId = postId;
        this.s3Key = s3Key;
        this.s3Url = s3Url;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.displayOrder = displayOrder;
        this.uploadedAt = uploadedAt;
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

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getS3Url() {
        return s3Url;
    }

    public void setS3Url(String s3Url) {
        this.s3Url = s3Url;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "PostImage{" +
                "id=" + id +
                ", postId=" + postId +
                ", s3Key='" + s3Key + '\'' +
                ", s3Url='" + s3Url + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", contentType='" + contentType + '\'' +
                ", displayOrder=" + displayOrder +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
