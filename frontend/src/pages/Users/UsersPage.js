import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './UsersPage.css';

function UsersPage() {
  const [users, setUsers] = useState([]);
  const [newUser, setNewUser] = useState({ username: '', name: '', email: '', password: '' });
  const [error, setError] = useState('');

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await axios.get('/api/users');
      setUsers(response.data);
    } catch (error) {
      console.error('Error fetching users:', error);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (newUser.password.length < 6) {
      setError('パスワードは6文字以上である必要があります');
      return;
    }

    try {
      await axios.post('/api/auth/register', newUser);
      setNewUser({ username: '', name: '', email: '', password: '' });
      fetchUsers();
    } catch (error) {
      console.error('Error creating user:', error);
      if (error.response?.data?.message) {
        setError(error.response.data.message);
      } else {
        setError('ユーザーの追加に失敗しました');
      }
    }
  };

  const handleDelete = async (id) => {
    try {
      await axios.delete(`/api/users/${id}`);
      fetchUsers();
    } catch (error) {
      console.error('Error deleting user:', error);
    }
  };

  return (
    <div className="users-page">
      <div className="form-container">
        <h2>ユーザー追加</h2>
        {error && <div className="error-message" style={{color: 'red', marginBottom: '10px'}}>{error}</div>}
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="ユーザーID"
            value={newUser.username}
            onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
            required
          />
          <input
            type="text"
            placeholder="名前"
            value={newUser.name}
            onChange={(e) => setNewUser({ ...newUser, name: e.target.value })}
            required
          />
          <input
            type="email"
            placeholder="メールアドレス"
            value={newUser.email}
            onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
            required
          />
          <input
            type="password"
            placeholder="パスワード（6文字以上）"
            value={newUser.password}
            onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
            required
          />
          <button type="submit">追加</button>
        </form>
      </div>

      <div className="users-container">
        <h2>ユーザー一覧</h2>
        <ul>
          {users.map((user) => (
            <li key={user.id}>
              <span>{user.username} ({user.name}) - {user.email}</span>
              <button onClick={() => handleDelete(user.id)}>削除</button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default UsersPage;
