import React, { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './PostWritePage.css'; // Reuse PostWritePage styles

function PostEditPage() {
  const { boardId, postId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const board = location.state?.board;
  const initialPost = location.state?.post;

  const [formData, setFormData] = useState({
    title: '',
    content: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (initialPost) {
      setFormData({
        title: initialPost.title || '',
        content: initialPost.content || ''
      });
    } else {
      // If post data wasn't passed via state, fetch it
      fetchPost();
    }
  }, [initialPost]);

  const fetchPost = async () => {
    try {
      const response = await axios.get(`/api/posts/${postId}`);
      const post = response.data;

      // Check if current user is the author
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      if (user.id !== post.userId) {
        alert('自分の投稿のみ編集できます');
        navigate(`/boards/${boardId}/posts/${postId}`, { state: { board } });
        return;
      }

      setFormData({
        title: post.title || '',
        content: post.content || ''
      });
    } catch (error) {
      console.error('投稿の読み込みに失敗しました:', error);
      alert('投稿の読み込みに失敗しました');
      navigate(`/boards/${boardId}/posts`, { state: { board } });
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!formData.title.trim()) {
      setError('タイトルを入力してください');
      return;
    }
    if (!formData.content.trim()) {
      setError('内容を入力してください');
      return;
    }

    // Check authorization
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (!user.id) {
      setError('ログインが必要です');
      navigate('/login');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const updateData = {
        title: formData.title.trim(),
        content: formData.content.trim()
      };

      await axios.put(`/api/posts/${postId}`, updateData);

      // Success - redirect to post detail
      navigate(`/boards/${boardId}/posts/${postId}`, {
        state: { board }
      });
    } catch (err) {
      console.error('投稿の更新に失敗しました:', err);
      const errorMessage = err.response?.data?.message ||
                          err.response?.data?.error ||
                          err.message ||
                          '投稿の更新に失敗しました。もう一度お試しください。';
      setError(errorMessage);
      alert('エラー: ' + errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    if (window.confirm('編集中の内容が失われますが、よろしいですか?')) {
      navigate(`/boards/${boardId}/posts/${postId}`, { state: { board } });
    }
  };

  return (
    <div className="post-write-page">
      <div className="post-write-container">
        <div className="write-header">
          <h2>投稿編集</h2>
          <div className="board-info">
            <span className="board-name">{board?.name || '掲示板'}</span>
            {board?.isAnonymous && <span className="anonymous-badge">匿名</span>}
          </div>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit} className="write-form">
          <div className="form-group">
            <label htmlFor="title">タイトル</label>
            <input
              type="text"
              id="title"
              name="title"
              value={formData.title}
              onChange={handleChange}
              placeholder="タイトルを入力してください"
              maxLength={255}
              disabled={loading}
            />
            <div className="char-count">{formData.title.length} / 255</div>
          </div>

          <div className="form-group">
            <label htmlFor="content">内容</label>
            <textarea
              id="content"
              name="content"
              value={formData.content}
              onChange={handleChange}
              placeholder="内容を入力してください"
              maxLength={5000}
              rows={15}
              disabled={loading}
            />
            <div className="char-count">{formData.content.length} / 5000</div>
          </div>

          <div className="button-group">
            <button
              type="button"
              className="cancel-button"
              onClick={handleCancel}
              disabled={loading}
            >
              キャンセル
            </button>
            <button
              type="submit"
              className="submit-button"
              disabled={loading}
            >
              {loading ? '更新中...' : '更新する'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default PostEditPage;
