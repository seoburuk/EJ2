import React, { useState } from 'react';
import axios from 'axios';
import './AuthPages.css';

/**
 * パスワードリセットページコンポーネント
 */
function PasswordResetPage() {
  const [step, setStep] = useState(1); // 1: メール入力, 2: トークン＆新パスワード入力
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // ステップ1: パスワードリセットリクエスト
  const handleRequestReset = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const response = await axios.post('/api/auth/password-reset/request', {
        email: email
      });

      if (response.data.success) {
        setSuccess(response.data.message);
        setStep(2); // 次のステップへ
      } else {
        setError(response.data.message);
      }
    } catch (err) {
      if (err.response && err.response.data) {
        setError(err.response.data.message || 'リクエストに失敗しました');
      } else {
        setError('サーバーとの通信に失敗しました');
      }
    } finally {
      setLoading(false);
    }
  };

  // ステップ2: パスワードリセット確認
  const handleConfirmReset = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // パスワード一致チェック
    if (newPassword !== confirmPassword) {
      setError('パスワードが一致しません');
      return;
    }

    // パスワード長チェック
    if (newPassword.length < 6) {
      setError('パスワードは6文字以上である必要があります');
      return;
    }

    setLoading(true);

    try {
      const response = await axios.post('/api/auth/password-reset/confirm', {
        token: token,
        newPassword: newPassword
      });

      if (response.data.success) {
        setSuccess('パスワードが正常にリセットされました。ログインページへ移動してください。');
        // 3秒後にログインページへリダイレクト
        setTimeout(() => {
          window.location.href = '/login';
        }, 3000);
      } else {
        setError(response.data.message);
      }
    } catch (err) {
      if (err.response && err.response.data) {
        setError(err.response.data.message || 'リセットに失敗しました');
      } else {
        setError('サーバーとの通信に失敗しました');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2 className="auth-title">パスワードリセット</h2>

        {error && (
          <div className="auth-error">
            {error}
          </div>
        )}

        {success && (
          <div className="auth-success">
            {success}
          </div>
        )}

        {step === 1 ? (
          // ステップ1: メールアドレス入力
          <form onSubmit={handleRequestReset} className="auth-form">
            <p className="auth-description">
              登録したメールアドレスを入力してください。パスワードリセット用のトークンが表示されます。
            </p>

            <div className="form-group">
              <label htmlFor="email">メールアドレス</label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="メールアドレスを入力"
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              className="auth-button"
              disabled={loading}
            >
              {loading ? '送信中...' : 'リセットトークンを取得'}
            </button>

            <div className="auth-links">
              <a href="/login">ログインページへ戻る</a>
            </div>
          </form>
        ) : (
          // ステップ2: トークンと新パスワード入力
          <form onSubmit={handleConfirmReset} className="auth-form">
            <p className="auth-description">
              上記に表示されたリセットトークンと新しいパスワードを入力してください。
            </p>

            <div className="form-group">
              <label htmlFor="token">リセットトークン</label>
              <input
                type="text"
                id="token"
                value={token}
                onChange={(e) => setToken(e.target.value)}
                required
                placeholder="トークンを入力"
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="newPassword">新しいパスワード</label>
              <input
                type="password"
                id="newPassword"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                placeholder="新しいパスワードを入力（6文字以上）"
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword">パスワード確認</label>
              <input
                type="password"
                id="confirmPassword"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                placeholder="パスワードを再入力"
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              className="auth-button"
              disabled={loading}
            >
              {loading ? 'リセット中...' : 'パスワードをリセット'}
            </button>

            <div className="auth-links">
              <a href="/login">ログインページへ戻る</a>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

export default PasswordResetPage;
