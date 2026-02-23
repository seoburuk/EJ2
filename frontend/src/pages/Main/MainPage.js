import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './MainPage.css';

function MainPage() {
  const navigate = useNavigate();
  const [boards, setBoards] = useState([]);
  const [boardPosts, setBoardPosts] = useState({});
  const [popularPosts, setPopularPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState(null);

  useEffect(() => {
    fetchBoardsAndPosts();
    checkUserLogin();

    // ログイン・ログアウト時にMainPageの状態を更新するイベントリスナー
    const handleAuthChange = () => {
      checkUserLogin();
    };
    window.addEventListener('authChange', handleAuthChange);

    return () => {
      window.removeEventListener('authChange', handleAuthChange);
    };
  }, []);

  // ユーザーのログイン状態を確認
  const checkUserLogin = () => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const parsedUser = JSON.parse(storedUser);
      setUser(parsedUser);
    }
  };

  const fetchBoardsAndPosts = async () => {
    try {
      // 掲示板一覧を取得
      const boardsResponse = await axios.get('/api/boards');
      const boardsData = Array.isArray(boardsResponse.data) ? boardsResponse.data : [];

      console.log('取得した掲示板データ:', boardsData);

      if (boardsData.length === 0) {
        console.warn('掲示板データが空です。モックデータを使用します。');
        throw new Error('No boards data');
      }

      setBoards(boardsData);

      // 各掲示板の投稿を5件ずつ取得
      const postsData = {};
      const allPosts = [];

      for (const board of boardsData) {
        try {
          const postsResponse = await axios.get(`/api/posts/board/${board.id}`);
          // レスポンスはList<Post>を直接返すので、.contentではなく.dataを使用
          const posts = Array.isArray(postsResponse.data) ? postsResponse.data : [];
          const limitedPosts = posts.slice(0, 5); // 最初の5件のみ取得
          postsData[board.id] = limitedPosts;
          allPosts.push(...limitedPosts.map(post => ({ ...post, board })));
        } catch (error) {
          console.error(`掲示板${board.id}の投稿取得に失敗:`, error);
          postsData[board.id] = [];
        }
      }

      // 人気投稿（いいね数でソート）
      const popular = allPosts.sort((a, b) => b.likeCount - a.likeCount).slice(0, 10);
      setPopularPosts(popular);

      setBoardPosts(postsData);
      setLoading(false);
    } catch (error) {
      console.error('データの読み込みに失敗:', error);
      // モックデータ
      const mockBoards = [
        { id: 1, code: 'GENERAL', name: '自由掲示板', isAnonymous: false },
        { id: 2, code: 'ANONYMOUS', name: '匿名掲示板', isAnonymous: true },
        { id: 3, code: 'EVENT', name: 'イベント掲示板', isAnonymous: false },
        { id: 4, code: 'MARKET', name: '中古市場', isAnonymous: false },
      ];

      const mockPosts = {
        1: [
          { id: 1, title: '時間割管理システムの使い方', userId: 1, viewCount: 234, likeCount: 45, commentCount: 5, createdAt: '2026-01-20T10:30:00' },
          { id: 2, title: 'おすすめの教養科目教えてください', userId: 2, viewCount: 156, likeCount: 38, commentCount: 15, createdAt: '2026-01-20T09:15:00' },
          { id: 3, title: '図書館の座席予約のコツ', userId: 3, viewCount: 289, likeCount: 56, commentCount: 23, createdAt: '2026-01-19T16:45:00' },
          { id: 4, title: '履修登録期間について', userId: 1, viewCount: 120, likeCount: 10, commentCount: 7, createdAt: '2026-01-19T14:20:00' },
          { id: 5, title: 'サークル新歓情報', userId: 4, viewCount: 198, likeCount: 32, commentCount: 14, createdAt: '2026-01-19T11:30:00' },
        ],
        2: [
          { id: 11, title: '授業で寝てる人多すぎ', userId: 5, anonymousId: '匿名1', viewCount: 456, likeCount: 67, commentCount: 23, createdAt: '2026-01-20T11:00:00' },
          { id: 12, title: '教授の評判ってどう？', userId: 6, anonymousId: '匿名2', viewCount: 534, likeCount: 89, commentCount: 45, createdAt: '2026-01-20T08:30:00' },
          { id: 13, title: 'この大学選んでよかった', userId: 7, anonymousId: '匿名3', viewCount: 567, likeCount: 78, commentCount: 34, createdAt: '2026-01-19T18:20:00' },
          { id: 14, title: '課題が多すぎる件', userId: 8, anonymousId: '匿名4', viewCount: 345, likeCount: 28, commentCount: 19, createdAt: '2026-01-19T15:10:00' },
          { id: 15, title: '食堂のおすすめメニュー', userId: 9, anonymousId: '匿名5', viewCount: 189, likeCount: 22, commentCount: 11, createdAt: '2026-01-19T12:45:00' },
        ],
        3: [
          { id: 21, title: '学園祭ボランティア募集', userId: 10, viewCount: 423, likeCount: 42, commentCount: 16, createdAt: '2026-01-20T09:00:00' },
          { id: 22, title: '就活セミナー開催のお知らせ', userId: 11, viewCount: 634, likeCount: 95, commentCount: 28, createdAt: '2026-01-19T17:30:00' },
          { id: 23, title: 'スポーツ大会参加者募集', userId: 12, viewCount: 256, likeCount: 31, commentCount: 12, createdAt: '2026-01-19T14:00:00' },
          { id: 24, title: '交換留学説明会', userId: 13, viewCount: 498, likeCount: 72, commentCount: 29, createdAt: '2026-01-19T10:15:00' },
          { id: 25, title: '図書館イベント情報', userId: 14, viewCount: 187, likeCount: 19, commentCount: 8, createdAt: '2026-01-18T16:40:00' },
        ],
        4: [
          { id: 31, title: '教科書売ります（経済学入門）', userId: 15, viewCount: 345, likeCount: 28, commentCount: 14, createdAt: '2026-01-20T12:00:00' },
          { id: 32, title: 'ノートPC譲ります', userId: 16, viewCount: 689, likeCount: 103, commentCount: 35, createdAt: '2026-01-20T07:30:00' },
          { id: 33, title: '自転車探してます', userId: 17, viewCount: 234, likeCount: 16, commentCount: 9, createdAt: '2026-01-19T19:20:00' },
          { id: 34, title: '電子辞書買取希望', userId: 18, viewCount: 198, likeCount: 12, commentCount: 5, createdAt: '2026-01-19T13:50:00' },
          { id: 35, title: '家具無料で差し上げます', userId: 19, viewCount: 434, likeCount: 58, commentCount: 22, createdAt: '2026-01-19T09:30:00' },
        ],
      };

      setBoards(mockBoards);
      setBoardPosts(mockPosts);

      // 人気投稿を生成
      const allMockPosts = [];
      mockBoards.forEach(board => {
        if (mockPosts[board.id]) {
          mockPosts[board.id].forEach(post => {
            allMockPosts.push({ ...post, board });
          });
        }
      });
      const popular = allMockPosts.sort((a, b) => b.likeCount - a.likeCount).slice(0, 10);
      setPopularPosts(popular);

      setLoading(false);
    }
  };

  const getTimeAgo = (dateString) => {
    if (!dateString) return '---';

    const now = new Date();
    const past = new Date(dateString);

    // Check if date is valid
    if (isNaN(past.getTime())) return '---';

    const diffInMinutes = Math.floor((now - past) / (1000 * 60));

    if (diffInMinutes < 1) return '今';
    if (diffInMinutes < 60) return `${diffInMinutes}分前`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}時間前`;
    return `${Math.floor(diffInMinutes / 1440)}日前`;
  };

  const handlePostClick = (boardId, postId, board) => {
    navigate(`/boards/${boardId}/posts/${postId}`, { state: { board } });
  };

  const handleBoardClick = (board) => {
    navigate(`/boards/${board.id}/posts`, { state: { board } });
  };

  if (loading) {
    return <div className="loading">読み込み中...</div>;
  }

  return (
    <div className="main-page">
      <div className="main-container-eta">
        {/* Left: Board Sections */}
        <div className="boards-content">
          {boards.map(board => (
            <div key={board.id} className="board-section">
              <div className="board-section-header">
                <h2 className="board-section-title">
                  {board.name}
                  {board.isAnonymous && <span className="anonymous-badge">匿名</span>}
                </h2>
                <button
                  className="view-more-button"
                  onClick={() => handleBoardClick(board)}
                >
                  もっと見る
                </button>
              </div>

              <div className="posts-list-eta">
                {boardPosts[board.id] && boardPosts[board.id].length > 0 ? (
                  boardPosts[board.id].map(post => (
                    <div
                      key={post.id}
                      className="post-item-eta"
                      onClick={() => handlePostClick(board.id, post.id, board)}
                    >
                      <div className="post-item-title">{post.title}</div>
                      <div className="post-item-meta">
                        <div className="post-meta-left">
                          <span className="post-item-author">
                            {board.isAnonymous ? (post.anonymousId || '匿名') : (post.authorNickname || 'Unknown User')}
                          </span>
                          <span className="post-item-separator">•</span>
                          <span className="post-item-time">{getTimeAgo(post.createdAt)}</span>
                        </div>
                        <div className="post-item-stats">
                          <span>👁 {post.viewCount || 0}</span>
                          <span>👍 {post.likeCount || 0}</span>
                          <span>💬 {post.commentCount || 0}</span>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="no-posts">まだ投稿がありません</div>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Right: Sidebar */}
        <div className="sidebar-eta">
          {/* Chat Shortcut */}
          <div className="chat-shortcut-section" onClick={() => {
            window.open('/chat', 'ej2-chat', 'width=500,height=700,scrollbars=yes,resizable=yes');
          }}>
            <div className="chat-shortcut-content">
              <span className="chat-shortcut-icon">💬</span>
              <div>
                <h3 className="chat-shortcut-title">匿名チャット</h3>
                <p className="chat-shortcut-desc">リアルタイムで匿名チャットに参加しよう</p>
              </div>
            </div>
            <span className="chat-shortcut-arrow">↗</span>
          </div>

          {/* Admin Shortcut - ADMINのみ表示 */}
          {user && user.role === 'ADMIN' && (
            <div className="admin-shortcut-section" onClick={() => navigate('/admin')}>
              <div className="admin-shortcut-content">
                <span className="admin-shortcut-icon">🛡️</span>
                <div>
                  <h3 className="admin-shortcut-title">管理者ダッシュボード</h3>
                  <p className="admin-shortcut-desc">システム統計と管理機能</p>
                </div>
              </div>
              <span className="admin-shortcut-arrow">→</span>
            </div>
          )}

          <div className="popular-section-eta">
            <h3 className="sidebar-title-eta">👑 人気トピック</h3>
            <div className="popular-list-eta">
              {popularPosts.map((post, index) => (
                <div
                  key={`${post.board?.id}-${post.id}`}
                  className="popular-item-eta"
                  onClick={() => handlePostClick(post.board?.id, post.id, post.board)}
                >
                  <div className="popular-rank-eta">{index + 1}</div>
                  <div className="popular-content-eta">
                    <div className="popular-board-name">{post.board?.name}</div>
                    <div className="popular-title-eta">{post.title}</div>
                    <div className="popular-stats-eta">
                      👁 {post.viewCount || 0}
                      👍 {post.likeCount || 0}
                      💬 {post.commentCount || 0}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default MainPage;
