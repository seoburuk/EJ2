import React, { useState, useEffect, useRef } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import MainPage from './pages/Main/MainPage';
import BoardListPage from './pages/Board/BoardListPage';
import PostListPage from './pages/Board/PostListPage';
import PostWritePage from './pages/Board/PostWritePage';
import PostEditPage from './pages/Board/PostEditPage';
import PostDetailPage from './pages/Board/PostDetailPage';
import BoardPage from './pages/Board/BoardPage';
import TimetablePage from './pages/Timetable/TimetablePage.tsx';
import UsersPage from './pages/Users/UsersPage';
import LoginPage from './pages/Auth/LoginPage';
import RegisterPage from './pages/Auth/RegisterPage';
import PasswordResetPage from './pages/Auth/PasswordResetPage';
import FindAccountPage from './pages/Auth/FindAccountPage';
import ChatPage from './pages/Chat/ChatPage';
import AdminPage from './pages/Admin/AdminPage';
import AdminUsersPage from './pages/Admin/AdminUsersPage';
import AdminBoardsPage from './pages/Admin/AdminBoardsPage';
import './App.css';

function NavBar() {
  const [boards, setBoards] = useState([]);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [user, setUser] = useState(null);
  const dropdownRef = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    fetchBoards();
    checkUserLogin();

    // ログイン・ログアウト時にNavBarの状態を更新するイベントリスナー
    const handleAuthChange = () => {
      checkUserLogin();
    };
    window.addEventListener('authChange', handleAuthChange);

    return () => {
      window.removeEventListener('authChange', handleAuthChange);
    };
  }, [location]); // location 추가해서 페이지 이동시마다 확인하도록....

  // ユーザーのログイン状態を確認
  const checkUserLogin = () => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    } else {
      setUser(null);
    }
  };

  // ログアウト処理
  const handleLogout = async () => {
    // 로그아웃도 백엔드 요청 보내도록 수정함
    try {
      await axios.post('/api/auth/logout', {}, { withCredentials : true });

      localStorage.removeItem('user');
      setUser(null);
      navigate('/login');
    }
    catch(err) {
      localStorage.removeItem('user');
      setUser(null);
      navigate('/login');
    }
  };

  useEffect(() => {
    if (!isDropdownOpen) return;

    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isDropdownOpen]);

  const fetchBoards = async () => {
    try {
      const response = await axios.get('/api/boards');
      setBoards(response.data);
    } catch (error) {
      console.error('掲示板の読み込みに失敗しました:', error);
      const mockBoards = [
        { id: 1, code: 'GENERAL', name: '自由掲示板', description: '自由に話題を共有する掲示板です', isAnonymous: false },
        { id: 2, code: 'ANONYMOUS', name: '匿名掲示板', description: '匿名で自由に意見を交換できます', isAnonymous: true },
        { id: 3, code: 'EVENT', name: 'イベント掲示板', description: '大学のイベント情報を共有します', isAnonymous: false },
        { id: 4, code: 'MARKET', name: '中古市場', description: '中古品を売買する掲示板です', isAnonymous: false },
        { id: 5, code: 'BEST', name: 'ベスト掲示板', description: '人気投稿が集まる掲示板です', isAnonymous: false },
      ];
      setBoards(mockBoards);
    }
  };

  const getBoardIcon = (code) => {
    const icons = {
      'GENERAL': '💬',
      'ANONYMOUS': '👤',
      'EVENT': '📅',
      'MARKET': '🛒',
      'BEST': '⭐'
    };
    return icons[code] || '📋';
  };

  const handleBoardSelect = (board) => {
    navigate(`/boards/${board.id}/posts`, { state: { board } });
    setIsDropdownOpen(false);
  };

  return (
    <nav className="navbar">
      <div className="nav-container">
        <Link to="/" className="nav-logo-link">
          <h1 className="nav-logo">EJ2</h1>
        </Link>
        <ul className="nav-menu">
          <li className="nav-item">
            <Link to="/" className="nav-link">ホーム</Link>
          </li>
          <li className="nav-item nav-dropdown" ref={dropdownRef}>
            <button
              className="nav-link nav-dropdown-toggle"
              onClick={() => setIsDropdownOpen(!isDropdownOpen)}
            >
              掲示板 <span className="dropdown-arrow">{isDropdownOpen ? '▲' : '▼'}</span>
            </button>
            {isDropdownOpen && (
              <ul className="nav-dropdown-menu">
                <li>
                  <Link
                    to="/boards"
                    className="nav-dropdown-item"
                    onClick={() => setIsDropdownOpen(false)}
                  >
                    📋 すべての掲示板
                  </Link>
                </li>
                <li className="nav-dropdown-divider"></li>
                {boards.map(board => (
                  <li key={board.id}>
                    <button
                      className="nav-dropdown-item"
                      onClick={() => handleBoardSelect(board)}
                    >
                      {getBoardIcon(board.code)} {board.name}
                      {board.isAnonymous && <span className="anonymous-badge-small">匿名</span>}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </li>
          <li className="nav-item">
            <a href="/chat" className="nav-link" onClick={(e) => {
              e.preventDefault();
              window.open('/chat', 'ej2-chat', 'width=500,height=700,scrollbars=yes,resizable=yes');
            }}>チャット</a>
          </li>
          <li className="nav-item">
            <Link to="/timetable" className="nav-link">時間割</Link>
          </li>
          <li className="nav-item">
            <Link to="/users" className="nav-link">ユーザー</Link>
          </li>

          {/* 管理者メニュー（ADMINのみ表示） */}
          {user && user.role === 'ADMIN' && (
            <li className="nav-item">
              <Link to="/admin" className="nav-link nav-admin">
                🛡️ 管理者
              </Link>
            </li>
          )}

          {/* 認証ボタン */}
          <li className="nav-item nav-auth">
            {user ? (
              <div className="nav-user-info">
                <span className="nav-username">👤 {user.name}</span>
                <button onClick={handleLogout} className="nav-link nav-logout">
                  ログアウト
                </button>
              </div>
            ) : (
              <Link to="/login" className="nav-link nav-login">
                ログイン
              </Link>
            )}
          </li>
        </ul>
      </div>
    </nav>
  );
}

function AppContent() {
  const location = useLocation();
  const isChatPage = location.pathname === '/chat';

  return (
    <div className="App">
      {!isChatPage && <NavBar />}

      <main className={isChatPage ? 'main-content-chat' : 'main-content'}>
        <Routes>
          <Route path="/" element={<MainPage />} />
          <Route path="/boards" element={<BoardListPage />} />
          <Route path="/boards/:boardId/posts" element={<PostListPage />} />
          <Route path="/boards/:boardId/write" element={<PostWritePage />} />
          <Route path="/boards/:boardId/posts/:postId" element={<PostDetailPage />} />
          <Route path="/boards/:boardId/posts/:postId/edit" element={<PostEditPage />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/timetable" element={<TimetablePage />} />
          <Route path="/users" element={<UsersPage />} />

          {/* 認証ページ */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/password-reset" element={<PasswordResetPage />} />
          <Route path="/find-account" element={<FindAccountPage />} />

          {/* 管理者ページ */}
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/boards" element={<AdminBoardsPage />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

export default App;
