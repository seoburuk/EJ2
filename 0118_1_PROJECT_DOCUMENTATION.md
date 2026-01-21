# EJ2 시간표 관리 시스템 문서

## 프로젝트 개요
EJ2는 대학교 시간표를 관리하는 웹 애플리케이션입니다. 사용자별로 학기별 시간표를 생성하고, 과목을 추가/수정/삭제할 수 있습니다.

---

## 기술 스택

### Backend
- **Framework**: Spring Framework (Legacy Servlet API - javax.servlet)
- **Web Server**: Apache Tomcat 9
- **Database**: MariaDB 10.x
- **ORM**: Hibernate JPA
- **Build Tool**: Maven
- **Java Version**: Java 8+

### Frontend
- **Framework**: React 18.2.0
- **Language**: JavaScript + TypeScript (.tsx)
- **Routing**: React Router DOM 7.12.0
- **HTTP Client**: Axios 1.6.0
- **Build Tool**: React Scripts 5.0.1
- **Additional Libraries**: html2canvas 1.4.1

---

## 시스템 아키텍처

```
┌─────────────────┐         ┌──────────────────┐         ┌──────────────┐
│   React Frontend│  HTTP   │  Spring Backend  │  JDBC   │   MariaDB    │
│   (Port 3000)   ├────────→│   (Port 8080)    ├────────→│  (Port 3306) │
│                 │  /ej2   │                  │         │              │
└─────────────────┘         └──────────────────┘         └──────────────┘
```

### 주요 구성 요소

#### Backend Layer
1. **Controller Layer** (`com.ej2.controller`)
   - `TimetableController`: 시간표 및 과목 CRUD API
   - `UserController`: 사용자 관리 API

2. **Service Layer** (`com.ej2.service`)
   - `TimetableService`: 비즈니스 로직 처리

3. **Repository Layer** (`com.ej2.repository`)
   - `TimetableRepository`: 시간표 데이터 접근
   - `TimetableCourseRepository`: 과목 데이터 접근
   - `UserRepository`: 사용자 데이터 접근

4. **Model Layer** (`com.ej2.model`)
   - `Timetable`: 시간표 엔티티
   - `TimetableCourse`: 과목 엔티티
   - `User`: 사용자 엔티티

#### Frontend Layer
1. **Pages**
   - `TimetablePage.tsx`: 시간표 메인 페이지
   - `UsersPage.js`: 사용자 관리 페이지

2. **Components**
   - `CourseModal.tsx`: 과목 추가/수정 모달
   - Navigation: App.js에 내장된 네비게이션 바

---

## 데이터베이스 스키마

### `users` 테이블
```sql
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### `timetable` 테이블
```sql
CREATE TABLE timetable (
    timetable_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    year INT NOT NULL,
    semester VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

### `timetable_course` 테이블
```sql
CREATE TABLE timetable_course (
    course_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timetable_id BIGINT NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    professor_name VARCHAR(100),
    classroom VARCHAR(100),
    day_of_week INT NOT NULL,
    period_start INT NOT NULL,
    period_end INT NOT NULL,
    credits DOUBLE DEFAULT 3.0,
    color_code VARCHAR(20) DEFAULT '#3b82f6',
    memo TEXT,
    FOREIGN KEY (timetable_id) REFERENCES timetable(timetable_id) ON DELETE CASCADE
);
```

---

## API 명세

### Base URL
```
http://localhost:8080/ej2/api
```

### 시간표 API

#### 1. 시간표 조회
```http
GET /timetable?userId={userId}&year={year}&semester={semester}
```

**Query Parameters:**
- `userId` (optional, default: 1): 사용자 ID
- `year` (required): 연도 (예: 2026)
- `semester` (required): 학기 ("Spring" 또는 "Fall")

**Response:**
```json
{
  "timetable": {
    "timetableId": 1,
    "userId": 1,
    "year": 2026,
    "semester": "Spring",
    "name": null,
    "createdAt": "2024-01-15T10:30:00"
  },
  "courses": [
    {
      "courseId": 1,
      "courseName": "데이터베이스",
      "professorName": "김교수",
      "classroom": "공학관 301",
      "dayOfWeek": 1,
      "periodStart": 1,
      "periodEnd": 2,
      "credits": 3.0,
      "colorCode": "#3b82f6",
      "memo": ""
    }
  ]
}
```

#### 2. 과목 추가
```http
POST /timetable/course
Content-Type: application/json
```

**Request Body:**
```json
{
  "timetableId": 1,
  "courseName": "알고리즘",
  "professorName": "이교수",
  "classroom": "IT관 201",
  "dayOfWeek": 2,
  "periodStart": 3,
  "periodEnd": 4,
  "credits": 3.0,
  "colorCode": "#ef4444",
  "memo": "중간고사 11월 15일"
}
```

**Required Fields:**
- `timetableId`: 시간표 ID
- `courseName`: 과목명
- `dayOfWeek`: 요일 (0: 월요일, 1: 화요일, ..., 4: 금요일)
- `periodStart`: 시작 교시 (1-9)
- `periodEnd`: 종료 교시 (1-9)

**Optional Fields:**
- `professorName`: 교수명 (default: "")
- `classroom`: 강의실 (default: "")
- `credits`: 학점 (default: 3.0)
- `colorCode`: 색상 코드 (default: "#3b82f6")
- `memo`: 메모

**Response:**
```json
{
  "courseId": 2,
  "courseName": "알고리즘",
  "professorName": "이교수",
  "classroom": "IT관 201",
  "dayOfWeek": 2,
  "periodStart": 3,
  "periodEnd": 4,
  "credits": 3.0,
  "colorCode": "#ef4444",
  "memo": "중간고사 11월 15일"
}
```

#### 3. 과목 수정
```http
PUT /timetable/course/{courseId}
Content-Type: application/json
```

**Request Body:** (과목 추가와 동일, timetableId 제외)

#### 4. 과목 삭제
```http
DELETE /timetable/course/{courseId}
```

**Response:**
```json
"삭제 완료"
```

### 사용자 API

#### 1. 사용자 목록 조회
```http
GET /users
```

**Response:**
```json
[
  {
    "userId": 1,
    "userName": "홍길동",
    "email": "hong@example.com",
    "createdAt": "2024-01-10T09:00:00"
  }
]
```

#### 2. 사용자 추가
```http
POST /users
Content-Type: application/json
```

**Request Body:**
```json
{
  "userName": "김철수",
  "email": "kim@example.com"
}
```

#### 3. 사용자 삭제
```http
DELETE /users/{userId}
```

---

## 개발 환경 설정

### 1. 데이터베이스 설정

#### MariaDB 설치 및 설정
```bash
# MariaDB 설치 (Mac)
brew install mariadb

# MariaDB 시작
brew services start mariadb

# root 비밀번호 설정
sudo mysql -u root
ALTER USER 'root'@'localhost' IDENTIFIED BY 'tn1111';
FLUSH PRIVILEGES;
EXIT;

# 데이터베이스 생성
mysql -u root -p
CREATE DATABASE ej2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ej2;

# 테이블 자동 생성 (Hibernate가 자동으로 생성하므로 수동 생성 불필요)
```

### 2. Backend 설정

#### RootConfig.java 데이터베이스 연결 설정
```java
// /Users/yunsu-in/Downloads/EJ2/backend/src/main/java/com/ej2/config/RootConfig.java

dataSource.setUrl("jdbc:mariadb://localhost:3306/ej2");
dataSource.setUsername("root");
dataSource.setPassword("tn1111");
```

#### Backend 빌드 및 배포
```bash
cd /Users/yunsu-in/Downloads/EJ2/backend

# Maven 빌드
mvn clean package

# 결과물 확인
ls target/ej2.war

# Tomcat에 배포
cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/

# Tomcat 시작
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run
```

**중요**: Tomcat 9을 사용해야 합니다. Tomcat 11은 Jakarta EE를 사용하지만, 이 프로젝트는 Java EE (javax.servlet)를 사용합니다.

### 3. Frontend 설정

#### 의존성 설치
```bash
cd /Users/yunsu-in/Downloads/EJ2/frontend

# Node.js 패키지 설치
npm install
```

#### package.json 프록시 설정
```json
{
  "proxy": "http://localhost:8080/ej2"
}
```

이 설정으로 `/api/*` 요청이 `http://localhost:8080/ej2/api/*`로 프록시됩니다.

#### Frontend 시작
```bash
npm start
```

브라우저가 자동으로 `http://localhost:3000`을 엽니다.

---

## 주요 문제 해결 기록

### 1. Tomcat 버전 호환성 문제
**문제:** Tomcat 11에서 `javax.servlet.ServletContextListener` ClassNotFoundException 발생

**원인:** Tomcat 11은 Jakarta EE (jakarta.servlet)를 사용하지만, 프로젝트는 Java EE (javax.servlet)를 사용

**해결:** Tomcat 9로 다운그레이드

```bash
brew install tomcat@9
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run
```

### 2. TypeScript 모듈 해결 오류
**문제:** `Module not found: Error: Can't resolve './pages/Timetable/TimetablePage'`

**원인:** TypeScript 파일 import 시 .tsx 확장자 누락

**해결:**
```javascript
// App.js
import TimetablePage from './pages/Timetable/TimetablePage.tsx';  // .tsx 명시
```

### 3. NullPointerException 발생
**문제:** `Cannot invoke Object.toString() because the return value of java.util.Map.get(Object) is null`

**원인:** 요청 데이터에서 값을 가져올 때 null 체크 누락

**해결:** TimetableController에 모든 필수 필드에 대한 null 체크 추가

```java
// TimetableController.java
if (requestData.get("timetableId") == null) {
    return ResponseEntity.badRequest().body("시간표 ID가 필요합니다");
}
if (requestData.get("courseName") == null) {
    return ResponseEntity.badRequest().body("과목명이 필요합니다");
}
// ... 모든 필수 필드에 대해 체크
```

### 4. API 404 에러
**문제:** `/api/timetable` 요청이 404 반환

**원인:** 프록시 설정에 `/ej2` 컨텍스트 경로 누락

**해결:**
```json
// package.json
{
  "proxy": "http://localhost:8080/ej2"  // /ej2 추가
}
```

### 5. 학기(semester) 대소문자 불일치
**문제:** 프론트엔드에서 "spring"을 보내지만 데이터베이스에는 "Spring"으로 저장됨

**원인:** 대소문자 구분하는 문자열 비교

**해결:** 프론트엔드에서 대문자로 시작하도록 변경

```typescript
// TimetablePage.tsx
const [selectedSemester, setSelectedSemester] = useState('Spring');

<select value={selectedSemester} onChange={(e) => setSelectedSemester(e.target.value)}>
  <option value="Spring">봄학기</option>
  <option value="Fall">가을학기</option>
</select>
```

### 6. 시간표 로딩 전 과목 추가 시도
**문제:** "시간표를 먼저 불러와주세요" 경고 발생

**원인:** `timetable` 상태가 null인 상태에서 과목 추가 시도

**해결:** 과목 저장 전 null 체크 추가

```typescript
// TimetablePage.tsx
const handleSaveCourse = async (courseData) => {
  if (!timetable || !timetable.timetableId) {
    alert('시간표를 먼저 불러와주세요');
    return;
  }
  // ... 저장 로직
};
```

---

## 프로젝트 구조

```
EJ2/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── ej2/
│   │       │           ├── config/
│   │       │           │   ├── RootConfig.java        # 데이터베이스 설정
│   │       │           │   └── WebConfig.java         # Spring MVC 설정
│   │       │           ├── controller/
│   │       │           │   ├── TimetableController.java
│   │       │           │   └── UserController.java
│   │       │           ├── service/
│   │       │           │   └── TimetableService.java
│   │       │           ├── repository/
│   │       │           │   ├── TimetableRepository.java
│   │       │           │   ├── TimetableCourseRepository.java
│   │       │           │   └── UserRepository.java
│   │       │           └── model/
│   │       │               ├── Timetable.java
│   │       │               ├── TimetableCourse.java
│   │       │               └── User.java
│   │       └── webapp/
│   │           └── WEB-INF/
│   │               └── web.xml
│   ├── target/
│   │   └── ej2.war                                     # 빌드 결과물
│   └── pom.xml                                         # Maven 설정
│
└── frontend/
    ├── public/
    │   └── index.html
    ├── src/
    │   ├── pages/
    │   │   ├── Timetable/
    │   │   │   ├── TimetablePage.tsx                   # 시간표 메인 페이지
    │   │   │   ├── TimetablePage.css
    │   │   │   ├── CourseModal.tsx                     # 과목 추가/수정 모달
    │   │   │   └── CourseModal.css
    │   │   └── Users/
    │   │       ├── UsersPage.js                        # 사용자 관리 페이지
    │   │       └── UsersPage.css
    │   ├── App.js                                       # 메인 앱 (라우팅, 네비게이션)
    │   ├── App.css
    │   └── index.js
    ├── package.json                                     # npm 설정 및 프록시
    └── package-lock.json
```

---

## 주요 기능 설명

### 1. 시간표 표시
- 5일(월-금) x 9교시 그리드 형태로 시간표 표시
- 각 셀을 클릭하여 과목 추가 가능
- 과목은 색상으로 구분되어 표시

### 2. 과목 관리
- **추가**: 그리드 셀 클릭 → 모달에서 과목 정보 입력 → 저장
- **수정**: 등록된 과목 클릭 → 모달에서 정보 수정 → 저장
- **삭제**: 과목 수정 모달에서 삭제 버튼 클릭

### 3. 사용자별 시간표
- 사용자 선택 드롭다운으로 다른 사용자의 시간표 확인 가능
- 각 사용자는 학기별로 독립적인 시간표 소유

### 4. 학기별 시간표
- 연도와 학기(봄/가을) 선택
- 선택한 조건에 맞는 시간표 자동 로드
- 존재하지 않는 시간표는 자동 생성

### 5. 사용자 관리
- 사용자 추가 (이름, 이메일)
- 사용자 삭제
- 사용자 목록 조회

---

## 코드 주요 로직

### Backend - TimetableService.java

#### 시간표와 과목 조회
```java
public Map<String, Object> getTimetableWithCourses(Long userId, Integer year, String semester) {
    // 1. 시간표 조회 (없으면 생성)
    Timetable timetable = timetableRepository
        .findByUserIdAndYearAndSemester(userId, year, semester)
        .orElseGet(() -> {
            Timetable newTimetable = new Timetable();
            newTimetable.setUserId(userId);
            newTimetable.setYear(year);
            newTimetable.setSemester(semester);
            return timetableRepository.save(newTimetable);
        });

    // 2. 해당 시간표의 과목 목록 조회
    List<TimetableCourse> courses = timetableCourseRepository
        .findByTimetableId(timetable.getTimetableId());

    // 3. 결과 반환
    Map<String, Object> result = new HashMap<>();
    result.put("timetable", timetable);
    result.put("courses", courses);
    return result;
}
```

#### 시간표 충돌 검사
```java
private void checkTimeConflict(Long timetableId, TimetableCourse newCourse, Long excludeCourseId) {
    List<TimetableCourse> existingCourses = timetableCourseRepository.findByTimetableId(timetableId);

    for (TimetableCourse existing : existingCourses) {
        // 자기 자신은 제외
        if (excludeCourseId != null && existing.getCourseId().equals(excludeCourseId)) {
            continue;
        }

        // 같은 요일인지 확인
        if (!existing.getDayOfWeek().equals(newCourse.getDayOfWeek())) {
            continue;
        }

        // 시간 겹침 확인
        if (!(newCourse.getPeriodEnd() < existing.getPeriodStart() ||
              newCourse.getPeriodStart() > existing.getPeriodEnd())) {
            throw new RuntimeException("시간표가 겹칩니다: " + existing.getCourseName());
        }
    }
}
```

### Frontend - TimetablePage.tsx

#### 시간표 로드
```typescript
const loadTimetable = async () => {
  try {
    const response = await axios.get('/api/timetable', {
      params: {
        semester: selectedSemester,  // "Spring" 또는 "Fall"
        year: selectedYear,          // 예: 2026
        userId: selectedUserId       // 예: 1
      }
    });

    setTimetable(response.data.timetable);
    setCourses(response.data.courses);
  } catch (error) {
    console.error('시간표 로딩 실패', error);
  }
};
```

#### 과목 저장
```typescript
const handleSaveCourse = async (courseData) => {
  // 1. 시간표 로드 확인
  if (!timetable || !timetable.timetableId) {
    alert('시간표를 먼저 불러와주세요');
    return;
  }

  try {
    if (editingCourse) {
      // 2-1. 수정 모드
      await axios.put(`/api/timetable/course/${editingCourse.courseId}`, courseData);
    } else {
      // 2-2. 추가 모드
      await axios.post('/api/timetable/course', {
        ...courseData,
        timetableId: timetable.timetableId
      });
    }

    // 3. 시간표 새로고침
    loadTimetable();
    setIsModalOpen(false);
  } catch (error) {
    alert('저장 실패: ' + error.response.data);
  }
};
```

#### 그리드 렌더링
```typescript
const renderCell = (day: number, period: number) => {
  // 해당 셀에 과목이 있는지 확인
  const course = courses.find(
    c => c.dayOfWeek === day &&
         period >= c.periodStart &&
         period <= c.periodEnd
  );

  if (!course) {
    // 빈 셀 - 클릭 시 과목 추가
    return (
      <div
        className="time-cell empty"
        onClick={() => handleCellClick(day, period)}
      />
    );
  }

  // 과목이 있는 셀
  if (period === course.periodStart) {
    // 시작 교시에만 과목 정보 표시
    const height = (course.periodEnd - course.periodStart + 1) * 60;
    return (
      <div
        className="time-cell course"
        style={{
          height: `${height}px`,
          backgroundColor: course.colorCode
        }}
        onClick={() => handleCourseClick(course)}
      >
        <div className="course-name">{course.courseName}</div>
        <div className="course-info">
          {course.professorName}
          {course.classroom && ` • ${course.classroom}`}
        </div>
      </div>
    );
  }

  // 중간/끝 교시는 렌더링 스킵
  return null;
};
```

---

## 테스트 방법

### 1. API 테스트

#### 시간표 조회
```bash
curl "http://localhost:8080/ej2/api/timetable?userId=1&year=2026&semester=Spring"
```

#### 과목 추가
```bash
curl -X POST http://localhost:8080/ej2/api/timetable/course \
  -H "Content-Type: application/json" \
  -d '{
    "timetableId": 1,
    "courseName": "데이터베이스",
    "professorName": "김교수",
    "classroom": "공학관 301",
    "dayOfWeek": 0,
    "periodStart": 1,
    "periodEnd": 2,
    "credits": 3.0,
    "colorCode": "#3b82f6"
  }'
```

#### 사용자 목록 조회
```bash
curl "http://localhost:8080/ej2/api/users"
```

### 2. UI 테스트

1. **사용자 추가**
   - 사용자 페이지 이동
   - 이름과 이메일 입력 후 추가
   - 사용자 목록에 표시 확인

2. **시간표 조회**
   - 시간표 페이지에서 사용자, 연도, 학기 선택
   - "시간표 불러오기" 클릭
   - 빈 시간표 그리드 표시 확인

3. **과목 추가**
   - 그리드의 빈 셀 클릭
   - 모달에서 과목 정보 입력
   - 저장 후 그리드에 과목 표시 확인

4. **과목 수정**
   - 등록된 과목 클릭
   - 모달에서 정보 수정
   - 저장 후 변경사항 반영 확인

5. **과목 삭제**
   - 과목 클릭 → 삭제 버튼 클릭
   - 그리드에서 과목 제거 확인

6. **시간 충돌 확인**
   - 이미 과목이 있는 시간대에 다른 과목 추가 시도
   - "시간표가 겹칩니다" 오류 메시지 확인

---

## 배포 가이드

### 개발 환경
1. MariaDB 시작: `brew services start mariadb`
2. Backend 빌드 및 배포: `cd backend && mvn clean package && cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/`
3. Tomcat 시작: `/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run`
4. Frontend 시작: `cd frontend && npm start`

### 프로덕션 환경 (추후 구현)
1. **Backend**: WAR 파일을 프로덕션 Tomcat에 배포
2. **Frontend**: `npm run build`로 정적 파일 생성 후 웹 서버(Nginx, Apache) 배포
3. **환경 변수 설정**: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`

---

## 향후 개선 사항

### 기능 추가
- [ ] 로그인/인증 시스템
- [ ] 시간표 공유 기능
- [ ] 시간표 PDF/이미지 내보내기
- [ ] 과목 검색 및 필터링
- [ ] 모바일 반응형 디자인 개선
- [ ] 다크 모드 지원

### 성능 최적화
- [ ] React 컴포넌트 메모이제이션 (React.memo, useMemo)
- [ ] 불필요한 API 호출 최소화
- [ ] 데이터베이스 인덱스 최적화
- [ ] 프론트엔드 번들 크기 최적화

### 코드 품질
- [ ] TypeScript 전환 (현재 일부만 .tsx)
- [ ] 단위 테스트 작성 (Jest, JUnit)
- [ ] ESLint/Prettier 설정
- [ ] API 에러 핸들링 개선

---

## 라이선스
MIT License

## 작성자
yunsu-in

## 작성일
2026-01-18

## 버전
1.0.0
