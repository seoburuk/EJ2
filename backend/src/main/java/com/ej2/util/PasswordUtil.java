package com.ej2.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * パスワード暗号化とトークン生成のためのユーティリティクラス
 */
public class PasswordUtil {

    private static final int BCRYPT_ROUNDS = 12;

    /**
     * BCryptを使用してパスワードをハッシュ化
     * @param plainPassword 平文パスワード
     * @return ハッシュ化されたパスワード
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * パスワードをハッシュと照合して検証
     * @param plainPassword 平文パスワード
     * @param hashedPassword ハッシュ化されたパスワード
     * @return パスワードが一致する場合はtrue、そうでない場合はfalse
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    /**
     * パスワードリセット用のランダムトークンを生成
     * @return ランダムなトークン文字列
     */
    public static String generateResetToken() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * テスト用mainメソッド - BCryptハッシュを生成
     */
    public static void main(String[] args) {
        String password = "password123";

        String hash = hashPassword(password);
        System.out.println("BCrypt hash for 'password123': " + hash);

        boolean isValid = verifyPassword(password, hash);
        System.out.println("Password verification result: " + isValid);
    }
}