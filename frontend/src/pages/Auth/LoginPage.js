import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './AuthPages.css';

/**
 * ログインページコンポーネント
 */
function LoginPage() {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // フォーム入力変更ハンドラー
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    setError(''); // エラーをクリア
  };

  // ログインフォーム送信ハンドラー
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await axios.post('/api/auth/login', {
        username: formData.username,
        password: formData.password
      }, { withCredentials: true });

      if (response.data.success) {
        // ログイン成功: ユーザー情報をlocalStorageに保存
        localStorage.setItem('user', JSON.stringify(response.data.user));

        // NavBarにログイン状態の変更を通知
        window.dispatchEvent(new Event('authChange'));

        // メインページへリダイレクト
        navigate('/');
      } else {
        setError(response.data.message);
        console.log(response.data.message); // 확인용 지울거
      }
    } catch (err) {
      if (err.response && err.response.data) {
        setError(err.response.data.message || 'ログインに失敗しました');
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
        <h2 className="auth-title">Welcome Back 👊</h2>

        {error && (
          <div className="auth-error">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="username">ユーザー名</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
              placeholder="Username"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">パスワード</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              placeholder="Password"
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="auth-button"
            disabled={loading}
          >
            {loading ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <div className="auth-links">
          <a href="/register">Sign Up</a>
          <span style={{ margin: '0 8px', color: '#ccc' }}>|</span>
          <a href="/find-account">ID / パスワードを探す</a>
        </div>

        <div className="concept-links">
          view concept for <a href="/concept/mobile">mobile</a> or for <a href="/concept/desktop">desktop</a>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
