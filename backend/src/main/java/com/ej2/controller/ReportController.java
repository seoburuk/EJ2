package com.ej2.controller;

import com.ej2.dto.ReportDTO;
import com.ej2.model.Report;
import com.ej2.model.User;
import com.ej2.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一般ユーザー用報告API
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 報告を提出
     * POST /api/reports
     */
    @PostMapping
    public ResponseEntity<?> submitReport(
            @RequestBody Map<String, Object> request,
            HttpSession session
    ) {
        try {
            // セッションからユーザー取得
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("message", "ログインが必要です");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // リクエストからパラメータ取得
            String reportType = (String) request.get("reportType");
            Long entityId = Long.valueOf(request.get("entityId").toString());
            String reason = (String) request.get("reason");
            String description = (String) request.get("description");

            // 報告提出
            Report report = reportService.submitReport(
                    currentUser.getId(),
                    reportType,
                    entityId,
                    reason,
                    description
            );

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("message", "報告が受理されました");
            response.put("reportId", report.getId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("message", "報告の提出に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 自分の報告履歴を取得
     * GET /api/reports/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyReports(HttpSession session) {
        try {
            // セッションからユーザー取得
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("message", "ログインが必要です");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            List<Report> reports = reportService.getMyReports(currentUser.getId());
            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("message", "報告履歴の取得に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
