import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './UsersPage.css';

function UsersPage() {
  const [users, setUsers] = useState([]);
  const [newUser, setNewUser] = useState({ name: '', email: '' });

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
    try {
      await axios.post('/api/users', newUser);
      setNewUser({ name: '', email: '' });
      fetchUsers();
    } catch (error) {
      console.error('Error creating user:', error);
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
        <h2>사용자 추가</h2>
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="이름"
            value={newUser.name}
            onChange={(e) => setNewUser({ ...newUser, name: e.target.value })}
            required
          />
          <input
            type="email"
            placeholder="이메일"
            value={newUser.email}
            onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
            required
          />
          <button type="submit">추가</button>
        </form>
      </div>

      <div className="users-container">
        <h2>사용자 목록</h2>
        <ul>
          {users.map((user) => (
            <li key={user.id}>
              <span>{user.name} - {user.email}</span>
              <button onClick={() => handleDelete(user.id)}>삭제</button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default UsersPage;
