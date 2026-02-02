package com.ej2.service;

import com.ej2.dto.ActivityDTO;
import com.ej2.dto.BoardPostStatsDTO;
import com.ej2.dto.DashboardStatsDTO;
import com.ej2.dto.WeeklyStatDTO;
import com.ej2.mapper.AdminMapper;
import com.ej2.model.Board;
import com.ej2.model.Post;
import com.ej2.model.User;
import com.ej2.repository.BoardRepository;
import com.ej2.repository.PostRepository;
import com.ej2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * 管理者機能サービス
 * MyBatisとJPAのハイブリッド構成
 */
@Service
@Transactional
public class AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostRepository postRepository;

    // ==================== ユーザー管理 ====================

    /**
     * 全ユーザー一覧取得（ページネーション）
     */
    public List<User> getAllUsers(int page, int size) {
        int offset = page * size;
        return adminMapper.selectUsers(offset, size);
    }

    /**
     * 全ユーザー数を取得
     */
    public int getTotalUserCount() {
        return adminMapper.countUsers();
    }

    /**
     * ユーザー権限を変更
     */
    public User updateUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        user.setRole(newRole);
        return userRepository.save(user);
    }

    /**
     * ユーザーを削除
     */
    public void deleteUser(Long userId) {
        adminMapper.deleteUser(userId);
    }

    // ==================== 掲示板管理 (JPA) ====================

    /**
     * 掲示板を作成
     */
    public Board createBoard(Board board) {
        return boardRepository.save(board);
    }

    /**
     * 掲示板を更新
     */
    public Board updateBoard(Long boardId, Board boardDetails) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found: " + boardId));
        board.setName(boardDetails.getName());
        board.setDescription(boardDetails.getDescription());
        board.setCode(boardDetails.getCode());
        board.setIsAnonymous(boardDetails.getIsAnonymous());
        board.setRequireAdmin(boardDetails.getRequireAdmin());
        return boardRepository.save(board);
    }

    /**
     * 掲示板を削除
     */
    public void deleteBoard(Long boardId) {
        boardRepository.deleteById(boardId);
    }

    /**
     * 全掲示板一覧を取得
     */
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }

    // ==================== ダッシュボード統計 (MyBatis) ====================

    /**
     * ダッシュボード基本統計を取得（MyBatis使用）
     */
    public DashboardStatsDTO getDashboardStatsMyBatis() {
        return adminMapper.selectDashboardStats();
    }

    /**
     * ダッシュボード統計を取得（Map形式 - 既存互換）
     */
    public Map<String, Object> getDashboardStats() {
        DashboardStatsDTO stats = adminMapper.selectDashboardStats();
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("totalUsers", stats.getTotalUsers());
        result.put("adminCount", stats.getAdminCount());
        result.put("userCount", stats.getTotalUsers() - stats.getAdminCount());
        result.put("totalBoards", stats.getTotalBoards());
        result.put("totalPosts", stats.getTotalPosts());
        result.put("totalComments", stats.getTotalComments());
        result.put("newUsersThisWeek", stats.getNewUsersThisWeek());
        result.put("newPostsThisWeek", stats.getNewPostsThisWeek());

        // ユーザー分布（ドーナツチャート用）
        List<Map<String, Object>> userDistribution = new ArrayList<Map<String, Object>>();
        Map<String, Object> adminData = new HashMap<String, Object>();
        adminData.put("name", "管理者");
        adminData.put("value", stats.getAdminCount());
        adminData.put("color", "#f6d365");
        userDistribution.add(adminData);

        Map<String, Object> userData = new HashMap<String, Object>();
        userData.put("name", "一般ユーザー");
        userData.put("value", stats.getTotalUsers() - stats.getAdminCount());
        userData.put("color", "#667eea");
        userDistribution.add(userData);

        result.put("userDistribution", userDistribution);

        return result;
    }

    /**
     * 週間トレンドデータを取得（MyBatis使用）
     */
    public List<WeeklyStatDTO> getWeeklyStatsMyBatis() {
        return adminMapper.selectWeeklyStats();
    }

    /**
     * 週間活動トレンドデータを取得（チャート用）
     */
    public List<Map<String, Object>> getWeeklyStats() {
        List<WeeklyStatDTO> weeklyStats = adminMapper.selectWeeklyStats();
        List<Map<String, Object>> weeklyData = new ArrayList<Map<String, Object>>();

        String[] dayNames = {"日", "月", "火", "水", "木", "金", "土"};

        for (WeeklyStatDTO stat : weeklyStats) {
            Map<String, Object> dayData = new HashMap<String, Object>();

            // 曜日名
            if (stat.getDate() != null) {
                int dayOfWeek = stat.getDate().getDayOfWeek().getValue() % 7;
                dayData.put("name", dayNames[dayOfWeek]);
                dayData.put("date", stat.getDate().toString());
            }

            dayData.put("users", stat.getUserCount() != null ? stat.getUserCount() : 0);
            dayData.put("posts", stat.getPostCount() != null ? stat.getPostCount() : 0);
            dayData.put("comments", stat.getCommentCount() != null ? stat.getCommentCount() : 0);

            weeklyData.add(dayData);
        }

        return weeklyData;
    }

    /**
     * 最近のアクティビティを取得（MyBatis使用）
     */
    public List<ActivityDTO> getRecentActivityMyBatis(int limit) {
        return adminMapper.selectRecentActivity(limit);
    }

    /**
     * 最近のアクティビティフィードを取得
     */
    public List<Map<String, Object>> getRecentActivity() {
        List<ActivityDTO> activities = adminMapper.selectRecentActivity(5);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        for (ActivityDTO activity : activities) {
            Map<String, Object> activityMap = new HashMap<String, Object>();
            activityMap.put("type", activity.getType());
            activityMap.put("time", activity.getCreatedAt());

            // タイプに応じたアイコンとメッセージを設定
            if ("USER_JOINED".equals(activity.getType())) {
                activityMap.put("icon", "👤");
                activityMap.put("message", activity.getContent() + "さんが登録しました");
            } else if ("POST_CREATED".equals(activity.getType())) {
                activityMap.put("icon", "📝");
                activityMap.put("message", "新規投稿: " + truncateString(activity.getContent(), 20));
            } else if ("COMMENT_ADDED".equals(activity.getType())) {
                activityMap.put("icon", "💬");
                activityMap.put("message", "新規コメント: " + truncateString(activity.getContent(), 20));
            }

            result.add(activityMap);
        }

        return result;
    }

    /**
     * 文字列を切り詰めるヘルパー
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    // ==================== 掲示板別投稿統計 ====================

    /**
     * 掲示板別投稿統計を取得（バーチャート用）
     * 各掲示板の総投稿数と週間増加数を返します
     */
    public List<Map<String, Object>> getBoardPostStats() {
        List<BoardPostStatsDTO> stats = adminMapper.selectBoardPostStats();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        for (BoardPostStatsDTO stat : stats) {
            Map<String, Object> boardData = new HashMap<String, Object>();
            boardData.put("boardId", stat.getBoardId());
            boardData.put("name", stat.getBoardName());
            boardData.put("totalPosts", stat.getTotalPosts());
            boardData.put("weeklyIncrease", stat.getWeeklyIncrease());
            result.add(boardData);
        }

        return result;
    }
}
