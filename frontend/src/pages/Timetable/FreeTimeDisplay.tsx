import React from 'react';
import { TimetableCourse, DAYS, PERIODS } from '../../types/timetable';

interface Props {
  courses: TimetableCourse[];
}

const FreeTimeDisplay: React.FC<Props> = ({ courses }) => {
  const getFreeSlots = () => {
    const freeSlots: { day: number; periods: number[] }[] = [];

    DAYS.forEach((_, dayIndex) => {
      const dayNum = dayIndex + 1;
      const freePeriods = PERIODS.filter(period => {
        return !courses.some(
          c => c.dayOfWeek === dayNum &&
               c.periodStart <= period.number &&
               c.periodEnd >= period.number
        );
      }).map(p => p.number);

      if (freePeriods.length > 0) {
        freeSlots.push({ day: dayNum, periods: freePeriods });
      }
    });

    return freeSlots;
  };

  const freeSlots = getFreeSlots();

  return (
    <div className="free-time-panel">
      <h3>⏰ 空きコマ</h3>
      {freeSlots.length === 0 ? (
        <p>空きコマがありません</p>
      ) : (
        freeSlots.map(slot => (
          <div key={slot.day} className="free-day">
            <strong>{DAYS[slot.day - 1]}曜日:</strong>
            {slot.periods.join('限, ')}限
          </div>
        ))
      )}
    </div>
  );
};

export default FreeTimeDisplay;