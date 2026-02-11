import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './BoardListPage.css';

function BoardListPage() {
  const [boards, setBoards] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchBoards();
  }, []);

  const fetchBoards = async () => {
    try {
      const response = await axios.get('/api/boards');
      setBoards(response.data);
      setLoading(false);
    } catch (error) {
      console.error('掲示板の読み込みに失敗しました:', error);
      // モックデータをフォールバックとして使用
      const mockBoards = [
        { id: 1, code: 'GENERAL', name: '自由掲示板', description: '自由に話題を共有する掲示板です', isAnonymous: false },
        { id: 2, code: 'ANONYMOUS', name: '匿名掲示板', description: '匿名で自由に意見を交換できます', isAnonymous: true },
        { id: 3, code: 'EVENT', name: 'イベント掲示板', description: '大学のイベント情報を共有します', isAnonymous: false },
        { id: 4, code: 'MARKET', name: '中古市場', description: '中古品を売買する掲示板です', isAnonymous: false },
        { id: 5, code: 'BEST', name: 'ベスト掲示板', description: '人気投稿が集まる掲示板です', isAnonymous: false },
      ];
      setBoards(mockBoards);
      setLoading(false);
    }
  };

  const handleBoardClick = (board) => {
    navigate(`/boards/${board.id}/posts`, { state: { board } });
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

  if (loading) {
    return <div className="loading">読み込み中...</div>;
  }

  return (
    <div className="board-list-page">
      <div className="board-list-container">
        <div className="board-list-header">
          <h1>掲示板一覧</h1>
          <p className="board-list-subtitle">興味のある掲示板を選んでください</p>
        </div>

        <div className="boards-grid">
          {boards.map(board => (
            <div
              key={board.id}
              className="board-card"
              onClick={() => handleBoardClick(board)}
            >
              <div className="board-card-left">
                <div className="board-icon">{getBoardIcon(board.code)}</div>
                <div className="board-info">
                  <h3 className="board-name">
                    {board.name}
                    {board.isAnonymous && <span className="anonymous-badge">匿名</span>}
                  </h3>
                  <p className="board-description">{board.description}</p>
                </div>
              </div>
              <div className="board-arrow">→</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default BoardListPage;
