import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './AuthPages.css';

/**
 * 会員登録ページコンポーネント
 */
function RegisterPage() {
  const [formData, setFormData] = useState({
    username: '',
    name: '',
    email: '',
    password: '',
    confirmPassword: ''
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

  // バリデーション
  const validateForm = () => {
    if (formData.password !== formData.confirmPassword) {
      setError('パスワードが一致しません');
      return false;
    }

    if (formData.password.length < 6) {
      setError('パスワードは6文字以上である必要があります');
      return false;
    }

    if (formData.username.length < 3) {
      setError('ユーザー名は3文字以上である必要があります');
      return false;
    }

    return true;
  };

  // 会員登録フォーム送信ハンドラー
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      const response = await axios.post('/api/auth/register', {
        username: formData.username,
        name: formData.name,
        email: formData.email,
        password: formData.password
      });

      if (response.data.success) {
        // 会員登録成功: ユーザー情報をlocalStorageに保存
        localStorage.setItem('user', JSON.stringify(response.data.user));

        // メインページへリダイレクト
        navigate('/');
      } else {
        setError(response.data.message);
      }
    } catch (err) {
      if (err.response && err.response.data) {
        setError(err.response.data.message || '会員登録に失敗しました');
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
        <h2 className="auth-title">会員登録</h2>

        {error && (
          <div className="auth-error">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="username">ユーザー名 *</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
              placeholder="ユーザー名を入力（3文字以上）"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="name">名前 *</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
              placeholder="名前を入力"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">メールアドレス *</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              placeholder="メールアドレスを入力"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">パスワード *</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              placeholder="パスワードを入力（6文字以上）"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">パスワード確認 *</label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
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
            {loading ? '登録中...' : '会員登録'}
          </button>
        </form>

        <div className="auth-links">
          <p>
            すでにアカウントをお持ちですか？
            <a href="/login">ログイン</a>
          </p>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
