# EJ2 시간표 500 에러 해결 가이드 (초보자용)

작성일: 2026-01-18
난이도: ⭐⭐⭐ (중급)

## 📌 이 문서를 읽기 전에

이 문서는 프로그래밍을 배우기 시작한 초보자를 위해 작성되었습니다. 각 개념을 최대한 쉽게 설명하려고 노력했으니 천천히 읽어보세요.

### 필요한 사전 지식
- Java 기본 문법 (클래스, 메서드)
- JavaScript/React 기본 (useState, useEffect)
- HTTP 요청/응답의 기본 개념
- 데이터베이스 기초 (테이블, 컬럼)

---

## 🎯 문제 상황 요약

### 무엇이 잘못되었나요?

시간표 페이지에 접속하면 화면이 비어있고, 브라우저 콘솔에 다음과 같은 에러가 표시되었습니다:

```
GET http://localhost:3000/api/timetable?semester=spring&year=2026&userId=1 500 (Internal Server Error)
```

### 왜 이런 일이 발생했나요?

총 **4가지 문제**가 차례대로 발생했습니다:

1. **프론트엔드와 데이터베이스의 값이 달랐어요** (대소문자 차이)
2. **순환 참조 문제가 있었어요** (서로를 계속 참조)
3. **날짜 데이터를 JSON으로 바꾸지 못했어요**
4. **데이터베이스 연결이 끊긴 후 데이터를 읽으려 했어요**

각 문제를 하나씩 자세히 알아볼게요!

---

## 🔍 문제 1: 대소문자 불일치

### 🤔 무슨 일이 있었나요?

상황을 그림으로 표현하면:

```
프론트엔드 (TimetablePage.tsx)
   └─> "semester=Spring" 전송 📤

서버 (Java)
   └─> 데이터베이스에서 "semester=Spring" 검색 🔍

데이터베이스 (MariaDB)
   └─> "semester=spring" 만 있음 (소문자) 💾
   └─> 매칭 실패! ❌
```

### 📝 코드로 보기

**문제가 있던 코드:**

```javascript
// frontend/src/pages/Timetable/TimetablePage.tsx (10번째 줄)
const [selectedSemester, setSelectedSemester] = useState('Spring'); // 대문자 S

// 드롭다운 메뉴 (159-160번째 줄)
<option value="Spring">봄학기</option>
<option value="Fall">가을학기</option>
```

```sql
-- backend/src/main/resources/sql/schema/timetable_schema.sql (34번째 줄)
INSERT INTO timetables (user_id, year, semester, name)
VALUES (1, 2025, 'spring', '2025년 봄학기'); -- 소문자 spring
```

### 💡 왜 문제가 될까요?

데이터베이스에서 검색할 때는 기본적으로 **대소문자를 구분**합니다. 이는 마치:

```
"사과" ≠ "사과" ≠ "SaGwa"
```

처럼 생각하면 됩니다. 컴퓨터는 대소문자를 다른 글자로 인식해요.

### ✅ 해결 방법

**수정된 코드:**

```javascript
// TimetablePage.tsx - 모두 소문자로 통일
const [selectedSemester, setSelectedSemester] = useState('spring'); // 소문자로 변경

<option value="spring">봄학기</option>  // 소문자로 변경
<option value="fall">가을학기</option>   // 소문자로 변경
```

### 🎓 배운 점

> **API 계약(Contract)**: 프론트엔드와 백엔드가 주고받을 데이터의 형식을 미리 정해두는 것이 중요합니다. 이를 "API 계약"이라고 부릅니다.

**실무 팁:**
```javascript
// 이렇게 상수로 정의하면 실수를 줄일 수 있어요
const SEMESTERS = {
  SPRING: 'spring',
  FALL: 'fall'
};

const [selectedSemester, setSelectedSemester] = useState(SEMESTERS.SPRING);
```

---

## 🔍 문제 2: 순환 참조 (Circular Reference)

### 🤔 무슨 일이 있었나요?

이 문제는 조금 복잡해요. 천천히 이해해봅시다.

**데이터베이스 관계:**

```
┌─────────────┐           ┌──────────────────┐
│  Timetable  │◄─────────│ TimetableCourse  │
│  (시간표)    │  여러 개   │   (과목)         │
└─────────────┘           └──────────────────┘

하나의 시간표는 여러 과목을 가질 수 있어요.
각 과목은 하나의 시간표에 속해있어요.
```

**Java 코드에서의 관계:**

```java
// Timetable.java
public class Timetable {
    private List<TimetableCourse> courses; // 시간표가 과목 리스트를 참조
}

// TimetableCourse.java
public class TimetableCourse {
    private Timetable timetable; // 과목이 시간표를 참조
}
```

### 🌀 순환 참조가 뭔가요?

Jackson(JSON 변환기)이 이 객체를 JSON으로 바꾸려고 할 때:

```
1. Timetable을 JSON으로 변환 시작
   ↓
2. courses 리스트를 변환하려고 함
   ↓
3. Course[0]을 변환 시작
   ↓
4. Course 안의 timetable을 변환하려고 함
   ↓
5. 다시 Timetable을 변환... (1번으로 돌아감)
   ↓
6. 무한 반복! 🔄♾️
```

이것이 **순환 참조**입니다. 서로를 계속 참조하는 상황이에요.

### 🎨 그림으로 이해하기

```
Timetable ──> courses ──> Course[0] ──> timetable ──> courses ──> Course[0] ──> ...
   ▲                                        |
   └────────────────────────────────────────┘
```

### ✅ 해결 방법

**수정된 코드:**

```java
// Timetable.java
@OneToMany(mappedBy = "timetable")
@JsonIgnore  // "이 필드는 JSON에 포함하지 마세요"
private List<TimetableCourse> courses;

// TimetableCourse.java
@ManyToOne
@JsonBackReference  // "부모로 돌아가는 참조는 무시하세요"
private Timetable timetable;
```

### 📚 어노테이션 설명

| 어노테이션 | 무슨 뜻인가요? | 비유 |
|-----------|--------------|------|
| `@JsonIgnore` | JSON으로 변환할 때 이 필드는 건너뛰세요 | "이 방은 구경 금지!" |
| `@JsonBackReference` | 뒤로 가는 참조는 무시하세요 | "온 길로 되돌아가지 마세요" |
| `@JsonManagedReference` | 앞으로 가는 참조는 포함하세요 | "이 길로 계속 가세요" |

### 🎓 배운 점

> **왜 @JsonIgnore를 선택했나요?**
>
> 서비스 계층에서 이미 courses를 별도로 조회해서 반환하고 있었기 때문에, Timetable 안에 courses를 또 포함할 필요가 없었습니다.

**실제 응답 구조:**
```json
{
  "timetable": {
    "timetableId": 1,
    "semester": "spring"
    // courses는 여기에 없음
  },
  "courses": [...]  // 여기에 별도로 있음
}
```

---

## 🔍 문제 3: LocalDateTime 직렬화 실패

### 🤔 무슨 일이 있었나요?

에러 메시지:
```
Java 8 date/time type `java.time.LocalDateTime` not supported by default
```

### 📖 직렬화(Serialization)란?

**직렬화**: Java 객체를 JSON 문자열로 바꾸는 것

```java
// Java 객체
LocalDateTime createdAt = LocalDateTime.of(2026, 1, 18, 20, 1, 41);

// ↓ 직렬화 ↓

// JSON 문자열
"2026-01-18 20:01:41"
```

### 🔧 Jackson이란?

Jackson은 Java 객체와 JSON을 서로 변환해주는 라이브러리입니다.

```
Java Object ←──Jackson──→ JSON String
```

### ❌ 문제의 원인

Jackson은 기본적으로 Java 8의 새로운 날짜 타입(`LocalDateTime`, `LocalDate` 등)을 어떻게 변환할지 모릅니다.

```java
// Timetable.java
private LocalDateTime createdAt; // Jackson: "이게 뭐지? 🤷"
private LocalDateTime updatedAt; // Jackson: "어떻게 바꿔야 하지? 🤔"
```

### ✅ 해결 방법 (3단계)

**1단계: JSR310 모듈 추가**

```xml
<!-- pom.xml에 추가 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>2.15.2</version>
</dependency>
```

> **JSR310**: Java 8부터 추가된 새로운 날짜/시간 API의 이름입니다.

**2단계: Jackson 설정**

```java
// WebConfig.java
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule()); // 날짜 변환 모듈 등록

    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(objectMapper);

    converters.add(converter);
}
```

**3단계: 날짜 형식 지정**

```java
// Timetable.java
@Column(name = "created_at")
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")  // "2026-01-18 20:01:41" 형식
private LocalDateTime createdAt;
```

### 🎨 형식 패턴 설명

```
yyyy-MM-dd HH:mm:ss
│    │  │  │  │  └─ 초 (00-59)
│    │  │  │  └──── 분 (00-59)
│    │  │  └─────── 시 (00-23)
│    │  └────────── 일 (01-31)
│    └───────────── 월 (01-12)
└────────────────── 연도 (4자리)
```

**예시:**
- `2026-01-18 20:01:41` → 2026년 1월 18일 20시 1분 41초

### 🎓 배운 점

> **왜 이렇게 복잡한가요?**
>
> 날짜와 시간은 나라마다, 시스템마다 표현 방식이 다릅니다:
> - 한국: 2026년 1월 18일
> - 미국: January 18, 2026
> - ISO: 2026-01-18
>
> 그래서 변환 규칙을 명확히 정해줘야 합니다!

---

## 🔍 문제 4: Hibernate 지연 로딩 문제

### 🤔 무슨 일이 있었나요?

에러 메시지:
```
failed to lazily initialize a collection, could not initialize proxy - no Session
```

이건 정말 까다로운 문제예요. 차근차근 알아봅시다.

### 📚 기본 개념

**1. Hibernate란?**
- Java 객체와 데이터베이스를 연결해주는 도구
- 우리가 SQL을 직접 쓰지 않아도 데이터를 가져올 수 있게 해줌

**2. 세션(Session)이란?**
- 데이터베이스와 연결된 상태
- 마치 전화 통화 중인 상태와 비슷해요

```
세션 열림 📞 ─────────────────────────> 세션 닫힘 📴
         (데이터 가져올 수 있음)    (데이터 못 가져옴)
```

**3. 지연 로딩(Lazy Loading)이란?**

```java
@OneToMany(fetch = FetchType.LAZY) // 기본값
private List<TimetableCourse> courses;
```

"지금 당장 필요하지 않으면 나중에 가져오자"는 전략입니다.

### 🎬 문제 상황 시나리오

**Act 1: 서비스 계층 (트랜잭션 안)**
```java
@Transactional  // 세션 열림 📞
public Map<String, Object> getTimetableWithCourses(...) {
    Timetable timetable = repository.find(...); // ✅ 데이터 조회 성공
    // courses는 아직 안 가져옴 (LAZY 로딩)

    return Map.of("timetable", timetable, "courses", courses);
} // 여기서 트랜잭션 종료 → 세션 닫힘 📴
```

**Act 2: 컨트롤러 (트랜잭션 밖)**
```java
// Jackson이 JSON으로 변환하려고 시도
// timetable.getCourses()를 호출하려 함
// ❌ 세션이 닫혀있어서 에러 발생!
```

### 🎨 타임라인으로 보기

```
시간 ──────────────────────────────────────────>

1. 서비스 시작
   │ 세션 열림 📞
   │
2. Timetable 조회 ✅
   │ (courses는 아직 안 가져옴)
   │
3. 서비스 종료
   │ 세션 닫힘 📴
   │
4. JSON 변환 시도
   │ courses 접근 시도 ❌
   │ "세션이 없어요!" 에러
   └─> LazyInitializationException
```

### ✅ 해결 방법

```java
// Timetable.java
@OneToMany(mappedBy = "timetable")
@JsonIgnore  // JSON 변환에서 제외
private List<TimetableCourse> courses;
```

### 🤔 다른 해결 방법들

| 방법 | 코드 | 장점 | 단점 |
|------|------|------|------|
| **@JsonIgnore** | `@JsonIgnore` | 간단함, 성능 좋음 | courses를 JSON에 포함 못함 |
| **EAGER 로딩** | `fetch = FetchType.EAGER` | 사용 간편 | 성능 저하 (항상 조회) |
| **트랜잭션 확장** | `@Transactional(readOnly = true)` | 모든 데이터 접근 가능 | 커넥션 점유 시간 증가 |

### 🎓 배운 점

> **우리는 왜 @JsonIgnore를 선택했나요?**
>
> 서비스에서 이미 courses를 별도로 조회해서 반환하고 있었기 때문입니다:
>
> ```java
> Map<String, Object> result = new HashMap<>();
> result.put("timetable", timetable);
> result.put("courses", courses); // 여기서 별도로 반환
> ```

---

## 🛠️ 전체 수정 사항 요약

### 1. 프론트엔드 수정

**파일: `frontend/src/pages/Timetable/TimetablePage.tsx`**

```javascript
// 변경 전
const [selectedSemester, setSelectedSemester] = useState('Spring');
<option value="Spring">봄학기</option>

// 변경 후
const [selectedSemester, setSelectedSemester] = useState('spring');
<option value="spring">봄학기</option>
```

### 2. 백엔드 수정

**파일 1: `backend/pom.xml`**

```xml
<!-- JSR310 모듈 추가 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>2.15.2</version>
</dependency>
```

**파일 2: `backend/src/main/java/com/ej2/config/WebConfig.java`**

```java
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(objectMapper);

    converters.add(converter);
}
```

**파일 3: `backend/src/main/java/com/ej2/model/Timetable.java`**

```java
// Import 추가
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

// 날짜 필드 수정
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createdAt;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime updatedAt;

// courses 필드 수정
@JsonIgnore
private List<TimetableCourse> courses;
```

**파일 4: `backend/src/main/java/com/ej2/model/TimetableCourse.java`**

```java
// Import 추가
import com.fasterxml.jackson.annotation.JsonBackReference;

// timetable 필드 수정
@JsonBackReference
private Timetable timetable;
```

### 3. 데이터베이스 수정

```sql
-- 잘못된 데이터 수정
UPDATE timetables
SET semester = 'spring'
WHERE semester = 'Spring';
```

---

## 🧪 테스트 방법

### 1. 백엔드 직접 테스트

```bash
# 터미널에서 실행
curl "http://localhost:8080/ej2/api/timetable?semester=spring&year=2026&userId=1"
```

**성공 시 응답:**
```json
{
  "courses": [],
  "timetable": {
    "timetableId": 1,
    "userId": 1,
    "year": 2026,
    "semester": "spring",
    "name": null,
    "createdAt": "2026-01-18 20:01:41",
    "updatedAt": "2026-01-18 20:01:41"
  }
}
```

**HTTP Status가 200이면 성공! 🎉**

### 2. 브라우저에서 테스트

1. 프론트엔드 서버 시작: `npm start`
2. 브라우저에서 시간표 페이지 접속
3. F12 → Console 탭 확인
4. 에러가 없으면 성공!

---

## 🎯 핵심 개념 정리

### Jackson 어노테이션 한눈에 보기

```java
public class Example {
    @JsonIgnore              // JSON에서 완전히 제외
    private String secret;

    @JsonFormat(pattern = "yyyy-MM-dd")  // 날짜 형식 지정
    private LocalDate date;

    @JsonManagedReference    // 부모 → 자식 (포함)
    private List<Child> children;
}

public class Child {
    @JsonBackReference       // 자식 → 부모 (제외)
    private Example parent;
}
```

### JPA Fetch 전략

| LAZY (지연 로딩) | EAGER (즉시 로딩) |
|-----------------|------------------|
| 필요할 때만 가져옴 | 항상 함께 가져옴 |
| 메모리 절약 | 편리함 |
| 성능 좋음 | 성능 저하 가능 |
| LazyInitializationException 주의 | 세션 신경 안 써도 됨 |

**권장 사항:** 기본은 LAZY, 꼭 필요한 경우만 EAGER

---

## 🔧 디버깅 팁 (초보자용)

### 1. 에러 메시지 읽는 법

```
org.springframework.http.converter.HttpMessageNotWritableException:
Could not write JSON: failed to lazily initialize a collection
```

**읽는 방법:**
1. 맨 앞의 예외 이름 확인: `HttpMessageNotWritableException`
   → "HTTP 메시지를 쓸 수 없어요"
2. 뒤의 설명 읽기: `Could not write JSON`
   → "JSON을 만들 수 없어요"
3. 근본 원인 찾기: `failed to lazily initialize a collection`
   → "컬렉션을 지연 로딩할 수 없어요"

### 2. 로그 보는 법

```bash
# Tomcat 로그 실시간으로 보기
tail -f /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/logs/catalina.out
```

**주목할 부분:**
- `ERROR`, `SEVERE`: 에러 발생
- `WARN`, `WARNING`: 경고 (문제가 될 수 있음)
- `INFO`: 정보 (정상 동작)

### 3. 브라우저 개발자 도구 사용법

1. **F12** 또는 **우클릭 → 검사**
2. **Network 탭**: HTTP 요청/응답 확인
3. **Console 탭**: JavaScript 에러 확인
4. **Application 탭**: 로컬 스토리지, 쿠키 확인

---

## 💡 실무 팁

### 1. 실수를 줄이는 방법

```javascript
// ❌ 나쁜 예: 문자열 직접 사용
const semester = 'Spring'; // 오타 가능

// ✅ 좋은 예: 상수 사용
const SEMESTERS = {
  SPRING: 'spring',
  FALL: 'fall'
};
const semester = SEMESTERS.SPRING; // 자동완성, 오타 방지
```

### 2. DTO 사용하기

**DTO(Data Transfer Object)**: 데이터 전송용 객체

```java
// Entity를 직접 반환하는 대신
public class TimetableDTO {
    private Long timetableId;
    private String semester;
    private String createdAt; // LocalDateTime → String 변환

    // courses는 포함하지 않음 (순환 참조 방지)
}
```

**장점:**
- 순환 참조 걱정 없음
- 필요한 데이터만 전송
- 클라이언트에게 보여줄 데이터 제어 가능

### 3. 테스트 코드 작성

```java
@Test
public void testGetTimetable() {
    // Given (준비)
    Long userId = 1L;
    Integer year = 2026;
    String semester = "spring";

    // When (실행)
    ResponseEntity<?> response = restTemplate.getForEntity(
        "/api/timetable?userId=" + userId +
        "&year=" + year +
        "&semester=" + semester,
        Map.class
    );

    // Then (검증)
    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
}
```

---

## 📚 더 공부하면 좋은 주제

### 초급
- [ ] HTTP 상태 코드 (200, 404, 500 등)
- [ ] JSON 형식
- [ ] REST API 기초
- [ ] Git 기본 명령어

### 중급
- [ ] JPA/Hibernate 기초
- [ ] Spring Boot 기본
- [ ] React Hooks (useState, useEffect)
- [ ] Promise와 async/await

### 고급
- [ ] N+1 문제
- [ ] 트랜잭션 관리
- [ ] DTO vs Entity
- [ ] Spring Data JPA

---

## ❓ 자주 묻는 질문 (FAQ)

### Q1: @JsonIgnore와 @JsonBackReference의 차이는?

**A:**
- `@JsonIgnore`: 해당 필드를 JSON에서 완전히 제외
- `@JsonBackReference`: 양방향 관계에서 역방향 참조만 제외

```java
// @JsonIgnore
{ "id": 1, "name": "test" } // secret 필드는 아예 없음

// @JsonBackReference
{
  "timetable": { "id": 1 },
  "courses": [
    { "courseName": "수학" }
    // timetable 필드는 없음 (역참조 제외)
  ]
}
```

### Q2: LAZY 로딩은 언제 사용하나요?

**A:**
- **LAZY 사용**: 자주 안 쓰는 데이터 (기본값)
- **EAGER 사용**: 항상 같이 쓰는 데이터

예: 사용자와 프로필 사진
- 사용자 목록: LAZY (사진 안 보여줌)
- 사용자 상세: EAGER (사진 같이 보여줌)

### Q3: 왜 이렇게 복잡한가요?

**A:**
복잡해 보이지만, 각각 이유가 있어요:
- **JPA**: SQL 직접 안 써도 됨
- **Jackson**: JSON 자동 변환
- **Lazy Loading**: 성능 최적화

처음에는 어려워도 익숙해지면 편해져요!

### Q4: 데이터베이스는 어떻게 확인하나요?

**A:**
```bash
# 터미널에서 실행
mysql -u root -p
USE ej2;
SELECT * FROM timetables;
SELECT * FROM timetable_courses;
```

---

## 🎉 마치며

이 문서에서 배운 내용:

1. ✅ **대소문자 일치**: API 계약을 명확히 하자
2. ✅ **순환 참조**: @JsonIgnore로 해결
3. ✅ **날짜 직렬화**: JSR310 모듈 필요
4. ✅ **지연 로딩**: 세션과 트랜잭션 이해

### 핵심 메시지

> 에러는 두렵지 않아요!
> 에러 메시지를 잘 읽고, 하나씩 해결해나가면 됩니다.
> 오늘 해결한 문제는 내일의 경험이 됩니다. 💪

### 다음 단계

1. [ ] 이 문서를 북마크하세요
2. [ ] 비슷한 에러 발생 시 참고하세요
3. [ ] 이해 안 되는 부분은 질문하세요
4. [ ] 직접 코드를 수정해보며 연습하세요

---

**도움이 필요하면:**
- GitHub Issues에 질문하세요
- 에러 메시지를 Google에서 검색해보세요
- Stack Overflow를 활용하세요

**Happy Coding! 🚀**

---

## 📝 부록: Bash 명령어 완전 가이드

이 섹션에서는 문제 해결 과정에서 사용한 모든 Bash 명령어를 초보자도 이해할 수 있도록 설명합니다.

### 🔍 데이터베이스 명령어

#### 1. 데이터베이스 접속 및 조회

```bash
mysql -u root -ptn1111
```
**설명:**
- `mysql`: MariaDB/MySQL 클라이언트 실행
- `-u root`: root 사용자로 접속
- `-p`: 비밀번호 입력 (바로 뒤에 비밀번호 입력)
- `tn1111`: 비밀번호 (보안상 좋지 않은 방법, `-p` 후 엔터 치고 입력하는 게 더 안전)

```bash
mysql -u root -p
# 비밀번호 입력: ****
```
**더 안전한 방법** (비밀번호가 히스토리에 남지 않음)

---

```bash
mysql -u root -ptn1111 -e "USE ej2; SELECT * FROM timetables;"
```
**설명:**
- `-e "SQL"`: Execute (SQL 명령어를 직접 실행)
- `USE ej2`: ej2 데이터베이스 선택
- `SELECT * FROM timetables`: timetables 테이블의 모든 데이터 조회

**사용 시기:** 데이터베이스 상태를 빠르게 확인할 때

---

```sql
UPDATE timetables SET semester = 'spring' WHERE semester = 'Spring';
```
**설명:**
- `UPDATE`: 데이터 수정
- `SET semester = 'spring'`: semester 컬럼을 'spring'으로 변경
- `WHERE semester = 'Spring'`: 'Spring'인 행만 선택

**주의:** WHERE 절을 빼먹으면 모든 행이 수정됨! ⚠️

---

### 🏗️ Maven 빌드 명령어

#### 2. 프로젝트 빌드 및 배포

```bash
mvn clean compile
```
**설명:**
- `mvn`: Maven 명령어 실행
- `clean`: target 폴더 삭제 (이전 빌드 결과물 제거)
- `compile`: 소스 코드를 컴파일 (`.java` → `.class`)

**언제 사용하나요?**
- 코드를 수정한 후 컴파일만 확인할 때
- 빌드 에러가 있는지 빠르게 체크할 때

---

```bash
mvn package -DskipTests
```
**설명:**
- `package`: 컴파일 + 패키징 (`.war` 파일 생성)
- `-DskipTests`: 테스트 건너뛰기
- `-D`: Define (Maven 속성 정의)

**결과:** `target/ej2.war` 파일 생성

---

```bash
mvn clean package -DskipTests
```
**설명:**
- `clean` + `package`: 이전 빌드 삭제 후 새로 패키징
- 깨끗한 상태에서 빌드할 때 사용

**비유:** 방 청소하고 다시 정리하기

---

### 🚀 Tomcat 배포 명령어

#### 3. WAR 파일 배포

```bash
cp target/ej2.war /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/webapps/
```
**설명:**
- `cp`: Copy (파일 복사)
- `target/ej2.war`: 소스 파일 (복사할 파일)
- `/opt/homebrew/.../webapps/`: 대상 폴더 (Tomcat의 웹앱 폴더)

**동작 원리:**
1. WAR 파일을 webapps 폴더에 복사
2. Tomcat이 자동으로 감지
3. 자동으로 압축 해제 및 배포 (Hot Deploy)

---

```bash
mvn clean package -DskipTests && cp target/ej2.war /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/webapps/
```
**설명:**
- `&&`: 앞 명령어가 성공하면 다음 명령어 실행
- 빌드 성공 시에만 배포

**단계별 실행:**
```
1. mvn clean package  ✅ 성공
   ↓
2. cp target/ej2.war  ✅ 실행

1. mvn clean package  ❌ 실패
   ↓
2. cp (실행 안 함)
```

---

### 📊 프로세스 및 로그 관리

#### 4. 실행 중인 프로세스 확인

```bash
ps aux | grep -i "java.*ej2\|tomcat" | grep -v grep
```
**설명:**
- `ps aux`: 모든 프로세스 목록 표시
  - `a`: 모든 사용자의 프로세스
  - `u`: 사용자 친화적 형식
  - `x`: 터미널 없이 실행 중인 프로세스 포함
- `|`: 파이프 (앞 명령어의 출력을 다음 명령어의 입력으로)
- `grep -i "java.*ej2\|tomcat"`: 패턴 검색
  - `-i`: 대소문자 무시
  - `.*`: 임의의 문자 0개 이상
  - `\|`: OR 연산자
- `grep -v grep`: grep 명령어 자신은 제외

**예시 출력:**
```
yunsu-in  5124  0.0  0.6  417229984  99920  ??  java -Dcatalina.home=...
```

---

#### 5. 로그 파일 확인

```bash
tail -f /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/logs/catalina.out
```
**설명:**
- `tail`: 파일의 마지막 부분 출력
- `-f`: Follow (실시간으로 추가되는 내용 계속 출력)
- 로그 파일 경로

**사용법:**
```bash
tail -f catalina.out   # 실시간 모니터링 (Ctrl+C로 종료)
tail -50 catalina.out  # 마지막 50줄만 보기
tail -n 100 catalina.out  # 마지막 100줄 보기
```

---

```bash
tail -200 /path/to/localhost.2026-01-18.log | grep "심각\|SEVERE" | tail -5
```
**설명:**
- `tail -200`: 마지막 200줄 가져오기
- `grep "심각\|SEVERE"`: 심각한 에러만 필터링
- `tail -5`: 그 중 마지막 5개만 출력

**파이프라인 흐름:**
```
전체 로그 파일
   ↓ tail -200
200줄만 선택
   ↓ grep
에러만 필터링
   ↓ tail -5
최근 5개만 출력
```

---

### 🌐 API 테스트 명령어

#### 6. HTTP 요청 테스트

```bash
curl "http://localhost:8080/ej2/api/timetable?semester=spring&year=2026&userId=1"
```
**설명:**
- `curl`: URL로 데이터 요청/전송하는 도구
- URL에 쿼리 파라미터 포함
  - `semester=spring`
  - `year=2026`
  - `userId=1`

**결과:** JSON 응답 출력

---

```bash
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/..."
```
**설명:**
- `-s`: Silent (진행 상황 숨김)
- `-w`: Write out (추가 정보 출력)
- `%{http_code}`: HTTP 상태 코드
- `\n`: 줄바꿈

**출력 예시:**
```json
{"timetable": {...}}
HTTP Status: 200
```

---

```bash
curl -s "http://..." | jq .
```
**설명:**
- `|`: 파이프 (curl 출력을 jq로 전달)
- `jq`: JSON 파서 (예쁘게 포맷팅)
- `.`: 전체 JSON 출력

**변환 예시:**
```
변환 전: {"courses":[],"timetable":{"id":1}}
변환 후:
{
  "courses": [],
  "timetable": {
    "id": 1
  }
}
```

---

### 📁 파일 및 디렉토리 관리

#### 7. 파일 찾기 및 탐색

```bash
find . -name "application.properties"
```
**설명:**
- `find`: 파일/디렉토리 검색
- `.`: 현재 디렉토리부터 시작
- `-name`: 이름으로 검색
- `"application.properties"`: 찾을 파일명

**더 많은 옵션:**
```bash
find . -name "*.java"        # 모든 .java 파일
find . -type f -name "*.xml" # 파일만 검색 (디렉토리 제외)
find . -type d -name "config" # 디렉토리만 검색
```

---

```bash
ls -la pom.xml build.gradle 2>/dev/null | head -5
```
**설명:**
- `ls -la`: 파일 상세 정보 출력
  - `-l`: Long format (상세 정보)
  - `-a`: All (숨김 파일 포함)
- `2>/dev/null`: 에러 메시지 숨김
  - `2`: 표준 에러 (stderr)
  - `>`: 리다이렉트
  - `/dev/null`: 휴지통 (버림)
- `head -5`: 처음 5줄만 출력

---

### ⏰ 시간 지연 명령어

#### 8. 대기 명령어

```bash
sleep 5
```
**설명:**
- 5초 동안 대기
- 서버가 재시작될 때까지 기다릴 때 사용

```bash
sleep 5 && curl "http://..."
```
**설명:**
- 5초 기다린 후 API 요청
- Tomcat이 배포를 완료할 시간을 주기 위함

---

### 🔧 복합 명령어 예시

#### 9. 실전에서 자주 쓰는 조합

```bash
cd /Users/yunsu-in/Downloads/EJ2/backend && mvn clean package -DskipTests && cp target/ej2.war /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/webapps/ && sleep 8 && curl -s "http://localhost:8080/ej2/api/timetable?semester=spring&year=2026&userId=1"
```

**단계별 분해:**
```bash
# 1단계: 프로젝트 디렉토리 이동
cd /Users/yunsu-in/Downloads/EJ2/backend

# 2단계: 빌드
mvn clean package -DskipTests

# 3단계: 배포
cp target/ej2.war /opt/.../webapps/

# 4단계: 대기 (배포 완료될 때까지)
sleep 8

# 5단계: API 테스트
curl -s "http://localhost:8080/ej2/api/timetable?..."
```

---

### 📖 명령어 연결 연산자

#### `&&` (AND)
```bash
command1 && command2
```
- command1이 **성공**하면 command2 실행
- 하나라도 실패하면 중단

**예시:**
```bash
mvn clean package && echo "빌드 성공!"
# 빌드 성공 시에만 "빌드 성공!" 출력
```

---

#### `;` (세미콜론)
```bash
command1 ; command2
```
- command1 실행 후 **무조건** command2 실행
- 성공/실패 관계없이 계속 진행

**예시:**
```bash
mvn clean ; ls target
# 빌드 실패해도 ls 실행
```

---

#### `|` (파이프)
```bash
command1 | command2
```
- command1의 **출력**을 command2의 **입력**으로

**예시:**
```bash
cat file.txt | grep "error" | wc -l
# file.txt에서 "error" 단어가 몇 번 나오는지 세기
```

---

#### `>` (리다이렉트)
```bash
command > file.txt
```
- 출력을 파일로 저장 (덮어쓰기)

```bash
command >> file.txt
```
- 출력을 파일 끝에 추가

**예시:**
```bash
echo "Log entry" >> app.log
# app.log 파일 끝에 추가
```

---

### 🎯 자주 쓰는 명령어 치트시트

| 명령어 | 설명 | 예시 |
|--------|------|------|
| `cd` | 디렉토리 이동 | `cd /path/to/project` |
| `pwd` | 현재 위치 출력 | `pwd` |
| `ls` | 파일 목록 | `ls -la` |
| `cat` | 파일 내용 출력 | `cat file.txt` |
| `grep` | 패턴 검색 | `grep "error" log.txt` |
| `tail` | 파일 끝부분 출력 | `tail -f catalina.out` |
| `head` | 파일 앞부분 출력 | `head -20 file.txt` |
| `cp` | 복사 | `cp src dest` |
| `mv` | 이동/이름 변경 | `mv old.txt new.txt` |
| `rm` | 삭제 | `rm file.txt` |
| `mkdir` | 디렉토리 생성 | `mkdir newfolder` |
| `ps` | 프로세스 목록 | `ps aux` |
| `kill` | 프로세스 종료 | `kill 1234` |
| `curl` | HTTP 요청 | `curl http://api.com` |

---

### 💡 명령어 사용 팁

#### 1. 명령어 히스토리
```bash
history           # 이전에 실행한 명령어 목록
!123              # 123번 명령어 다시 실행
!!                # 직전 명령어 반복
Ctrl + R          # 명령어 검색 (터미널에서)
```

#### 2. 자동완성
```bash
cd /Users/y<TAB>  # Tab 키로 자동완성
```

#### 3. 경로 단축키
```bash
~     # 홈 디렉토리 (/Users/username)
.     # 현재 디렉토리
..    # 상위 디렉토리
-     # 이전 디렉토리

cd ~           # 홈으로 이동
cd ..          # 상위로 이동
cd -           # 이전 위치로 이동
```

#### 4. 명령어 도움말
```bash
man command    # 매뉴얼 보기 (q로 종료)
command --help # 간단한 도움말
```

---

### ⚠️ 주의사항

#### 위험한 명령어
```bash
rm -rf /       # ❌ 절대 실행하지 마세요! (모든 파일 삭제)
chmod 777 -R / # ❌ 모든 파일 권한 변경 (보안 위험)
> /dev/sda     # ❌ 하드디스크 초기화
```

#### 안전한 습관
```bash
# 1. 삭제 전 확인
ls file.txt     # 파일 확인
rm file.txt     # 삭제

# 2. 백업 만들기
cp important.txt important.txt.backup

# 3. 위험한 명령어는 먼저 테스트
ls file.txt     # 영향받을 파일 확인
# rm file.txt   # 확인 후 주석 제거하고 실행
```

---

### 📚 더 공부하면 좋은 명령어

#### 초급
- `echo`: 텍스트 출력
- `which`: 명령어 위치 찾기
- `whoami`: 현재 사용자 확인
- `date`: 현재 날짜/시간

#### 중급
- `awk`: 텍스트 처리
- `sed`: 스트림 편집
- `xargs`: 명령어 인자 전달
- `tar`: 압축/해제

#### 고급
- `rsync`: 동기화/백업
- `ssh`: 원격 접속
- `screen`/`tmux`: 터미널 멀티플렉서
- `cron`: 작업 스케줄링

---

이 부록이 Bash 명령어를 이해하는 데 도움이 되었기를 바랍니다! 🚀

**Happy Coding! 🚀**

---

*이 문서는 EJ2 프로젝트의 실제 에러를 해결한 경험을 바탕으로 작성되었습니다.*
*작성일: 2026-01-18*
*버전: 2.0*
