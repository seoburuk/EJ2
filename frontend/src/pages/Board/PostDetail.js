import React from 'react';
import './PostDetail.css';

function PostDetail({ post, onClose, onEdit, onDelete }) {
    if (!post) return null;

    const handleDelete = () => {
        if (window.confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
            onDelete(post.id);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content detail-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>게시글 상세</h2>
                    <button className="close-btn" onClick={onClose}>&times;</button>
                </div>

                <div className="post-detail-content">
                    <div className="post-header">
                        <h1 className="post-title">{post.title}</h1>
                        <div className="post-meta">
                            <span className="post-author">작성자: {post.author}</span>
                            <span className="post-date">
                                작성일: {new Date(post.createdAt).toLocaleString('ko-KR')}
                            </span>
                            {post.updatedAt && post.updatedAt !== post.createdAt && (
                                <span className="post-updated">
                                    (수정됨: {new Date(post.updatedAt).toLocaleString('ko-KR')})
                                </span>
                            )}
                        </div>
                    </div>

                    <div className="post-body">
                        <pre className="post-content">{post.content}</pre>
                    </div>

                    <div className="post-actions">
                        <button className="btn-edit" onClick={() => onEdit(post)}>
                            수정
                        </button>
                        <button className="btn-delete" onClick={handleDelete}>
                            삭제
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default PostDetail;
