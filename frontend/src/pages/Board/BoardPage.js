import React, { useState, useEffect } from 'react';
import axios from 'axios';
import PostForm from './PostForm';
import PostDetail from './PostDetail';
import './BoardPage.css';

function BoardPage() {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [selectedPost, setSelectedPost] = useState(null);
    const [editingPost, setEditingPost] = useState(null);

    useEffect(() => {
        // Fetch posts when component mounts
        fetchPosts();
    }, []);

    const fetchPosts = async () => {
        setLoading(true);
        setError(null);

        try {
            const response = await axios.get('ej2/api/posts');
            setPosts(response.data);
        } catch (error) {
            setError(error.message);
            console.error('Failed to fetch posts : ', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return <div className="board-container">Loading...</div>;
    }

    if (error) {
        return <div className="board-container">Error: {error}</div>;
    }

    const handleCreatePost = () => {
        setEditingPost(null);
        setShowForm(true);
    };

    const handleCloseForm = () => {
        setShowForm(false);
        setEditingPost(null);
    };

    const handleSubmitSuccess = () => {
        fetchPosts(); // Refresh the post list
    };

    const handlePostClick = (post) => {
        setSelectedPost(post);
    };

    const handleCloseDetail = () => {
        setSelectedPost(null);
    };

    const handleEdit = (post) => {
        setSelectedPost(null);
        setEditingPost(post);
        setShowForm(true);
    };

    const handleDelete = async (postId) => {
        try {
            await axios.delete(`/api/posts/${postId}`);
            setSelectedPost(null);
            fetchPosts();
        } catch (error) {
            console.error('Failed to delete post:', error);
            alert('게시글 삭제에 실패했습니다.');
        }
    };

    return (
        <div className="board-container">
            <div className="board-header">
                <h1>게시판</h1>
                <button className="btn-create" onClick={handleCreatePost}>글쓰기</button>
            </div>

            <div className="post-list">
                {posts.length === 0 ? (
                    <p className="no-posts">게시글이 없습니다.</p>
                ) : (
                    <table className="post-table">
                        <thead>
                            <tr>
                                <th>번호</th>
                                <th>제목</th>
                                <th>작성자</th>
                                <th>작성일</th>
                            </tr>
                        </thead>
                        <tbody>
                            {posts.map((post, index) => (
                                <tr key={post.id} onClick={() => handlePostClick(post)}>
                                    <td>{posts.length - index}</td>
                                    <td className="post-title">{post.title}</td>
                                    <td>{post.author}</td>
                                    <td>{new Date(post.createdAt).toLocaleDateString()}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {showForm && (
                <PostForm
                    post={editingPost}
                    onClose={handleCloseForm}
                    onSubmitSuccess={handleSubmitSuccess}
                />
            )}

            {selectedPost && (
                <PostDetail
                    post={selectedPost}
                    onClose={handleCloseDetail}
                    onEdit={handleEdit}
                    onDelete={handleDelete}
                />
            )}
        </div>
    );
}

export default BoardPage;
