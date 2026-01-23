package com.ej2.service;

import com.ej2.dto.*;
import com.ej2.model.User;
import com.ej2.repository.UserRepository;
import com.ej2.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 認証サービス - ログイン、会員登録、パスワードリセットのビジネスロジック
 */
@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    private static final int RESET_TOKEN_EXPIRY_HOURS = 24;

    /**
     * ユーザーログイン
     * @param request ログインリクエスト
     * @return 認証レスポンス
     */
    public AuthResponse login(LoginRequest request) {
        // 1. usernameでユーザーを検索
        User user = userRepository.findByUsername(request.getUsername());

        System.out.println("=== ログイン試行 ===");
        System.out.println("ユーザー名: " + request.getUsername());
        System.out.println("ユーザー検索結果: " + (user != null ? "見つかりました" : "見つかりません"));

        // 2. ユーザーが存在しない場合
        if (user == null) {
            // タイミング攻撃防止: 実際のパスワード検証と同じ時間がかかるように
            PasswordUtil.verifyPassword("dummy", "$2a$12$dummyhashfordemo");
            return new AuthResponse(false, "ユーザー名またはパスワードが正しくありません");
        }

        System.out.println("入力されたパスワード: " + request.getPassword());
        System.out.println("DBのハッシュ: " + user.getPassword());

        // 3. パスワード検証
        boolean isPasswordValid = PasswordUtil.verifyPassword(
                request.getPassword(),
                user.getPassword()
        );

        System.out.println("パスワード検証結果: " + isPasswordValid);

        // 4. パスワードが一致しない場合
        if (!isPasswordValid) {
            return new AuthResponse(false, "ユーザー名またはパスワードが正しくありません");
        }

        // 5. 成功時、ユーザー情報と共に応答を返す
        return new AuthResponse(true, "ログインに成功しました", user);
    }
    /**
     * 新規ユーザー登録
     * @param request 会員登録リクエスト
     * @return 認証レスポンス
     */
    public AuthResponse register(RegisterRequest request) {
        // ユーザー名の重複チェック
        if (userRepository.existsByUsername(request.getUsername())) {
            return new AuthResponse(false, "ユーザー名は既に使用されています");
        }

        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(false, "メールアドレスは既に登録されています");
        }

        // バリデーション
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return new AuthResponse(false, "ユーザー名は必須です");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return new AuthResponse(false, "パスワードは6文字以上である必要があります");
        }

        // パスワードをハッシュ化
        String hashedPassword = PasswordUtil.hashPassword(request.getPassword());

        // 新規ユーザーを作成
        User user = new User(
                request.getUsername(),
                request.getName(),
                request.getEmail(),
                hashedPassword
        );

        // ユーザーを保存
        User savedUser = userRepository.save(user);

        return new AuthResponse(true, "会員登録が完了しました", savedUser);
    }

    /**
     * パスワードリセットトークンを生成してメール送信（メール送信は今回は省略）
     * @param request パスワードリセットリクエスト
     * @return 認証レスポンス
     */
    public AuthResponse requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail());

        if (user == null) {
            // セキュリティ上、ユーザーが存在しない場合でも成功メッセージを返す
            return new AuthResponse(true, "リセットトークンがメールアドレスに送信されました");
        }

        // リセットトークンを生成
        String resetToken = PasswordUtil.generateResetToken();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS));

        userRepository.save(user);

        // TODO: 実際のアプリケーションでは、ここでメール送信処理を実装
        // 今回はトークンをレスポンスに含めて返す（開発用）
        System.out.println("パスワードリセットトークン: " + resetToken);
        System.out.println("ユーザー: " + user.getEmail());

        return new AuthResponse(true, "リセットトークン: " + resetToken);
    }

    /**
     * パスワードリセットを確認して新しいパスワードを設定
     * @param request パスワードリセット確認リクエスト
     * @return 認証レスポンス
     */
    public AuthResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByResetToken(request.getToken());

        if (user == null) {
            return new AuthResponse(false, "無効なリセットトークンです");
        }

        // トークンの有効期限をチェック
        if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            return new AuthResponse(false, "リセットトークンの有効期限が切れています");
        }

        // 新しいパスワードをハッシュ化
        String hashedPassword = PasswordUtil.hashPassword(request.getNewPassword());
        user.setPassword(hashedPassword);

        // リセットトークンをクリア
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);

        return new AuthResponse(true, "パスワードが正常にリセットされました");
    }
}
