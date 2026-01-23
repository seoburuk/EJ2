package com.ej2.dto;

/**
 * パスワードリセットリクエスト用のDTO
 */
public class PasswordResetRequest {
    private String email;

    public PasswordResetRequest() {
    }

    public PasswordResetRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
