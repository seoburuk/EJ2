import React, { useState } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './PostWritePage.css';

function PostWritePage() {
  const { boardId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const board = location.state?.board;

  const [formData, setFormData] = useState({
    title: '',
    content: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Image upload state
  const [selectedImages, setSelectedImages] = useState([]);
  const [imagePreviewUrls, setImagePreviewUrls] = useState([]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    setError('');
  };

  // Handle image selection
  const handleImageSelect = (e) => {
    const files = Array.from(e.target.files);

    // Validate total count
    if (selectedImages.length + files.length > 5) {
      setError('画像は最大5枚までアップロードできます');
      return;
    }

    const validFiles = [];
    const validPreviews = [];

    for (const file of files) {
      // Validate file size (5MB max)
      if (file.size > 5 * 1024 * 1024) {
        setError(`${file.name} は5MBを超えています`);
        continue;
      }

      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
      if (!validTypes.includes(file.type)) {
        setError(`${file.name} は対応していないファイル形式です`);
        continue;
      }

      validFiles.push(file);

      // Create preview URL
      const previewUrl = URL.createObjectURL(file);
      validPreviews.push(previewUrl);
    }

    // Update state
    setSelectedImages(prev => [...prev, ...validFiles]);
    setImagePreviewUrls(prev => [...prev, ...validPreviews]);
  };

  // Remove image
  const handleRemoveImage = (index) => {
    // Revoke object URL to free memory
    URL.revokeObjectURL(imagePreviewUrls[index]);

    setSelectedImages(prev => prev.filter((_, i) => i !== index));
    setImagePreviewUrls(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    console.log('=== 投稿処理開始 ===');

    // Validation
    if (!formData.title.trim()) {
      console.log('エラー: タイトルが空です');
      setError('タイトルを入力してください');
      return;
    }
    if (!formData.content.trim()) {
      console.log('エラー: 内容が空です');
      setError('内容を入力してください');
      return;
    }

    // Get current user
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    console.log('現在のユーザー:', user);

    if (!user.id) {
      console.log('エラー: ユーザーがログインしていません');
      setError('ログインが必要です');
      navigate('/login');
      return;
    }

    setLoading(true);
    setError('');
    console.log('投稿データ準備中...');

    try {
      let imageUrls = [];

      // Upload images if there are any
      if (selectedImages.length > 0) {
        const imageFormData = new FormData();
        selectedImages.forEach(image => {
          imageFormData.append('files', image);
        });

        const uploadResponse = await axios.post('/api/upload/images', imageFormData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });

        if (uploadResponse.data.success && uploadResponse.data.files) {
          imageUrls = uploadResponse.data.files.map(file => file.url);
        }
      }

      // Create post (imageUrls will be saved separately)
      const postData = {
        boardId: parseInt(boardId),
        userId: user.id,
        title: formData.title.trim(),
        content: formData.content.trim(),
        viewCount: 0,
        likeCount: 0,
        dislikeCount: 0,
        commentCount: 0
      };

      // TODO: Save imageUrls to PostImage table after creating post
      // For now, images are uploaded but not linked to the post
      console.log('Uploaded image URLs:', imageUrls);
      console.log('投稿データ:', postData);

      const response = await axios.post('/api/posts', postData);
      console.log('投稿作成成功:', response.data);

      // Clean up preview URLs
      imagePreviewUrls.forEach(url => URL.revokeObjectURL(url));

      // Success - redirect to post detail
      navigate(`/boards/${boardId}/posts/${response.data.id}`, {
        state: { board }
      });
    } catch (err) {
      console.error('=== 投稿作成エラー ===');
      console.error('エラー詳細:', err);
      console.error('レスポンス:', err.response);
      console.error('レスポンスデータ:', err.response?.data);

      const errorMessage = err.response?.data?.message ||
                          err.response?.data?.error ||
                          err.message ||
                          '投稿の作成に失敗しました。もう一度お試しください。';
      setError(errorMessage);
      alert('エラー: ' + errorMessage); // デバッグ用
    } finally {
      setLoading(false);
      console.log('=== 投稿処理完了 ===');
    }
  };

  const handleCancel = () => {
    if (window.confirm('作成中の内容が失われますが、よろしいですか?')) {
      // Clean up preview URLs
      imagePreviewUrls.forEach(url => URL.revokeObjectURL(url));
      navigate(`/boards/${boardId}/posts`, { state: { board } }); // /posts 누락된부분 추가
    }
  };

  return (
    <div className="post-write-page">
      <div className="post-write-container">
        <div className="write-header">
          <h2>新規投稿作成</h2>
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

          {/* Image upload section */}
          <div className="form-group">
            <label>
              画像 ({selectedImages.length} / 5枚、各5MB以下)
            </label>
            <div className="image-upload-section">
              <input
                type="file"
                id="image-upload"
                accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
                multiple
                onChange={handleImageSelect}
                disabled={loading || selectedImages.length >= 5}
                style={{ display: 'none' }}
              />
              <label htmlFor="image-upload" className="upload-button">
                📷 画像を選択
              </label>

              {/* Image previews */}
              {imagePreviewUrls.length > 0 && (
                <div className="image-preview-container">
                  {imagePreviewUrls.map((url, index) => (
                    <div key={index} className="image-preview-item">
                      <img src={url} alt={`Preview ${index + 1}`} />
                      <button
                        type="button"
                        className="remove-image-button"
                        onClick={() => handleRemoveImage(index)}
                        disabled={loading}
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
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
              {loading ? '投稿中...' : '投稿する'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default PostWritePage;
