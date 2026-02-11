package com.ej2.dto;

/**
 * メール認証再送信リクエスト用のDTO
 */
public class ResendVerificationRequest {
    private String email;

    public ResendVerificationRequest() {
    }

    public ResendVerificationRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
