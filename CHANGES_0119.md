# 시간표 시스템 변경 사항 (2026-01-19)

## 📝 변경 개요

이번 업데이트에서는 **다중 요일 선택**과 **연속 교시 표시** 기능을 추가했습니다.

### 문제점
1. **연속 교시 문제**: 1-3교시 과목 추가 시 공간만 생기고 시각적으로 연결되지 않음
2. **다중 요일 문제**: 월+수 같이 여러 요일에 걸친 과목을 한 번에 추가할 수 없음

### 해결 방법
1. **CSS Grid `display: contents`** 사용하여 연속 교시를 하나의 셀로 병합
2. **다중 요일 배열 (`daysOfWeek`)** 도입하여 여러 요일 동시 선택

---

## 🗄️ 데이터베이스 변경사항

### 새로 추가된 컬럼

**timetable_courses 테이블**

```sql
-- 다중 요일을 JSON 배열로 저장하는 컬럼 추가
ALTER TABLE timetable_courses
ADD COLUMN days_of_week VARCHAR(100) NULL
COMMENT '다중 요일 (JSON 형식: [1,3,5] = 월수금)';
```

### 데이터 저장 예시

| 필드 | 기존 방식 | 새로운 방식 |
|------|-----------|-------------|
| day_of_week | `1` (월요일만) | `1` (첫 번째 요일, 호환용) |
| days_of_week | (없음) | `"[1,3]"` (월+수, JSON 문자열) |

**예시 데이터:**
```
과목: 데이터구조와알고리즘
day_of_week: 1
days_of_week: "[1,3]"  ← 월요일(1) + 수요일(3)
period_start: 1
period_end: 3
```

### 마이그레이션 스크립트

**backend/migration.sql**

```sql
-- 1. 새 컬럼 추가
ALTER TABLE timetable_courses
ADD COLUMN days_of_week VARCHAR(100) NULL;

-- 2. 기존 데이터를 새 형식으로 변환
-- day_of_week=1 → days_of_week="[1]"
UPDATE timetable_courses
SET days_of_week = CONCAT('[', day_of_week, ']')
WHERE day_of_week IS NOT NULL;

-- 3. 기존 컬럼은 deprecated 표시 (삭제하지 않음)
ALTER TABLE timetable_courses
MODIFY COLUMN day_of_week INT NULL
COMMENT '단일 요일 (deprecated, use days_of_week)';
```

**실행 방법:**
```bash
mysql -u root -p ej2 < backend/migration.sql
```

---

## 💾 백엔드 변경사항

### 1. IntegerListConverter.java (신규 파일)

**위치:** `backend/src/main/java/com/ej2/converter/IntegerListConverter.java`

**역할:** Java의 `List<Integer>`를 데이터베이스의 JSON 문자열로 변환

```java
package com.ej2.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Converter
public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Java → DB: [1, 3] → "[1,3]"
    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("배열을 JSON으로 변환 실패", e);
        }
    }

    // DB → Java: "[1,3]" → [1, 3]
    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Integer>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON을 배열로 변환 실패", e);
        }
    }
}
```

**핵심 개념:**
- `AttributeConverter`: JPA에서 제공하는 타입 변환 인터페이스
- `ObjectMapper`: Jackson 라이브러리의 JSON 변환 클래스
- 자동 변환: 엔티티 저장/조회 시 자동으로 호출됨

### 2. TimetableCourse.java (수정)

**변경 내용:**

```java
@Entity
@Table(name = "timetable_courses")
public class TimetableCourse {

    // ... 기존 필드들 ...

    // ⚠️ 기존 필드 (하위 호환성 유지)
    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    // ✅ 새로 추가된 필드
    @Convert(converter = IntegerListConverter.class)  // ← 변환기 적용
    @Column(name = "days_of_week", length = 100)
    private List<Integer> daysOfWeek;  // [1, 3] = 월+수

    // ... 나머지 필드들 ...

    // Getter/Setter
    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }
}
```

**변경 포인트:**
- `@Convert` 어노테이션으로 변환기 연결
- `dayOfWeek`는 삭제하지 않고 유지 (하위 호환성)

### 3. TimetableController.java (수정)

**새로 추가된 헬퍼 메서드:**

```java
/**
 * 요청 데이터에서 요일 배열 추출
 */
@SuppressWarnings("unchecked")
private List<Integer> extractDaysOfWeek(Map<String, Object> requestData) {
    Object daysOfWeekObj = requestData.get("daysOfWeek");

    // 새 방식: daysOfWeek 배열
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

    // 옛날 방식: dayOfWeek 단일 값 (하위 호환성)
    Object dayOfWeekObj = requestData.get("dayOfWeek");
    if (dayOfWeekObj != null) {
        List<Integer> days = new ArrayList<>();
        days.add(Integer.valueOf(dayOfWeekObj.toString()));
        return days;
    }

    return null;
}
```

**addCourse 메서드 수정:**

```java
@PostMapping("/course")
public ResponseEntity<?> addCourse(@RequestBody Map<String, Object> requestData) {
    try {
        // 요일 추출
        List<Integer> daysOfWeek = extractDaysOfWeek(requestData);

        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return ResponseEntity.badRequest().body("요일이 필요합니다");
        }

        TimetableCourse course = new TimetableCourse();

        // 다중 요일 설정
        course.setDaysOfWeek(daysOfWeek);  // [1, 3]

        // 하위 호환성: 첫 번째 요일을 dayOfWeek에도 저장
        course.setDayOfWeek(daysOfWeek.get(0));  // 1

        // ... 나머지 코드 ...
    }
}
```

**변경 포인트:**
- `extractDaysOfWeek()` 헬퍼 메서드로 요일 데이터 추출 통일
- 신규/구버전 데이터 모두 처리 가능

---

## 🎨 프론트엔드 변경사항

### 1. timetable.ts (수정)

**위치:** `frontend/src/types/timetable.ts`

**변경 내용:**

```typescript
export interface TimetableCourse {
  courseId?: number;
  courseName: string;
  professorName?: string;
  classroom?: string;

  // ⚠️ 기존 필드 (deprecated)
  dayOfWeek?: number;

  // ✅ 새 필드 (다중 요일)
  daysOfWeek: number[];  // [1, 3] = 월+수

  periodStart: number;
  periodEnd: number;
  credits?: number;
  colorCode?: string;
  memo?: string;
}
```

**변경 포인트:**
- `dayOfWeek`를 옵셔널(`?`)로 변경
- `daysOfWeek`를 필수 배열로 추가

### 2. CourseModal.tsx (수정)

**변경 부분 1: 초기 상태**

```typescript
const [formData, setFormData] = useState<TimetableCourse>({
  courseName: '',
  daysOfWeek: defaultDay ? [defaultDay] : [1],  // ← 배열로 초기화
  periodStart: defaultPeriod || 1,
  periodEnd: defaultPeriod || 1,
  credits: 3,
  colorCode: COURSE_COLORS[0],
});
```

**변경 부분 2: 요일 토글 함수 (신규)**

```typescript
/**
 * 요일 버튼 클릭 시 선택/해제 토글
 */
const toggleDay = (dayNumber: number) => {
  setFormData(prev => {
    const currentDays = prev.daysOfWeek || [];

    // 이미 선택됨 → 제거
    // 선택 안됨 → 추가
    const newDays = currentDays.includes(dayNumber)
      ? currentDays.filter(d => d !== dayNumber)
      : [...currentDays, dayNumber].sort();

    return { ...prev, daysOfWeek: newDays };
  });
};
```

**동작 예시:**
```
초기: daysOfWeek = []
월 클릭 → [1]
수 클릭 → [1, 3]
월 다시 클릭 → [3]
```

**변경 부분 3: 요일 선택 UI (신규)**

```typescript
<div className="form-group">
  <label>요일 * (복수 선택 가능)</label>
  <div className="days-selector">
    {DAYS.map((day, index) => {
      const dayNumber = index + 1;
      const isSelected = formData.daysOfWeek?.includes(dayNumber);

      return (
        <button
          key={dayNumber}
          type="button"
          className={`day-button ${isSelected ? 'selected' : ''}`}
          onClick={() => toggleDay(dayNumber)}
        >
          {day}
        </button>
      );
    })}
  </div>
</div>
```

**기존 코드 (삭제됨):**
```typescript
// ❌ 기존: 드롭다운 방식
<select value={formData.dayOfWeek}>
  <option value={1}>월</option>
  <option value={2}>화</option>
  ...
</select>
```

**새 코드:**
```typescript
// ✅ 신규: 버튼 토글 방식
<div className="days-selector">
  <button className="day-button selected">月</button>
  <button className="day-button">火</button>
  <button className="day-button selected">水</button>
  ...
</div>
```

### 3. CourseModal.css (신규 스타일)

**추가된 CSS:**

```css
/* 요일 선택 컨테이너 */
.days-selector {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 요일 버튼 기본 스타일 */
.day-button {
  padding: 10px 16px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  background: white;
  color: #495057;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 50px;
}

/* 호버 효과 */
.day-button:hover {
  border-color: #007bff;
  background: #f8f9fa;
}

/* 선택된 상태 */
.day-button.selected {
  background: #007bff;
  color: white;
  border-color: #007bff;
  transform: scale(1.05);
}
```

### 4. TimetablePage.tsx (수정)

**변경 부분 1: 과목 찾기 로직**

```typescript
/**
 * 특정 요일/교시에 있는 과목 찾기
 */
const getCourseAtSlot = (day: number, period: number): TimetableCourse | undefined => {
  return courses.find(c => {
    // ✅ 하위 호환성: daysOfWeek가 없으면 dayOfWeek 사용
    const days = c.daysOfWeek || (c.dayOfWeek ? [c.dayOfWeek] : []);

    return days.includes(day) &&        // 해당 요일 포함?
           c.periodStart <= period &&   // 시작 교시 이전?
           c.periodEnd >= period;       // 종료 교시 이후?
  });
};
```

**기존 코드:**
```typescript
// ❌ 기존: 단일 요일만 체크
return c.dayOfWeek === day && ...
```

**새 코드:**
```typescript
// ✅ 신규: 배열에서 요일 포함 여부 체크
const days = c.daysOfWeek || ...
return days.includes(day) && ...
```

**변경 부분 2: 그리드 렌더링 (연속 교시 처리)**

```typescript
{DAYS.map((_, dayIndex) => {
  const day = dayIndex + 1;
  const course = getCourseAtSlot(day, period.number);

  // ⭐ 핵심: 연속 교시 처리
  const isStart = course && course.periodStart === period.number;
  const span = course ? (course.periodEnd - course.periodStart + 1) : 1;

  // 시작 교시가 아니면 렌더링 스킵
  if (course && !isStart) {
    return null;
  }

  return (
    <div
      className="course-cell"
      style={{
        backgroundColor: course?.colorCode,
        gridRow: isStart ? `span ${span}` : undefined,  // ← 여러 칸 차지
      }}
    >
      {/* 과목 정보 */}
    </div>
  );
})}
```

**동작 원리:**
```
1교시 (periodStart=1):
  → isStart = true
  → span = 3
  → gridRow: "span 3" (3칸 차지)
  → 렌더링 ✅

2교시 (periodStart=1):
  → isStart = false
  → return null
  → 렌더링 스킵 ❌

3교시 (periodStart=1):
  → isStart = false
  → return null
  → 렌더링 스킵 ❌
```

### 5. TimetablePage.css (수정)

**핵심 변경: `display: contents` 추가**

```css
/* 시간표 그리드 */
.timetable-grid {
  display: grid;
  grid-template-columns: 100px repeat(5, 1fr);
  grid-auto-rows: 80px;
}

/* ✅ 핵심: 계층 평탄화 */
.grid-header {
  display: contents;  /* ← 중요! */
}

.grid-row {
  display: contents;  /* ← 중요! */
}

/* 과목 셀 */
.course-cell {
  border: 1px solid #ddd;
  padding: 8px;
  /* gridRow은 인라인 스타일로 동적 적용 */
}
```

**`display: contents`가 필요한 이유:**

```html
<!-- display: contents 없을 때 -->
<div class="timetable-grid">          ← 그리드 컨테이너
  <div class="grid-row">              ← 중간 계층 (문제!)
    <div class="course-cell"          ← 그리드 아이템
         style="grid-row: span 3">
    </div>
  </div>
</div>
<!-- grid-row: span 3이 작동 안함! -->

<!-- display: contents 있을 때 -->
<div class="timetable-grid">          ← 그리드 컨테이너
  <div class="course-cell"            ← 직접 자식처럼 동작
       style="grid-row: span 3">
  </div>
</div>
<!-- grid-row: span 3이 정상 작동! -->
```

---

## 📊 변경 사항 요약

### 백엔드

| 파일 | 변경 유형 | 내용 |
|------|-----------|------|
| `IntegerListConverter.java` | 신규 | List↔JSON 변환기 |
| `TimetableCourse.java` | 수정 | `daysOfWeek` 필드 추가 |
| `TimetableController.java` | 수정 | `extractDaysOfWeek()` 메서드 추가 |
| `migration.sql` | 신규 | DB 마이그레이션 스크립트 |

### 프론트엔드

| 파일 | 변경 유형 | 내용 |
|------|-----------|------|
| `timetable.ts` | 수정 | `daysOfWeek: number[]` 타입 추가 |
| `CourseModal.tsx` | 수정 | 요일 토글 UI 구현 |
| `CourseModal.css` | 수정 | 요일 버튼 스타일 추가 |
| `TimetablePage.tsx` | 수정 | 다중 요일 + 연속 교시 렌더링 |
| `TimetablePage.css` | 수정 | `display: contents` 추가 |

### 데이터베이스

| 테이블 | 변경 유형 | 내용 |
|--------|-----------|------|
| `timetable_courses` | 컬럼 추가 | `days_of_week VARCHAR(100)` |

---

## 🔄 데이터 흐름

### 과목 추가 시

```
1. 사용자가 월+수 선택, 1-3교시 입력
   ↓
2. React State: daysOfWeek = [1, 3]
   ↓
3. Axios POST /api/timetable/course
   Body: { daysOfWeek: [1, 3], periodStart: 1, periodEnd: 3 }
   ↓
4. Spring Controller: extractDaysOfWeek() → [1, 3]
   ↓
5. TimetableCourse Entity
   setDaysOfWeek([1, 3])
   ↓
6. IntegerListConverter
   [1, 3] → "[1,3]"
   ↓
7. MariaDB 저장
   days_of_week = "[1,3]"
```

### 시간표 조회 시

```
1. Axios GET /api/timetable
   ↓
2. Spring Repository → JPA Query
   ↓
3. MariaDB 조회
   days_of_week = "[1,3]"
   ↓
4. IntegerListConverter
   "[1,3]" → [1, 3]
   ↓
5. TimetableCourse Entity
   daysOfWeek = [1, 3]
   ↓
6. JSON 응답
   { daysOfWeek: [1, 3] }
   ↓
7. React: getCourseAtSlot()
   days.includes(1) → true (월요일에 표시)
   days.includes(3) → true (수요일에 표시)
```

---

## 🧪 테스트 방법

### 1. 다중 요일 테스트

```bash
# API 직접 호출
curl -X POST http://localhost:8080/ej2/api/timetable/course \
  -H "Content-Type: application/json" \
  -d '{
    "timetableId": 1,
    "courseName": "테스트과목",
    "daysOfWeek": [1, 3, 5],
    "periodStart": 1,
    "periodEnd": 2
  }'

# 예상 결과: 월/수/금 1-2교시에 모두 표시
```

### 2. 연속 교시 테스트

```bash
# 1-3교시 과목 추가
curl -X POST http://localhost:8080/ej2/api/timetable/course \
  -H "Content-Type: application/json" \
  -d '{
    "timetableId": 1,
    "courseName": "긴과목",
    "daysOfWeek": [1],
    "periodStart": 1,
    "periodEnd": 3
  }'

# 예상 결과: 1-3교시가 하나의 긴 셀로 표시
```

### 3. 브라우저 테스트

1. http://localhost:3000 접속
2. "과목 추가" 버튼 클릭
3. 월, 수, 금 버튼 클릭 (파란색으로 변함)
4. 1교시-3교시 선택
5. 저장
6. 월/수/금 모두에 1-3교시 셀이 하나로 합쳐져 표시됨

---

## 🐛 알려진 이슈 및 해결

### Issue 1: "요일이 필요합니다" 에러

**원인:** `days_of_week` 컬럼이 DB에 없음

**해결:**
```bash
mysql -u root -p ej2 < backend/migration.sql
```

### Issue 2: 연속 교시가 분리됨

**원인:** CSS `display: contents` 누락

**해결:**
```css
.grid-row {
  display: contents;  /* 추가 */
}
```

### Issue 3: Tomcat 시작 실패

**원인:** Tomcat 10/11 사용 중

**해결:** Tomcat 9로 변경
```bash
brew install tomcat@9
cp target/ej2.war /opt/homebrew/opt/tomcat@9/libexec/webapps/
```

---

## 📚 핵심 개념 정리

### 1. JPA AttributeConverter
- Java 타입 ↔ DB 타입 변환
- `@Convert` 어노테이션으로 적용
- 자동으로 저장/조회 시 변환

### 2. CSS Grid `display: contents`
- 부모 요소를 "투명하게" 만듦
- 자식이 조부모의 직접 자식처럼 동작
- `grid-row: span`이 작동하려면 필수

### 3. React 배열 State 관리
- `includes()`: 배열에 값이 있는지 확인
- `filter()`: 배열에서 값 제거
- `[...array, value]`: 배열에 값 추가

---

**작성일:** 2026-01-19
**버전:** 1.0
