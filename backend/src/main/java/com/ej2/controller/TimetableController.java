package com.ej2.controller;

import com.ej2.model.DaySchedule;
import com.ej2.model.TimetableCourse;
import com.ej2.service.TimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timetable")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TimetableController {

    @Autowired
    private TimetableService timetableService;

    /**
     * リクエストデータから曜日リストを取得するヘルパーメソッド
     * daysOfWeekが存在すればそれを使用し、なければdayOfWeekを配列化
     */
    @SuppressWarnings("unchecked")
    private List<Integer> extractDaysOfWeek(Map<String, Object> requestData) {
        Object daysOfWeekObj = requestData.get("daysOfWeek");

        // daysOfWeekが存在する場合
        if (daysOfWeekObj != null) {
            if (daysOfWeekObj instanceof List) {
                List<?> rawList = (List<?>) daysOfWeekObj;
                List<Integer> days = new ArrayList<>();
                for (Object item : rawList) {
                    days.add(Integer.valueOf(item.toString()));
                }
                return days;
            }
        }

        // 下位互換性: dayOfWeekが存在する場合
        Object dayOfWeekObj = requestData.get("dayOfWeek");
        if (dayOfWeekObj != null) {
            List<Integer> days = new ArrayList<>();
            days.add(Integer.valueOf(dayOfWeekObj.toString()));
            return days;
        }

        return null;
    }

    /**
     * セッションからログイン中のユーザーIDを取得
     * @param session HTTPセッション
     * @return ユーザーID（未ログインの場合はnull）
     */
    private Long getLoggedInUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId != null ? (Long) userId : null;
    }

    // 시간표와 과목 목록 조회
    @GetMapping
    public ResponseEntity<?> getTimetable(
            @RequestParam Integer year,
            @RequestParam String semester,
            HttpSession session) {

        // ログインチェック
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("ログインが必要です");
        }

        try {
            Map<String, Object> result = timetableService.getTimetableWithCourses(userId, year, semester);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("시간표 로딩 실패: " + e.getMessage());
        }
    }

    // 과목 추가
    @PostMapping("/course")
    public ResponseEntity<?> addCourse(@RequestBody Map<String, Object> requestData, HttpSession session) {
        // ログインチェック
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("ログインが必要です");
        }

        try {
            // デバッグ: 受信データを確認
            System.out.println("📥 受信したデータ: " + requestData);
            System.out.println("📅 daySchedules値: " + requestData.get("daySchedules"));

            // Null 체크
            if (requestData.get("timetableId") == null) {
                return ResponseEntity.badRequest().body("시간표 ID가 필요합니다");
            }

            Long timetableId = Long.valueOf(requestData.get("timetableId").toString());

            // 権限チェック: 時間割の所有者かどうか確認
            if (!timetableService.isOwner(timetableId, userId)) {
                return ResponseEntity.status(403).body("この時間割を編集する権限がありません");
            }
            if (requestData.get("courseName") == null) {
                return ResponseEntity.badRequest().body("과목명이 필요합니다");
            }

            TimetableCourse course = new TimetableCourse();
            course.setCourseName((String) requestData.get("courseName"));
            course.setProfessorName(requestData.get("professorName") != null ?
                (String) requestData.get("professorName") : "");
            course.setClassroom(requestData.get("classroom") != null ?
                (String) requestData.get("classroom") : "");

            // daySchedules 처리 (새로운 구조 우선)
            Object daySchedulesObj = requestData.get("daySchedules");
            if (daySchedulesObj != null && daySchedulesObj instanceof List) {
                List<?> schedulesList = (List<?>) daySchedulesObj;
                List<DaySchedule> daySchedules = new ArrayList<>();
                List<Integer> daysOfWeek = new ArrayList<>();

                for (Object item : schedulesList) {
                    if (item instanceof Map) {
                        Map<?, ?> scheduleMap = (Map<?, ?>) item;
                        Integer day = Integer.valueOf(scheduleMap.get("day").toString());
                        Integer periodStart = Integer.valueOf(scheduleMap.get("periodStart").toString());
                        Integer periodEnd = Integer.valueOf(scheduleMap.get("periodEnd").toString());

                        daySchedules.add(new DaySchedule(day, periodStart, periodEnd));
                        daysOfWeek.add(day);
                    }
                }

                if (daySchedules.isEmpty()) {
                    return ResponseEntity.badRequest().body("요일이 필요합니다");
                }

                course.setDaySchedules(daySchedules);
                course.setDaysOfWeek(daysOfWeek);
                course.setDayOfWeek(daysOfWeek.get(0));
                // 하위 호환성을 위해 첫 번째 스케줄의 시간 설정
                course.setPeriodStart(daySchedules.get(0).getPeriodStart());
                course.setPeriodEnd(daySchedules.get(0).getPeriodEnd());
            } else {
                // 하위 호환성: 기존 방식 처리
                List<Integer> daysOfWeek = extractDaysOfWeek(requestData);
                if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                    return ResponseEntity.badRequest().body("요일이 필요합니다");
                }
                if (requestData.get("periodStart") == null) {
                    return ResponseEntity.badRequest().body("시작 교시가 필요합니다");
                }
                if (requestData.get("periodEnd") == null) {
                    return ResponseEntity.badRequest().body("종료 교시가 필요합니다");
                }

                course.setDaysOfWeek(daysOfWeek);
                course.setDayOfWeek(daysOfWeek.get(0));
                course.setPeriodStart(Integer.valueOf(requestData.get("periodStart").toString()));
                course.setPeriodEnd(Integer.valueOf(requestData.get("periodEnd").toString()));
            }

            if (requestData.get("credits") != null) {
                course.setCredits(Double.valueOf(requestData.get("credits").toString()));
            } else {
                course.setCredits(3.0);
            }
            if (requestData.get("colorCode") != null) {
                course.setColorCode((String) requestData.get("colorCode"));
            } else {
                course.setColorCode("#3b82f6");
            }
//            if (requestData.get("memo") != null) {
//                course.setMemo((String) requestData.get("memo"));
//            }

            TimetableCourse savedCourse = timetableService.addCourse(timetableId, course);
            return ResponseEntity.ok(savedCourse);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("과목 추가 실패: " + e.getMessage());
        }
    }

    // 과목 수정
    @PutMapping("/course/{courseId}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> requestData,
            HttpSession session) {

        // ログインチェック
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("ログインが必要です");
        }

        try {
            // 権限チェック: 科目の所有者かどうか確認
            if (!timetableService.isCourseOwner(courseId, userId)) {
                return ResponseEntity.status(403).body("この科目を編集する権限がありません");
            }

            // Null 체크
            if (requestData.get("courseName") == null) {
                return ResponseEntity.badRequest().body("과목명이 필요합니다");
            }

            TimetableCourse course = new TimetableCourse();
            course.setCourseName((String) requestData.get("courseName"));
            course.setProfessorName(requestData.get("professorName") != null ?
                (String) requestData.get("professorName") : "");
            course.setClassroom(requestData.get("classroom") != null ?
                (String) requestData.get("classroom") : "");

            // daySchedules 처리 (새로운 구조 우선)
            Object daySchedulesObj = requestData.get("daySchedules");
            if (daySchedulesObj != null && daySchedulesObj instanceof List) {
                List<?> schedulesList = (List<?>) daySchedulesObj;
                List<DaySchedule> daySchedules = new ArrayList<>();
                List<Integer> daysOfWeek = new ArrayList<>();

                for (Object item : schedulesList) {
                    if (item instanceof Map) {
                        Map<?, ?> scheduleMap = (Map<?, ?>) item;
                        Integer day = Integer.valueOf(scheduleMap.get("day").toString());
                        Integer periodStart = Integer.valueOf(scheduleMap.get("periodStart").toString());
                        Integer periodEnd = Integer.valueOf(scheduleMap.get("periodEnd").toString());

                        daySchedules.add(new DaySchedule(day, periodStart, periodEnd));
                        daysOfWeek.add(day);
                    }
                }

                if (daySchedules.isEmpty()) {
                    return ResponseEntity.badRequest().body("요일이 필요합니다");
                }

                course.setDaySchedules(daySchedules);
                course.setDaysOfWeek(daysOfWeek);
                course.setDayOfWeek(daysOfWeek.get(0));
                // 하위 호환성을 위해 첫 번째 스케줄의 시간 설정
                course.setPeriodStart(daySchedules.get(0).getPeriodStart());
                course.setPeriodEnd(daySchedules.get(0).getPeriodEnd());
            } else {
                // 하위 호환성: 기존 방식 처리
                List<Integer> daysOfWeek = extractDaysOfWeek(requestData);
                if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                    return ResponseEntity.badRequest().body("요일이 필요합니다");
                }
                if (requestData.get("periodStart") == null) {
                    return ResponseEntity.badRequest().body("시작 교시가 필요합니다");
                }
                if (requestData.get("periodEnd") == null) {
                    return ResponseEntity.badRequest().body("종료 교시가 필요합니다");
                }

                course.setDaysOfWeek(daysOfWeek);
                course.setDayOfWeek(daysOfWeek.get(0));
                course.setPeriodStart(Integer.valueOf(requestData.get("periodStart").toString()));
                course.setPeriodEnd(Integer.valueOf(requestData.get("periodEnd").toString()));
            }

            if (requestData.get("credits") != null) {
                course.setCredits(Double.valueOf(requestData.get("credits").toString()));
            } else {
                course.setCredits(3.0);
            }
            if (requestData.get("colorCode") != null) {
                course.setColorCode((String) requestData.get("colorCode"));
            } else {
                course.setColorCode("#3b82f6");
            }
//            if (requestData.get("memo") != null) {
//                course.setMemo((String) requestData.get("memo"));
//            }

            TimetableCourse updatedCourse = timetableService.updateCourse(courseId, course);
            return ResponseEntity.ok(updatedCourse);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("과목 수정 실패: " + e.getMessage());
        }
    }

    // 과목 삭제
    @DeleteMapping("/course/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId, HttpSession session) {
        // ログインチェック
        Long userId = getLoggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("ログインが必要です");
        }

        try {
            // 権限チェック: 科目の所有者かどうか確認
            if (!timetableService.isCourseOwner(courseId, userId)) {
                return ResponseEntity.status(403).body("この科目を削除する権限がありません");
            }

            timetableService.deleteCourse(courseId);
            return ResponseEntity.ok("삭제 완료");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("삭제 실패: " + e.getMessage());
        }
    }
}
