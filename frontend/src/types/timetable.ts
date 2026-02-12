// 時間割の型定義

export interface Timetable {
  timetableId: number;
  userId: number;
  year: number;
  semester: string;
  name?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DaySchedule {
  day: number;  // 1=月, 2=火, 3=水, 4=木, 5=金
  periodStart: number;  // 1-7
  periodEnd: number;    // 1-7
}

export interface TimetableCourse {
  courseId?: number;
  timetableId?: number;
  courseName: string;
  professorName?: string;
  classroom?: string;
  dayOfWeek?: number;  // 1=月, 2=火, 3=水, 4=木, 5=金, 6=土, 7=日 (deprecated, use daySchedules)
  daysOfWeek?: number[];  // [1, 3] = 月+水 (deprecated, use daySchedules)
  periodStart?: number;  // 1-9 (deprecated, use daySchedules)
  periodEnd?: number;    // 1-9 (deprecated, use daySchedules)
  daySchedules: DaySchedule[];  // [{day: 1, periodStart: 1, periodEnd: 3}, {day: 3, periodStart: 1, periodEnd: 3}]
  credits?: number;
  colorCode?: string;
  //memo?: string;
}

export const DAYS = ['月', '火', '水', '木', '金'] as const;

export const PERIODS = [
  { number: 1, time: '09:00-09:50' },
  { number: 2, time: '10:00-10:50' },
  { number: 3, time: '11:00-11:50' },
  { number: 4, time: '12:00-12:50' },
  { number: 5, time: '13:00-13:50' },
  { number: 6, time: '14:00-14:50' },
  { number: 7, time: '15:00-15:50' },
  { number: 8, time: '16:00-16:50' },
  { number: 9, time: '17:00-17:50' },
  { number: 10, time: '18:00-18:50' },
] as const;

export const COURSE_COLORS = [
  '#FFB3BA', '#FFDFBA', '#FFFFBA', '#BAFFC9',
  '#BAE1FF', '#E0BBE4', '#FFDFD3', '#C7CEEA',
  '#B4E7CE', '#FFC8DD'
] as const;
