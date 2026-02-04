package com.ej2.dto;

public class ReportSearchCriteria {
    private String status;
    private String reportType;
    private String sortBy;
    private String sortOrder;
    private Integer page;
    private Integer size;
    private Long reporterId;   

    // Constructors
    public ReportSearchCriteria() {
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getReporterId() {
        return reporterId;
    }
    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }
}
