package com.ej2.dto;

import java.time.LocalDateTime;

public class ReportDetailDTO {
    // Report basic info
    private Long id;
    private String reportType;
    private Long entityId;
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private String reason;
    private String description;
    private String status;
    private String adminNote;
    private Long resolvedBy;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private String resolutionAction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity context (polymorphic)
    private String entityTitle;
    private String entityContent;
    private String entityAuthorName;
    private Long entityAuthorId;
    private LocalDateTime entityCreatedAt;

    // Constructors
    public ReportDetailDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolvedByName() {
        return resolvedByName;
    }

    public void setResolvedByName(String resolvedByName) {
        this.resolvedByName = resolvedByName;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionAction() {
        return resolutionAction;
    }

    public void setResolutionAction(String resolutionAction) {
        this.resolutionAction = resolutionAction;
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

    public String getEntityTitle() {
        return entityTitle;
    }

    public void setEntityTitle(String entityTitle) {
        this.entityTitle = entityTitle;
    }

    public String getEntityContent() {
        return entityContent;
    }

    public void setEntityContent(String entityContent) {
        this.entityContent = entityContent;
    }

    public String getEntityAuthorName() {
        return entityAuthorName;
    }

    public void setEntityAuthorName(String entityAuthorName) {
        this.entityAuthorName = entityAuthorName;
    }

    public Long getEntityAuthorId() {
        return entityAuthorId;
    }

    public void setEntityAuthorId(Long entityAuthorId) {
        this.entityAuthorId = entityAuthorId;
    }

    public LocalDateTime getEntityCreatedAt() {
        return entityCreatedAt;
    }

    public void setEntityCreatedAt(LocalDateTime entityCreatedAt) {
        this.entityCreatedAt = entityCreatedAt;
    }
}
