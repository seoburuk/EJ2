import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { TimetableCourse, Timetable, DAYS, PERIODS } from '../../types/timetable.ts';
import CourseModal from './CourseModal.tsx';
import './TimetablePage.css';

const TimetablePage: React.FC = () => {
  const [timetable, setTimetable] = useState<Timetable | null>(null);
  const [courses, setCourses] = useState<TimetableCourse[]>([]);
  const [selectedSemester, setSelectedSemester] = useState('spring');
  const [selectedYear, setSelectedYear] = useState(2026);
  const [selectedUserId, setSelectedUserId] = useState(1);
  const [users, setUsers] = useState<any[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState<{day: number, period: number} | null>(null);
  const [editingCourse, setEditingCourse] = useState<TimetableCourse | null>(null);

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    loadTimetable();
  }, [selectedSemester, selectedYear, selectedUserId]);

  const loadUsers = async () => {
    try {
      const response = await axios.get('/api/users');
      setUsers(response.data);
      if (response.data.length > 0) {
        setSelectedUserId(response.data[0].id);
      }
    } catch (error) {
      console.error('사용자 목록 로딩 실패', error);
    }
  };

  const loadTimetable = async () => {
    try {
      const response = await axios.get('/api/timetable', {
        params: {
          semester: selectedSemester,
          year: selectedYear,
          userId: selectedUserId
        }
      });
      console.log('📥 백엔드에서 받은 courses:', response.data.courses);
      setTimetable(response.data.timetable);
      setCourses(response.data.courses);
    } catch (error) {
      console.error('시간표 로딩 실패', error);
    }
  };

  const handleSlotClick = (day: number, period: number) => {
    const existingCourse = courses.find(c => {
      // 새로운 daySchedules 구조 우선 사용
      if (c.daySchedules && c.daySchedules.length > 0) {
        return c.daySchedules.some(schedule =>
          schedule.day === day &&
          schedule.periodStart <= period &&
          schedule.periodEnd >= period
        );
      }

      // 하위 호환성: 기존 구조 지원
      const days = c.daysOfWeek || (c.dayOfWeek ? [c.dayOfWeek] : []);
      return days.includes(day) &&
             (c.periodStart || 0) <= period &&
             (c.periodEnd || 0) >= period;
    });

    if (existingCourse) {
      setEditingCourse(existingCourse);
    } else {
      setSelectedSlot({ day, period });
    }
    setIsModalOpen(true);
  };

  const handleSaveCourse = async (course: TimetableCourse) => {
    try {
      if (!timetable || !timetable.timetableId) {
        alert('시간표를 먼저 불러와주세요');
        return;
      }

      // daySchedules를 백엔드 형식으로 변환
      const daysOfWeek = course.daySchedules.map(s => s.day);
      const periodStart = course.daySchedules.length > 0 ? course.daySchedules[0].periodStart : 1;
      const periodEnd = course.daySchedules.length > 0 ? course.daySchedules[0].periodEnd : 1;

      const dataToSend = {
        ...course,
        timetableId: course.courseId ? undefined : timetable.timetableId,
        daysOfWeek,
        periodStart,
        periodEnd,
        daySchedules: course.daySchedules  // 새로운 형식도 함께 전송
      };

      // デバッグ: バックエンドに送信するデータを確認
      console.log('🚀 バックエンドに送信:', dataToSend);
      console.log('📅 daysOfWeek値:', dataToSend.daysOfWeek);
      console.log('📅 daySchedules値:', dataToSend.daySchedules);

      if (course.courseId) {
        await axios.put(`/api/timetable/course/${course.courseId}`, dataToSend);
      } else {
        await axios.post('/api/timetable/course', dataToSend);
      }
      loadTimetable();
      closeModal();
    } catch (error: any) {
      console.error('❌ エラー:', error.response?.data);
      alert(error.response?.data || '저장 실패');
    }
  };

  const handleDeleteCourse = async (courseId: number) => {
    if (window.confirm('이 과목을 삭제하시겠습니까?')) {
      try {
        await axios.delete(`/api/timetable/course/${courseId}`);
        loadTimetable();
        closeModal();
      } catch (error) {
        alert('삭제 실패');
      }
    }
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedSlot(null);
    setEditingCourse(null);
  };

  const getCourseAtSlot = (day: number, period: number): TimetableCourse | undefined => {
    return courses.find(c => {
      // 새로운 daySchedules 구조 우선 사용
      if (c.daySchedules && c.daySchedules.length > 0) {
        return c.daySchedules.some(schedule =>
          schedule.day === day &&
          schedule.periodStart <= period &&
          schedule.periodEnd >= period
        );
      }

      // 하위 호환성: 기존 구조 지원
      const days = c.daysOfWeek || (c.dayOfWeek ? [c.dayOfWeek] : []);
      return days.includes(day) &&
             (c.periodStart || 0) <= period &&
             (c.periodEnd || 0) >= period;
    });
  };

  const totalCredits = courses.reduce((sum, course) => {
    return sum + (course.credits || 0);
  }, 0);

  return (
    <div className="timetable-container">
      <div className="credits-summary">
        📚 총 학점: <strong>{totalCredits.toFixed(1)}</strong>
      </div>

      <div className="timetable-header">
        <h1>시간표</h1>
      </div>

      <div className="semester-selector">
        <select
          value={selectedUserId}
          onChange={(e) => setSelectedUserId(Number(e.target.value))}
          className="user-selector"
        >
          {users.length === 0 ? (
            <option value={1}>사용자를 추가해주세요</option>
          ) : (
            users.map(user => (
              <option key={user.id} value={user.id}>
                {user.name} ({user.email})
              </option>
            ))
          )}
        </select>
        <select
          value={selectedYear}
          onChange={(e) => setSelectedYear(Number(e.target.value))}
        >
          <option value={2024}>2024년</option>
          <option value={2025}>2025년</option>
          <option value={2026}>2026년</option>
        </select>
        <select
          value={selectedSemester}
          onChange={(e) => setSelectedSemester(e.target.value)}
        >
          <option value="spring">봄학기</option>
          <option value="fall">가을학기</option>
        </select>
      </div>

      <div className="timetable-grid">
        <div className="grid-header">
          <div className="period-column"></div>
          {DAYS.map(day => (
            <div key={day} className="day-header">{day}</div>
          ))}
        </div>

        {PERIODS.map(period => (
          <div key={period.number} className="grid-row">
            <div className="period-cell">
              <div className="period-number">{period.number}限</div>
              <div className="period-time">{period.time}</div>
            </div>
{DAYS.map((_, dayIndex) => {
              const day = dayIndex + 1;
              const course = getCourseAtSlot(day, period.number);

              // 해당 요일의 스케줄 찾기
              let daySchedule = null;
              let isStart = false;
              let span = 1;

              if (course) {
                if (course.daySchedules && course.daySchedules.length > 0) {
                  // 새로운 구조: daySchedules 사용
                  daySchedule = course.daySchedules.find(s => s.day === day);
                  if (daySchedule) {
                    isStart = daySchedule.periodStart === period.number;
                    span = daySchedule.periodEnd - daySchedule.periodStart + 1;
                  }
                } else {
                  // 기존 구조: periodStart/End 사용
                  isStart = (course.periodStart || 0) === period.number;
                  span = (course.periodEnd || 0) - (course.periodStart || 0) + 1;
                }
              }

              // 連続科目の中間セルはスキップ (nullを返す)
              if (course && !isStart) {
                return null;
              }

              return (
                <div
                  key={`${dayIndex}-${period.number}`}
                  className={`course-cell ${course ? 'has-course' : ''}`}
                  style={{
                    backgroundColor: course?.colorCode || 'transparent',
                    gridRow: isStart ? `span ${span}` : undefined,
                    zIndex: isStart ? 10 : 1
                  }}
                  onClick={() => handleSlotClick(day, period.number)}
                >
                  {course && (
                    <div className="course-info">
                      <div className="course-name">{course.courseName}</div>
                      {course.classroom && (
                        <div className="course-classroom">{course.classroom}</div>
                      )}
                      {course.professorName && (
                        <div className="course-professor">{course.professorName}</div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        ))}
      </div>

      {isModalOpen && (
        <CourseModal
          course={editingCourse}
          defaultDay={selectedSlot?.day}
          defaultPeriod={selectedSlot?.period}
          onSave={handleSaveCourse}
          onDelete={editingCourse ? () => handleDeleteCourse(editingCourse.courseId!) : undefined}
          onClose={closeModal}
        />
      )}
    </div>
  );
};

export default TimetablePage;