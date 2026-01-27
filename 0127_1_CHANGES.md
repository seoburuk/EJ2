# 0127_1 변경 이력 - 시간표 접근 제한 (본인만 편집 가능)

## 뭘 바꿨나?

**변경 전**: 로그인하면 모든 사용자의 시간표를 볼 수 있고 수정도 가능했음 (드롭다운으로 선택)
**변경 후**: 로그인한 사용자 본인의 시간표만 보이고, 본인 것만 편집 가능하게 변경

---

## 변경한 파일 목록 (8개)

| # | 파일 | 뭘 바꿨나? |
|---|------|-----------|
| 1 | `backend/.../config/WebConfig.java` | CORS 설정에 Cookie 허용 추가 |
| 2 | `backend/.../controller/AuthController.java` | 로그인 시 세션 저장, 로그아웃·사용자 확인 API 추가 |
| 3 | `backend/.../controller/TimetableController.java` | 모든 API에 로그인 체크 & 권한 체크 추가 |
| 4 | `backend/.../controller/UserController.java` | CORS 설정 수정 |
| 5 | `backend/.../service/AuthService.java` | 더미 패스워드 해시 버그 수정 |
| 6 | `backend/.../service/TimetableService.java` | 소유권 체크 메소드 추가 |
| 7 | `frontend/.../Auth/LoginPage.js` | 로그인 시 Cookie 전송 추가 |
| 8 | `frontend/.../Timetable/TimetablePage.tsx` | 사용자 선택 삭제, 로그인 체크, 에러 처리 추가 |

---

## 각 파일 상세 설명

### 1. WebConfig.java (백엔드 설정)

**위치**: `backend/src/main/java/com/ej2/config/WebConfig.java`

**변경 전**:
```java
registry.addMapping("/**")
        .allowedOrigins("*")                    // 모든 오리진 허용
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*");
```

**변경 후**:
```java
registry.addMapping("/**")
        .allowedOrigins("http://localhost:3000") // 구체적인 오리진 지정
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);                 // Cookie 전송 허용
```

**왜 바꿨나?**
- `withCredentials: true` (브라우저에서 Cookie를 보내는 설정)를 사용할 경우, `allowedOrigins("*")`는 사용 불가
- 구체적인 오리진 `http://localhost:3000`을 지정해야 함
- `allowCredentials(true)`로 세션 Cookie 전송을 허용

---

### 2. AuthController.java (인증 컨트롤러)

**위치**: `backend/src/main/java/com/ej2/controller/AuthController.java`

**추가한 기능**:

#### ① 로그인 시 세션에 저장
```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpSession session) {
    AuthResponse response = authService.login(request);
    if (response.isSuccess()) {
        // ★ 여기가 새로 추가됨: 세션에 사용자 정보를 저장
        session.setAttribute("userId", response.getUser().getId());
        session.setAttribute("user", response.getUser());
        return ResponseEntity.ok(response);
    }
    ...
}
```
- `HttpSession`은 Tomcat이 자동으로 관리하는 서버 측 저장소
- 로그인 성공 시 `userId`를 저장 → 이후 요청에서 사용자를 식별

#### ② 로그아웃 API (신규)
```java
@PostMapping("/logout")
public ResponseEntity<AuthResponse> logout(HttpSession session) {
    session.invalidate();  // 세션 파기
    return ResponseEntity.ok(new AuthResponse(true, "ログアウトしました"));
}
```

#### ③ 현재 로그인 사용자 확인 API (신규)
```java
@GetMapping("/me")
public ResponseEntity<AuthResponse> getCurrentUser(HttpSession session) {
    Object user = session.getAttribute("user");
    if (user != null) {
        return ResponseEntity.ok(new AuthResponse(true, "ログイン中", (User) user));
    }
    return ResponseEntity.status(401).body(new AuthResponse(false, "ログインしていません"));
}
```

**CORS 설정도 변경**:
```java
// 변경 전: @CrossOrigin(origins = "*")
// 변경 후:
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
```

---

### 3. TimetableController.java (시간표 컨트롤러) ★핵심 변경

**위치**: `backend/src/main/java/com/ej2/controller/TimetableController.java`

#### ① 세션에서 사용자 ID 가져오는 헬퍼 메소드 (신규)
```java
private Long getLoggedInUserId(HttpSession session) {
    Object userId = session.getAttribute("userId");
    return userId != null ? (Long) userId : null;
}
```

#### ② 시간표 조회 API (변경)

**변경 전**: URL 파라미터로 `userId`를 받음 (누구든 변경 가능! 위험!)
```java
public ResponseEntity<?> getTimetable(
    @RequestParam(defaultValue = "1") Long userId,  // ← 누구든 바꿀 수 있음
    @RequestParam Integer year,
    @RequestParam String semester)
```

**변경 후**: 세션에서 자동으로 가져옴 (안전)
```java
public ResponseEntity<?> getTimetable(
    @RequestParam Integer year,
    @RequestParam String semester,
    HttpSession session) {               // ← 세션에서 가져옴

    Long userId = getLoggedInUserId(session);
    if (userId == null) {
        return ResponseEntity.status(401).body("ログインが必要です");
    }
    ...
}
```

#### ③ 과목 추가 API (권한 체크 추가)
```java
public ResponseEntity<?> addCourse(..., HttpSession session) {
    Long userId = getLoggedInUserId(session);
    if (userId == null) {
        return ResponseEntity.status(401).body("ログインが必要です");  // 미로그인
    }

    Long timetableId = Long.valueOf(requestData.get("timetableId").toString());

    // ★ 권한 체크: 이 시간표의 소유자인지 확인
    if (!timetableService.isOwner(timetableId, userId)) {
        return ResponseEntity.status(403).body("この時間割を編集する権限がありません");
    }
    ...
}
```

#### ④ 과목 수정/삭제 API (권한 체크 추가)
```java
// 수정 API
if (!timetableService.isCourseOwner(courseId, userId)) {
    return ResponseEntity.status(403).body("この科目を編集する権限がありません");
}

// 삭제 API
if (!timetableService.isCourseOwner(courseId, userId)) {
    return ResponseEntity.status(403).body("この科目を削除する権限がありません");
}
```

**HTTP 상태 코드 의미**:
- `401` = 로그인 안 됨 (인증 에러)
- `403` = 로그인은 됐지만 권한 없음 (인가 에러)
- `400` = 요청 데이터가 이상함

---

### 4. UserController.java

**위치**: `backend/src/main/java/com/ej2/controller/UserController.java`

**변경**: CORS 설정만 수정
```java
// 변경 전: @CrossOrigin(origins = "*")
// 변경 후:
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
```

---

### 5. AuthService.java (버그 수정)

**위치**: `backend/src/main/java/com/ej2/service/AuthService.java`

**수정 내용**: 타이밍 공격 방지용 더미 패스워드 해시가 짧아서 생긴 버그

**변경 전** (버그):
```java
PasswordUtil.verifyPassword("dummy", "$2a$12$dummyhashfordemo");
// ↑ 29글자 → BCrypt는 60글자 필요 → StringIndexOutOfBoundsException 발생!
```

**변경 후** (수정):
```java
PasswordUtil.verifyPassword("dummy", "$2a$12$000000000000000000000uGTACuPSTOQRhMqaViHUXn4eOJyGkm");
// ↑ 60글자의 올바른 BCrypt 해시 형식
```

**이 코드의 목적**:
- 존재하지 않는 사용자명으로 로그인했을 때도, 패스워드 검증과 같은 시간이 걸리게 함
- 이게 없으면 "사용자명이 존재하는지"가 응답 시간 차이로 알 수 있음 (타이밍 공격)

---

### 6. TimetableService.java (소유권 체크 추가)

**위치**: `backend/src/main/java/com/ej2/service/TimetableService.java`

**추가한 메소드**:

```java
// 시간표의 소유자인지 체크
public boolean isOwner(Long timetableId, Long userId) {
    Timetable timetable = timetableRepository.findById(timetableId);
    return timetable != null && timetable.getUserId().equals(userId);
}

// 과목의 소유자인지 체크 (과목 → 시간표 → 사용자를 따라감)
public boolean isCourseOwner(Long courseId, Long userId) {
    TimetableCourse course = courseRepository.findById(courseId);
    if (course == null || course.getTimetable() == null) {
        return false;
    }
    return course.getTimetable().getUserId().equals(userId);
}
```

**동작 흐름**:
```
과목 삭제 요청(courseId=5)
  → courseRepository.findById(5) 로 과목 조회
  → course.getTimetable() 로 소속 시간표 조회
  → timetable.getUserId() 가 로그인 사용자와 일치하는지 확인
  → 일치하지 않으면 403 에러
```

---

### 7. LoginPage.js (프론트엔드)

**위치**: `frontend/src/pages/Auth/LoginPage.js`

**변경**: 로그인 요청에 Cookie 전송 설정 추가

```javascript
// 변경 전:
const response = await axios.post('/api/auth/login', { ... });

// 변경 후:
const response = await axios.post('/api/auth/login', { ... }, { withCredentials: true });
```

**`withCredentials: true`란?**
- 브라우저가 Cookie(JSESSIONID)를 함께 보내는 설정
- 이게 없으면 서버가 세션을 식별할 수 없음

---

### 8. TimetablePage.tsx (프론트엔드) ★핵심 변경

**위치**: `frontend/src/pages/Timetable/TimetablePage.tsx`

#### ① 사용자 선택 드롭다운 삭제

**변경 전**: 모든 사용자를 드롭다운에서 선택 가능
```tsx
const [selectedUserId, setSelectedUserId] = useState(1);
const [users, setUsers] = useState<any[]>([]);

// 드롭다운으로 사용자 선택
<select value={selectedUserId} onChange={...}>
  {users.map(user => <option ...>{user.name}</option>)}
</select>
```

**변경 후**: 로그인 사용자 이름만 표시
```tsx
const [currentUser, setCurrentUser] = useState<User | null>(null);

// 이름만 표시
<span className="current-user-label">
  {currentUser.name}
</span>
```

#### ② 로그인 체크 (신규)
```tsx
const checkLogin = () => {
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    setCurrentUser(JSON.parse(storedUser));
  } else {
    // 로그인 안 했으면 로그인 페이지로 이동
    navigate('/login', {
      state: { from: '/timetable', message: '시간표를 보려면 로그인이 필요합니다.' }
    });
  }
};
```

#### ③ API 요청에 Cookie 추가
```tsx
// 모든 API 요청에 withCredentials: true 추가
const response = await axios.get('/api/timetable', {
  params: { semester, year },
  withCredentials: true    // ← 세션 Cookie 전송
});
```

#### ④ 에러 핸들링
```tsx
} catch (error: any) {
  const status = error.response?.status;
  if (status === 401) {
    // 세션 만료 → 로그인 페이지로
    localStorage.removeItem('user');
    navigate('/login', { state: { message: '세션이 만료되었습니다. 다시 로그인해주세요.' } });
  } else if (status === 403) {
    // 권한 없음
    alert('이 작업을 수행할 권한이 없습니다.');
  } else {
    alert(error.response?.data || '저장에 실패했습니다');
  }
}
```

---

## 보안 구조 (전체 흐름)

```
브라우저                     Nginx                  Tomcat (백엔드)
  │                          │                         │
  │ 1. 로그인                │                         │
  │ POST /api/auth/login ──→│──→ POST /api/auth/login │
  │                          │                         │
  │ 2. 세션 Cookie 반환      │                         │
  │ ←── Set-Cookie: ─────── │←── Set-Cookie: JSESSION │
  │     JSESSIONID=xxx       │    ID=xxx               │
  │                          │                         │
  │ 3. 시간표 조회            │                         │
  │ GET /api/timetable ────→│──→ 세션에서 userId       │
  │ Cookie: JSESSIONID=xxx   │    가져옴 → 본인 시간표  │
  │                          │    만 반환               │
  │ ←── 본인 시간표 데이터 ──│←──                      │
  │                          │                         │
  │ 4. 남의 시간표 편집 시도  │                         │
  │ POST /api/timetable/    │──→ isOwner(timetableId,  │
  │ course                   │    userId) → false!      │
  │ ←── 403 Forbidden ──────│←── 권한 없음!            │
```

**3단계 보안**:
1. **프론트엔드**: 미로그인 → 로그인 페이지로 리다이렉트
2. **백엔드 인증**: 세션 없음 → 401 에러
3. **백엔드 인가**: 남의 시간표 → 403 에러

---

## 트러블슈팅

### 증상 1: 시간표 페이지가 401 에러로 로그인 페이지로 이동됨

**원인**: 서버 세션이 만료됨 (Docker 재빌드 후 등)

**해결 방법**:
1. 브라우저에서 로그아웃
2. 다시 로그인
3. 그래도 안 되면: 브라우저 DevTools → Application → Local Storage → `user` 삭제 → 재로그인

### 증상 2: 로그인 시 500 에러

**원인**: 존재하지 않는 사용자명으로 로그인 시 발생하던 버그 (수정 완료)

**확인 방법**:
```bash
docker-compose logs --tail=50 backend | grep -i error
```

### 증상 3: 변경이 반영되지 않음 (이전 UI가 표시됨)

**원인**: Docker 캐시

**해결 방법**:
```bash
# 프론트엔드 & 백엔드 재빌드
docker-compose up --build -d frontend backend

# 브라우저 하드 리프레시
# Mac: Cmd + Shift + R
# Windows: Ctrl + Shift + R
```

### 증상 4: 콘솔에 CORS 에러가 나옴

**원인**: `@CrossOrigin(origins = "*")`가 남아있음 (`withCredentials: true`와 호환 불가)

**확인 방법**: 모든 컨트롤러의 CORS 설정 확인
```bash
grep -r "@CrossOrigin" backend/src/
```

전부 `@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")` 이면 OK

### 증상 5: 과목 추가에서 "권한이 없습니다" 에러

**원인**: 다른 사용자의 시간표에 과목을 추가하려고 함

**확인 방법**:
```bash
# DB에서 시간표 소유자 확인
docker exec mariadb mysql -u appuser -papppassword \
  -e "SELECT timetable_id, user_id FROM appdb.timetables;"
```

---

## 테스트 계정 정보

```bash
# DB에서 확인하는 명령어
docker exec mariadb mysql -u appuser -papppassword \
  -e "SELECT id, username, name, email FROM appdb.users;"
```

| ID | username | name | email | password |
|----|----------|------|-------|----------|
| 6 | newuser1 | sfdsds | newuser@test.com | password123 |

---

## 동작 확인 명령어

```bash
# 1. 로그인 테스트 (세션 Cookie 획득)
curl -s -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser1","password":"password123"}' \
  -c /tmp/cookies.txt

# 2. 본인 시간표 조회 (세션 포함)
curl -s -b /tmp/cookies.txt \
  "http://localhost:3000/api/timetable?year=2026&semester=spring"

# 3. 남의 시간표에 부정 접근 테스트 (403이 돌아오면 OK)
curl -s -b /tmp/cookies.txt -X POST \
  "http://localhost:3000/api/timetable/course" \
  -H "Content-Type: application/json" \
  -d '{"timetableId":1,"courseName":"부정과목","daySchedules":[{"day":1,"periodStart":1,"periodEnd":2}]}'

# 4. 로그아웃 테스트
curl -s -b /tmp/cookies.txt -X POST \
  "http://localhost:3000/api/auth/logout"
```

---

## 용어 설명

| 용어 | 의미 |
|------|------|
| **HttpSession** | 서버 측에서 사용자 정보를 저장하는 구조. Tomcat이 자동 관리 |
| **JSESSIONID** | 세션을 식별하는 Cookie 이름. Tomcat이 자동으로 발행 |
| **withCredentials** | 브라우저가 Cookie를 요청에 포함하는 설정 |
| **CORS** | 다른 도메인 간 통신을 허용하는 구조 (Cross-Origin Resource Sharing) |
| **401 Unauthorized** | 로그인 안 됨 (인증 에러) |
| **403 Forbidden** | 로그인은 됐지만 권한 없음 (인가 에러) |
| **인증 (Authentication)** | "너는 누구야?" 를 확인하는 것 |
| **인가 (Authorization)** | "너는 뭘 할 수 있어?" 를 확인하는 것 |
