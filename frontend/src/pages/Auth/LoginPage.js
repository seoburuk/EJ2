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

        // メインページへリダイレクト
        navigate('/');
      } else {
        setError(response.data.message);
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
        <h2 className="auth-title">ログイン</h2>

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
              placeholder="ユーザー名を入力"
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
              placeholder="パスワードを入力"
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="auth-button"
            disabled={loading}
          >
            {loading ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>

        <div className="auth-links">
          <a href="/password-reset">パスワードをお忘れですか？</a>
          <p>
            アカウントをお持ちでないですか？
            <a href="/register">会員登録</a>
          </p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
