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
            System.out.println("認証URL: " + frontendUrl + "/verify-email?token=[TOKEN]");
            System.out.println("有効期限: 24時間");
            System.out.println("注意: メール設定未構成のため、実際のメールは送信されていません");
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
     * パスワードリセット用のメールを送信
     * @param recipientEmail 受信者のメールアドレス
     * @param recipientName 受信者の名前
     * @param resetToken リセットトークン
     * @throws MessagingException JavaMail送信時のエラー
     */
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) throws MessagingException {
        // Gmail認証情報が設定されていない場合は、コンソールに出力のみ
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            System.out.println("==================== パスワードリセット情報 ====================");
            System.out.println("受信者: " + recipientEmail + " (" + recipientName + ")");
            System.out.println("リセットURL: " + frontendUrl + "/password-reset/confirm?token=[TOKEN]");
            System.out.println("有効期限: 24時間");
            System.out.println("注意: メール設定未構成のため、実際のメールは送信されていません");
            System.out.println("==============================================================");
            return;
        }

        String resetUrl = frontendUrl + "/password-reset/confirm?token=" + resetToken;
        String htmlContent = buildPasswordResetEmailHtml(recipientName, resetUrl);

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
            message.setSubject("【EJ2】パスワードリセットのご案内");
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            // 送信
            Transport.send(message);
            System.out.println("パスワードリセットメール送信成功: " + recipientEmail);
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
                "              <p style='font-size: 16px; color: #333; margin: 0 0 20px;'>こんにちは、<strong>" + recipientName + "</strong> さん！</p>" +
                "              <p style='font-size: 16px; color: #333; margin: 0 0 10px; line-height: 1.6;'>EJ2へようこそ！会員登録ありがとうございます。</p>" +
                "              <p style='font-size: 16px; color: #333; margin: 0 0 30px; line-height: 1.6;'>EJ2は、あなたの時間割管理をサポートする便利なサービスです。</p>" +
                "              <div style='background-color: #e3f2fd; padding: 20px; border-radius: 6px; margin: 0 0 30px;'>" +
                "                <p style='font-size: 15px; color: #1976d2; margin: 0 0 10px; font-weight: bold;'>📧 メール認証を完了してください</p>" +
                "                <p style='font-size: 14px; color: #333; margin: 0; line-height: 1.6;'>アカウントを有効化するには、以下のボタンをクリックしてメールアドレスの認証を完了してください。</p>" +
                "              </div>" +
                "              <!-- Verification Button -->" +
                "              <div style='text-align: center; margin: 40px 0;'>" +
                "                <a href='" + verificationUrl + "' style='display: inline-block; background-color: #3498db; color: #ffffff; text-decoration: none; padding: 16px 45px; border-radius: 6px; font-size: 18px; font-weight: bold;'>メールアドレスを認証する</a>" +
                "              </div>" +
                "              <p style='font-size: 14px; color: #666; margin: 30px 0 20px; line-height: 1.6;'>ボタンが機能しない場合は、以下のリンクをブラウザにコピー＆ペーストしてください：</p>" +
                "              <p style='font-size: 12px; color: #3498db; word-break: break-all; background-color: #f8f9fa; padding: 15px; border-radius: 4px; border-left: 4px solid #3498db;'>" + verificationUrl + "</p>" +
                "              <!-- Next Steps -->" +
                "              <div style='margin-top: 40px; padding: 25px; background-color: #f8f9fa; border-radius: 6px;'>" +
                "                <p style='font-size: 15px; color: #333; margin: 0 0 15px; font-weight: bold;'>📋 次のステップ</p>" +
                "                <ul style='font-size: 14px; color: #555; margin: 0; padding-left: 20px; line-height: 1.8;'>" +
                "                  <li>メール認証を完了</li>" +
                "                  <li>ログインして時間割作成を開始</li>" +
                "                  <li>コースを追加して自分だけの時間割を作成</li>" +
                "                </ul>" +
                "              </div>" +
                "              <div style='margin-top: 30px; padding-top: 30px; border-top: 1px solid #eeeeee;'>" +
                "                <p style='font-size: 14px; color: #999; margin: 0 0 10px;'><strong>⚠️ 注意事項：</strong></p>" +
                "                <ul style='font-size: 14px; color: #999; margin: 0; padding-left: 20px;'>" +
                "                  <li>この認証リンクの有効期限は<strong>24時間</strong>です</li>" +
                "                  <li>このメールは会員登録を要請した方にのみ送信されます</li>" +
                "                  <li>心当たりがない場合は、このメールを無視してください</li>" +
                "                  <li>メールが届かない場合は、迷惑メールフォルダをご確認ください</li>" +
                "                </ul>" +
                "              </div>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style='background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px;'>" +
                "              <p style='font-size: 12px; color: #999; margin: 0 0 10px;'>お困りの際は、support@ej2.com までお問い合わせください。</p>" +
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

    /**
     * パスワードリセット用のHTMLコンテンツを生成
     * @param recipientName 受信者の名前
     * @param resetUrl リセットURL
     * @return HTMLメールコンテンツ
     */
    private String buildPasswordResetEmailHtml(String recipientName, String resetUrl) {
        return "<!DOCTYPE html>" +
                "<html lang='ja'>" +
                "<head>" +
                "  <meta charset='UTF-8'>" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "  <title>パスワードリセット</title>" +
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
                "              <p style='font-size: 16px; color: #333; margin: 0 0 20px; line-height: 1.6;'>EJ2アカウントのパスワードリセットリクエストを受け付けました。</p>" +
                "              <p style='font-size: 16px; color: #333; margin: 0 0 30px; line-height: 1.6;'>パスワードをリセットするには、以下のボタンをクリックして新しいパスワードを設定してください。</p>" +
                "              <!-- Reset Button -->" +
                "              <div style='text-align: center; margin: 40px 0;'>" +
                "                <a href='" + resetUrl + "' style='display: inline-block; background-color: #3498db; color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 18px; font-weight: bold;'>パスワードをリセットする</a>" +
                "              </div>" +
                "              <p style='font-size: 14px; color: #666; margin: 30px 0 20px; line-height: 1.6;'>ボタンが機能しない場合は、以下のリンクをブラウザにコピー＆ペーストしてください：</p>" +
                "              <p style='font-size: 12px; color: #3498db; word-break: break-all; background-color: #f8f9fa; padding: 15px; border-radius: 4px; border-left: 4px solid #3498db;'>" + resetUrl + "</p>" +
                "              <!-- Security Notice -->" +
                "              <div style='margin-top: 30px; padding: 20px; background-color: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px;'>" +
                "                <p style='font-size: 14px; color: #856404; margin: 0 0 10px;'><strong>⚠️ セキュリティに関するお知らせ：</strong></p>" +
                "                <p style='font-size: 14px; color: #856404; margin: 0; line-height: 1.6;'>このパスワードリセットをリクエストしていない場合は、このメールを無視してください。あなたのアカウントは安全です。第三者がこのメールを受信することはできません。</p>" +
                "              </div>" +
                "              <div style='margin-top: 30px; padding-top: 30px; border-top: 1px solid #eeeeee;'>" +
                "                <p style='font-size: 14px; color: #999; margin: 0 0 10px;'><strong>注意事項：</strong></p>" +
                "                <ul style='font-size: 14px; color: #999; margin: 0; padding-left: 20px;'>" +
                "                  <li>このリセットリンクの有効期限は<strong>24時間</strong>です</li>" +
                "                  <li>リンクは1回のみ使用可能です</li>" +
                "                  <li>心当たりがない場合は、このメールを無視してください</li>" +
                "                </ul>" +
                "              </div>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style='background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px;'>" +
                "              <p style='font-size: 12px; color: #999; margin: 0 0 10px;'>お困りの際は、support@ej2.com までお問い合わせください。</p>" +
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
