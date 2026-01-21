import React, { useState } from 'react';
import axios from 'axios';
import './CourseSearchModal.css';

interface CourseCatalog {
  catalogId: number;
  courseName: string;
  professorName: string;
  dayOfWeek: number;
  periodStart: number;
  periodEnd: number;
  classroom: string;
  credits: number;
  department: string;
}

interface Props {
  timetableId: number;
  onClose: () => void;
  onAdd: () => void;
}

const CourseSearchModal: React.FC<Props> = ({ timetableId, onClose, onAdd }) => {
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<CourseCatalog[]>([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    if (!keyword.trim()) return;
    
    setLoading(true);
    try {
      const response = await axios.get('/api/courses/search', {
        params: { keyword }
      });
      setResults(response.data);
    } catch (error) {
      alert('検索エラー');
    } finally {
      setLoading(false);
    }
  };

  const handleAddCourse = async (catalogId: number) => {
    try {
      await axios.post(`/api/courses/add-from-catalog/${catalogId}`, {
        timetableId
      });
      alert('追加しました！');
      onAdd();
      onClose();
    } catch (error: any) {
      alert(error.response?.data || '追加に失敗しました');
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="search-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>🔍 科目検索</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        <div className="search-box">
          <input
            type="text"
            placeholder="科目名または教授名で検索..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
          />
          <button onClick={handleSearch} disabled={loading}>
            {loading ? '検索中...' : '検索'}
          </button>
        </div>

        <div className="search-results">
          {results.length === 0 ? (
            <div className="no-results">
              検索結果がありません
            </div>
          ) : (
            results.map(course => (
              <div key={course.catalogId} className="course-item">
                <div className="course-main-info">
                  <h3>{course.courseName}</h3>
                  <span className="credits">{course.credits}単位</span>
                </div>
                <div className="course-details">
                  <span>👨‍🏫 {course.professorName}</span>
                  <span>📅 {['', '月', '火', '水', '木', '金', '土', '日'][course.dayOfWeek]} {course.periodStart}-{course.periodEnd}限</span>
                  <span>📍 {course.classroom}</span>
                  <span className="department">{course.department}</span>
                </div>
                <button 
                  className="add-btn"
                  onClick={() => handleAddCourse(course.catalogId)}
                >
                  ＋ 追加
                </button>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default CourseSearchModal;