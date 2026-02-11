package com.ej2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * メール送信サービス - Gmail SMTPを使用したメール送信機能
 */
@Service
public class EmailService {

    @Value("${mail.username:}")
    private String mailUsername;

    @Value("${mail.password:}")
    private String mailPassword;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String FROM_NAME = "EJ2 - エブリージャパン";

    /**
     * メール認証用のメールを送信
     * @param recipientEmail 受信者のメールアドレス
     * @param recipientName 受信者の名前
     * @param verificationToken 認証トークン
     * @throws MessagingException JavaMail送信時のエラー
     */
    public void sendVerificationEmail(String recipientEmail, String recipientName, String verificationToken) throws MessagingException {
        // Gmail認証情報が設定されていない場合は、コンソールに出力のみ
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            System.out.println("==================== メール認証情報 ====================");
            System.out.println("受信者: " + recipientEmail + " (" + recipientName + ")");
            System.out.println("認証URL: " + frontendUrl + "/verify-email?token=" + verificationToken);
            System.out.println("トークン: " + verificationToken);
            System.out.println("有効期限: 24時間");
            System.out.println("=========================================================");
            return;
        }

        String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
        String htmlContent = buildVerificationEmailHtml(recipientName, verificationUrl);

        // Gmail SMTP設定
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        // セッション生成
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailUsername, mailPassword);
            }
        });

        try {
            // メッセージ生成
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailUsername, FROM_NAME, "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("【EJ2】メールアドレスの認証をお願いします");
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            // 送信
            Transport.send(message);
            System.out.println("メール送信成功: " + recipientEmail);
        } catch (MessagingException e) {
            System.err.println("Gmail SMTP送信エラー: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("メール送信エラー: " + e.getMessage());
            throw new MessagingException("メール送信に失敗しました", e);
        }
    }

    /**
     * メール認証用のHTMLコンテンツを生成
     * @param recipientName 受信者の名前
     * @param verificationUrl 認証URL
     * @return HTMLメールコンテンツ
     */
    private String buildVerificationEmailHtml(String recipientName, String verificationUrl) {
        return "<!DOCTYPE html>" +
                "<html lang='ja'>" +
                "<head>" +
                "  <meta charset='UTF-8'>" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "  <title>メールアドレス認証</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
                "  <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f4f4f4; padding: 40px 0;'>" +
                "    <tr>" +
                "      <td align='center'>" +
                "        <table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                "          <!-- Header -->" +
                "          <tr>" +
                "            <td style='background-color: #3498db; color: #ffffff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "              <h1 style='margin: 0; font-size: 28px;'>EJ2 - エブリージャパン</h1>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Content -->" +
                "          <tr>" +
                "            <td style='padding: 40px 30px;'>" +
                "              <p style='font-size: 16px; color: #333; margin: 0 0 20px;'>こんにちは、<strong>" + recipientName + "</strong> さん</p>" +
                "              <p style='font-size: 16px; color: #333; margin: 0 0 20px; line-height: 1.6;'>EJ2への会員登録ありがとうございます。</p>" +
                "              <p style='font-size: 16px; color: #333; margin: 0 0 30px; line-height: 1.6;'>アカウントを有効化するには、以下のボタンをクリックしてメールアドレスの認証を完了してください。</p>" +
                "              <!-- Verification Button -->" +
                "              <div style='text-align: center; margin: 40px 0;'>" +
                "                <a href='" + verificationUrl + "' style='display: inline-block; background-color: #3498db; color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 18px; font-weight: bold;'>メールアドレスを認証する</a>" +
                "              </div>" +
                "              <p style='font-size: 14px; color: #666; margin: 30px 0 20px; line-height: 1.6;'>ボタンが機能しない場合は、以下のリンクをブラウザにコピー＆ペーストしてください：</p>" +
                "              <p style='font-size: 12px; color: #3498db; word-break: break-all; background-color: #f8f9fa; padding: 15px; border-radius: 4px; border-left: 4px solid #3498db;'>" + verificationUrl + "</p>" +
                "              <div style='margin-top: 30px; padding-top: 30px; border-top: 1px solid #eeeeee;'>" +
                "                <p style='font-size: 14px; color: #999; margin: 0 0 10px;'><strong>注意事項：</strong></p>" +
                "                <ul style='font-size: 14px; color: #999; margin: 0; padding-left: 20px;'>" +
                "                  <li>この認証リンクの有効期限は<strong>24時間</strong>です</li>" +
                "                  <li>メールに心当たりがない場合は、このメールを無視してください</li>" +
                "                  <li>メールが届かない場合は、迷惑メールフォルダをご確認ください</li>" +
                "                </ul>" +
                "              </div>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style='background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px;'>" +
                "              <p style='font-size: 12px; color: #999; margin: 0;'>&copy; 2026 EJ2 - エブリージャパン. All rights reserved.</p>" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }
}
