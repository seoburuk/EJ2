package com.ej2.controller;

import com.ej2.dto.*;
import com.ej2.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

import javax.servlet.http.HttpSession;

/**
 * 認証コントローラー - ログイン、会員登録、パスワードリセットのエンドポイント
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * ログインエンドポイント
     * POST /api/auth/login
     * @param request ログインリクエスト（username, password）
     * @param session HTTPセッション
     * @return 認証レスポンス
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        AuthResponse response = authService.login(request);

        if (response.isSuccess()) {
            // セッションにユーザーIDを保存
            session.setAttribute("userId", response.getUser().getId());
            session.setAttribute("user", response.getUser());

            // Spring Security SecurityContext에 인증 정보를 저장한다.
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(response.getUser().getId(), 
                                                        null, 
                                                        Collections.singletonList(new SimpleGrantedAuthority("USER")));

            // Context에 설정
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // 세션에 Context 저장
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * ログアウトエンドポイント
     * POST /api/auth/logout
     * @param session HTTPセッション
     * @return 認証レスポンス
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpSession session) {
        // SecurityContext 초기화
        SecurityContextHolder.clearContext();

        if (session != null) {
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

            session.invalidate();
        }
        return ResponseEntity.ok(new AuthResponse(true, "ログアウトしました"));
    }

    /**
     * 現在のログインユーザー情報を取得
     * GET /api/auth/me
     * @param session HTTPセッション
     * @return 認証レスポンス
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user != null) {
            return ResponseEntity.ok(new AuthResponse(true, "ログイン中", (com.ej2.model.User) user));
        } else {
            return ResponseEntity.status(401).body(new AuthResponse(false, "ログインしていません"));
        }
    }

    /**
     * 会員登録エンドポイント
     * POST /api/auth/register
     * @param request 会員登録リクエスト（username, name, email, password）
     * @param session HTTPセッション
     * @return 認証レスポンス
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpSession session) {
        AuthResponse response = authService.register(request);

        if (response.isSuccess()) {
            // メール認証が必要なため、セッションは作成しない
            // ユーザーはメール認証後にログインする必要がある
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * ID検索エンドポイント
     * POST /api/auth/find-username
     * @param request リクエスト（email, name）
     * @return 認証レスポンス（ユーザー名を含む）
     */
    @PostMapping("/find-username")
    public ResponseEntity<AuthResponse> findUsername(@RequestBody FindUsernameRequest request) {
        AuthResponse response = authService.findUsernameByEmailAndName(request.getEmail(), request.getName());
        return ResponseEntity.ok(response);
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

    /**
     * メールアドレス認証エンドポイント
     * POST /api/auth/verify-email
     * @param request メール認証リクエスト（token）
     * @return 認証レスポンス
     */
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody EmailVerificationRequest request) {
        try {
            AuthResponse response = authService.verifyEmail(request.getToken());

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                if ("TOKEN_EXPIRED".equals(response.getMessage())) {
                    return ResponseEntity.badRequest().body(response);
                }
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new AuthResponse(false, "認証処理中にエラーが発生しました"));
        }
    }

    /**
     * メール認証メール再送信エンドポイント
     * POST /api/auth/resend-verification
     * @param request 再送信リクエスト（email）
     * @return 認証レスポンス
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerification(@RequestBody ResendVerificationRequest request) {
        AuthResponse response = authService.resendVerificationEmail(request.getEmail());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
