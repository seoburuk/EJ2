import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import CommentSection from './CommentSection';
import './PostDetailPage.css';

function PostDetailPage() {
  const { boardId, postId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const board = location.state?.board;

  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(true);

  // 報告モーダル状態
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportReason, setReportReason] = useState('SPAM');
  const [reportDescription, setReportDescription] = useState('');

  useEffect(() => {
    fetchPost();
    incrementViewCount();
  }, [postId]);

  const fetchPost = async () => {
    try {
      const response = await axios.get(`/api/posts/${postId}`);
      setPost(response.data);
      setLoading(false);
    } catch (error) {
      console.error('投稿の読み込みに失敗しました:', error);
      // モックデータ
      const mockPost = {
        id: postId,
        title: '時間割管理システムの使い方について',
        content: `# 時間割管理システムの使い方

このシステムを使って簡単に時間割を作成・管理できます。

## 主な機能
1. 時間割の作成
2. 科目の追加・削除
3. 時間割の共有
4. エクスポート機能

ぜひ活用してみてください！`,
        userId: 1,
        viewCount: 234,
        likeCount: 12,
        commentCount: 5,
        createdAt: '2026-01-20T10:30:00',
        updatedAt: '2026-01-20T10:30:00'
      };
      setPost(mockPost);
      setLoading(false);
    }
  };

  const incrementViewCount = async () => {
    try {
      // Get current user if logged in
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const userId = user.id || null;

      // Send user ID as query parameter
      const params = userId ? { userId } : {};
      await axios.post(`/api/posts/${postId}/view`, null, { params });
    } catch (error) {
      console.error('閲覧数の更新に失敗しました:', error);
    }
  };

  const handleLikePost = async () => {
    try {
      // Get current user if logged in
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const userId = user.id || null;

      // Send user ID as query parameter
      const params = userId ? { userId } : {};
      await axios.post(`/api/posts/${postId}/like`, null, { params });
      fetchPost();
    } catch (error) {
      console.error('いいねに失敗しました:', error);
    }
  };

  const handleDislikePost = async () => {
    try {
      // Get current user if logged in
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const userId = user.id || null;

      // Send user ID as query parameter
      const params = userId ? { userId } : {};
      await axios.post(`/api/posts/${postId}/dislike`, null, { params });
      fetchPost();
    } catch (error) {
      console.error('よくないに失敗しました:', error);  
    } 
  };


  const handleBack = () => {
    navigate(`/boards/${boardId}/posts`, { state: { board } });
  };

  const handleEdit = () => {
    navigate(`/boards/${boardId}/posts/${postId}/edit`, {
      state: { board, post }
    });
  };

  // 投稿共有ハンドラ
  const handleSharePost = () => {
    const postUrl = `${window.location.origin}/boards/${boardId}/posts/${postId}`;
    navigator.clipboard.writeText(postUrl).then(() => {
      alert('URLのコピーに成功しました: ' );
    }).catch((err) => {
      console.error('URLのコピーに失敗しました:', err);
    });
  };


  const handleDelete = async () => {
    if (!window.confirm('本当にこの投稿を削除しますか？')) {
      return;
    }

    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (!user.id) {
      alert('ログインが必要です');
      return;
    }

    try {
      await axios.delete(`/api/posts/${postId}?userId=${user.id}`);
      alert('投稿が削除されました');
      navigate(`/boards/${boardId}/posts`, { state: { board } });
    } catch (error) {
      console.error('投稿の削除に失敗しました:', error);
      if (error.response && error.response.status === 403) {
        alert('自分の投稿のみ削除できます。');
      } else {
        alert('投稿の削除に失敗しました');
      }
    }
  };

  const canModifyPost = () => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    return user.id && post && user.id === post.userId;
  };

  // 報告提出ハンドラ
  const handleSubmitReport = async () => {
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      if (!user.id) {
        alert('ログインが必要です');
        return;
      }

      await axios.post(
        '/api/reports',
        {
          reportType: 'POST',
          entityId: parseInt(postId),
          reason: reportReason,
          description: reportDescription
        },
        { withCredentials: true }
      );

      alert('報告が受理されました');
      setShowReportModal(false);
      setReportReason('SPAM');
      setReportDescription('');
    } catch (error) {
      console.error('報告の提出に失敗しました:', error);
      if (error.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert('報告の提出に失敗しました');
      }
    }
  };

  const getFormattedDate = (dateString) => {
    if (!dateString) return '---';

    const date = new Date(dateString);

    // Check if date is valid
    if (isNaN(date.getTime())) return '---';

    return date.toLocaleString('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return <div className="loading">読み込み中...</div>;
  }

  if (!post) {
    return <div className="error">投稿が見つかりませんでした</div>;
  }

  return (
    <div className="post-detail-page">
      <div className="post-detail-container">
        <div className="post-detail-header">
          <button className="back-button" onClick={handleBack}>
            ← 一覧に戻る
          </button>
          <div className="header-right">
            <div className="board-badge">{board?.name || '掲示板'}</div>
            {!canModifyPost() && (
              <button
                className="report-button"
                onClick={() => setShowReportModal(true)}
              >
                🚨 報告
              </button>
            )}
            {canModifyPost() && (
              <>
                <button className="edit-button" onClick={handleEdit}>
                  ✏️ 編集
                </button>
                <button className="delete-button" onClick={handleDelete}>
                  🗑️ 削除
                </button>
              </>
            )}
          </div>
        </div>

        <div className="post-content-wrapper">
          <div className="post-header">
            <h1 className="post-title">{post.title}</h1>
            <div className="post-meta">
              <div className="meta-left">
                <span className="author">
                  {board?.isAnonymous ? post.anonymousId || '匿名' : (post.authorNickname || 'Unknown User')}
                </span>
                <span className="separator">•</span>
                <span className="date">{getFormattedDate(post.createdAt)}</span>
                {post.updatedAt && post.createdAt &&
                 new Date(post.updatedAt).getTime() - new Date(post.createdAt).getTime() > 1000 && (
                  <>
                    <span className="separator">•</span>
                    <span className="edited">(編集済み)</span>
                  </>
                )}
              </div>
              <div className="meta-right">
                <span className="stat">👁 {post.viewCount}</span>
                <span className="stat">👍 {post.likeCount}</span>
                <span className="stat">💬 {post.commentCount}</span>
              </div>
            </div>
          </div>

          <div className="post-body">
            <div className="post-content">
              {post.content.split('\n').map((line, index) => (
                <p key={index}>{line}</p>
              ))}
            </div>
          </div>

          <div className="post-actions">
            <button className="action-button like-button" onClick={handleLikePost}>
              👍 いいね ({post.likeCount})
            </button>
            <button className="action-button dislike-button" onClick={handleDislikePost}>
              👎 よくない ({post.dislikeCount || 0})
            </button>
            <button className="action-button scrap-button">
              ⭐ スクラップ
            </button>
            <button className="action-button share-button" onClick={handleSharePost}>
              🔗 共有
            </button>
          </div>
        </div>

        <CommentSection
          postId={postId}
          boardId={boardId}
          isAnonymous={board?.isAnonymous || false}
        />
      </div>

      {/* 報告モーダル */}
      {showReportModal && (
        <div className="modal-overlay" onClick={() => setShowReportModal(false)}>
          <div className="report-modal" onClick={(e) => e.stopPropagation()}>
            <h2>投稿を報告</h2>
            <p className="modal-description">
              この投稿が利用規約に違反していると思われる場合は、以下のフォームで報告してください。
            </p>

            <div className="form-group">
              <label>報告理由 <span className="required">*</span></label>
              <div className="radio-group">
                <label className="radio-label">
                  <input
                    type="radio"
                    value="SPAM"
                    checked={reportReason === 'SPAM'}
                    onChange={(e) => setReportReason(e.target.value)}
                  />
                  <span>スパム/広告</span>
                </label>
                <label className="radio-label">
                  <input
                    type="radio"
                    value="HARASSMENT"
                    checked={reportReason === 'HARASSMENT'}
                    onChange={(e) => setReportReason(e.target.value)}
                  />
                  <span>嫌がらせ</span>
                </label>
                <label className="radio-label">
                  <input
                    type="radio"
                    value="INAPPROPRIATE"
                    checked={reportReason === 'INAPPROPRIATE'}
                    onChange={(e) => setReportReason(e.target.value)}
                  />
                  <span>不適切なコンテンツ</span>
                </label>
                <label className="radio-label">
                  <input
                    type="radio"
                    value="HATE_SPEECH"
                    checked={reportReason === 'HATE_SPEECH'}
                    onChange={(e) => setReportReason(e.target.value)}
                  />
                  <span>ヘイトスピーチ</span>
                </label>
                <label className="radio-label">
                  <input
                    type="radio"
                    value="OTHER"
                    checked={reportReason === 'OTHER'}
                    onChange={(e) => setReportReason(e.target.value)}
                  />
                  <span>その他</span>
                </label>
              </div>
            </div>

            <div className="form-group">
              <label>詳細説明 (任意)</label>
              <textarea
                className="report-textarea"
                placeholder="報告の詳細を入力してください..."
                value={reportDescription}
                onChange={(e) => setReportDescription(e.target.value)}
                rows="4"
              />
            </div>

            <div className="modal-buttons">
              <button
                className="cancel-button"
                onClick={() => {
                  setShowReportModal(false);
                  setReportReason('SPAM');
                  setReportDescription('');
                }}
              >
                キャンセル
              </button>
              <button className="submit-button" onClick={handleSubmitReport}>
                報告する
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default PostDetailPage;
