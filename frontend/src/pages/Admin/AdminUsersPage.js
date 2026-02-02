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
                <td className="td-date">{formatDate(user.createdAt)}</td>
                <td>
                  <button
                    className="btn-danger btn-sm"
                    onClick={() => handleDeleteUser(user.id, user.name)}
                    title="削除"
                  >
                    <FiTrash2 />
                  </button>
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
    </div>
  );
}

export default AdminUsersPage;
