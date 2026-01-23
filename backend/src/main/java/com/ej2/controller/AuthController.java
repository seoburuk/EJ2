package com.ej2.controller;

import com.ej2.dto.*;
import com.ej2.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 認証コントローラー - ログイン、会員登録、パスワードリセットのエンドポイント
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * ログインエンドポイント
     * POST /api/auth/login
     * @param request ログインリクエスト（username, password）
     * @return 認証レスポンス
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * 会員登録エンドポイント
     * POST /api/auth/register
     * @param request 会員登録リクエスト（username, name, email, password）
     * @return 認証レスポンス
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * パスワードリセットリクエストエンドポイント
     * POST /api/auth/password-reset/request
     * @param request パスワードリセットリクエスト（email）
     * @return 認証レスポンス
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<AuthResponse> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        AuthResponse response = authService.requestPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    /**
     * パスワードリセット確認エンドポイント
     * POST /api/auth/password-reset/confirm
     * @param request パスワードリセット確認リクエスト（token, newPassword）
     * @return 認証レスポンス
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<AuthResponse> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        AuthResponse response = authService.confirmPasswordReset(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
