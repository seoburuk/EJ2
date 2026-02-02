import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, Legend
} from 'recharts';
import { FiUsers, FiShield, FiLayout, FiFileText, FiActivity, FiSettings } from 'react-icons/fi';
import './AdminPages.css';

// カウントアップアニメーション用カスタムフック
const useCountUp = (end, duration = 1000) => {
  const [count, setCount] = useState(0);

  useEffect(() => {
    if (end === 0) return;

    let startTime = null;
    const animate = (currentTime) => {
      if (startTime === null) startTime = currentTime;
      const progress = Math.min((currentTime - startTime) / duration, 1);
      setCount(Math.floor(progress * end));

      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };

    requestAnimationFrame(animate);
  }, [end, duration]);

  return count;
};

// 統計カードコンポーネント
const StatCard = ({ icon: Icon, title, value, trend, color, gradient }) => {
  const animatedValue = useCountUp(value || 0);

  return (
    <div className="stat-card" style={{ '--card-gradient': gradient }}>
      <div className="stat-icon" style={{ background: gradient }}>
        <Icon size={24} />
      </div>
      <div className="stat-content">
        <h3 className="stat-title">{title}</h3>
        <p className="stat-value">{animatedValue.toLocaleString()}</p>
        {trend !== undefined && trend !== 0 && (
          <span className={`stat-trend ${trend > 0 ? 'positive' : 'negative'}`}>
            {trend > 0 ? '↑' : '↓'} {Math.abs(trend)} 今週
          </span>
        )}
      </div>
    </div>
  );
};

// 相対時間表示
const getRelativeTime = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return '今';
  if (diffMins < 60) return `${diffMins}分前`;
  if (diffHours < 24) return `${diffHours}時間前`;
  return `${diffDays}日前`;
};

function AdminPage() {
  const [stats, setStats] = useState(null);
  const [weeklyData, setWeeklyData] = useState([]);
  const [boardStats, setBoardStats] = useState([]);
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentTime, setCurrentTime] = useState(new Date());
  const navigate = useNavigate();

  // 現在時刻を更新
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // 管理者権限チェック
  const checkAdminAccess = useCallback(() => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || user.role !== 'ADMIN') {
      navigate('/');
      return false;
    }
    return true;
  }, [navigate]);

  // データ取得
  const fetchDashboardData = useCallback(async () => {
    try {
      const [statsRes, weeklyRes, activityRes, boardStatsRes] = await Promise.all([
        axios.get('/api/admin/dashboard', { withCredentials: true }),
        axios.get('/api/admin/dashboard/weekly', { withCredentials: true }),
        axios.get('/api/admin/dashboard/activity', { withCredentials: true }),
        axios.get('/api/admin/dashboard/board-stats', { withCredentials: true })
      ]);

      setStats(statsRes.data);
      setWeeklyData(weeklyRes.data);
      setActivities(activityRes.data);
      setBoardStats(boardStatsRes.data);
      setLoading(false);
    } catch (err) {
      console.error('Dashboard data fetch error:', err);
      setError('データの読み込みに失敗しました');
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (checkAdminAccess()) {
      fetchDashboardData();
    }
  }, [checkAdminAccess, fetchDashboardData]);

  // ユーザー情報
  const user = JSON.parse(localStorage.getItem('user'));

  if (loading) {
    return (
      <div className="admin-dashboard">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>読み込み中...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="admin-dashboard">
        <div className="error-container">
          <p>{error}</p>
          <button onClick={fetchDashboardData}>再試行</button>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-dashboard">
      {/* ヘッダー */}
      <header className="dashboard-header">
        <div className="header-content">
          <div className="header-title">
            <h1>管理者ダッシュボード</h1>
            <p className="header-welcome">ようこそ、{user?.name || '管理者'}さん</p>
          </div>
          <div className="header-time">
            <span className="current-time">
              {currentTime.toLocaleTimeString('ja-JP')}
            </span>
            <span className="current-date">
              {currentTime.toLocaleDateString('ja-JP', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                weekday: 'long'
              })}
            </span>
          </div>
        </div>
      </header>

      {/* 統計カード */}
      <section className="stats-section">
        <div className="stats-grid">
          <StatCard
            icon={FiUsers}
            title="総ユーザー数"
            value={stats?.totalUsers}
            trend={stats?.newUsersThisWeek}
            gradient="linear-gradient(135deg, #4fc3f7 0%, #29b6f6 100%)"
          />
          <StatCard
            icon={FiShield}
            title="管理者数"
            value={stats?.adminCount}
            gradient="linear-gradient(135deg, #78909c 0%, #607d8b 100%)"
          />
          <StatCard
            icon={FiLayout}
            title="掲示板数"
            value={stats?.totalBoards}
            gradient="linear-gradient(135deg, #26a69a 0%, #4db6ac 100%)"
          />
          <StatCard
            icon={FiFileText}
            title="投稿数"
            value={stats?.totalPosts}
            trend={stats?.newPostsThisWeek}
            gradient="linear-gradient(135deg, #90a4ae 0%, #78909c 100%)"
          />
        </div>
      </section>

      {/* チャートセクション */}
      <section className="charts-section">
        <div className="charts-grid">
          {/* 週間活動チャート */}
          <div className="chart-card">
            <h3 className="chart-title">
              <FiActivity /> 週間活動推移
            </h3>
            <div className="chart-container">
              <ResponsiveContainer width="100%" height={250}>
                <AreaChart data={weeklyData}>
                  <defs>
                    <linearGradient id="colorUsers" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#4fc3f7" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#4fc3f7" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorPosts" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#26a69a" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#26a69a" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorComments" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#90a4ae" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#90a4ae" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                  <XAxis dataKey="name" stroke="rgba(255,255,255,0.7)" />
                  <YAxis stroke="rgba(255,255,255,0.7)" />
                  <Tooltip
                    contentStyle={{
                      background: 'rgba(30, 41, 59, 0.95)',
                      border: 'none',
                      borderRadius: '8px',
                      color: '#fff'
                    }}
                  />
                  <Area type="monotone" dataKey="users" stroke="#4fc3f7" fillOpacity={1} fill="url(#colorUsers)" name="登録" />
                  <Area type="monotone" dataKey="posts" stroke="#26a69a" fillOpacity={1} fill="url(#colorPosts)" name="投稿" />
                  <Area type="monotone" dataKey="comments" stroke="#90a4ae" fillOpacity={1} fill="url(#colorComments)" name="コメント" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
            <div className="chart-legend">
              <span className="legend-item"><span className="legend-dot" style={{background: '#4fc3f7'}}></span>登録</span>
              <span className="legend-item"><span className="legend-dot" style={{background: '#26a69a'}}></span>投稿</span>
              <span className="legend-item"><span className="legend-dot" style={{background: '#90a4ae'}}></span>コメント</span>
            </div>
          </div>

          {/* 掲示板別投稿数チャート */}
          <div className="chart-card">
            <h3 className="chart-title">
              <FiLayout /> 掲示板別投稿数
            </h3>
            <div className="chart-container">
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={boardStats} layout="vertical" margin={{ left: 20, right: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                  <XAxis type="number" stroke="rgba(255,255,255,0.7)" />
                  <YAxis
                    type="category"
                    dataKey="name"
                    stroke="rgba(255,255,255,0.7)"
                    width={80}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip
                    contentStyle={{
                      background: 'rgba(30, 41, 59, 0.95)',
                      border: 'none',
                      borderRadius: '8px',
                      color: '#fff'
                    }}
                    formatter={(value, name) => [value, name === 'totalPosts' ? '総投稿数' : '週間増加']}
                  />
                  <Legend
                    formatter={(value) => value === 'totalPosts' ? '総投稿数' : '週間増加'}
                    wrapperStyle={{ color: 'rgba(255,255,255,0.7)' }}
                  />
                  <Bar dataKey="totalPosts" fill="#4fc3f7" name="totalPosts" radius={[0, 4, 4, 0]} />
                  <Bar dataKey="weeklyIncrease" fill="#90a4ae" name="weeklyIncrease" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="chart-legend">
              <span className="legend-item"><span className="legend-dot" style={{background: '#4fc3f7'}}></span>総投稿数</span>
              <span className="legend-item"><span className="legend-dot" style={{background: '#90a4ae'}}></span>週間増加</span>
            </div>
          </div>
        </div>
      </section>

      {/* 下部セクション */}
      <section className="bottom-section">
        <div className="bottom-grid">
          {/* 最近のアクティビティ */}
          <div className="activity-card">
            <h3 className="card-title">
              <FiActivity /> 最近のアクティビティ
            </h3>
            <div className="activity-list">
              {activities.length === 0 ? (
                <p className="no-activity">アクティビティがありません</p>
              ) : (
                activities.map((activity, index) => (
                  <div key={index} className="activity-item">
                    <span className="activity-icon">{activity.icon}</span>
                    <div className="activity-content">
                      <p className="activity-message">{activity.message}</p>
                      <span className="activity-time">{getRelativeTime(activity.time)}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* クイックメニュー */}
          <div className="quick-menu-card">
            <h3 className="card-title">
              <FiSettings /> クイック管理
            </h3>
            <div className="quick-menu-list">
              <Link to="/admin/users" className="quick-menu-item">
                <FiUsers />
                <span>ユーザー管理</span>
                <span className="menu-arrow">→</span>
              </Link>
              <Link to="/admin/boards" className="quick-menu-item">
                <FiLayout />
                <span>掲示板管理</span>
                <span className="menu-arrow">→</span>
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default AdminPage;
