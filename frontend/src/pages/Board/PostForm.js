import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './PostForm.css';

function PostForm({ post, onClose, onSubmitSuccess }) {
    const [formData, setFormData] = useState({
        title: '',
        content: '',
        author: ''
    });
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const isEditMode = !!post;

    useEffect(() => {
        if (post) {
            setFormData({
                title: post.title,
                content: post.content,
                author: post.author
            });
        }
    }, [post]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    // TODO(human): Implement form submission logic
    // Hint: Use axios.post('/api/posts', formData) to create a new post
    // On success, call onSubmitSuccess() and onClose()
    // On error, set the error state to display to the user

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        setError(null);

        try {
            if (isEditMode) {
                await axios.put(`/api/posts/${post.id}`, formData);
            } else {
                await axios.post('/api/posts', formData);
            }

            onSubmitSuccess();
            onClose();
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to submit post';
            setError(errorMessage);
            console.error('Failed to submit post:', error);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>{isEditMode ? '글 수정' : '새 글쓰기'}</h2>
                    <button className="close-btn" onClick={onClose}>&times;</button>
                </div>

                {error && (
                    <div className="error-message">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="author">작성자</label>
                        <input
                            type="text"
                            id="author"
                            name="author"
                            value={formData.author}
                            onChange={handleChange}
                            placeholder="작성자 이름을 입력하세요"
                            disabled={isEditMode}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="title">제목</label>
                        <input
                            type="text"
                            id="title"
                            name="title"
                            value={formData.title}
                            onChange={handleChange}
                            placeholder="제목을 입력하세요"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="content">내용</label>
                        <textarea
                            id="content"
                            name="content"
                            value={formData.content}
                            onChange={handleChange}
                            placeholder="내용을 입력하세요"
                            rows="10"
                            required
                        />
                    </div>

                    <div className="form-actions">
                        <button
                            type="button"
                            className="btn-cancel"
                            onClick={onClose}
                            disabled={submitting}
                        >
                            취소
                        </button>
                        <button
                            type="submit"
                            className="btn-submit"
                            disabled={submitting}
                        >
                            {submitting ? (isEditMode ? '수정 중...' : '작성 중...') : (isEditMode ? '수정완료' : '작성완료')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default PostForm;
