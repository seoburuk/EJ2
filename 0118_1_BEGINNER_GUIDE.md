# EJ2 시간표 시스템 - 초보자를 위한 완벽 가이드

## 목차
1. [시작하기 전에](#시작하기-전에)
2. [프로젝트란 무엇인가?](#프로젝트란-무엇인가)
3. [기술 스택 쉽게 이해하기](#기술-스택-쉽게-이해하기)
4. [처음부터 시작하는 설치 가이드](#처음부터-시작하는-설치-가이드)
5. [코드 구조 이해하기](#코드-구조-이해하기)
6. [주요 개념 설명](#주요-개념-설명)
7. [자주 하는 실수와 해결법](#자주-하는-실수와-해결법)
8. [코드 수정하는 방법](#코드-수정하는-방법)
9. [문제 해결 체크리스트](#문제-해결-체크리스트)
10. [Bash 명령어 완전 가이드](#bash-명령어-완전-가이드)

---

## 시작하기 전에

### 이 프로젝트는 무엇인가요?
EJ2는 대학생들이 수강하는 과목들을 시간표 형태로 관리할 수 있는 웹 애플리케이션입니다.
마치 네이버 캘린더나 구글 캘린더처럼 과목을 추가하고, 수정하고, 삭제할 수 있습니다.

### 누구를 위한 프로젝트인가요?
- 프로그래밍을 배우는 학생
- 웹 개발에 입문하고 싶은 분
- Spring과 React를 함께 사용하는 풀스택 프로젝트를 경험하고 싶은 분

### 배울 수 있는 것들
- **Backend**: Java, Spring Framework, 데이터베이스
- **Frontend**: React, TypeScript, HTTP 통신
- **전체**: 클라이언트-서버 구조, REST API, 데이터베이스 설계

---

## 프로젝트란 무엇인가?

### 웹 애플리케이션의 구조

웹 애플리케이션은 크게 3가지로 나뉩니다:

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Frontend   │         │   Backend    │         │   Database   │
│              │         │              │         │              │
│ 사용자가 보는 │  ────>  │ 로직을 처리   │  ────>  │ 데이터를 저장 │
│ 화면         │  <────  │ 하는 서버     │  <────  │ 하는 곳      │
│              │         │              │         │              │
└──────────────┘         └──────────────┘         └──────────────┘
    React                    Spring                  MariaDB
```

### 동작 과정 예시

**"데이터베이스 과목을 월요일 1-2교시에 추가한다"**를 예로 들어보겠습니다:

1. **Frontend (화면)**:
   - 사용자가 월요일 1교시 칸을 클릭
   - 모달(팝업)이 뜨고 "과목명: 데이터베이스"를 입력
   - "저장" 버튼 클릭

2. **Frontend (JavaScript 코드)**:
   ```javascript
   // 사용자가 입력한 정보를 서버로 전송
   axios.post('/api/timetable/course', {
     courseName: '데이터베이스',
     dayOfWeek: 0,  // 0 = 월요일
     periodStart: 1,
     periodEnd: 2
   })
   ```

3. **Backend (Spring Server)**:
   ```java
   // 요청을 받아서 데이터베이스에 저장
   @PostMapping("/course")
   public ResponseEntity<?> addCourse(@RequestBody Map<String, Object> data) {
       TimetableCourse course = new TimetableCourse();
       course.setCourseName(data.get("courseName"));
       // ... 데이터베이스에 저장
       return ResponseEntity.ok(savedCourse);
   }
   ```

4. **Database (MariaDB)**:
   ```
   timetable_course 테이블에 새 행 추가:
   | course_id | course_name  | day_of_week | period_start | period_end |
   |-----------|-------------|-------------|--------------|------------|
   | 1         | 데이터베이스  | 0           | 1            | 2          |
   ```

5. **Backend → Frontend**:
   - "저장 완료!" 응답을 보냄

6. **Frontend (화면 업데이트)**:
   - 시간표를 다시 불러와서 화면에 "데이터베이스" 과목이 표시됨

---

## 기술 스택 쉽게 이해하기

### Backend (서버 쪽)

#### 1. Java
- **역할**: 프로그래밍 언어
- **비유**: 건축에서 사용하는 도구 (망치, 톱 등)
- **예시**:
  ```java
  String courseName = "데이터베이스";  // 변수 선언
  if (courseName != null) {            // 조건문
      System.out.println(courseName);  // 출력
  }
  ```

#### 2. Spring Framework
- **역할**: Java로 웹 서버를 쉽게 만들 수 있게 해주는 도구 모음
- **비유**: 건축에서 미리 만들어진 자재 (벽돌, 시멘트 등)
- **주요 기능**:
  - `@RestController`: "이 클래스는 웹 요청을 받는 곳이다"라고 표시
  - `@GetMapping`: "이 메서드는 GET 요청을 처리한다"
  - `@PostMapping`: "이 메서드는 POST 요청을 처리한다"
  - `@Autowired`: "이 객체를 자동으로 가져와라"

#### 3. Hibernate JPA
- **역할**: Java 객체와 데이터베이스를 자동으로 연결
- **비유**: 통역사 (Java 언어 ↔ SQL 언어)
- **예시**:
  ```java
  // Java 코드로 작성하면
  timetableRepository.save(course);

  // Hibernate가 자동으로 SQL로 변환
  INSERT INTO timetable_course (course_name, day_of_week, ...)
  VALUES ('데이터베이스', 0, ...);
  ```

#### 4. Tomcat
- **역할**: Java 웹 애플리케이션을 실행하는 서버
- **비유**: 식당 주방 (요리를 만드는 곳)
- **포트**: 8080 (http://localhost:8080)

#### 5. MariaDB
- **역할**: 데이터를 저장하는 데이터베이스
- **비유**: 도서관 (책을 보관하고 찾는 곳)
- **포트**: 3306

### Frontend (화면 쪽)

#### 1. React
- **역할**: 사용자 인터페이스를 만드는 JavaScript 라이브러리
- **비유**: 레고 블록 (작은 조각들을 조립해서 큰 화면을 만듦)
- **특징**:
  - **컴포넌트**: 화면의 작은 부분들 (예: 버튼, 입력창, 시간표 그리드)
  - **State**: 변화하는 데이터 (예: 선택한 학기, 과목 목록)

#### 2. TypeScript (.tsx 파일)
- **역할**: JavaScript에 타입을 추가한 언어
- **비유**: 안전장치가 있는 JavaScript (실수를 미리 방지)
- **예시**:
  ```typescript
  // TypeScript - 타입을 명시
  const courseName: string = "데이터베이스";
  const credits: number = 3.0;

  // JavaScript - 타입이 없음
  const courseName = "데이터베이스";
  const credits = 3.0;
  ```

#### 3. Axios
- **역할**: 서버와 HTTP 통신을 쉽게 해주는 라이브러리
- **비유**: 우체부 (메시지를 서버에 전달하고 응답을 받아옴)
- **예시**:
  ```javascript
  // 서버에 데이터 요청
  axios.get('/api/timetable')  // GET 요청

  // 서버에 데이터 전송
  axios.post('/api/timetable/course', { courseName: '데이터베이스' })  // POST 요청
  ```

#### 4. React Router
- **역할**: 페이지 이동 관리
- **비유**: 지도 네비게이션 (어느 페이지로 갈지 안내)
- **예시**:
  ```javascript
  <Route path="/" element={<TimetablePage />} />        // 메인 페이지
  <Route path="/users" element={<UsersPage />} />       // 사용자 페이지
  ```

---

## 처음부터 시작하는 설치 가이드

### 1단계: 필수 프로그램 설치 (Mac 기준)

#### Homebrew 설치
```bash
# Homebrew는 Mac에서 프로그램을 쉽게 설치하게 해주는 도구입니다
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

#### Java 설치
```bash
# Java 11 설치 (Spring 프로젝트 실행에 필요)
brew install openjdk@11

# Java 버전 확인
java -version
# 출력 예시: openjdk version "11.0.12"
```

#### Maven 설치
```bash
# Maven은 Java 프로젝트 빌드 도구
brew install maven

# Maven 버전 확인
mvn -version
# 출력 예시: Apache Maven 3.8.6
```

#### Node.js 설치
```bash
# Node.js와 npm (React 프로젝트 실행에 필요)
brew install node

# Node.js 버전 확인
node -v
# 출력 예시: v18.17.0

# npm 버전 확인
npm -v
# 출력 예시: 9.6.7
```

#### MariaDB 설치
```bash
# MariaDB 데이터베이스 설치
brew install mariadb

# MariaDB 버전 확인
mysql --version
# 출력 예시: mysql Ver 15.1 Distrib 10.11.4-MariaDB
```

#### Tomcat 9 설치
```bash
# Tomcat 9 설치
brew install tomcat@9

# Tomcat 버전 확인
/usr/local/Cellar/tomcat@9/9.0.98/bin/version.sh
# 출력 예시: Apache Tomcat/9.0.98
```

---

### 2단계: 데이터베이스 설정

#### MariaDB 시작
```bash
# MariaDB 서비스 시작
brew services start mariadb

# MariaDB 상태 확인
brew services list
# mariadb가 started 상태여야 함
```

#### 데이터베이스 비밀번호 설정
```bash
# MariaDB에 root 사용자로 접속 (비밀번호 없음)
sudo mysql -u root

# 접속되면 아래 명령어 입력:
```

```sql
-- root 비밀번호를 'tn1111'로 설정
ALTER USER 'root'@'localhost' IDENTIFIED BY 'tn1111';

-- 변경사항 적용
FLUSH PRIVILEGES;

-- MariaDB 종료
EXIT;
```

#### 데이터베이스 생성
```bash
# 이제 비밀번호로 접속
mysql -u root -p
# 비밀번호 입력: tn1111
```

```sql
-- 'ej2' 데이터베이스 생성
CREATE DATABASE ej2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 생성 확인
SHOW DATABASES;
-- 'ej2'가 목록에 있어야 함

-- ej2 데이터베이스 사용
USE ej2;

-- 테이블 확인 (처음엔 비어있음)
SHOW TABLES;

-- 종료
EXIT;
```

**💡 팁**: Hibernate가 자동으로 테이블을 생성하므로 수동으로 CREATE TABLE 할 필요 없습니다!

---

### 3단계: Backend 설정 및 실행

#### 프로젝트 다운로드 (이미 있다면 스킵)
```bash
cd /Users/yunsu-in/Downloads/EJ2/backend
```

#### RootConfig.java 데이터베이스 설정 확인
```bash
# 파일 열기
nano src/main/java/com/ej2/config/RootConfig.java
```

아래 부분이 올바른지 확인:
```java
dataSource.setUrl("jdbc:mariadb://localhost:3306/ej2");
dataSource.setUsername("root");
dataSource.setPassword("tn1111");
```

#### Backend 빌드
```bash
# backend 디렉토리에서 실행
cd /Users/yunsu-in/Downloads/EJ2/backend

# Maven으로 프로젝트 빌드
mvn clean package

# 빌드 완료 메시지 확인
# [INFO] BUILD SUCCESS
# [INFO] ------------------------------------------------------------------------

# 빌드 결과물 확인
ls -lh target/ej2.war
# -rw-r--r--  1 user  staff   15M  1 18 14:30 target/ej2.war
```

**🔍 무슨 일이 일어나는가?**
- `mvn clean`: 이전 빌드 결과물 삭제
- `mvn package`: Java 코드를 컴파일하고 WAR 파일로 패키징
- WAR 파일: Tomcat에 배포할 수 있는 압축 파일 (ZIP과 비슷)

#### Tomcat에 배포
```bash
# WAR 파일을 Tomcat의 webapps 디렉토리에 복사
cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/

# 복사 확인
ls -lh /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/
# ej2.war 파일이 있어야 함
```

#### Tomcat 시작
```bash
# Tomcat 시작 (로그를 화면에 출력)
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run
```

**✅ 성공 확인:**
- 터미널에 다음 메시지가 보이면 성공:
  ```
  INFO: Server startup in [3456] milliseconds
  ```
- 웹 브라우저에서 `http://localhost:8080` 접속 → Tomcat 페이지가 보임
- `http://localhost:8080/ej2/api/users` 접속 → `[]` 또는 사용자 목록이 보임

**❌ 실패 시:**
- `Address already in use` 에러: 이미 8080 포트를 사용 중
  ```bash
  # 8080 포트 사용 프로세스 확인
  lsof -i :8080
  # 프로세스 종료
  kill -9 [PID]
  ```

---

### 4단계: Frontend 설정 및 실행

#### 프로젝트 디렉토리로 이동
```bash
# 새 터미널 창 열기 (Tomcat은 계속 실행 중이어야 함!)
cd /Users/yunsu-in/Downloads/EJ2/frontend
```

#### npm 패키지 설치
```bash
# package.json에 명시된 모든 패키지 설치
npm install

# 설치 완료 확인
ls -lh node_modules/
# react, axios, react-router-dom 등의 디렉토리가 있어야 함
```

**🔍 무슨 일이 일어나는가?**
- `npm install`은 `node_modules/` 디렉토리에 필요한 라이브러리들을 다운로드합니다
- `package.json`을 보면 어떤 라이브러리가 설치되는지 확인할 수 있습니다

#### package.json 프록시 설정 확인
```bash
# 파일 확인
cat package.json
```

아래 설정이 있는지 확인:
```json
{
  "proxy": "http://localhost:8080/ej2"
}
```

**💡 프록시란?**
- Frontend(3000번 포트)에서 Backend(8080번 포트)로 요청을 보낼 때 사용
- `/api/timetable` 요청 → `http://localhost:8080/ej2/api/timetable`로 자동 변환

#### Frontend 시작
```bash
npm start
```

**✅ 성공 확인:**
- 터미널에 다음 메시지가 보이면 성공:
  ```
  Compiled successfully!

  You can now view ej2-frontend in the browser.

    Local:            http://localhost:3000
  ```
- 브라우저가 자동으로 `http://localhost:3000` 페이지를 엽니다
- "EJ2" 로고와 "시간표", "사용자" 메뉴가 보입니다

---

## 코드 구조 이해하기

### Backend 파일 구조

```
backend/src/main/java/com/ej2/
├── config/
│   ├── RootConfig.java          # 데이터베이스 설정
│   └── WebConfig.java            # Spring MVC 설정
├── controller/
│   ├── TimetableController.java  # API 엔드포인트 (요청 받는 곳)
│   └── UserController.java
├── service/
│   └── TimetableService.java     # 비즈니스 로직 (실제 처리)
├── repository/
│   ├── TimetableRepository.java  # 데이터베이스 접근
│   ├── TimetableCourseRepository.java
│   └── UserRepository.java
└── model/
    ├── Timetable.java            # 데이터 모델 (테이블 구조)
    ├── TimetableCourse.java
    └── User.java
```

#### 각 레이어의 역할

**1. Controller (컨트롤러)**
- 역할: HTTP 요청을 받고 응답을 반환
- 비유: 식당의 웨이터 (주문을 받고 음식을 서빙)
- 예시:
  ```java
  @RestController
  @RequestMapping("/api/timetable")
  public class TimetableController {

      @GetMapping  // GET 요청 처리
      public ResponseEntity<?> getTimetable(
          @RequestParam Long userId,
          @RequestParam Integer year,
          @RequestParam String semester
      ) {
          // Service에 처리 위임
          Map<String, Object> result = timetableService.getTimetableWithCourses(userId, year, semester);
          return ResponseEntity.ok(result);
      }
  }
  ```

**2. Service (서비스)**
- 역할: 실제 비즈니스 로직 처리
- 비유: 식당의 요리사 (재료를 요리해서 음식을 만듦)
- 예시:
  ```java
  @Service
  public class TimetableService {

      public Map<String, Object> getTimetableWithCourses(Long userId, Integer year, String semester) {
          // 1. 시간표 조회 (없으면 새로 생성)
          Timetable timetable = timetableRepository
              .findByUserIdAndYearAndSemester(userId, year, semester)
              .orElseGet(() -> {
                  Timetable newTimetable = new Timetable();
                  newTimetable.setUserId(userId);
                  newTimetable.setYear(year);
                  newTimetable.setSemester(semester);
                  return timetableRepository.save(newTimetable);
              });

          // 2. 과목 목록 조회
          List<TimetableCourse> courses = timetableCourseRepository.findByTimetableId(timetable.getTimetableId());

          // 3. 결과 반환
          Map<String, Object> result = new HashMap<>();
          result.put("timetable", timetable);
          result.put("courses", courses);
          return result;
      }
  }
  ```

**3. Repository (리포지토리)**
- 역할: 데이터베이스와 직접 통신
- 비유: 식당의 창고 관리자 (재료를 보관하고 꺼내옴)
- 예시:
  ```java
  public interface TimetableRepository extends JpaRepository<Timetable, Long> {

      // 메서드 이름만으로 쿼리 자동 생성!
      Optional<Timetable> findByUserIdAndYearAndSemester(Long userId, Integer year, String semester);

      // 실제 실행되는 SQL:
      // SELECT * FROM timetable WHERE user_id = ? AND year = ? AND semester = ?
  }
  ```

**4. Model (모델)**
- 역할: 데이터 구조 정의 (테이블과 매핑)
- 비유: 식당의 메뉴판 (어떤 재료가 들어가는지 명시)
- 예시:
  ```java
  @Entity  // 이 클래스는 데이터베이스 테이블이다
  @Table(name = "timetable_course")
  public class TimetableCourse {

      @Id  // 기본 키
      @GeneratedValue(strategy = GenerationType.IDENTITY)  // 자동 증가
      private Long courseId;

      @Column(nullable = false)  // NOT NULL 제약
      private String courseName;

      private String professorName;
      private String classroom;

      @Column(nullable = false)
      private Integer dayOfWeek;  // 0=월, 1=화, 2=수, 3=목, 4=금

      @Column(nullable = false)
      private Integer periodStart;  // 시작 교시 (1-9)

      @Column(nullable = false)
      private Integer periodEnd;    // 종료 교시 (1-9)

      private Double credits;       // 학점
      private String colorCode;     // 색상 코드
      private String memo;          // 메모

      // Getter, Setter 생략...
  }
  ```

---

### Frontend 파일 구조

```
frontend/src/
├── App.js                        # 메인 앱 (라우팅, 네비게이션)
├── App.css
├── index.js                      # 진입점
├── pages/
│   ├── Timetable/
│   │   ├── TimetablePage.tsx     # 시간표 페이지
│   │   ├── TimetablePage.css
│   │   ├── CourseModal.tsx       # 과목 추가/수정 모달
│   │   └── CourseModal.css
│   └── Users/
│       ├── UsersPage.js          # 사용자 관리 페이지
│       └── UsersPage.css
└── package.json                  # npm 설정
```

#### React의 핵심 개념

**1. 컴포넌트 (Component)**
- 역할: 화면의 작은 부분 (재사용 가능)
- 비유: 레고 블록
- 예시:
  ```typescript
  function TimetablePage() {
      // 이 함수가 하나의 컴포넌트
      return (
          <div>
              <h1>시간표</h1>
              <button>과목 추가</button>
          </div>
      );
  }
  ```

**2. State (상태)**
- 역할: 변화하는 데이터
- 비유: 메모장 (데이터를 적어두고 변경)
- 예시:
  ```typescript
  const [courses, setCourses] = useState([]);  // 과목 목록 상태

  // 과목 목록 변경
  setCourses([...courses, newCourse]);  // 새 과목 추가
  ```

**3. useEffect (효과)**
- 역할: 컴포넌트가 화면에 나타날 때 실행할 작업
- 비유: 자동 실행 (페이지 로드 시 자동으로 데이터 불러오기)
- 예시:
  ```typescript
  useEffect(() => {
      // 컴포넌트가 처음 렌더링될 때 실행
      loadTimetable();  // 시간표 데이터 불러오기
  }, []);  // 빈 배열 = 한 번만 실행

  useEffect(() => {
      // selectedSemester가 변경될 때마다 실행
      loadTimetable();
  }, [selectedSemester]);  // selectedSemester가 변경되면 실행
  ```

**4. Props (속성)**
- 역할: 부모 컴포넌트에서 자식 컴포넌트로 데이터 전달
- 비유: 함수의 매개변수
- 예시:
  ```typescript
  // 부모 컴포넌트
  <CourseModal
      isOpen={isModalOpen}
      onClose={() => setIsModalOpen(false)}
      onSave={handleSaveCourse}
  />

  // 자식 컴포넌트
  function CourseModal({ isOpen, onClose, onSave }) {
      // isOpen, onClose, onSave를 사용
  }
  ```

---

## 주요 개념 설명

### REST API란?

REST API는 클라이언트와 서버가 HTTP를 통해 데이터를 주고받는 규칙입니다.

#### HTTP 메서드

| 메서드 | 용도 | 예시 |
|--------|------|------|
| GET | 데이터 조회 (읽기) | `GET /api/timetable?userId=1&year=2026&semester=Spring` |
| POST | 데이터 생성 (쓰기) | `POST /api/timetable/course` (Body에 과목 정보) |
| PUT | 데이터 수정 | `PUT /api/timetable/course/1` (Body에 수정할 정보) |
| DELETE | 데이터 삭제 | `DELETE /api/timetable/course/1` |

#### 요청과 응답 예시

**1. GET 요청 - 시간표 조회**

요청:
```http
GET /api/timetable?userId=1&year=2026&semester=Spring HTTP/1.1
Host: localhost:8080
```

응답:
```json
{
  "timetable": {
    "timetableId": 1,
    "userId": 1,
    "year": 2026,
    "semester": "Spring"
  },
  "courses": [
    {
      "courseId": 1,
      "courseName": "데이터베이스",
      "dayOfWeek": 0,
      "periodStart": 1,
      "periodEnd": 2
    }
  ]
}
```

**2. POST 요청 - 과목 추가**

요청:
```http
POST /api/timetable/course HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "timetableId": 1,
  "courseName": "알고리즘",
  "dayOfWeek": 1,
  "periodStart": 3,
  "periodEnd": 4
}
```

응답:
```json
{
  "courseId": 2,
  "courseName": "알고리즘",
  "dayOfWeek": 1,
  "periodStart": 3,
  "periodEnd": 4
}
```

---

### 데이터베이스의 관계

#### 테이블 간의 관계

```
┌─────────────┐         ┌──────────────┐         ┌───────────────────┐
│   users     │         │  timetable   │         │ timetable_course  │
├─────────────┤         ├──────────────┤         ├───────────────────┤
│ user_id (PK)│◄───────┤ user_id (FK) │         │                   │
│ user_name   │         │ timetable_id │◄───────┤ timetable_id (FK) │
│ email       │         │ year         │         │ course_id (PK)    │
└─────────────┘         │ semester     │         │ course_name       │
                        └──────────────┘         │ day_of_week       │
                                                 │ period_start      │
                                                 └───────────────────┘
```

**관계 설명:**
- 1명의 사용자는 여러 개의 시간표를 가질 수 있음 (1:N)
- 1개의 시간표는 여러 개의 과목을 가질 수 있음 (1:N)

**예시:**
```
사용자: 홍길동 (user_id=1)
  ├─ 시간표: 2026년 봄학기 (timetable_id=1)
  │    ├─ 과목: 데이터베이스 (course_id=1)
  │    └─ 과목: 알고리즘 (course_id=2)
  └─ 시간표: 2026년 가을학기 (timetable_id=2)
       ├─ 과목: 운영체제 (course_id=3)
       └─ 과목: 네트워크 (course_id=4)
```

---

## 자주 하는 실수와 해결법

### 1. Backend 실행 오류

#### 오류: `java.sql.SQLException: Access denied for user 'root'@'localhost'`

**원인**: MariaDB 비밀번호가 틀림

**해결법**:
```bash
# MariaDB 접속해서 비밀번호 재설정
sudo mysql -u root

# SQL 실행
ALTER USER 'root'@'localhost' IDENTIFIED BY 'tn1111';
FLUSH PRIVILEGES;
EXIT;
```

#### 오류: `Address already in use`

**원인**: 8080 포트를 이미 다른 프로세스가 사용 중

**해결법**:
```bash
# 8080 포트 사용 프로세스 확인
lsof -i :8080

# 출력 예시:
# COMMAND   PID   USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
# java    12345  user   42u  IPv6 0x...      0t0  TCP *:http-alt (LISTEN)

# 프로세스 종료 (PID를 위 출력에서 확인)
kill -9 12345
```

#### 오류: `ClassNotFoundException: javax.servlet.ServletContextListener`

**원인**: Tomcat 11을 사용 중 (Jakarta EE vs Java EE 문제)

**해결법**:
```bash
# Tomcat 11 제거
brew uninstall tomcat

# Tomcat 9 설치
brew install tomcat@9

# Tomcat 9로 실행
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run
```

---

### 2. Frontend 실행 오류

#### 오류: `Module not found: Error: Can't resolve './pages/Timetable/TimetablePage'`

**원인**: TypeScript 파일 import 시 확장자 누락

**해결법**:
```javascript
// 잘못된 코드
import TimetablePage from './pages/Timetable/TimetablePage';

// 올바른 코드
import TimetablePage from './pages/Timetable/TimetablePage.tsx';
```

#### 오류: `proxy error: Could not proxy request /api/timetable`

**원인**: Backend가 실행 중이지 않거나 프록시 설정이 틀림

**해결법**:
```bash
# 1. Backend가 실행 중인지 확인
curl http://localhost:8080/ej2/api/users

# 2. package.json 프록시 설정 확인
cat frontend/package.json
# "proxy": "http://localhost:8080/ej2" 가 있어야 함

# 3. Frontend 재시작
# Ctrl+C로 종료 후
npm start
```

---

### 3. 데이터베이스 오류

#### 오류: `Unknown database 'ej2'`

**원인**: 데이터베이스가 생성되지 않음

**해결법**:
```bash
mysql -u root -p
# 비밀번호 입력: tn1111
```

```sql
-- 데이터베이스 생성
CREATE DATABASE ej2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 생성 확인
SHOW DATABASES;

EXIT;
```

#### 오류: `Table 'ej2.timetable' doesn't exist`

**원인**: Hibernate가 테이블을 자동 생성하지 못함

**해결법**:
```bash
# RootConfig.java 확인
cat backend/src/main/java/com/ej2/config/RootConfig.java
```

아래 설정이 있는지 확인:
```java
properties.setProperty("hibernate.hbm2ddl.auto", "update");
```

만약 없다면 추가하고 Backend 재시작.

---

### 4. API 호출 오류

#### 오류: `시간표 ID가 필요합니다`

**원인**: Frontend에서 timetable을 먼저 로드하지 않고 과목 추가 시도

**해결법**:
```typescript
// TimetablePage.tsx의 handleSaveCourse 함수에 추가
const handleSaveCourse = async (courseData) => {
    // 시간표가 로드되었는지 확인
    if (!timetable || !timetable.timetableId) {
        alert('시간표를 먼저 불러와주세요');
        return;  // 여기서 함수 종료
    }

    // 나머지 저장 로직...
};
```

#### 오류: `시간표가 겹칩니다`

**원인**: 이미 과목이 있는 시간대에 다른 과목 추가 시도

**해결법**:
- 다른 시간대에 과목 추가
- 또는 기존 과목을 먼저 삭제

---

## 코드 수정하는 방법

### 과목 색상 변경하기

**위치**: `backend/src/main/java/com/ej2/controller/TimetableController.java`

```java
// 기존 코드 (파란색 기본값)
course.setColorCode("#3b82f6");

// 수정 후 (초록색 기본값)
course.setColorCode("#10b981");
```

**색상 코드 예시**:
- 파란색: `#3b82f6`
- 빨간색: `#ef4444`
- 초록색: `#10b981`
- 보라색: `#8b5cf6`
- 노란색: `#f59e0b`

---

### 기본 학점 변경하기

**위치**: `backend/src/main/java/com/ej2/controller/TimetableController.java`

```java
// 기존 코드 (3.0 학점)
course.setCredits(3.0);

// 수정 후 (2.0 학점)
course.setCredits(2.0);
```

---

### 시간표 그리드 크기 변경하기

**위치**: `frontend/src/pages/Timetable/TimetablePage.css`

```css
/* 기존 코드 (60px 높이) */
.time-cell {
    height: 60px;
}

/* 수정 후 (80px 높이) */
.time-cell {
    height: 80px;
}
```

---

### 요일 추가하기 (예: 토요일)

**1. Backend Model 수정** (필요 없음 - dayOfWeek는 숫자로 저장)

**2. Frontend 수정**

`frontend/src/pages/Timetable/TimetablePage.tsx`:

```typescript
// 기존 코드
const DAYS = ['월', '화', '수', '목', '금'];

// 수정 후
const DAYS = ['월', '화', '수', '목', '금', '토'];
```

**3. CSS 수정**

`frontend/src/pages/Timetable/TimetablePage.css`:

```css
/* 기존 코드 (5일) */
.timetable-grid {
    display: grid;
    grid-template-columns: 80px repeat(5, 1fr);
}

/* 수정 후 (6일) */
.timetable-grid {
    display: grid;
    grid-template-columns: 80px repeat(6, 1fr);
}
```

---

## 문제 해결 체크리스트

### Backend 문제 체크리스트

- [ ] MariaDB가 실행 중인가?
  ```bash
  brew services list | grep mariadb
  ```

- [ ] 데이터베이스 'ej2'가 존재하는가?
  ```bash
  mysql -u root -p -e "SHOW DATABASES;"
  ```

- [ ] RootConfig.java의 비밀번호가 올바른가?
  ```bash
  grep "setPassword" backend/src/main/java/com/ej2/config/RootConfig.java
  ```

- [ ] WAR 파일이 최신인가?
  ```bash
  ls -lh backend/target/ej2.war
  ```

- [ ] Tomcat이 실행 중인가?
  ```bash
  curl http://localhost:8080
  ```

- [ ] API가 응답하는가?
  ```bash
  curl http://localhost:8080/ej2/api/users
  ```

---

### Frontend 문제 체크리스트

- [ ] node_modules가 설치되었는가?
  ```bash
  ls frontend/node_modules/ | wc -l
  # 수백 개 이상이어야 함
  ```

- [ ] package.json에 프록시 설정이 있는가?
  ```bash
  grep "proxy" frontend/package.json
  ```

- [ ] Backend가 실행 중인가?
  ```bash
  curl http://localhost:8080/ej2/api/users
  ```

- [ ] 브라우저 콘솔에 에러가 있는가?
  - Chrome: F12 → Console 탭
  - Safari: Develop → Show JavaScript Console

- [ ] 네트워크 탭에서 API 요청이 보이는가?
  - Chrome: F12 → Network 탭
  - 페이지 새로고침 후 API 요청 확인

---

## 추가 학습 자료

### Java & Spring
- [Spring 공식 문서](https://spring.io/guides)
- [Baeldung - Spring Tutorial](https://www.baeldung.com/spring-tutorial)

### React & TypeScript
- [React 공식 문서](https://react.dev/learn)
- [TypeScript 핸드북](https://www.typescriptlang.org/docs/handbook/intro.html)

### 데이터베이스
- [MariaDB 공식 문서](https://mariadb.com/kb/en/documentation/)
- [SQL 기초 튜토리얼](https://www.w3schools.com/sql/)

---

## 질문과 답변

### Q1: "시간표를 먼저 불러와주세요" 메시지가 계속 나와요

**A**: 다음 순서로 확인하세요:

1. "시간표 불러오기" 버튼을 눌렀는가?
2. 브라우저 콘솔(F12)에서 API 응답 확인:
   ```javascript
   // 정상 응답
   {timetable: {timetableId: 1, ...}, courses: [...]}
   ```
3. 사용자, 연도, 학기가 올바르게 선택되었는가?

### Q2: 과목 추가 후 화면에 안 나타나요

**A**: 다음을 확인하세요:

1. 브라우저 콘솔(F12)에서 에러 확인
2. Network 탭에서 POST 요청 응답 확인
3. 페이지 새로고침 (Cmd+R 또는 Ctrl+R)
4. 데이터베이스 확인:
   ```sql
   USE ej2;
   SELECT * FROM timetable_course;
   ```

### Q3: Backend를 수정했는데 변경사항이 반영되지 않아요

**A**: 다음 순서로 재배포하세요:

```bash
# 1. Tomcat 종료 (Ctrl+C)

# 2. Backend 재빌드
cd backend
mvn clean package

# 3. WAR 파일 재배포
cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/

# 4. Tomcat 재시작
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run
```

### Q4: Frontend를 수정했는데 변경사항이 반영되지 않아요

**A**: 다음을 시도하세요:

```bash
# 1. 브라우저 캐시 강제 새로고침
# Mac: Cmd+Shift+R
# Windows: Ctrl+Shift+R

# 2. Frontend 재시작
# Ctrl+C로 종료 후
npm start
```

---

## 마치며

이 문서는 EJ2 프로젝트를 처음 접하는 초보자를 위해 작성되었습니다.

**다음 단계**:
1. 직접 코드를 수정해보세요
2. 새로운 기능을 추가해보세요 (예: 과목 검색, 필터링)
3. 문제가 발생하면 위의 체크리스트를 활용하세요

**도움이 필요하면**:
- GitHub Issues에 질문 올리기
- 관련 커뮤니티에서 도움 요청

프로그래밍은 실습이 가장 중요합니다. 두려워하지 말고 직접 코드를 만져보세요! 🚀

---

## Bash 명령어 완전 가이드

이 섹션에서는 프로젝트에서 사용하는 모든 Bash 명령어를 초보자 관점에서 자세히 설명합니다.

### Bash란?

**Bash**(Bourne Again SHell)는 Unix/Linux/Mac에서 사용하는 명령줄 인터페이스(CLI)입니다.
- **비유**: 컴퓨터와 대화하는 언어
- **용도**: 파일 관리, 프로그램 실행, 시스템 제어
- **실행 위치**: 터미널(Terminal) 또는 iTerm2

---

### 기본 개념

#### 1. 터미널 열기
- **Mac**: Cmd + Space → "터미널" 검색 → Enter
- **단축키**: Applications → Utilities → Terminal

#### 2. 프롬프트(Prompt)
터미널에 나타나는 기호로, 명령어를 입력할 수 있음을 표시합니다.
```bash
user@MacBook-Pro ~ %    # Mac (zsh)
user@MacBook-Pro ~ $    # Mac (bash)
```

#### 3. 경로(Path)
파일이나 디렉토리의 위치를 나타냅니다.
- **절대 경로**: `/Users/yunsu-in/Downloads/EJ2` (최상위부터 전체 경로)
- **상대 경로**: `./backend` (현재 위치 기준)
- **홈 디렉토리**: `~` (사용자 홈 디렉토리, 예: /Users/yunsu-in)

---

### 자주 사용하는 기본 명령어

#### 1. `ls` - 파일/디렉토리 목록 보기
```bash
ls
# 현재 디렉토리의 파일과 폴더 목록 표시

ls -l
# 상세 정보와 함께 목록 표시
# 출력 예시:
# drwxr-xr-x  5 user  staff   160 Jan 18 10:00 backend
# -rw-r--r--  1 user  staff  1024 Jan 18 11:30 README.md

ls -lh
# 파일 크기를 사람이 읽기 쉬운 형태로 표시 (KB, MB)
# 출력 예시:
# -rw-r--r--  1 user  staff   15M Jan 18 14:30 ej2.war

ls -a
# 숨김 파일(.으로 시작하는 파일)도 함께 표시
```

**옵션 설명**:
- `-l`: long format (상세 정보)
- `-h`: human-readable (사람이 읽기 쉬운 크기)
- `-a`: all (숨김 파일 포함)

#### 2. `cd` - 디렉토리 이동
```bash
cd /Users/yunsu-in/Downloads/EJ2
# 지정한 경로로 이동 (절대 경로)

cd backend
# 현재 디렉토리의 backend 폴더로 이동 (상대 경로)

cd ..
# 상위 디렉토리로 이동
# 예: /Users/yunsu-in/Downloads/EJ2/backend → /Users/yunsu-in/Downloads/EJ2

cd ~
# 홈 디렉토리로 이동 (/Users/yunsu-in)

cd -
# 이전 디렉토리로 돌아가기
```

#### 3. `pwd` - 현재 디렉토리 경로 확인
```bash
pwd
# 출력 예시: /Users/yunsu-in/Downloads/EJ2
```

#### 4. `cat` - 파일 내용 보기
```bash
cat package.json
# package.json 파일의 전체 내용을 화면에 출력

cat frontend/package.json
# 상대 경로로 파일 읽기
```

#### 5. `grep` - 텍스트 검색
```bash
grep "proxy" frontend/package.json
# frontend/package.json 파일에서 "proxy"가 포함된 줄 찾기
# 출력 예시: "proxy": "http://localhost:8080/ej2"

grep "setPassword" backend/src/main/java/com/ej2/config/RootConfig.java
# RootConfig.java에서 "setPassword"가 포함된 줄 찾기
```

#### 6. `echo` - 텍스트 출력
```bash
echo "Hello, World!"
# 화면에 "Hello, World!" 출력

echo $PATH
# PATH 환경 변수의 값 출력
```

#### 7. `mkdir` - 디렉토리 생성
```bash
mkdir new_folder
# 현재 디렉토리에 new_folder 생성

mkdir -p parent/child/grandchild
# 중간 디렉토리까지 한 번에 생성
```

#### 8. `rm` - 파일/디렉토리 삭제
```bash
rm file.txt
# file.txt 삭제

rm -rf old_folder
# old_folder와 그 안의 모든 내용 강제 삭제
# 주의: 복구 불가능!
```

**옵션 설명**:
- `-r`: recursive (디렉토리와 내용물 모두 삭제)
- `-f`: force (확인 없이 강제 삭제)

#### 9. `cp` - 파일/디렉토리 복사
```bash
cp source.txt destination.txt
# source.txt를 destination.txt로 복사

cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/
# ej2.war 파일을 Tomcat webapps 디렉토리로 복사
```

#### 10. `mv` - 파일/디렉토리 이동 또는 이름 변경
```bash
mv old_name.txt new_name.txt
# 파일 이름 변경

mv file.txt /path/to/destination/
# 파일을 다른 디렉토리로 이동

mv PROJECT_DOCUMENTATION.md 0118_1_PROJECT_DOCUMENTATION.md
# 파일 이름 변경 예시
```

---

### 프로세스 관리 명령어

#### 11. `ps` - 실행 중인 프로세스 확인
```bash
ps aux
# 시스템의 모든 프로세스 표시

ps aux | grep java
# Java 프로세스만 필터링해서 표시
```

**옵션 설명**:
- `a`: 모든 사용자의 프로세스
- `u`: 자세한 정보
- `x`: 터미널과 연결되지 않은 프로세스 포함

#### 12. `lsof` - 포트 사용 확인
```bash
lsof -i :8080
# 8080 포트를 사용하는 프로세스 확인
# 출력 예시:
# COMMAND   PID   USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
# java    12345  user   42u  IPv6 0x...      0t0  TCP *:http-alt (LISTEN)
```

**컬럼 설명**:
- `COMMAND`: 프로세스 이름
- `PID`: 프로세스 ID (종료할 때 필요)
- `USER`: 프로세스 소유자
- `NAME`: 사용 중인 포트

#### 13. `kill` - 프로세스 종료
```bash
kill 12345
# PID가 12345인 프로세스를 정상 종료

kill -9 12345
# PID가 12345인 프로세스를 강제 종료
# -9: SIGKILL (즉시 종료)
```

**시그널 종류**:
- `-15` (기본값): SIGTERM (정상 종료 요청)
- `-9`: SIGKILL (즉시 강제 종료)

---

### 패키지 관리 명령어 (Homebrew)

#### 14. `brew` - Homebrew 패키지 관리자
```bash
brew install mariadb
# MariaDB 설치

brew install openjdk@11
# Java 11 설치

brew uninstall tomcat
# Tomcat 제거

brew services start mariadb
# MariaDB 서비스 시작

brew services stop mariadb
# MariaDB 서비스 중지

brew services restart mariadb
# MariaDB 서비스 재시작

brew services list
# 설치된 서비스 목록과 상태 확인
# 출력 예시:
# Name    Status  User       File
# mariadb started yunsu-in   ~/Library/LaunchAgents/homebrew.mxcl.mariadb.plist
```

---

### 데이터베이스 명령어

#### 15. `mysql` - MariaDB 클라이언트
```bash
mysql -u root -p
# root 사용자로 MariaDB 접속 (비밀번호 입력 필요)

sudo mysql -u root
# sudo 권한으로 접속 (비밀번호 없이 접속 가능)

mysql -u root -p -e "SHOW DATABASES;"
# 명령어 실행 후 즉시 종료 (-e: execute)

mysql --version
# MariaDB 버전 확인
```

**옵션 설명**:
- `-u`: 사용자 이름 (user)
- `-p`: 비밀번호 입력 요청 (password)
- `-e`: SQL 명령어 직접 실행 (execute)

---

### Java 빌드 명령어 (Maven)

#### 16. `mvn` - Maven 빌드 도구
```bash
mvn clean
# 이전 빌드 결과물(target/ 디렉토리) 삭제

mvn package
# 프로젝트를 컴파일하고 WAR/JAR 파일로 패키징

mvn clean package
# 이전 결과물 삭제 후 다시 빌드 (가장 자주 사용)

mvn -version
# Maven 버전 확인
```

**빌드 라이프사이클**:
1. `clean`: 이전 빌드 결과 삭제
2. `compile`: Java 소스 코드 컴파일
3. `test`: 단위 테스트 실행
4. `package`: WAR/JAR 파일 생성
5. `install`: 로컬 저장소에 설치

**빌드 성공 메시지**:
```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.234 s
```

---

### Node.js/npm 명령어

#### 17. `npm` - Node Package Manager
```bash
npm install
# package.json에 명시된 모든 패키지 설치
# node_modules/ 디렉토리에 설치됨

npm install axios
# axios 패키지만 설치하고 package.json에 추가

npm start
# package.json의 "start" 스크립트 실행
# 이 프로젝트에서는: react-scripts start

npm run build
# 프로덕션 빌드 생성

npm -v
# npm 버전 확인
```

#### 18. `node` - Node.js 런타임
```bash
node -v
# Node.js 버전 확인
# 출력 예시: v18.17.0

node script.js
# JavaScript 파일 실행
```

---

### Tomcat 관련 명령어

#### 19. Tomcat 시작/중지
```bash
# Tomcat 시작 (포그라운드)
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run

# Tomcat 시작 (백그라운드)
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh start

# Tomcat 중지
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh stop

# Tomcat 버전 확인
/usr/local/Cellar/tomcat@9/9.0.98/bin/version.sh
```

**포그라운드 vs 백그라운드**:
- **포그라운드** (`run`): 터미널에 로그가 실시간으로 출력됨. Ctrl+C로 종료.
- **백그라운드** (`start`): 터미널과 독립적으로 실행. `stop` 명령으로 종료.

---

### HTTP 요청 명령어

#### 20. `curl` - URL로 HTTP 요청 보내기
```bash
curl http://localhost:8080
# 지정한 URL에 GET 요청 보내기
# 응답 내용을 화면에 출력

curl http://localhost:8080/ej2/api/users
# API 엔드포인트 테스트

curl -s "http://localhost:8080/ej2/api/timetable?userId=1&year=2026&semester=Spring"
# 쿼리 파라미터 포함 요청 (-s: silent, 진행 상황 숨김)

curl -X POST http://localhost:8080/ej2/api/timetable/course \
  -H "Content-Type: application/json" \
  -d '{"courseName": "데이터베이스", "dayOfWeek": 0}'
# POST 요청 (JSON 데이터 전송)
```

**옵션 설명**:
- `-s`: silent (진행 상황 표시 숨김)
- `-X`: HTTP 메서드 지정 (GET, POST, PUT, DELETE)
- `-H`: 헤더 추가
- `-d`: 데이터 전송 (POST body)

---

### 파이프와 리다이렉션

#### 21. `|` (파이프) - 명령어 연결
```bash
ls -lh | grep ".war"
# ls 결과에서 ".war"가 포함된 줄만 표시

ps aux | grep java
# 모든 프로세스 중 "java"가 포함된 것만 표시

brew services list | grep mariadb
# 서비스 목록에서 mariadb만 표시

ls frontend/node_modules/ | wc -l
# node_modules의 패키지 개수 세기
```

**동작 원리**:
- 왼쪽 명령어의 출력을 오른쪽 명령어의 입력으로 전달

#### 22. `>` (리다이렉션) - 출력을 파일로 저장
```bash
ls -l > file_list.txt
# ls 결과를 file_list.txt 파일에 저장 (덮어쓰기)

echo "New line" >> file.txt
# 파일 끝에 추가 (append)

curl http://localhost:8080/ej2/api/users > users.json
# API 응답을 파일로 저장
```

**차이점**:
- `>`: 기존 파일 내용을 덮어씀
- `>>`: 기존 파일 끝에 추가

#### 23. `wc` - 줄/단어/문자 수 세기
```bash
wc -l file.txt
# file.txt의 줄 수 출력

ls frontend/node_modules/ | wc -l
# node_modules에 설치된 패키지 개수 세기
```

---

### 권한과 소유권

#### 24. `sudo` - 관리자 권한으로 실행
```bash
sudo mysql -u root
# 관리자 권한으로 MySQL 접속

sudo rm -rf /some/protected/folder
# 관리자 권한으로 삭제 (주의!)
```

**주의사항**:
- `sudo`는 시스템 전체에 영향을 주는 명령어에만 사용
- 비밀번호 입력 필요 (Mac 로그인 비밀번호)
- 불필요하게 사용하지 말 것

#### 25. `chmod` - 파일 권한 변경
```bash
chmod +x script.sh
# 파일에 실행 권한 추가

chmod 755 file.txt
# rwxr-xr-x (소유자: 읽기+쓰기+실행, 그룹/기타: 읽기+실행)
```

**권한 표기**:
- `r`: 읽기 (4)
- `w`: 쓰기 (2)
- `x`: 실행 (1)
- 예: 755 = 7(rwx) + 5(r-x) + 5(r-x)

---

### 시스템 정보 명령어

#### 26. `java -version` - Java 버전 확인
```bash
java -version
# 출력 예시:
# openjdk version "11.0.12" 2021-07-20
# OpenJDK Runtime Environment (build 11.0.12+7)
```

#### 27. `which` - 명령어 경로 찾기
```bash
which java
# 출력 예시: /usr/bin/java

which mvn
# 출력 예시: /usr/local/bin/mvn
```

#### 28. `du` - 디스크 사용량 확인
```bash
du -sh backend/target
# backend/target 디렉토리의 전체 크기 표시
# -s: summary (합계만), -h: human-readable
```

---

### 복합 명령어 예시

#### 예시 1: Backend 재빌드 및 배포
```bash
cd /Users/yunsu-in/Downloads/EJ2/backend && mvn clean package && cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/
```

**분해 설명**:
1. `cd /Users/yunsu-in/Downloads/EJ2/backend`: backend 디렉토리로 이동
2. `&&`: 이전 명령이 성공하면 다음 명령 실행
3. `mvn clean package`: Maven 빌드
4. `&&`: 빌드가 성공하면 다음 명령 실행
5. `cp target/ej2.war ...`: WAR 파일을 Tomcat에 복사

#### 예시 2: 8080 포트 사용 프로세스 확인 및 종료
```bash
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9
```

**분해 설명**:
1. `lsof -i :8080`: 8080 포트 사용 프로세스 확인
2. `| grep LISTEN`: LISTEN 상태인 줄만 필터링
3. `| awk '{print $2}'`: 두 번째 컬럼(PID) 추출
4. `| xargs kill -9`: 추출한 PID를 kill 명령에 전달

**주의**: 이 명령어는 강제 종료하므로 신중하게 사용!

---

### 단축키

터미널에서 유용한 키보드 단축키:

| 단축키 | 기능 |
|--------|------|
| `Ctrl + C` | 현재 실행 중인 명령 중단 |
| `Ctrl + D` | 터미널 종료 (또는 입력 종료) |
| `Ctrl + L` | 화면 지우기 (clear와 동일) |
| `Ctrl + A` | 커서를 줄 맨 앞으로 이동 |
| `Ctrl + E` | 커서를 줄 맨 뒤로 이동 |
| `Ctrl + U` | 커서 앞의 모든 텍스트 삭제 |
| `Ctrl + K` | 커서 뒤의 모든 텍스트 삭제 |
| `Ctrl + R` | 명령어 히스토리 검색 |
| `Tab` | 명령어/파일명 자동 완성 |
| `↑` / `↓` | 이전/다음 명령어 히스토리 |

---

### 문제 상황별 명령어 모음

#### 상황 1: "Backend가 실행 중인지 확인하고 싶어요"
```bash
# 방법 1: 포트 확인
lsof -i :8080

# 방법 2: 프로세스 확인
ps aux | grep tomcat

# 방법 3: HTTP 요청
curl http://localhost:8080
```

#### 상황 2: "MariaDB가 실행 중인지 확인하고 싶어요"
```bash
# 방법 1: Homebrew 서비스 확인
brew services list | grep mariadb

# 방법 2: 포트 확인
lsof -i :3306

# 방법 3: 직접 접속 시도
mysql -u root -p
```

#### 상황 3: "빌드 결과물이 최신인지 확인하고 싶어요"
```bash
# WAR 파일 수정 시간 확인
ls -lh backend/target/ej2.war

# 자바 소스 파일과 비교
ls -lh backend/src/main/java/com/ej2/controller/TimetableController.java
```

#### 상황 4: "node_modules가 제대로 설치되었는지 확인하고 싶어요"
```bash
# 설치된 패키지 개수 확인
ls frontend/node_modules/ | wc -l

# 특정 패키지 확인
ls frontend/node_modules/ | grep react
```

---

### 초보자가 자주 하는 실수

#### 실수 1: 경로를 잘못 지정
```bash
# ❌ 잘못된 예
cd backend/src/main/java/com/ej2/controller/TimetableController.java
# Error: TimetableController.java는 파일이므로 cd 불가

# ✅ 올바른 예
cd backend/src/main/java/com/ej2/controller/
cat TimetableController.java
```

#### 실수 2: 공백이 있는 경로를 따옴표 없이 사용
```bash
# ❌ 잘못된 예
cd /Users/yunsu in/Downloads/EJ2
# Error: "yunsu"와 "in"을 별도 인자로 인식

# ✅ 올바른 예
cd "/Users/yunsu in/Downloads/EJ2"
# 또는
cd /Users/yunsu\ in/Downloads/EJ2  # 백슬래시로 공백 이스케이프
```

#### 실수 3: sudo를 불필요하게 사용
```bash
# ❌ 불필요한 sudo
sudo npm install
sudo mvn clean package

# ✅ sudo 없이 실행
npm install
mvn clean package

# 💡 sudo는 시스템 파일 접근이나 서비스 관리에만 필요
```

#### 실수 4: 명령어 실행 위치 확인 안 함
```bash
# 현재 위치 확인
pwd

# ❌ backend 디렉토리에 있는데 frontend 명령 실행
cd backend
npm start  # Error: package.json not found

# ✅ frontend로 이동 후 실행
cd ../frontend
npm start
```

---

### 명령어 조합 팁

#### 팁 1: 명령어 체이닝
```bash
# && : 이전 명령 성공 시에만 다음 실행
cd backend && mvn clean package && echo "빌드 성공!"

# || : 이전 명령 실패 시에만 다음 실행
curl http://localhost:8080 || echo "서버가 실행 중이지 않습니다"

# ; : 이전 명령 결과와 무관하게 다음 실행
cd backend ; pwd ; ls
```

#### 팁 2: 백그라운드 실행
```bash
# 명령어 끝에 &를 붙이면 백그라운드 실행
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run &

# 백그라운드 작업 목록 확인
jobs

# 백그라운드 작업 포그라운드로 가져오기
fg %1
```

#### 팁 3: 명령어 히스토리 활용
```bash
# 명령어 히스토리 보기
history

# 특정 번호의 명령어 재실행
!42  # 42번 명령어 실행

# 마지막 명령어 재실행
!!

# 마지막 mvn 명령어 재실행
!mvn
```

---

### 명령어 도움말 보기

모든 명령어는 도움말을 제공합니다:

```bash
# 방법 1: --help 옵션
ls --help
mvn --help
npm --help

# 방법 2: man (manual) 명령어
man ls
man grep
man curl

# man 페이지 내 단축키:
# - Space: 다음 페이지
# - b: 이전 페이지
# - /검색어: 검색
# - q: 종료
```

---

### 마치며

**명령어 연습 팁**:
1. 두려워하지 말고 직접 타이핑해보세요
2. Tab 키로 자동완성을 적극 활용하세요
3. 실수해도 괜찮습니다 (삭제 명령만 조심!)
4. `--help`나 `man`으로 도움말을 자주 참고하세요

**위험한 명령어 (절대 주의!)**:
- `rm -rf /`: 시스템 전체 삭제 (실행하지 마세요!)
- `sudo rm -rf`: 관리자 권한으로 강제 삭제
- `:(){ :|:& };:`: Fork bomb (시스템 멈춤)

프로그래밍에서 CLI는 필수 도구입니다. 처음엔 어렵지만 익숙해지면 GUI보다 훨씬 빠르고 강력합니다! 💪

---

### 이 프로젝트에서 실제로 사용한 명령어 모음

이 섹션은 EJ2 프로젝트를 설정하고 문제를 해결하는 과정에서 **실제로 사용한** 명령어들입니다.

#### 1. 파일 이름 변경
```bash
mv PROJECT_DOCUMENTATION.md 0118_1_PROJECT_DOCUMENTATION.md
mv BEGINNER_GUIDE.md 0118_1_BEGINNER_GUIDE.md
```
**용도**: 문서 파일에 날짜 접두사를 추가하여 버전 관리

#### 2. 파일 존재 확인
```bash
ls -lh 0118_1_*.md
```
**출력 예시**:
```
-rw-r--r--@ 1 yunsu-in  staff    31K Jan 18 22:28 0118_1_BEGINNER_GUIDE.md
-rw-r--r--@ 1 yunsu-in  staff    21K Jan 18 22:25 0118_1_PROJECT_DOCUMENTATION.md
```

#### 3. API 테스트 (시간표 조회)
```bash
curl -s "http://localhost:8080/ej2/api/timetable?userId=1&year=2026&semester=Spring"
```
**출력 예시**:
```json
{
  "courses": [],
  "timetable": {
    "timetableId": 1,
    "userId": 1,
    "year": 2026,
    "semester": "Spring",
    "name": null,
    "createdAt": "2026-01-18T14:30:00"
  }
}
```
**용도**: Backend API가 정상 작동하는지 확인

#### 4. 특정 텍스트가 포함된 줄 찾기
```bash
grep -n "## Bash 명령어 완전 가이드" 0118_1_BEGINNER_GUIDE.md
```
**출력 예시**:
```
1190:## Bash 명령어 완전 가이드
```
**용도**: 문서에서 특정 섹션의 위치(줄 번호) 확인

#### 5. 파일 총 줄 수 확인
```bash
wc -l 0118_1_BEGINNER_GUIDE.md
```
**출력 예시**:
```
1907 0118_1_BEGINNER_GUIDE.md
```
**용도**: 문서의 전체 크기 파악

#### 6. 특정 패턴이 있는 줄 개수 세기
```bash
grep -c "^#### [0-9]*\. \`" 0118_1_BEGINNER_GUIDE.md
```
**출력 예시**:
```
27
```
**용도**: 문서에 몇 개의 Bash 명령어 섹션이 있는지 확인

#### 7. 패턴 매칭으로 섹션 제목 찾기 (처음 20개)
```bash
grep -E "^#### [0-9]+\. " 0118_1_BEGINNER_GUIDE.md | head -20
```
**출력 예시**:
```
#### 1. Java
#### 2. Spring Framework
#### 3. Hibernate JPA
...
#### 1. `ls` - 파일/디렉토리 목록 보기
#### 2. `cd` - 디렉토리 이동
```
**용도**: 문서의 구조 빠르게 파악

#### 8. 여러 명령어를 &&로 연결 (순차 실행)
```bash
cd /Users/yunsu-in/Downloads/EJ2/backend && mvn clean package && cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/
```
**설명**:
1. `cd /Users/yunsu-in/Downloads/EJ2/backend`: backend 디렉토리로 이동
2. `&&`: 이전 명령 성공 시에만 다음 실행
3. `mvn clean package`: Maven 빌드
4. `&&`: 빌드 성공 시에만 다음 실행
5. `cp target/ej2.war ...`: WAR 파일을 Tomcat에 복사

**💡 중요**: `&&`를 사용하면 이전 단계가 실패하면 다음 단계가 실행되지 않아 안전합니다!

#### 9. 파이프로 명령어 연결 (파이프라인)
```bash
grep -E "^#### [0-9]+\. " 0118_1_BEGINNER_GUIDE.md | head -20
```
**설명**:
1. `grep -E "^#### [0-9]+\. " 0118_1_BEGINNER_GUIDE.md`: 패턴 매칭으로 줄 찾기
2. `|`: 첫 번째 명령의 출력을 두 번째 명령의 입력으로 전달
3. `head -20`: 처음 20줄만 표시

#### 10. 파일 내용 패턴 검색 (-c로 개수만 출력)
```bash
grep -c "패턴" 파일명
```
**옵션**:
- `-c`: count (매칭되는 줄의 개수만 출력)
- `-n`: 줄 번호와 함께 출력
- `-E`: 확장 정규표현식 사용

#### 11. 정규표현식으로 파일 검색
```bash
ls frontend/node_modules/ | wc -l
```
**설명**:
1. `ls frontend/node_modules/`: node_modules의 파일/폴더 목록
2. `| wc -l`: 줄 수를 세서 패키지 개수 확인

#### 12. 프로세스 확인 및 필터링
```bash
ps aux | grep java
```
**설명**:
1. `ps aux`: 모든 프로세스 표시
2. `| grep java`: "java"가 포함된 프로세스만 필터링

#### 13. 포트 사용 확인
```bash
lsof -i :8080
```
**용도**: 8080 포트를 사용하는 프로세스 확인 (Tomcat이 실행 중인지)

#### 14. MariaDB 서비스 상태 확인
```bash
brew services list | grep mariadb
```
**출력 예시**:
```
mariadb started yunsu-in ~/Library/LaunchAgents/homebrew.mxcl.mariadb.plist
```

#### 15. MariaDB 접속 및 쿼리 실행
```bash
mysql -u root -p -e "SHOW DATABASES;"
```
**옵션**:
- `-u root`: root 사용자로 접속
- `-p`: 비밀번호 입력 요청
- `-e "SQL"`: SQL 실행 후 즉시 종료

#### 16. Homebrew 서비스 관리
```bash
# 서비스 시작
brew services start mariadb

# 서비스 중지
brew services stop mariadb

# 서비스 재시작
brew services restart mariadb

# 서비스 목록 확인
brew services list
```

#### 17. Maven 빌드
```bash
# 이전 빌드 삭제 후 새로 빌드
mvn clean package

# 빌드 성공 메시지:
# [INFO] BUILD SUCCESS
# [INFO] Total time:  15.234 s
```

#### 18. npm 패키지 관리
```bash
# 패키지 설치
npm install

# 개발 서버 시작
npm start

# 프로덕션 빌드
npm run build
```

#### 19. 파일 복사 (Backend 배포)
```bash
cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/
```
**용도**: 빌드된 WAR 파일을 Tomcat의 webapps 디렉토리에 복사

#### 20. Tomcat 시작/중지
```bash
# 포그라운드로 시작 (로그 실시간 출력)
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run

# 백그라운드로 시작
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh start

# 중지
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh stop
```

---

### 실전 트러블슈팅 명령어

#### 문제: "시간표를 먼저 불러와주세요" 에러

**1단계: API가 정상 작동하는지 확인**
```bash
curl -s "http://localhost:8080/ej2/api/timetable?userId=1&year=2026&semester=Spring"
```

**2단계: Backend가 실행 중인지 확인**
```bash
lsof -i :8080
```

**3단계: MariaDB가 실행 중인지 확인**
```bash
brew services list | grep mariadb
```

**4단계: 데이터베이스 확인**
```bash
mysql -u root -p
```
```sql
USE ej2;
SHOW TABLES;
SELECT * FROM timetable WHERE user_id = 1;
```

#### 문제: Backend를 수정했는데 변경사항이 반영되지 않음

**해결 순서**:
```bash
# 1. Tomcat 종료 (실행 중인 터미널에서 Ctrl+C)

# 2. Backend 디렉토리로 이동
cd /Users/yunsu-in/Downloads/EJ2/backend

# 3. Maven 재빌드
mvn clean package

# 4. 빌드 성공 확인
ls -lh target/ej2.war

# 5. Tomcat에 재배포
cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/

# 6. Tomcat 재시작
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run
```

#### 문제: 8080 포트가 이미 사용 중

**해결 방법 1: 프로세스 확인 후 종료**
```bash
# 포트 사용 프로세스 확인
lsof -i :8080

# 출력 예시:
# COMMAND   PID   USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
# java    12345  user   42u  IPv6 0x...      0t0  TCP *:http-alt (LISTEN)

# PID로 프로세스 종료
kill -9 12345
```

**해결 방법 2: 원라이너로 한 번에 종료**
```bash
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9
```

**분해 설명**:
1. `lsof -i :8080`: 8080 포트 사용 프로세스
2. `grep LISTEN`: LISTEN 상태만 필터링
3. `awk '{print $2}'`: PID(두 번째 컬럼) 추출
4. `xargs kill -9`: 추출한 PID를 kill 명령에 전달

---

### 명령어 조합 실전 예제

#### 예제 1: 전체 재시작 (Backend + Frontend)

**Backend 재시작**:
```bash
# 한 줄로 실행
cd /Users/yunsu-in/Downloads/EJ2/backend && mvn clean package && cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/ && /usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run
```

**Frontend 재시작** (새 터미널):
```bash
cd /Users/yunsu-in/Downloads/EJ2/frontend && npm start
```

#### 예제 2: 서비스 상태 일괄 확인
```bash
echo "=== MariaDB 상태 ===" && brew services list | grep mariadb && echo "=== Tomcat 상태 ===" && lsof -i :8080 && echo "=== Frontend 상태 ===" && lsof -i :3000
```

#### 예제 3: 로그 파일 실시간 모니터링
```bash
# Tomcat 로그 실시간 확인
tail -f /usr/local/Cellar/tomcat@9/9.0.98/libexec/logs/catalina.out
```

#### 예제 4: API 응답을 파일로 저장
```bash
# API 응답을 JSON 파일로 저장
curl -s "http://localhost:8080/ej2/api/timetable?userId=1&year=2026&semester=Spring" > timetable_response.json

# 저장된 파일 확인
cat timetable_response.json
```

---

### 자주 사용하는 명령어 체크리스트

**개발 환경 시작 체크리스트**:
```bash
# 1. MariaDB 시작
brew services start mariadb

# 2. MariaDB 상태 확인
brew services list | grep mariadb

# 3. Backend 빌드 및 실행
cd /Users/yunsu-in/Downloads/EJ2/backend
mvn clean package
cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/
/usr/local/Cellar/tomcat@9/9.0.98/bin/catalina.sh run

# 4. Frontend 실행 (새 터미널)
cd /Users/yunsu-in/Downloads/EJ2/frontend
npm start

# 5. 서비스 확인
curl http://localhost:8080/ej2/api/users
curl http://localhost:3000
```

**개발 환경 종료 체크리스트**:
```bash
# 1. Frontend 종료 (Frontend 터미널에서)
Ctrl + C

# 2. Tomcat 종료 (Backend 터미널에서)
Ctrl + C

# 3. MariaDB 중지 (선택사항)
brew services stop mariadb
```

---

### 명령어 팁과 트릭

#### 팁 1: 명령어 히스토리 활용
```bash
# 이전에 실행한 curl 명령어 재실행
!curl

# 이전에 실행한 mvn 명령어 재실행
!mvn

# 명령어 히스토리에서 검색 (Ctrl + R 누른 후)
# curl 입력 → 가장 최근 curl 명령어 표시
```

#### 팁 2: 자주 사용하는 명령어는 alias로 등록
```bash
# ~/.zshrc 또는 ~/.bashrc에 추가
alias backend-restart='cd /Users/yunsu-in/Downloads/EJ2/backend && mvn clean package && cp target/ej2.war /usr/local/Cellar/tomcat@9/9.0.98/libexec/webapps/'

alias check-ports='lsof -i :8080; lsof -i :3000; lsof -i :3306'

alias ej2-start='brew services start mariadb && cd /Users/yunsu-in/Downloads/EJ2/backend'

# 적용
source ~/.zshrc

# 사용
backend-restart
check-ports
ej2-start
```

#### 팁 3: 여러 터미널 창 관리
- **터미널 1**: Tomcat 실행 (로그 모니터링)
- **터미널 2**: Frontend 실행 (개발 서버)
- **터미널 3**: Git, 파일 작업, 테스트 명령어

---

이제 실전에서 바로 사용할 수 있는 명령어 레퍼런스가 완성되었습니다! 🎉
