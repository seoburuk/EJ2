package com.ej2.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import com.ej2.converter.IntegerListConverter;
import com.ej2.converter.DayScheduleListConverter;
import java.util.List;



@Entity
@Table(name = "timetable_courses")
public class TimetableCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    @JsonBackReference
    private Timetable timetable;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(name = "professor_name", length = 100)
    private String professorName;

    @Column(length = 50)
    private String classroom;

    @Column(name = "day_of_week")
    private Integer dayOfWeek; // 1=月, 2=火, 3=水, 4=木, 5=金 (deprecated)

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "days_of_week", length = 100)
    private List<Integer> daysOfWeek; // [1, 3] = 月+水 (deprecated, use daySchedules)

    @Column(name = "period_start")
    private Integer periodStart; // 1-7 (deprecated, use daySchedules)

    @Column(name = "period_end")
    private Integer periodEnd; // 1-7 (deprecated, use daySchedules)

    @Convert(converter = DayScheduleListConverter.class)
    @Column(name = "day_schedules", length = 1000)
    private List<DaySchedule> daySchedules; // [{day:1, periodStart:1, periodEnd:3}, {day:3, periodStart:1, periodEnd:3}]

    @Column
    private Double credits;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Column(length = 500)
    private String memo;

    public TimetableCourse() {
    }

    public TimetableCourse(Timetable timetable, String courseName, Integer dayOfWeek,
                          Integer periodStart, Integer periodEnd) {
        this.timetable = timetable;
        this.courseName = courseName;
        this.dayOfWeek = dayOfWeek;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters and Setters
    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Timetable getTimetable() {
        return timetable;
    }

    public void setTimetable(Timetable timetable) {
        this.timetable = timetable;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getProfessorName() {
        return professorName;
    }

    public void setProfessorName(String professorName) {
        this.professorName = professorName;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public Integer getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Integer periodStart) {
        this.periodStart = periodStart;
    }

    public Integer getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Integer periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Double getCredits() {
        return credits;
    }

    public void setCredits(Double credits) {
        this.credits = credits;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public List<DaySchedule> getDaySchedules() {
        return daySchedules;
    }

    public void setDaySchedules(List<DaySchedule> daySchedules) {
        this.daySchedules = daySchedules;
    }
}
