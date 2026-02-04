import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { FiUsers, FiArrowLeft, FiTrash2, FiShield, FiUser } from 'react-icons/fi';
import './AdminPages.css';

function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  // 停止モーダル状態
  const [showSuspendModal, setShowSuspendModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [suspendDuration, setSuspendDuration] = useState('7_DAYS');
  const [suspendReason, setSuspendReason] = useState('');

  // 管理者権限チェック
  const checkAdminAccess = useCallback(() => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || user.role !== 'ADMIN') {
      navigate('/');
      return false;
    }
    return true;
  }, [navigate]);

  // ユーザー一覧取得
  const fetchUsers = useCallback(async () => {
    try {
      const response = await axios.get(`/api/admin/users?page=${page}&size=20`, {
        withCredentials: true
      });
      setUsers(response.data);
      setLoading(false);
    } catch (err) {
      console.error('Failed to fetch users:', err);
      setError('ユーザー一覧の取得に失敗しました');
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    if (checkAdminAccess()) {
      fetchUsers();
    }
  }, [checkAdminAccess, fetchUsers]);

  // 権限変更
  const handleRoleChange = async (userId, newRole) => {
    const currentUser = JSON.parse(localStorage.getItem('user'));
    if (currentUser.id === userId) {
      alert('自分自身の権限は変更できません');
      return;
    }

    if (!window.confirm(`権限を ${newRole} に変更しますか？`)) return;

    try {
      await axios.put(`/api/admin/users/${userId}/role`,
        { role: newRole },
        { withCredentials: true }
      );
      fetchUsers();
    } catch (err) {
      alert('権限変更に失敗しました');
    }
  };

  // ユーザー削除
  const handleDeleteUser = async (userId, userName) => {
    const currentUser = JSON.parse(localStorage.getItem('user'));
    if (currentUser.id === userId) {
      alert('自分自身は削除できません');
      return;
    }

    if (!window.confirm(`「${userName}」を削除しますか？この操作は取り消せません。`)) return;

    try {
      await axios.delete(`/api/admin/users/${userId}`, { withCredentials: true });
      fetchUsers();
    } catch (err) {
      alert('ユーザー削除に失敗しました');
    }
  };

  // 停止モーダル開く
  const openSuspendModal = (user) => {
    setSelectedUser(user);
    setShowSuspendModal(true);
    setSuspendDuration('7_DAYS');
    setSuspendReason('');
  };

  // ユーザー停止
  const handleSuspendUser = async () => {
    if (!suspendReason.trim()) {
      alert('停止理由を入力してください');
      return;
    }

    try {
      await axios.post(
        `http://localhost:8080/api/admin/users/${selectedUser.id}/suspend`,
        { duration: suspendDuration, reason: suspendReason },
        { withCredentials: true }
      );
      alert('ユーザーが停止されました');
      setShowSuspendModal(false);
      setSuspendReason('');
      fetchUsers();
    } catch (err) {
      console.error('停止エラー:', err);
      alert('ユーザー停止に失敗しました');
    }
  };

  // 停止解除
  const handleUnsuspendUser = async (userId, userName) => {
    if (!window.confirm(`「${userName}」の停止を解除しますか？`)) return;

    try {
      await axios.post(
        `http://localhost:8080/api/admin/users/${userId}/unsuspend`,
        {},
        { withCredentials: true }
      );
      alert('停止が解除されました');
      fetchUsers();
    } catch (err) {
      console.error('停止解除エラー:', err);
      alert('停止解除に失敗しました');
    }
  };

  // ステータスバッジ
  const getStatusBadge = (user) => {
    if (user.status === 'BANNED') {
      return <span className="status-badge banned">永久停止</span>;
    } else if (user.status === 'SUSPENDED') {
      const until = user.suspendedUntil ? new Date(user.suspendedUntil).toLocaleDateString('ja-JP') : '';
      return <span className="status-badge suspended">停止中 {until && `(~${until})`}</span>;
    } else {
      return <span className="status-badge active">活動中</span>;
    }
  };

  // 日付フォーマット
  const formatDate = (dateString) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="admin-page">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>読み込み中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      {/* ヘッダー */}
      <header className="page-header">
        <Link to="/admin" className="back-link">
          <FiArrowLeft /> 戻る
        </Link>
        <h1><FiUsers /> ユーザー管理</h1>
      </header>

      {error && <div className="error-message">{error}</div>}

      {/* ユーザーテーブル */}
      <div className="table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>ユーザー名</th>
              <th>名前</th>
              <th>メール</th>
              <th>権限</th>
              <th>状態</th>
              <th>登録日</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id}>
                <td className="td-id">{user.id}</td>
                <td className="td-username">{user.username}</td>
                <td>{user.name}</td>
                <td className="td-email">{user.email}</td>
                <td>
                  <select
                    value={user.role || 'USER'}
                    onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    className={`role-select ${user.role === 'ADMIN' ? 'admin' : 'user'}`}
                  >
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td>{getStatusBadge(user)}</td>
                <td className="td-date">{formatDate(user.createdAt)}</td>
                <td>
                  <div className="action-buttons-group">
                    {(user.status === 'SUSPENDED' || user.status === 'BANNED') ? (
                      <button
                        className="btn-success btn-sm"
                        onClick={() => handleUnsuspendUser(user.id, user.name)}
                        title="停止解除"
                      >
                        解除
                      </button>
                    ) : (
                      <button
                        className="btn-warning btn-sm"
                        onClick={() => openSuspendModal(user)}
                        title="停止"
                      >
                        停止
                      </button>
                    )}
                    <button
                      className="btn-danger btn-sm"
                      onClick={() => handleDeleteUser(user.id, user.name)}
                      title="削除"
                    >
                      <FiTrash2 />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ページネーション */}
      <div className="pagination">
        <button
          className="btn-secondary"
          onClick={() => setPage(Math.max(0, page - 1))}
          disabled={page === 0}
        >
          前へ
        </button>
        <span className="page-info">ページ {page + 1}</span>
        <button
          className="btn-secondary"
          onClick={() => setPage(page + 1)}
          disabled={users.length < 20}
        >
          次へ
        </button>
      </div>

      {/* 凡例 */}
      <div className="role-legend">
        <span className="legend-item">
          <FiShield className="icon-admin" /> ADMIN = 管理者
        </span>
        <span className="legend-item">
          <FiUser className="icon-user" /> USER = 一般ユーザー
        </span>
      </div>

      {/* 停止モーダル */}
      {showSuspendModal && selectedUser && (
        <div className="modal-overlay" onClick={() => setShowSuspendModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>ユーザー停止</h2>
            <p className="modal-description">
              ユーザー「{selectedUser.name}」を停止しますか？
            </p>

            <div className="form-group">
              <label>停止期間 <span className="required">*</span></label>
              <select
                className="suspend-duration-select"
                value={suspendDuration}
                onChange={(e) => setSuspendDuration(e.target.value)}
              >
                <option value="1_DAY">1日</option>
                <option value="3_DAYS">3日</option>
                <option value="7_DAYS">7日</option>
                <option value="30_DAYS">30日</option>
                <option value="PERMANENT">永久停止</option>
              </select>
            </div>

            <div className="form-group">
              <label>停止理由 <span className="required">*</span></label>
              <textarea
                className="suspend-reason-input"
                placeholder="停止理由を入力してください..."
                value={suspendReason}
                onChange={(e) => setSuspendReason(e.target.value)}
                rows="4"
              />
            </div>

            <div className="modal-buttons">
              <button
                className="btn-secondary"
                onClick={() => {
                  setShowSuspendModal(false);
                  setSuspendReason('');
                }}
              >
                キャンセル
              </button>
              <button className="btn-warning" onClick={handleSuspendUser}>
                停止する
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminUsersPage;
