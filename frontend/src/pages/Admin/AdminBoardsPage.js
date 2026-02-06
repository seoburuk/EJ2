import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { FiLayout, FiArrowLeft, FiPlus, FiEdit2, FiTrash2, FiX } from 'react-icons/fi';
import './AdminPages.css';

function AdminBoardsPage() {
  const [boards, setBoards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingBoard, setEditingBoard] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    description: '',
    isAnonymous: false,
    requireAdmin: false
  });
  const navigate = useNavigate();

  // SUPER_ADMIN権限チェック（掲示板管理はSUPER_ADMIN専用）
  const checkAdminAccess = useCallback(() => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || user.role !== 'SUPER_ADMIN') {
      navigate('/');
      return false;
    }
    return true;
  }, [navigate]);

  // 掲示板一覧取得
  const fetchBoards = useCallback(async () => {
    try {
      const response = await axios.get('/api/admin/boards', {
        withCredentials: true
      });
      setBoards(response.data);
      setLoading(false);
    } catch (err) {
      console.error('Failed to fetch boards:', err);
      setError('掲示板一覧の取得に失敗しました');
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (checkAdminAccess()) {
      fetchBoards();
    }
  }, [checkAdminAccess, fetchBoards]);

  // フォームリセット
  const resetForm = () => {
    setFormData({
      name: '',
      code: '',
      description: '',
      isAnonymous: false,
      requireAdmin: false
    });
  };

  // 新規作成モーダルを開く
  const openCreateModal = () => {
    setEditingBoard(null);
    resetForm();
    setShowModal(true);
  };

  // 編集モーダルを開く
  const openEditModal = (board) => {
    setEditingBoard(board);
    setFormData({
      name: board.name || '',
      code: board.code || '',
      description: board.description || '',
      isAnonymous: board.isAnonymous || false,
      requireAdmin: board.requireAdmin || false
    });
    setShowModal(true);
  };

  // モーダルを閉じる
  const closeModal = () => {
    setShowModal(false);
    setEditingBoard(null);
    resetForm();
  };

  // フォーム送信
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.name.trim() || !formData.code.trim()) {
      alert('名前とコードは必須です');
      return;
    }

    try {
      if (editingBoard) {
        await axios.put(`/api/admin/boards/${editingBoard.id}`, formData, {
          withCredentials: true
        });
      } else {
        await axios.post('/api/admin/boards', formData, {
          withCredentials: true
        });
      }
      closeModal();
      fetchBoards();
    } catch (err) {
      alert(editingBoard ? '掲示板の更新に失敗しました' : '掲示板の作成に失敗しました');
    }
  };

  // 掲示板削除
  const handleDelete = async (boardId, boardName) => {
    if (!window.confirm(`「${boardName}」を削除しますか？\n関連する投稿も全て削除されます。`)) return;

    try {
      await axios.delete(`/api/admin/boards/${boardId}`, {
        withCredentials: true
      });
      fetchBoards();
    } catch (err) {
      alert('掲示板の削除に失敗しました');
    }
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
        <h1><FiLayout /> 掲示板管理</h1>
        <button className="btn-primary" onClick={openCreateModal}>
          <FiPlus /> 新規作成
        </button>
      </header>

      {error && <div className="error-message">{error}</div>}

      {/* 掲示板テーブル */}
      <div className="table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>コード</th>
              <th>名前</th>
              <th>説明</th>
              <th>匿名</th>
              <th>管理者専用</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {boards.length === 0 ? (
              <tr>
                <td colSpan="7" className="empty-message">掲示板がありません</td>
              </tr>
            ) : (
              boards.map(board => (
                <tr key={board.id}>
                  <td className="td-id">{board.id}</td>
                  <td className="td-code">{board.code}</td>
                  <td>{board.name}</td>
                  <td className="td-description">{board.description || '-'}</td>
                  <td>
                    <span className={`badge ${board.isAnonymous ? 'badge-yes' : 'badge-no'}`}>
                      {board.isAnonymous ? 'ON' : 'OFF'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${board.requireAdmin ? 'badge-yes' : 'badge-no'}`}>
                      {board.requireAdmin ? 'ON' : 'OFF'}
                    </span>
                  </td>
                  <td className="td-actions">
                    <button
                      className="btn-edit btn-sm"
                      onClick={() => openEditModal(board)}
                      title="編集"
                    >
                      <FiEdit2 />
                    </button>
                    <button
                      className="btn-danger btn-sm"
                      onClick={() => handleDelete(board.id, board.name)}
                      title="削除"
                    >
                      <FiTrash2 />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* モーダル */}
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingBoard ? '掲示板編集' : '新規掲示板'}</h2>
              <button className="modal-close" onClick={closeModal}>
                <FiX />
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>コード <span className="required">*</span></label>
                <input
                  type="text"
                  value={formData.code}
                  onChange={(e) => setFormData({...formData, code: e.target.value})}
                  placeholder="例: free, notice"
                  required
                />
                <small>URL用の識別子（英数字）</small>
              </div>
              <div className="form-group">
                <label>名前 <span className="required">*</span></label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  placeholder="例: 自由掲示板"
                  required
                />
              </div>
              <div className="form-group">
                <label>説明</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  placeholder="掲示板の説明"
                  rows="3"
                />
              </div>
              <div className="form-group checkbox-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.isAnonymous}
                    onChange={(e) => setFormData({...formData, isAnonymous: e.target.checked})}
                  />
                  <span>匿名掲示板</span>
                </label>
                <small>投稿者名を匿名で表示</small>
              </div>
              <div className="form-group checkbox-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.requireAdmin}
                    onChange={(e) => setFormData({...formData, requireAdmin: e.target.checked})}
                  />
                  <span>管理者専用</span>
                </label>
                <small>管理者のみ投稿可能</small>
              </div>
              <div className="modal-buttons">
                <button type="button" className="btn-secondary" onClick={closeModal}>
                  キャンセル
                </button>
                <button type="submit" className="btn-primary">
                  {editingBoard ? '更新' : '作成'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminBoardsPage;
