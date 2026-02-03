package com.ej2.service;

import com.ej2.dto.ReportDTO;
import com.ej2.dto.ReportDetailDTO;
import com.ej2.dto.ReportSearchCriteria;
import com.ej2.dto.ReportStatsDTO;
import com.ej2.mapper.ReportMapper;
import com.ej2.model.Comment;
import com.ej2.model.Post;
import com.ej2.model.Report;
import com.ej2.model.User;
import com.ej2.repository.CommentRepository;
import com.ej2.repository.PostRepository;
import com.ej2.repository.ReportRepository;
import com.ej2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Report management
 * Handles report submission, moderation actions, and admin review processes
 */
@Service
@Transactional
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Submit a new report for a post, comment, or user
     * Validates that user cannot report own content and prevents duplicate reports
     *
     * @param reporterId ID of the user submitting the report
     * @param reportType Type of report (POST, COMMENT, USER)
     * @param entityId ID of the entity being reported
     * @param reason Reason for the report (SPAM, HARASSMENT, etc.)
     * @param description Additional details about the report
     * @return The created Report entity
     * @throws RuntimeException if validation fails
     */
    public Report submitReport(Long reporterId, String reportType, Long entityId,
                              String reason, String description) {
        // Validate report submission
        validateReportSubmission(reporterId, reportType, entityId);

        // Check for duplicate report
        boolean alreadyReported = reportRepository.existsByReporterIdAndReportTypeAndEntityId(
            reporterId, reportType, entityId
        );

        if (alreadyReported) {
            throw new RuntimeException("You have already reported this " + reportType.toLowerCase());
        }

        // Create new report
        Report report = new Report(reportType, entityId, reporterId, reason, description);
        report = reportRepository.save(report);

        // Increment reported count for posts
        if ("POST".equals(reportType)) {
            Optional<Post> postOptional = postRepository.findById(entityId);
            if (postOptional.isPresent()) {
                Post post = postOptional.get();
                Integer currentCount = post.getReportedCount();
                post.setReportedCount(currentCount != null ? currentCount + 1 : 1);
                postRepository.save(post);
            }
        }

        return report;
    }

    /**
     * Get report statistics for admin dashboard
     * Includes counts by status and reason
     *
     * @return ReportStatsDTO containing aggregate statistics
     */
    public ReportStatsDTO getReportStats() {
        return reportMapper.selectReportStats();
    }

    /**
     * Search reports with dynamic filters and pagination
     *
     * @param criteria Search criteria (status, type, reason, date range, etc.)
     * @param page Page number (0-indexed)
     * @param size Number of records per page
     * @return List of ReportDTO matching the criteria
     */
    public List<ReportDTO> searchReports(ReportSearchCriteria criteria, int page, int size) {
        int offset = page * size;
        return reportMapper.selectReports(criteria, offset, size);
    }

    /**
     * Count total reports matching search criteria
     * Used for pagination
     *
     * @param criteria Search criteria
     * @return Total count of matching reports
     */
    public int countReports(ReportSearchCriteria criteria) {
        return reportMapper.countReports(criteria);
    }

    /**
     * Get detailed information about a specific report
     * Includes entity content and reporter information
     *
     * @param reportId ID of the report
     * @return ReportDetailDTO with comprehensive report information
     */
    public ReportDetailDTO getReportDetail(Long reportId) {
        return reportMapper.selectReportDetail(reportId);
    }

    /**
     * Update the status of a report
     * When resolved or dismissed, records admin information and timestamp
     *
     * @param reportId ID of the report to update
     * @param newStatus New status (PENDING, REVIEWING, RESOLVED, DISMISSED)
     * @param adminNote Optional note from the admin
     * @param adminId ID of the admin performing the action
     * @return Updated Report entity
     * @throws RuntimeException if report not found
     */
    public Report updateReportStatus(Long reportId, String newStatus,
                                    String adminNote, Long adminId) {
        Optional<Report> reportOptional = reportRepository.findById(reportId);

        if (!reportOptional.isPresent()) {
            throw new RuntimeException("Report not found with id: " + reportId);
        }

        Report report = reportOptional.get();
        report.setStatus(newStatus);

        if (adminNote != null && !adminNote.trim().isEmpty()) {
            report.setAdminNote(adminNote);
        }

        // Set resolution information for final states
        if ("RESOLVED".equals(newStatus) || "DISMISSED".equals(newStatus)) {
            report.setResolvedBy(adminId);
            report.setResolvedAt(LocalDateTime.now());
        }

        return reportRepository.save(report);
    }

    /**
     * Take moderation action on reported content
     * Actions include: BLIND_POST, DELETE_POST, DELETE_COMMENT, SUSPEND_USER, NO_ACTION
     * Automatically resolves all related reports for the same entity
     *
     * @param reportId ID of the report being acted upon
     * @param action Moderation action to take
     * @param adminNote Explanation for the action
     * @param adminId ID of the admin performing the action
     * @throws RuntimeException if report or entity not found, or action fails
     */
    public void takeModerationAction(Long reportId, String action,
                                    String adminNote, Long adminId) {
        // Get report detail
        ReportDetailDTO reportDetail = reportMapper.selectReportDetail(reportId);

        if (reportDetail == null) {
            throw new RuntimeException("Report not found with id: " + reportId);
        }

        Long entityId = reportDetail.getEntityId();
        String reportType = reportDetail.getReportType();

        // Execute moderation action based on action type
        switch (action) {
            case "BLIND_POST":
                handleBlindPost(entityId, adminNote);
                break;

            case "DELETE_POST":
                handleDeletePost(entityId);
                break;

            case "DELETE_COMMENT":
                handleDeleteComment(entityId);
                break;

            case "SUSPEND_USER":
                // User suspension will be handled separately by AdminService
                // This case just marks the report as resolved
                break;

            case "NO_ACTION":
                // No action required, just resolve the report
                break;

            default:
                throw new RuntimeException("Unknown moderation action: " + action);
        }

        // Auto-resolve all related reports for the same entity
        List<Report> relatedReports = reportRepository.findByReportTypeAndEntityId(
            reportType, entityId
        );

        for (Report relatedReport : relatedReports) {
            if ("PENDING".equals(relatedReport.getStatus()) ||
                "REVIEWING".equals(relatedReport.getStatus())) {
                relatedReport.setStatus("RESOLVED");
                relatedReport.setResolutionAction(action);
                relatedReport.setAdminNote(adminNote);
                relatedReport.setResolvedBy(adminId);
                relatedReport.setResolvedAt(LocalDateTime.now());
                reportRepository.save(relatedReport);
            }
        }
    }

    /**
     * Blind a post (hide from public view)
     *
     * @param postId ID of the post to blind
     * @param reason Reason for blinding the post
     * @throws RuntimeException if post not found
     */
    private void handleBlindPost(Long postId, String reason) {
        Optional<Post> postOptional = postRepository.findById(postId);

        if (!postOptional.isPresent()) {
            throw new RuntimeException("Post not found with id: " + postId);
        }

        Post post = postOptional.get();
        post.setIsBlinded(true);
        post.setBlindReason(reason);
        postRepository.save(post);
    }

    /**
     * Delete a post permanently
     *
     * @param postId ID of the post to delete
     * @throws RuntimeException if post not found
     */
    private void handleDeletePost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found with id: " + postId);
        }
        postRepository.deleteById(postId);
    }

    /**
     * Mark a comment as deleted (soft delete)
     *
     * @param commentId ID of the comment to delete
     * @throws RuntimeException if comment not found
     */
    private void handleDeleteComment(Long commentId) {
        Optional<Comment> commentOptional = commentRepository.findById(commentId);

        if (!commentOptional.isPresent()) {
            throw new RuntimeException("Comment not found with id: " + commentId);
        }

        Comment comment = commentOptional.get();
        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }

    /**
     * Validate report submission
     * Checks if entity exists and reporter is not the author
     *
     * @param reporterId ID of the reporter
     * @param reportType Type of report (POST, COMMENT, USER)
     * @param entityId ID of the entity being reported
     * @throws RuntimeException if validation fails
     */
    private void validateReportSubmission(Long reporterId, String reportType, Long entityId) {
        switch (reportType) {
            case "POST":
                validatePostReport(reporterId, entityId);
                break;

            case "COMMENT":
                validateCommentReport(reporterId, entityId);
                break;

            case "USER":
                validateUserReport(reporterId, entityId);
                break;

            default:
                throw new RuntimeException("Invalid report type: " + reportType);
        }
    }

    /**
     * Validate post report submission
     *
     * @param reporterId ID of the reporter
     * @param postId ID of the post being reported
     * @throws RuntimeException if validation fails
     */
    private void validatePostReport(Long reporterId, Long postId) {
        Optional<Post> postOptional = postRepository.findById(postId);

        if (!postOptional.isPresent()) {
            throw new RuntimeException("Post not found with id: " + postId);
        }

        Post post = postOptional.get();
        if (post.getUserId().equals(reporterId)) {
            throw new RuntimeException("You cannot report your own post");
        }
    }

    /**
     * Validate comment report submission
     *
     * @param reporterId ID of the reporter
     * @param commentId ID of the comment being reported
     * @throws RuntimeException if validation fails
     */
    private void validateCommentReport(Long reporterId, Long commentId) {
        Optional<Comment> commentOptional = commentRepository.findById(commentId);

        if (!commentOptional.isPresent()) {
            throw new RuntimeException("Comment not found with id: " + commentId);
        }

        Comment comment = commentOptional.get();
        if (comment.getUserId().equals(reporterId)) {
            throw new RuntimeException("You cannot report your own comment");
        }
    }

    /**
     * Validate user report submission
     *
     * @param reporterId ID of the reporter
     * @param targetUserId ID of the user being reported
     * @throws RuntimeException if validation fails
     */
    private void validateUserReport(Long reporterId, Long targetUserId) {
        if (reporterId.equals(targetUserId)) {
            throw new RuntimeException("You cannot report yourself");
        }

        Optional<User> userOptional = userRepository.findById(targetUserId);
        if (!userOptional.isPresent()) {
            throw new RuntimeException("User not found with id: " + targetUserId);
        }
    }
}
