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

    public ReportStatsDTO getReportStats() {
        return reportMapper.selectReportStats();
    }

    public List<ReportDTO> searchReports(ReportSearchCriteria criteria, int page, int size) {
        int offset = page * size;
        return reportMapper.selectReports(criteria, offset, size);
    }

    /**
     * [FIX] Controller에서 사용하는 내 신고 내역 조회 메소드 추가
     * (ReportSearchCriteria DTO에 reporterId 필드가 있어야 합니다)
     */
    public List<ReportDTO> getMyReports(Long userId) {
        ReportSearchCriteria criteria = new ReportSearchCriteria();
        criteria.setReporterId(userId); 
        return reportMapper.selectReports(criteria, 0, 100);
    }

    public int countReports(ReportSearchCriteria criteria) {
        return reportMapper.countReports(criteria);
    }

    public ReportDetailDTO getReportDetail(Long reportId) {
        return reportMapper.selectReportDetail(reportId);
    }

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

        if ("RESOLVED".equals(newStatus) || "DISMISSED".equals(newStatus)) {
            report.setResolvedBy(adminId);
            report.setResolvedAt(LocalDateTime.now());
        }

        return reportRepository.save(report);
    }

    public void takeModerationAction(Long reportId, String action,
                                     String adminNote, Long adminId) {
        ReportDetailDTO reportDetail = reportMapper.selectReportDetail(reportId);

        if (reportDetail == null) {
            throw new RuntimeException("Report not found with id: " + reportId);
        }

        Long entityId = reportDetail.getEntityId();
        String reportType = reportDetail.getReportType();

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
                break;
            case "NO_ACTION":
                break;
            default:
                throw new RuntimeException("Unknown moderation action: " + action);
        }

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

    private void handleDeletePost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found with id: " + postId);
        }
        postRepository.deleteById(postId);
    }

    private void handleDeleteComment(Long commentId) {
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (!commentOptional.isPresent()) {
            throw new RuntimeException("Comment not found with id: " + commentId);
        }
        Comment comment = commentOptional.get();
        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }

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
     * [FIX] UserRepository 타입 불일치 문제 해결
     * existsById 대신 findById(id) == null 체크 사용
     */
    private void validateUserReport(Long reporterId, Long targetUserId) {
        if (reporterId.equals(targetUserId)) {
            throw new RuntimeException("You cannot report yourself");
        }

        // UserRepository가 Optional이 아닌 User 객체를 반환하므로 null 체크로 검증
        User targetUser = userRepository.findById(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("User not found with id: " + targetUserId);
        }
    }
}