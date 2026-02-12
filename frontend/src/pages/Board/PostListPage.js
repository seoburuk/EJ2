import React, { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { FiArrowLeft } from 'react-icons/fi';
import './PostListPage.css';

function PostListPage() {
  const { boardId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const board = location.state?.board;
  const pagingCount = 10;

  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [sortBy, setSortBy] = useState('recent');
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    fetchPosts();
  }, [boardId, currentPage, sortBy]);

  const fetchPosts = async () => {
    try {
      const response = await axios.get(`/api/posts/board/${boardId}/${sortBy}`);
      // レスポンスはList<Post>を直接返すので、.contentではなく.dataを使用
      const posts = Array.isArray(response.data) ? response.data : [];
      const pagingPosts = posts.slice(currentPage * pagingCount, (currentPage + 1) * pagingCount);
      setPosts(pagingPosts);
      setTotalPages(Math.ceil(posts.length / pagingCount)); // 簡易的なページネーション
      setLoading(false);
    } catch (error) {
      console.error('投稿の読み込みに失敗しました:', error);
      // モックデータ
      const mockPosts = [
        { id: 1, title: '時間割管理システムの使い方', userId: 1, viewCount: 234, likeCount: 12, commentCount: 5, createdAt: '2026-01-20T10:30:00' },
        { id: 2, title: 'おすすめの教養科目教えてください', userId: 2, viewCount: 156, likeCount: 8, commentCount: 15, createdAt: '2026-01-20T09:15:00' },
        { id: 3, title: '図書館の座席予約のコツ', userId: 3, viewCount: 89, likeCount: 6, commentCount: 3, createdAt: '2026-01-19T16:45:00' },
      ];
      setPosts(mockPosts);
      setTotalPages(1);
      setLoading(false);
    }
  };

  const handleSortChange = (newSortBy) => {
    setSortBy(newSortBy);
    setCurrentPage(0);
  }

  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      fetchPosts();
      return;
    }

    try {
      const response = await axios.get(`/api/posts/search`, {
        params: {
          keyword: searchKeyword
        }
      });
      const searchResults = Array.isArray(response.data) ? response.data : [];
      // 現在の掲示板の投稿のみフィルタリング
      const filteredPosts = searchResults.filter(post => post.boardId === parseInt(boardId));
      setPosts(filteredPosts);
      setTotalPages(Math.ceil(filteredPosts.length / pagingCount));
      setCurrentPage(0);
    } catch (error) {
      console.error('検索に失敗しました:', error);
      // エラー時は全投稿を表示
      fetchPosts();
    }
  };

  const handlePostClick = (postId) => {
    navigate(`/boards/${boardId}/posts/${postId}`, { state: { board } });
  };

  const getTimeAgo = (dateString) => {
    const now = new Date();
    const past = new Date(dateString);
    const diffInMinutes = Math.floor((now - past) / (1000 * 60));

    if (diffInMinutes < 60) return `${diffInMinutes}分前`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}時間前`;
    return `${Math.floor(diffInMinutes / 1440)}日前`;
  };

  if (loading) {
    return <div className="loading">読み込み中...</div>;
  }

  return (
    <div className="post-list-page">
      <div className="post-list-container">
        <div className="post-list-header">
          <div className="board-title-section">
            <button className="back-button" onClick={() => navigate('/')}>
              <FiArrowLeft size={20} />
              <span>掲示板一覧</span>
            </button>
            <div className="board-title-wrapper">
              <h2>{board?.name || '掲示板'}</h2>
              {board?.isAnonymous && <span className="anonymous-badge">匿名</span>}
            </div>
          </div>

          <div className="controls-section">
            <div className="search-box">
              <input
                type="text"
                placeholder="検索..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              />
              <button onClick={handleSearch}>🔍</button>
            </div>

            <div className="sort-buttons">
              <button
                className={sortBy === 'recent' ? 'active' : ''}
                onClick={() => handleSortChange('recent')}
              >
                最新順
              </button>
              <button
                className={sortBy === 'likes' ? 'active' : ''}
                onClick={() => handleSortChange('likes')}
              >
                人気順
              </button>
              <button
                className={sortBy === 'views' ? 'active' : ''}
                onClick={() => handleSortChange('views')}
              >
                閲覧順
              </button>
            </div>

            <button
              className="write-post-button"
              onClick={() => {
                console.log('=== 投稿ボタンクリック ===');
                console.log('boardId:', boardId);
                console.log('board object:', board);
                console.log('Navigate to:', `/boards/${boardId}/write`);
                navigate(`/boards/${boardId}/write`, { state: { board } });
              }}
            >
              投稿する
            </button>
          </div>
        </div>

        <div className="posts-table">
          <div className="table-header">
            <div className="col-title">タイトル</div>
            <div className="col-author">作成者</div>
            <div className="col-time">作成日時</div>
            <div className="col-stats">統計</div>
          </div>

          {posts.map(post => (
            <div
              key={post.id}
              className="post-row"
              onClick={() => handlePostClick(post.id)}
            >
              <div className="col-title">
                <span className="post-title">{post.title}</span>
                {post.commentCount > 0 && (
                  <span className="comment-count">[{post.commentCount}]</span>
                )}
              </div>
              <div className="col-author">
                {board?.isAnonymous ? post.anonymousId || '匿名' : (post.authorNickname || 'Unknown User')}
              </div>
              <div className="col-time">{getTimeAgo(post.createdAt)}</div>
              <div className="col-stats">
                <span>👁 {post.viewCount}</span>
                <span>👍 {post.likeCount}</span>
              </div>
            </div>
          ))}
        </div>

        {totalPages > 1 && (
          <div className="pagination">
            <button
              disabled={currentPage === 0}
              onClick={() => setCurrentPage(currentPage - 1)}
            >
              前へ
            </button>
            {[...Array(totalPages)].map((_, index) => (
              <button
                key={index}
                className={currentPage === index ? 'active' : ''}
                onClick={() => setCurrentPage(index)}
              >
                {index + 1}
              </button>
            ))}
            <button
              disabled={currentPage === totalPages - 1}
              onClick={() => setCurrentPage(currentPage + 1)}
            >
              次へ
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default PostListPage;
