import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './CommentSection.css';

function CommentSection({ postId, boardId, isAnonymous }) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [replyContent, setReplyContent] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editContent, setEditContent] = useState('');
  const [likedComments, setLikedComments] = useState({});

  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    fetchComments();
  }, [postId]);

  const fetchComments = async () => {
    try {
      const response = await axios.get(`/api/comments/post/${postId}`);
      setComments(response.data);

      const likeStatuses = {};
      for (const comment of response.data) {
        try {
          const likeCheck = await axios.get(`/api/comments/${comment.id}/like/check`, {
            params: { userId: user.id || null }
          });
          likeStatuses[comment.id] = likeCheck.data.liked;
        } catch (e) {
          likeStatuses[comment.id] = false;
        }
      }
      setLikedComments(likeStatuses);
      setLoading(false);
    } catch (error) {
      console.error('コメントの読み込みに失敗しました:', error);
      setComments([]);
      setLoading(false);
    }
  };

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim() || !user.id) {
      if (!user.id) alert('ログインが必要です');
      return;
    }
    try {
      const commentData = {
        postId: parseInt(postId),
        userId: user.id,
        content: newComment,
        anonymousId: isAnonymous ? `匿名${Math.floor(Math.random() * 1000)}` : null
      };
      await axios.post('/api/comments', commentData);
      setNewComment('');
      fetchComments();
    } catch (error) {
      alert('コメントの作成に失敗しました。');
    }
  };

  const handleSubmitReply = async (e, parentId) => {
    e.preventDefault();
    if (!replyContent.trim() || !user.id) {
      if (!user.id) alert('ログインが必要です');
      return;
    }
    try {
      const replyData = {
        postId: parseInt(postId),
        userId: user.id,
        parentId: parentId,
        content: replyContent,
        anonymousId: isAnonymous ? `匿名${Math.floor(Math.random() * 1000)}` : null
      };
      await axios.post('/api/comments', replyData);
      setReplyContent('');
      setReplyTo(null);
      fetchComments();
    } catch (error) {
      alert('返信の作成に失敗しました。');
    }
  };

  const handleLikeComment = async (commentId) => {
    try {
      const response = await axios.post(`/api/comments/${commentId}/like`, null, {
        params: { userId: user.id || null }
      });
      setLikedComments(prev => ({ ...prev, [commentId]: response.data.liked }));
      setComments(prev => prev.map(comment => {
        if (comment.id === commentId) {
          return {
            ...comment,
            likeCount: response.data.liked ? (comment.likeCount || 0) + 1 : Math.max(0, (comment.likeCount || 0) - 1)
          };
        }
        return comment;
      }));
    } catch (error) {
      console.error('いいねに失敗しました:', error);
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm('コメントを削除しますか？') || !user.id) return;
    try {
      await axios.delete(`/api/comments/${commentId}?userId=${user.id}`);
      fetchComments();
    } catch (error) {
      alert('コメントの削除に失敗しました。');
    }
  };

  const handleSaveEdit = async (commentId) => {
    if (!editContent.trim() || !user.id) return;
    try {
      await axios.put(`/api/comments/${commentId}?userId=${user.id}`, { content: editContent });
      setEditingId(null);
      setEditContent('');
      fetchComments();
    } catch (error) {
      alert('コメントの更新に失敗しました。');
    }
  };

  const getTimeAgo = (dateString) => {
    const now = new Date();
    const past = new Date(dateString);
    const diffInMinutes = Math.floor((now - past) / (1000 * 60));
    if (diffInMinutes < 60) return `${diffInMinutes}分前`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}時間前`;
    if (diffInMinutes < 10080) return `${Math.floor(diffInMinutes / 1440)}日前`;
    return past.toLocaleDateString('ja-JP');
  };

  const topLevelComments = comments.filter(c => !c.parentId);
  const getReplies = (parentId) => comments.filter(c => c.parentId === parentId);

  if (loading) return <div className="comments-loading">コメントを読み込み中...</div>;

  return (
    <div className="comments-section">
      <div className="comments-header">
        <h3>コメント {comments.length}</h3>
      </div>

      <form className="comment-form" onSubmit={handleSubmitComment}>
        <textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="コメントを入力してください..."
          rows="3"
        />
        <button type="submit" className="submit-comment-btn">コメントを投稿</button>
      </form>

      <div className="comments-list">
        {topLevelComments.length === 0 ? (
          <div className="no-comments">最初のコメントを書いてみましょう！</div>
        ) : (
          topLevelComments.map(comment => (
            <div key={comment.id} className="comment-item">
              <div className="comment-header">
                <span className="comment-author">
                  {isAnonymous ? comment.anonymousId || '匿名' : (comment.authorNickname || 'Unknown User')}
                </span>
                <div>
                  <span className="comment-time">{getTimeAgo(comment.createdAt)}</span>
                </div>
              </div>

              {editingId === comment.id ? (
                <div className="comment-edit-form">
                  <textarea value={editContent} onChange={(e) => setEditContent(e.target.value)} rows="3" />
                  <div className="edit-actions">
                    <button onClick={() => setEditingId(null)}>キャンセル</button>
                    <button onClick={() => handleSaveEdit(comment.id)}>保存</button>
                  </div>
                </div>
              ) : (
                <div className="comment-content">{comment.content}</div>
              )}

              {!comment.isDeleted && (
                <div className="comment-actions">
                  <button className={`comment-action-btn ${likedComments[comment.id] ? 'liked' : ''}`} onClick={() => handleLikeComment(comment.id)}>
                    👍 {comment.likeCount || 0}
                  </button>
                  <button className="comment-action-btn" onClick={() => setReplyTo(replyTo === comment.id ? null : comment.id)}>
                    💬 返信
                  </button>
                  {user.id && comment.userId === user.id && (
                    <>
                      <button className="comment-action-btn edit" onClick={() => { setEditingId(comment.id); setEditContent(comment.content); }}>✏️ 編集</button>
                      <button className="comment-action-btn delete" onClick={() => handleDeleteComment(comment.id)}>🗑️ 削除</button>
                    </>
                  )}
                </div>
              )}

              {/* 답글 입력창 및 답글 리스트 */}
              {replyTo === comment.id && (
                <form className="reply-form" onSubmit={(e) => handleSubmitReply(e, comment.id)}>
                  <textarea value={replyContent} onChange={(e) => setReplyContent(e.target.value)} placeholder="返信を入力してください..." rows="2" />
                  <div className="reply-actions">
                    <button type="button" onClick={() => setReplyTo(null)}>キャンセル</button>
                    <button type="submit">返信を投稿</button>
                  </div>
                </form>
              )}

              {getReplies(comment.id).map(reply => (
                <div key={reply.id} className="comment-item reply">
                  <div className="comment-header">
                    <span className="comment-author">
                      {isAnonymous ? reply.anonymousId || '匿名' : (reply.authorNickname || 'Unknown User')}
                    </span>
                    <div>
                      <span className="comment-time">{getTimeAgo(reply.createdAt)}</span>
                      {/* 답글 수정됨 표시 로직 */}
                      {reply.updatedAt && reply.createdAt &&
                       new Date(reply.updatedAt).getTime() - new Date(reply.createdAt).getTime() > 1000 && (
                        <span className="comment-time">（編集済み）</span>
                      )}
                    </div>
                  </div>
                  {editingId === reply.id ? (
                    <div className="comment-edit-form">
                      <textarea value={editContent} onChange={(e) => setEditContent(e.target.value)} rows="2" />
                      <div className="edit-actions">
                        <button onClick={() => setEditingId(null)}>キャンセル</button>
                        <button onClick={() => handleSaveEdit(reply.id)}>保存</button>
                      </div>
                    </div>
                  ) : (
                    <div className="comment-content">{reply.content}</div>
                  )}
                  {!reply.isDeleted && (
                    <div className="comment-actions">
                      <button className={`comment-action-btn ${likedComments[reply.id] ? 'liked' : ''}`} onClick={() => handleLikeComment(reply.id)}>
                        👍 {reply.likeCount || 0}
                      </button>
                      {user.id && reply.userId === user.id && (
                        <>
                          <button className="comment-action-btn edit" onClick={() => { setEditingId(reply.id); setEditContent(reply.content); }}>✏️ 編集</button>
                          <button className="comment-action-btn delete" onClick={() => handleDeleteComment(reply.id)}>🗑️ 削除</button>
                        </>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default CommentSection;
