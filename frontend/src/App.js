import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import TimetablePage from './pages/Timetable/TimetablePage.tsx';
import UsersPage from './pages/Users/UsersPage';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <nav className="navbar">
          <div className="nav-container">
            <h1 className="nav-logo">EJ2</h1>
            <ul className="nav-menu">
              <li className="nav-item">
                <Link to="/" className="nav-link">시간표</Link>
              </li>
              <li className="nav-item">
                <Link to="/users" className="nav-link">사용자</Link>
              </li>
            </ul>
          </div>
        </nav>

        <main className="main-content">
          <Routes>
            <Route path="/" element={<TimetablePage />} />
            <Route path="/users" element={<UsersPage />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
