package com.ej2.service;

import com.ej2.model.Timetable;
import com.ej2.model.TimetableCourse;
import com.ej2.repository.TimetableRepository;
import com.ej2.repository.TimetableCourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TimetableService {

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private TimetableCourseRepository courseRepository;

    public Map<String, Object> getTimetableWithCourses(Long userId, Integer year, String semester) {
        // ユーザーの時間割を取得または作成
        Timetable timetable = timetableRepository.findByUserIdAndYearAndSemester(userId, year, semester);

        if (timetable == null) {
            timetable = new Timetable(userId, year, semester);
            timetable = timetableRepository.save(timetable);
        }

        // 科目リストを取得
        List<TimetableCourse> courses = courseRepository.findByTimetableId(timetable.getTimetableId());

        Map<String, Object> result = new HashMap<>();
        result.put("timetable", timetable);
        result.put("courses", courses);

        return result;
    }

    public TimetableCourse addCourse(Long timetableId, TimetableCourse courseData) {
        Timetable timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            throw new RuntimeException("時間割が見つかりません");
        }

        // 時間の重複チェック
        boolean hasConflict = courseRepository.hasTimeConflict(
                timetableId,
                courseData.getDayOfWeek(),
                courseData.getPeriodStart(),
                courseData.getPeriodEnd(),
                null
        );

        if (hasConflict) {
            throw new RuntimeException("この時間には既に科目があります");
        }

        courseData.setTimetable(timetable);
        return courseRepository.save(courseData);
    }

    public TimetableCourse updateCourse(Long courseId, TimetableCourse courseData) {
        TimetableCourse existingCourse = courseRepository.findById(courseId);
        if (existingCourse == null) {
            throw new RuntimeException("科目が見つかりません");
        }

        // 時間の重複チェック（自分自身を除く）
        boolean hasConflict = courseRepository.hasTimeConflict(
                existingCourse.getTimetable().getTimetableId(),
                courseData.getDayOfWeek(),
                courseData.getPeriodStart(),
                courseData.getPeriodEnd(),
                courseId
        );

        if (hasConflict) {
            throw new RuntimeException("この時間には既に科目があります");
        }

        // データの更新
        existingCourse.setCourseName(courseData.getCourseName());
        existingCourse.setProfessorName(courseData.getProfessorName());
        existingCourse.setClassroom(courseData.getClassroom());
        existingCourse.setDayOfWeek(courseData.getDayOfWeek());
        existingCourse.setDaysOfWeek(courseData.getDaysOfWeek());
        existingCourse.setDaySchedules(courseData.getDaySchedules());
        existingCourse.setPeriodStart(courseData.getPeriodStart());
        existingCourse.setPeriodEnd(courseData.getPeriodEnd());
        existingCourse.setCredits(courseData.getCredits());
        existingCourse.setColorCode(courseData.getColorCode());
        //existingCourse.setMemo(courseData.getMemo());

        return courseRepository.save(existingCourse);
    }

    public void deleteCourse(Long courseId) {
        courseRepository.deleteById(courseId);
    }

    public List<Timetable> getUserTimetables(Long userId) {
        return timetableRepository.findByUserId(userId);
    }

    /**
     * 時間割の所有者かどうか確認
     * @param timetableId 時間割ID
     * @param userId ユーザーID
     * @return 所有者であればtrue
     */
    public boolean isOwner(Long timetableId, Long userId) {
        Timetable timetable = timetableRepository.findById(timetableId);
        return timetable != null && timetable.getUserId().equals(userId);
    }

    /**
     * 科目の所有者かどうか確認（科目が属する時間割の所有者）
     * @param courseId 科目ID
     * @param userId ユーザーID
     * @return 所有者であればtrue
     */
    public boolean isCourseOwner(Long courseId, Long userId) {
        TimetableCourse course = courseRepository.findById(courseId);
        if (course == null || course.getTimetable() == null) {
            return false;
        }
        return course.getTimetable().getUserId().equals(userId);
    }
}
