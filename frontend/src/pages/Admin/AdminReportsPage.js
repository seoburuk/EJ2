import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { FiAlertTriangle, FiCheckCircle, FiXCircle, FiClock, FiEye, FiArrowLeft } from 'react-icons/fi';
import './AdminPages.css';

// 相対時間表示
const getRelativeTime = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return '今';
  if (diffMins < 60) return `${diffMins}分前`;
  if (diffHours < 24) return `${diffHours}時間前`;
  return `${diffDays}日前`;
};

// 統計カード
const StatCard = ({ icon: Icon, title, value, color }) => {
  return (
    <div className="stat-card" style={{ '--card-gradient': color }}>
      <div className="stat-icon" style={{ background: color }}>
        <Icon size={24} />
      </div>
      <div className="stat-content">
        <h3 className="stat-title">{title}</h3>
        <p className="stat-value">{value?.toLocaleString() || 0}</p>
      </div>
    </div>
  );
};

// ステータスバッジ
const StatusBadge = ({ status }) => {
  const statusConfig = {
    PENDING: { label: '保留中', color: '#f59e0b' },
    REVIEWING: { label: '審査中', color: '#3b82f6' },
    RESOLVED: { label: '解決済', color: '#10b981' },
    DISMISSED: { label: '却下', color: '#6b7280' }
  };

  const config = statusConfig[status] || statusConfig.PENDING;

  return (
    <span className="status-badge" style={{ backgroundColor: config.color }}>
      {config.label}
    </span>
  );
};

// 報告タイプバッジ
const ReportTypeBadge = ({ reportType }) => {
  const typeConfig = {
    POST: { label: '投稿', color: '#8b5cf6' },
    COMMENT: { label: 'コメント', color: '#ec4899' },
    USER: { label: 'ユーザー', color: '#f97316' }
  };

  const config = typeConfig[reportType] || typeConfig.POST;

  return (
    <span className="report-type-badge" style={{ backgroundColor: config.color }}>
      {config.label}
    </span>
  );
};

// 理由表示
const getReasonLabel = (reason) => {
  const reasonMap = {
    SPAM: 'スパム/広告',
    HARASSMENT: '嫌がらせ',
    INAPPROPRIATE: '不適切なコンテンツ',
    HATE_SPEECH: 'ヘイトスピーチ',
    OTHER: 'その他'
  };
  return reasonMap[reason] || reason;
};

function AdminReportsPage() {
  const [reports, setReports] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedReport, setSelectedReport] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [actionNote, setActionNote] = useState('');
  const navigate = useNavigate();

  // フィルタ・ページネーション状態
  const [filters, setFilters] = useState({
    status: '',
    reportType: '',
    sortBy: 'date',
    sortOrder: 'DESC'
  });
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const pageSize = 20;

  // 管理者権限チェック（ADMINとSUPER_ADMINの両方を許可）
  const checkAdminAccess = useCallback(() => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || (user.role !== 'ADMIN' && user.role !== 'SUPER_ADMIN')) {
      navigate('/');
      return false;
    }
    return true;
  }, [navigate]);

  // 統計取得
  const fetchStats = useCallback(async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/admin/reports/stats', {
        withCredentials: true
      });
      setStats(response.data);
    } catch (err) {
      console.error('統計取得エラー:', err);
    }
  }, []);

  // 報告一覧取得
  const fetchReports = useCallback(async () => {
    try {
      setLoading(true);
      const params = {
        page: currentPage,
        size: pageSize,
        ...filters
      };

      const response = await axios.get('http://localhost:8080/api/admin/reports', {
        params,
        withCredentials: true
      });

      setReports(response.data.reports || []);
      setTotalPages(response.data.totalPages || 0);
      setError('');
    } catch (err) {
      console.error('報告一覧取得エラー:', err);
      setError('報告一覧の取得に失敗しました');
    } finally {
      setLoading(false);
    }
  }, [currentPage, filters]);

  // 報告詳細取得
  const fetchReportDetail = async (reportId) => {
    try {
      const response = await axios.get(
        `http://localhost:8080/api/admin/reports/${reportId}`,
        { withCredentials: true }
      );
      setSelectedReport(response.data);
      setShowDetailModal(true);
    } catch (err) {
      console.error('報告詳細取得エラー:', err);
      alert('報告詳細の取得に失敗しました');
    }
  };

  // ステータス変更
  const handleStatusChange = async (reportId, newStatus) => {
    try {
      await axios.put(
        `http://localhost:8080/api/admin/reports/${reportId}/status`,
        { status: newStatus, adminNote: actionNote },
        { withCredentials: true }
      );
      alert('ステータスが更新されました');
      setShowDetailModal(false);
      fetchReports();
      fetchStats();
    } catch (err) {
      console.error('ステータス更新エラー:', err);
      alert('ステータスの更新に失敗しました');
    }
  };

  // モデレーションアクション実行
  const handleModerationAction = async (reportId, action) => {
    if (!window.confirm(`本当にこのアクション「${action}」を実行しますか？`)) {
      return;
    }

    try {
      await axios.post(
        `http://localhost:8080/api/admin/reports/${reportId}/actions`,
        { action, adminNote: actionNote },
        { withCredentials: true }
      );
      alert('アクションが実行されました');
      setShowDetailModal(false);
      setActionNote('');
      fetchReports();
      fetchStats();
    } catch (err) {
      console.error('アクション実行エラー:', err);
      alert('アクションの実行に失敗しました');
    }
  };

  // 初期化
  useEffect(() => {
    if (checkAdminAccess()) {
      fetchStats();
      fetchReports();
    }
  }, [checkAdminAccess, fetchStats, fetchReports]);

  if (loading && reports.length === 0) {
    return <div className="admin-container"><p>読み込み中...</p></div>;
  }

  return (
    <div className="admin-container">
      <div className="admin-header">
        <div className="header-content">
          <button className="back-button" onClick={() => navigate('/admin')}>
            <FiArrowLeft size={20} />
            <span>ダッシュボードに戻る</span>
          </button>
          <h1 className="admin-title centered-title">報告管理</h1>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* 統計カード */}
      {stats && (
        <div className="stats-grid">
          <StatCard
            icon={FiAlertTriangle}
            title="総報告数"
            value={stats.totalReports}
            color="linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
          />
          <StatCard
            icon={FiClock}
            title="保留中"
            value={stats.pendingReports}
            color="linear-gradient(135deg, #f59e0b 0%, #d97706 100%)"
          />
          <StatCard
            icon={FiEye}
            title="審査中"
            value={stats.reviewingReports}
            color="linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)"
          />
          <StatCard
            icon={FiCheckCircle}
            title="今日の解決数"
            value={stats.resolvedToday}
            color="linear-gradient(135deg, #10b981 0%, #059669 100%)"
          />
        </div>
      )}

      {/* フィルタ */}
      <div className="filter-section">
        <div className="filter-group">
          <label>ステータス:</label>
          <select
            value={filters.status}
            onChange={(e) => {
              setFilters({ ...filters, status: e.target.value });
              setCurrentPage(0);
            }}
          >
            <option value="">すべて</option>
            <option value="PENDING">保留中</option>
            <option value="REVIEWING">審査中</option>
            <option value="RESOLVED">解決済</option>
            <option value="DISMISSED">却下</option>
          </select>
        </div>

        <div className="filter-group">
          <label>報告タイプ:</label>
          <select
            value={filters.reportType}
            onChange={(e) => {
              setFilters({ ...filters, reportType: e.target.value });
              setCurrentPage(0);
            }}
          >
            <option value="">すべて</option>
            <option value="POST">投稿</option>
            <option value="COMMENT">コメント</option>
            <option value="USER">ユーザー</option>
          </select>
        </div>

        <div className="filter-group">
          <label>ソート:</label>
          <select
            value={filters.sortBy}
            onChange={(e) => {
              setFilters({ ...filters, sortBy: e.target.value });
              setCurrentPage(0);
            }}
          >
            <option value="date">日付順</option>
            <option value="status">ステータス順</option>
            <option value="type">タイプ順</option>
          </select>
        </div>
      </div>

      {/* 報告一覧テーブル */}
      <div className="table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>タイプ</th>
              <th>対象ID</th>
              <th>報告者</th>
              <th>理由</th>
              <th>ステータス</th>
              <th>報告日時</th>
              <th>アクション</th>
            </tr>
          </thead>
          <tbody>
            {reports.length === 0 ? (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center' }}>報告がありません</td>
              </tr>
            ) : (
              reports.map((report) => (
                <tr key={report.id}>
                  <td>{report.id}</td>
                  <td><ReportTypeBadge reportType={report.reportType} /></td>
                  <td>{report.entityId}</td>
                  <td>{report.reporterName}</td>
                  <td>{getReasonLabel(report.reason)}</td>
                  <td><StatusBadge status={report.status} /></td>
                  <td>{getRelativeTime(report.createdAt)}</td>
                  <td>
                    <button
                      className="action-button view"
                      onClick={() => fetchReportDetail(report.id)}
                    >
                      詳細
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ページネーション */}
      {totalPages > 1 && (
        <div className="pagination">
          <button
            disabled={currentPage === 0}
            onClick={() => setCurrentPage(currentPage - 1)}
          >
            前へ
          </button>
          <span>ページ {currentPage + 1} / {totalPages}</span>
          <button
            disabled={currentPage >= totalPages - 1}
            onClick={() => setCurrentPage(currentPage + 1)}
          >
            次へ
          </button>
        </div>
      )}

      {/* 詳細モーダル */}
      {showDetailModal && selectedReport && (
        <div className="modal-overlay" onClick={() => setShowDetailModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>報告詳細 (ID: {selectedReport.id})</h2>

            <div className="detail-section">
              <h3>報告情報</h3>
              <p><strong>タイプ:</strong> <ReportTypeBadge reportType={selectedReport.reportType} /></p>
              <p><strong>ステータス:</strong> <StatusBadge status={selectedReport.status} /></p>
              <p><strong>報告者:</strong> {selectedReport.reporterName} ({selectedReport.reporterEmail})</p>
              <p><strong>理由:</strong> {getReasonLabel(selectedReport.reason)}</p>
              {selectedReport.description && (
                <p><strong>詳細:</strong> {selectedReport.description}</p>
              )}
              <p><strong>報告日時:</strong> {new Date(selectedReport.createdAt).toLocaleString('ja-JP')}</p>
            </div>

            {selectedReport.entityTitle && (
              <div className="detail-section">
                <h3>報告対象コンテンツ</h3>
                <p><strong>タイトル/名前:</strong> {selectedReport.entityTitle}</p>
                {selectedReport.entityContent && (
                  <p><strong>内容:</strong> {selectedReport.entityContent}</p>
                )}
                {selectedReport.entityAuthorName && (
                  <p><strong>作成者:</strong> {selectedReport.entityAuthorName}</p>
                )}
              </div>
            )}

            {selectedReport.resolvedBy && (
              <div className="detail-section">
                <h3>解決情報</h3>
                <p><strong>処理者:</strong> {selectedReport.resolvedByName}</p>
                <p><strong>処理日時:</strong> {new Date(selectedReport.resolvedAt).toLocaleString('ja-JP')}</p>
                <p><strong>アクション:</strong> {selectedReport.resolutionAction}</p>
                {selectedReport.adminNote && (
                  <p><strong>管理者メモ:</strong> {selectedReport.adminNote}</p>
                )}
              </div>
            )}

            <div className="detail-section">
              <h3>管理者メモ</h3>
              <textarea
                className="admin-note-input"
                placeholder="メモを入力..."
                value={actionNote}
                onChange={(e) => setActionNote(e.target.value)}
                rows="3"
              />
            </div>

            <div className="modal-actions">
              <h3>ステータス変更</h3>
              <div className="action-buttons">
                <button
                  className="action-button reviewing"
                  onClick={() => handleStatusChange(selectedReport.id, 'REVIEWING')}
                  disabled={selectedReport.status === 'REVIEWING'}
                >
                  審査中にする
                </button>
                <button
                  className="action-button dismissed"
                  onClick={() => handleStatusChange(selectedReport.id, 'DISMISSED')}
                  disabled={selectedReport.status === 'DISMISSED'}
                >
                  却下する
                </button>
              </div>

              <h3 style={{ marginTop: '20px' }}>モデレーションアクション</h3>
              <div className="action-buttons">
                {selectedReport.reportType === 'POST' && (
                  <>
                    <button
                      className="action-button warning"
                      onClick={() => handleModerationAction(selectedReport.id, 'BLIND_POST')}
                    >
                      投稿をブラインド
                    </button>
                    <button
                      className="action-button danger"
                      onClick={() => handleModerationAction(selectedReport.id, 'DELETE_POST')}
                    >
                      投稿を削除
                    </button>
                  </>
                )}
                {selectedReport.reportType === 'COMMENT' && (
                  <button
                    className="action-button danger"
                    onClick={() => handleModerationAction(selectedReport.id, 'DELETE_COMMENT')}
                  >
                    コメントを削除
                  </button>
                )}
                <button
                  className="action-button info"
                  onClick={() => handleModerationAction(selectedReport.id, 'WARNING')}
                >
                  警告のみ
                </button>
              </div>
            </div>

            <button className="close-button" onClick={() => setShowDetailModal(false)}>
              閉じる
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminReportsPage;
