import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './AuthPages.css';

/**
 * パスワードリセット確認ページコンポーネント
 */
function PasswordResetConfirmPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [status, setStatus] = useState('form'); // 'form', 'loading', 'success', 'error', 'expired'
  const [message, setMessage] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [countdown, setCountdown] = useState(5);
  const [validationError, setValidationError] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('無効なリセットリンクです');
    }
  }, [token]);

  // パスワードバリデーション
  const validatePassword = () => {
    if (newPassword.length < 6) {
      setValidationError('パスワードは6文字以上である必要があります');
      return false;
    }
    if (newPassword !== confirmPassword) {
      setValidationError('パスワードが一致しません');
      return false;
    }
    setValidationError('');
    return true;
  };

  // パスワードリセットを実行
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validatePassword()) {
      return;
    }

    setStatus('loading');

    try {
      const response = await axios.post('/api/auth/password-reset/confirm', {
        token,
        newPassword
      });

      if (response.data.success) {
        setStatus('success');
        setMessage(response.data.message);
        startCountdown();
      } else {
        if (response.data.message === 'TOKEN_EXPIRED' ||
            response.data.message === 'リセットトークンの有効期限が切れています') {
          setStatus('expired');
          setMessage('リセットトークンの有効期限が切れています');
        } else {
          setStatus('error');
          setMessage(response.data.message);
        }
      }
    } catch (error) {
      setStatus('error');
      setMessage(error.response?.data?.message || 'パスワードリセットに失敗しました');
    }
  };

  // カウントダウンタイマーを開始
  const startCountdown = () => {
    const interval = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          clearInterval(interval);
          navigate('/login');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        {/* フォーム状態 */}
        {status === 'form' && (
          <div>
            <h2>新しいパスワードの設定</h2>
            <p className="auth-subtitle">新しいパスワードを入力してください</p>
            <form onSubmit={handleSubmit} className="auth-form">
              <div className="form-group">
                <label htmlFor="newPassword">新しいパスワード</label>
                <input
                  type="password"
                  id="newPassword"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="新しいパスワード（6文字以上）"
                  required
                  minLength="6"
                />
              </div>
              <div className="form-group">
                <label htmlFor="confirmPassword">パスワード確認</label>
                <input
                  type="password"
                  id="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="パスワードを再入力"
                  required
                  minLength="6"
                />
              </div>
              {validationError && (
                <p className="error-message">{validationError}</p>
              )}
              <button type="submit" className="auth-button">
                パスワードをリセット
              </button>
            </form>
          </div>
        )}

        {/* ローディング状態 */}
        {status === 'loading' && (
          <div className="verification-loading">
            <div className="loading-spinner"></div>
            <p>パスワードをリセット中...</p>
          </div>
        )}

        {/* 成功状態 */}
        {status === 'success' && (
          <div className="verification-success">
            <div className="success-icon">✓</div>
            <h2>パスワードリセット完了！</h2>
            <p>{message}</p>
            <p className="countdown">{countdown}秒後にログインページへ移動します</p>
            <button onClick={() => navigate('/login')} className="auth-button">
              今すぐログイン
            </button>
          </div>
        )}

        {/* 有効期限切れ状態 */}
        {status === 'expired' && (
          <div className="verification-error">
            <div className="error-icon">⚠</div>
            <h2>リセットリンクの有効期限切れ</h2>
            <p>{message || 'このリセットリンクは有効期限が切れています。'}</p>
            <p>新しいパスワードリセットをリクエストしてください。</p>
            <button onClick={() => navigate('/password-reset')} className="auth-button">
              パスワードリセットページへ
            </button>
          </div>
        )}

        {/* エラー状態 */}
        {status === 'error' && (
          <div className="verification-error">
            <div className="error-icon">✕</div>
            <h2>リセット失敗</h2>
            <p>{message}</p>
            <button onClick={() => navigate('/password-reset')} className="auth-button">
              パスワードリセットページへ戻る
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default PasswordResetConfirmPage;
