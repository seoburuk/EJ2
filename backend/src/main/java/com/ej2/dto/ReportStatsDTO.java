package com.ej2.dto;

public class ReportStatsDTO {
    private Long totalReports;
    private Long pendingReports;
    private Long reviewingReports;
    private Long resolvedToday;
    private Long dismissedToday;

    // Constructors
    public ReportStatsDTO() {
    }

    // Getters and Setters
    public Long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(Long totalReports) {
        this.totalReports = totalReports;
    }

    public Long getPendingReports() {
        return pendingReports;
    }

    public void setPendingReports(Long pendingReports) {
        this.pendingReports = pendingReports;
    }

    public Long getReviewingReports() {
        return reviewingReports;
    }

    public void setReviewingReports(Long reviewingReports) {
        this.reviewingReports = reviewingReports;
    }

    public Long getResolvedToday() {
        return resolvedToday;
    }

    public void setResolvedToday(Long resolvedToday) {
        this.resolvedToday = resolvedToday;
    }

    public Long getDismissedToday() {
        return dismissedToday;
    }

    public void setDismissedToday(Long dismissedToday) {
        this.dismissedToday = dismissedToday;
    }
}
