import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './AuthPages.css';

/**
 * メールアドレス認証ページコンポーネント
 */
function EmailVerificationPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [status, setStatus] = useState('loading'); // 'loading', 'success', 'error', 'expired'
  const [message, setMessage] = useState('');
  const [countdown, setCountdown] = useState(5);

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('無効な認証リンクです');
      return;
    }
    verifyEmail();
  }, [token]);

  // メール認証を実行
  const verifyEmail = async () => {
    try {
      const response = await axios.post('/api/auth/verify-email', { token });
      if (response.data.success) {
        setStatus('success');
        setMessage(response.data.message);
        startCountdown();
      } else {
        if (response.data.message === 'TOKEN_EXPIRED') {
          setStatus('expired');
        } else {
          setStatus('error');
          setMessage(response.data.message);
        }
      }
    } catch (error) {
      setStatus('error');
      setMessage(error.response?.data?.message || '認証に失敗しました');
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
        {/* ローディング状態 */}
        {status === 'loading' && (
          <div className="verification-loading">
            <div className="loading-spinner"></div>
            <p>メール認証を確認中...</p>
          </div>
        )}

        {/* 成功状態 */}
        {status === 'success' && (
          <div className="verification-success">
            <div className="success-icon">✓</div>
            <h2>メール認証完了！</h2>
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
            <h2>認証リンクの有効期限切れ</h2>
            <p>この認証リンクは有効期限が切れています。</p>
            <p>新しい認証メールをリクエストしてください。</p>
            <button onClick={() => navigate('/login')} className="auth-button">
              ログインページへ
            </button>
          </div>
        )}

        {/* エラー状態 */}
        {status === 'error' && (
          <div className="verification-error">
            <div className="error-icon">✕</div>
            <h2>認証失敗</h2>
            <p>{message}</p>
            <button onClick={() => navigate('/login')} className="auth-button">
              ログインページへ戻る
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default EmailVerificationPage;
