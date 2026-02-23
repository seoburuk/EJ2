import React, { useState } from 'react';
import axios from 'axios';
import './AuthPages.css';

/**
 * アカウント検索ページ（アイディ찾기 / パスワード찾기）
 */
function FindAccountPage() {
  const [activeTab, setActiveTab] = useState('username'); // 'username' or 'password'

  // 아이디 찾기 상태
  const [findEmail, setFindEmail] = useState('');
  const [findName, setFindName] = useState('');
  const [findResult, setFindResult] = useState('');
  const [findError, setFindError] = useState('');
  const [findLoading, setFindLoading] = useState(false);

  // 비밀번호 찾기 상태
  const [step, setStep] = useState(1);
  const [resetEmail, setResetEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [resetError, setResetError] = useState('');
  const [resetSuccess, setResetSuccess] = useState('');
  const [resetLoading, setResetLoading] = useState(false);

  // 아이디 찾기 제출
  const handleFindUsername = async (e) => {
    e.preventDefault();
    setFindError('');
    setFindResult('');
    setFindLoading(true);

    try {
      const response = await axios.post('/api/auth/find-username', {
        email: findEmail,
        name: findName
      });

      if (response.data.success) {
        setFindResult(response.data.message);
      } else {
        setFindError(response.data.message);
      }
    } catch (err) {
      if (err.response && err.response.data) {
        setFindError(err.response.data.message || '検索に失敗しました');
      } else {
        setFindError('サーバーとの通信に失敗しました');
      }
    } finally {
      setFindLoading(false);
    }
  };

  // 비밀번호 리셋 요청
  const handleRequestReset = async (e) => {
    e.preventDefault();
    setResetError('');
    setResetSuccess('');
    setResetLoading(true);

    try {
      const response = await axios.post('/api/auth/password-reset/request', {
        email: resetEmail
      });

      if (response.data.success) {
        setResetSuccess(response.data.message);
        setResetEmail(''); // 이메일 필드 초기화
        // setStep(2); // Step 2로 전환하지 않음 - 이메일 링크를 통한 재설정만 사용
      } else {
        setResetError(response.data.message);
      }
    } catch (err) {
      if (err.response && err.response.data) {
        setResetError(err.response.data.message || 'リクエストに失敗しました');
      } else {
        setResetError('サーバーとの通信に失敗しました');
      }
    } finally {
      setResetLoading(false);
    }
  };

  // 비밀번호 리셋 확인
  const handleConfirmReset = async (e) => {
    e.preventDefault();
    setResetError('');
    setResetSuccess('');

    if (newPassword !== confirmPassword) {
      setResetError('パスワードが一致しません');
      return;
    }

    if (newPassword.length < 6) {
      setResetError('パスワードは6文字以上である必要があります');
      return;
    }

    setResetLoading(true);

    try {
      const response = await axios.post('/api/auth/password-reset/confirm', {
        token: token,
        newPassword: newPassword
      });

      if (response.data.success) {
        setResetSuccess('パスワードが正常にリセットされました。');
        setTimeout(() => {
          window.location.href = '/login';
        }, 2000);
      } else {
        setResetError(response.data.message);
      }
    } catch (err) {
      if (err.response && err.response.data) {
        setResetError(err.response.data.message || 'リセットに失敗しました');
      } else {
        setResetError('サーバーとの通信に失敗しました');
      }
    } finally {
      setResetLoading(false);
    }
  };

  // 탭 전환 시 상태 초기화
  const handleTabChange = (tab) => {
    setActiveTab(tab);
    // 아이디 찾기 초기화
    setFindEmail('');
    setFindName('');
    setFindResult('');
    setFindError('');
    // 비밀번호 찾기 초기화
    setStep(1);
    setResetEmail('');
    setToken('');
    setNewPassword('');
    setConfirmPassword('');
    setResetError('');
    setResetSuccess('');
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2 className="auth-title">アカウント検索</h2>

        {/* 탭 메뉴 */}
        <div className="find-tabs">
          <button
            className={`find-tab ${activeTab === 'username' ? 'active' : ''}`}
            onClick={() => handleTabChange('username')}
          >
            IDを探す
          </button>
          <button
            className={`find-tab ${activeTab === 'password' ? 'active' : ''}`}
            onClick={() => handleTabChange('password')}
          >
            パスワードを探す
          </button>
        </div>

        {/* 아이디 찾기 탭 */}
        {activeTab === 'username' && (
          <>
            {findError && <div className="auth-error">{findError}</div>}
            {findResult && <div className="auth-success">{findResult}</div>}

            <form onSubmit={handleFindUsername} className="auth-form">
              <p className="auth-description">
                登録した名前とメールアドレスを入力してください。
              </p>

              <div className="form-group">
                <label htmlFor="findName">名前</label>
                <input
                  type="text"
                  id="findName"
                  value={findName}
                  onChange={(e) => setFindName(e.target.value)}
                  required
                  placeholder="名前を入力"
                  disabled={findLoading}
                />
              </div>

              <div className="form-group">
                <label htmlFor="findEmail">メールアドレス</label>
                <input
                  type="email"
                  id="findEmail"
                  value={findEmail}
                  onChange={(e) => setFindEmail(e.target.value)}
                  required
                  placeholder="メールアドレスを入力"
                  disabled={findLoading}
                />
              </div>

              <button
                type="submit"
                className="auth-button"
                disabled={findLoading}
              >
                {findLoading ? '検索中...' : 'IDを検索'}
              </button>
            </form>
          </>
        )}

        {/* 비밀번호 찾기 탭 */}
        {activeTab === 'password' && (
          <>
            {resetError && <div className="auth-error">{resetError}</div>}
            {resetSuccess && <div className="auth-success">{resetSuccess}</div>}

            {step === 1 ? (
              <form onSubmit={handleRequestReset} className="auth-form">
                <p className="auth-description">
                  登録したメールアドレスを入力してください。リセットトークンが表示されます。
                </p>

                <div className="form-group">
                  <label htmlFor="resetEmail">メールアドレス</label>
                  <input
                    type="email"
                    id="resetEmail"
                    value={resetEmail}
                    onChange={(e) => setResetEmail(e.target.value)}
                    required
                    placeholder="メールアドレスを入力"
                    disabled={resetLoading}
                  />
                </div>

                <button
                  type="submit"
                  className="auth-button"
                  disabled={resetLoading}
                >
                  {resetLoading ? '送信中...' : 'リセットトークンを取得'}
                </button>
              </form>
            ) : (
              <form onSubmit={handleConfirmReset} className="auth-form">
                <p className="auth-description">
                  上記のトークンと新しいパスワードを入力してください。
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
                    disabled={resetLoading}
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
                    placeholder="新しいパスワード（6文字以上）"
                    disabled={resetLoading}
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
                    disabled={resetLoading}
                  />
                </div>

                <button
                  type="submit"
                  className="auth-button"
                  disabled={resetLoading}
                >
                  {resetLoading ? 'リセット中...' : 'パスワードをリセット'}
                </button>
              </form>
            )}
          </>
        )}

        <div className="auth-links">
          <a href="/login">ログインページへ戻る</a>
        </div>
      </div>
    </div>
  );
}

export default FindAccountPage;
