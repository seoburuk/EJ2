package com.ej2.dto;

/**
 * メール認証リクエスト用のDTO
 */
public class EmailVerificationRequest {
    private String token;

    public EmailVerificationRequest() {
    }

    public EmailVerificationRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
