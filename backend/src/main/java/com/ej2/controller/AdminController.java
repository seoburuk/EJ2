package com.ej2.controller;

import com.ej2.model.Board;
import com.ej2.model.User;
import com.ej2.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ==================== 管理者権限検証 ====================

    private boolean isAdmin(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj == null) {
            return false;
        }
        User currentUser = (User) userObj;
        return "ADMIN".equals(currentUser.getRole());
    }

    private ResponseEntity<?> checkAdminAccess(HttpSession session) {
        if (!isAdmin(session)) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "管理者権限が必要です");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        return null;
    }

    // ==================== ダッシュボード統計API ====================

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        Map<String, Object> stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/weekly")
    public ResponseEntity<?> getWeeklyStats(HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        List<Map<String, Object>> weeklyStats = adminService.getWeeklyStats();
        return ResponseEntity.ok(weeklyStats);
    }

    @GetMapping("/dashboard/activity")
    public ResponseEntity<?> getRecentActivity(HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        List<Map<String, Object>> activities = adminService.getRecentActivity();
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/dashboard/board-stats")
    public ResponseEntity<?> getBoardPostStats(HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        List<Map<String, Object>> boardStats = adminService.getBoardPostStats();
        return ResponseEntity.ok(boardStats);
    }

    // ==================== ユーザー管理API ====================

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        List<User> users = adminService.getAllUsers(page, size);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        String newRole = request.get("role");
        if (newRole == null || (!newRole.equals("ADMIN") && !newRole.equals("USER"))) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "無効な権限です");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            User updatedUser = adminService.updateUserRole(userId, newRole);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        // 自分自身の削除を防止
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getId().equals(userId)) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "自分自身は削除できません");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            adminService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== 掲示板管理API ====================

    @GetMapping("/boards")
    public ResponseEntity<?> getAllBoards(HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        List<Board> boards = adminService.getAllBoards();
        return ResponseEntity.ok(boards);
    }

    @PostMapping("/boards")
    public ResponseEntity<?> createBoard(
            @RequestBody Board board,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            Board createdBoard = adminService.createBoard(board);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBoard);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/boards/{boardId}")
    public ResponseEntity<?> updateBoard(
            @PathVariable Long boardId,
            @RequestBody Board boardDetails,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            Board updatedBoard = adminService.updateBoard(boardId, boardDetails);
            return ResponseEntity.ok(updatedBoard);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/boards/{boardId}")
    public ResponseEntity<?> deleteBoard(
            @PathVariable Long boardId,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            adminService.deleteBoard(boardId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== 報告管理API ====================

    @GetMapping("/reports/stats")
    public ResponseEntity<?> getReportStats(HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            return ResponseEntity.ok(adminService.getReportStats());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "統計の取得に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<?> searchReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            Map<String, Object> result = adminService.searchReports(
                    status, reportType, sortBy, sortOrder, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "報告一覧の取得に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<?> getReportDetail(
            @PathVariable Long reportId,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            return ResponseEntity.ok(adminService.getReportDetail(reportId));
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "報告詳細の取得に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/reports/{reportId}/status")
    public ResponseEntity<?> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            User currentUser = (User) session.getAttribute("user");
            String status = request.get("status");
            String adminNote = request.get("adminNote");

            adminService.updateReportStatus(reportId, status, adminNote, currentUser.getId());

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "報告のステータスが更新されました");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "ステータス更新に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/reports/{reportId}/actions")
    public ResponseEntity<?> takeModerationAction(
            @PathVariable Long reportId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            User currentUser = (User) session.getAttribute("user");
            String action = request.get("action");
            String adminNote = request.get("adminNote");

            adminService.takeModerationAction(reportId, action, adminNote, currentUser.getId());

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "モデレーションアクションが実行されました");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "アクション実行に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ==================== ユーザー停止API ====================

    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<?> suspendUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            String duration = request.get("duration");
            String reason = request.get("reason");

            if (reason == null || reason.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<String, Object>();
                error.put("success", false);
                error.put("message", "停止理由は必須です");
                return ResponseEntity.badRequest().body(error);
            }

            adminService.suspendUser(userId, duration, reason);

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "ユーザーが停止されました");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "ユーザー停止に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/users/{userId}/unsuspend")
    public ResponseEntity<?> unsuspendUser(
            @PathVariable Long userId,
            HttpSession session) {
        ResponseEntity<?> accessCheck = checkAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            adminService.unsuspendUser(userId);

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "ユーザーの停止が解除されました");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "停止解除に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
