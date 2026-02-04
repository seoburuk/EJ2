import React, { useState, useEffect } from 'react';
import { TimetableCourse, DaySchedule, COURSE_COLORS, DAYS } from '../../types/timetable.ts';
import './CourseModal.css';

interface CourseModalProps {
  course: TimetableCourse | null;
  defaultDay?: number;
  defaultPeriod?: number;
  onSave: (course: TimetableCourse) => void;
  onDelete?: () => void;
  onClose: () => void;
}

const CourseModal: React.FC<CourseModalProps> = ({
  course,
  defaultDay,
  defaultPeriod,
  onSave,
  onDelete,
  onClose
}) => {
  const [formData, setFormData] = useState<TimetableCourse>({
    courseName: '',
    professorName: '',
    classroom: '',
    daySchedules: defaultDay && defaultPeriod
      ? [{ day: defaultDay, periodStart: defaultPeriod, periodEnd: defaultPeriod }]
      : [{ day: 1, periodStart: 1, periodEnd: 1 }],
    credits: 3,
    colorCode: COURSE_COLORS[0],
    memo: ''
  });

  useEffect(() => {
    if (course) {
      // 하위 호환성: 기존 데이터 구조를 새로운 구조로 변환
      let daySchedules: DaySchedule[] = [];

      if (course.daySchedules && course.daySchedules.length > 0) {
        daySchedules = course.daySchedules;
      } else if (course.daysOfWeek && course.daysOfWeek.length > 0) {
        // daysOfWeek를 daySchedules로 변환
        daySchedules = course.daysOfWeek.map(day => ({
          day,
          periodStart: course.periodStart || 1,
          periodEnd: course.periodEnd || 1
        }));
      } else if (course.dayOfWeek) {
        daySchedules = [{
          day: course.dayOfWeek,
          periodStart: course.periodStart || 1,
          periodEnd: course.periodEnd || 1
        }];
      }

      setFormData({
        ...course,
        daySchedules
      });
    }
  }, [course]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    console.log('📤 제출 시 formData:', formData);
    console.log('📅 제출 시 daySchedules:', formData.daySchedules);

    if (!formData.courseName.trim()) {
      alert('과목명을 입력해주세요');
      return;
    }

    if (formData.daySchedules.length === 0) {
      alert('최소 하나의 요일을 선택해주세요');
      return;
    }

    // 각 요일별 시간 검증
    for (const schedule of formData.daySchedules) {
      if (schedule.periodStart > schedule.periodEnd) {
        alert(`${DAYS[schedule.day - 1]}요일: 시작 교시는 종료 교시보다 작아야 합니다`);
        return;
      }
    }

    onSave(formData);
  };

  const handleChange = (field: keyof TimetableCourse, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const addDaySchedule = (day: number) => {
    setFormData(prev => ({
      ...prev,
      daySchedules: [...prev.daySchedules, { day, periodStart: 1, periodEnd: 1 }].sort((a, b) => a.day - b.day)
    }));
  };

  const removeDaySchedule = (day: number) => {
    setFormData(prev => ({
      ...prev,
      daySchedules: prev.daySchedules.filter(s => s.day !== day)
    }));
  };

  const updateDaySchedule = (day: number, field: 'periodStart' | 'periodEnd', value: number) => {
    console.log(`🔧 updateDaySchedule 호출: day=${day}, field=${field}, value=${value}`);
    setFormData(prev => {
      const updated = {
        ...prev,
        daySchedules: prev.daySchedules.map(s =>
          s.day === day ? { ...s, [field]: value } : s
        )
      };
      console.log('📝 업데이트된 daySchedules:', updated.daySchedules);
      return updated;
    });
  };

  const isDaySelected = (day: number) => {
    return formData.daySchedules.some(s => s.day === day);
  };

  const getDaySchedule = (day: number) => {
    return formData.daySchedules.find(s => s.day === day);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{course ? '과목 수정' : '과목 추가'}</h2>
          <button className="close-button" onClick={onClose}>×</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>과목명 *</label>
            <input
              type="text"
              value={formData.courseName}
              onChange={(e) => handleChange('courseName', e.target.value)}
              placeholder="예: 자료구조와 알고리즘"
              required
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>교수님</label>
              <input
                type="text"
                value={formData.professorName || ''}
                onChange={(e) => handleChange('professorName', e.target.value)}
                placeholder="예: 홍길동"
              />
            </div>

            <div className="form-group">
              <label>강의실</label>
              <input
                type="text"
                value={formData.classroom || ''}
                onChange={(e) => handleChange('classroom', e.target.value)}
                placeholder="예: A101"
              />
            </div>
          </div>

          <div className="form-group">
            <label>요일 및 시간 설정 * (복수 선택 가능)</label>
            <div className="days-selector">
              {DAYS.map((day, index) => {
                const dayNumber = index + 1;
                const isSelected = isDaySelected(dayNumber);
                return (
                  <button
                    key={dayNumber}
                    type="button"
                    className={`day-button ${isSelected ? 'selected' : ''}`}
                    onClick={() => isSelected ? removeDaySchedule(dayNumber) : addDaySchedule(dayNumber)}
                  >
                    {day}
                  </button>
                );
              })}
            </div>
          </div>

          {formData.daySchedules.length > 0 && (
            <div className="day-schedules-container">
              {formData.daySchedules.map(schedule => (
                <div key={schedule.day} className="day-schedule-row">
                  <div className="day-label">
                    {DAYS[schedule.day - 1]}요일
                  </div>
                  <div className="time-selectors">
                    <select
                      value={schedule.periodStart}
                      onChange={(e) => updateDaySchedule(schedule.day, 'periodStart', Number(e.target.value))}
                    >
                      {[1, 2, 3, 4, 5, 6, 7].map(p => (
                        <option key={p} value={p}>{p}교시</option>
                      ))}
                    </select>
                    <span className="time-separator">~</span>
                    <select
                      value={schedule.periodEnd}
                      onChange={(e) => updateDaySchedule(schedule.day, 'periodEnd', Number(e.target.value))}
                    >
                      {[1, 2, 3, 4, 5, 6, 7].map(p => (
                        <option key={p} value={p}>{p}교시</option>
                      ))}
                    </select>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="form-row">
            <div className="form-group">
              <label>학점</label>
              <input
                type="number"
                step="0.5"
                min="0"
                max="10"
                value={formData.credits || ''}
                onChange={(e) => handleChange('credits', parseFloat(e.target.value))}
              />
            </div>
          </div>

          <div className="form-group">
            <label>색상</label>
            <div className="color-picker">
              {COURSE_COLORS.map(color => (
                <button
                  key={color}
                  type="button"
                  className={`color-option ${formData.colorCode === color ? 'selected' : ''}`}
                  style={{ backgroundColor: color }}
                  onClick={() => handleChange('colorCode', color)}
                />
              ))}
            </div>
          </div>

          {/*
          <div className="form-group">
            <label>메모</label>
            <textarea
              value={formData.memo || ''}
              onChange={(e) => handleChange('memo', e.target.value)}
              placeholder="예: 중간고사 있음"
              rows={3}
            />
          </div>
          */}

          <div className="modal-actions">
            {onDelete && (
              <button type="button" className="delete-button" onClick={onDelete}>
                삭제
              </button>
            )}
            <button type="button" className="cancel-button" onClick={onClose}>
              취소
            </button>
            <button type="submit" className="save-button">
              저장
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CourseModal;
