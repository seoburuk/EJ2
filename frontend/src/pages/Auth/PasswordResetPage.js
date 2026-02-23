import React, { useState } from 'react';
import axios from 'axios';
import './AuthPages.css';

/**
 * パスワードリセットページコンポーネント
 */
function PasswordResetPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // パスワードリセットリクエスト
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
            <p className="auth-subtitle">メールに記載されたリンクをクリックして、パスワードをリセットしてください。</p>
          </div>
        )}

        <form onSubmit={handleRequestReset} className="auth-form">
          <p className="auth-description">
            登録したメールアドレスを入力してください。パスワードリセット用のリンクをメールで送信します。
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
            {loading ? '送信中...' : 'リセットメールを送信'}
          </button>

          <div className="auth-links">
            <a href="/login">ログインページへ戻る</a>
          </div>
        </form>
      </div>
    </div>
  );
}

export default PasswordResetPage;
