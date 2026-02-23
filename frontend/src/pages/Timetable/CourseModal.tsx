import React, { useState, useEffect } from 'react';
import { TimetableCourse, DaySchedule, COURSE_COLORS, DAYS, PERIODS } from '../../types/timetable.ts';
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
      alert('科目名を入力してください。');
      return;
    }

    if (formData.daySchedules.length === 0) {
      alert('最低一つの曜日を選んでください。');
      return;
    }

    // 각 요일별 시간 검증
    for (const schedule of formData.daySchedules) {
      if (schedule.periodStart > schedule.periodEnd) {
        alert(`${DAYS[schedule.day - 1]}曜日: 開始時限は終了時限より前の値を選択してください。`);
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
      console.log('📝 修正された daySchedules:', updated.daySchedules);
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
          <h2>{course ? '科目修正' : '科目追加'}</h2>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>科目名 *</label>
            <input
              type="text"
              value={formData.courseName}
              onChange={(e) => handleChange('courseName', e.target.value)}
              placeholder="例: 資料構造とアルゴリズム"
              required
            />
          </div>

          <div className="form-row-three">
            <div className="form-group">
              <label>教授</label>
              <input
                type="text"
                value={formData.professorName || ''}
                onChange={(e) => handleChange('professorName', e.target.value)}
                placeholder="例: 田中サイオ"
              />
            </div>

            <div className="form-group">
              <label>講義室</label>
              <input
                type="text"
                value={formData.classroom || ''}
                onChange={(e) => handleChange('classroom', e.target.value)}
                placeholder="例: A101"
              />
            </div>

            <div className="form-group">
              <label>単位</label>
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
            <label>曜日及び時間設定 * (複数選択可能)</label>
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
                    {DAYS[schedule.day - 1]}曜日
                  </div>
                  <div className="time-selectors">
                    <select
                      value={schedule.periodStart}
                      onChange={(e) => updateDaySchedule(schedule.day, 'periodStart', Number(e.target.value))}
                    >
                      {PERIODS.map(period => (
                        <option key={period.number} value={period.number}>{period.number}時限</option>
                      ))}
                    </select>
                    <span className="time-separator">~</span>
                    <select
                      value={schedule.periodEnd}
                      onChange={(e) => updateDaySchedule(schedule.day, 'periodEnd', Number(e.target.value))}
                    >
                      {PERIODS.map(period => (
                        <option key={period.number} value={period.number}>{period.number}時限</option>
                      ))}
                    </select>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="form-group">
            <label>カラー</label>
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
            <label>メモ</label>
            <textarea
              value={formData.memo || ''}
              onChange={(e) => handleChange('memo', e.target.value)}
              placeholder="例: 中間テストあり"
              rows={3}
            />
          </div>
          */}

          <div className="modal-actions">
            {onDelete && (
              <button type="button" className="delete-button" onClick={onDelete}>
                削除
              </button>
            )}
            <button type="button" className="cancel-button" onClick={onClose}>
              キャンセル
            </button>
            <button type="submit" className="save-button">
              保存
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CourseModal;
