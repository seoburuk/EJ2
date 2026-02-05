# 0205_1 트러블슈팅 및 학습내용 정리

## 개요
2026년 2월 5일에 진행한 버그 수정 및 기능 추가 과정을 정리한 문서입니다.

---

## 1. 코드 버그 수정

### 1.1 Comment.java - 변수명 수정 (`re` → `refreshUpdatedAt`)

**문제**
- 좋아요 버튼 클릭 시 "수정됨"이 표시됨
- 원인: `@PreUpdate`가 `save()` 호출마다 실행되어 `updatedAt`이 갱신됨

**수정 전**
```java
@Transient
private boolean re=true;

@PreUpdate
protected void onUpdate() {
    if(re){
        updatedAt = LocalDateTime.now();
    }
    re=true;
}

public void setLikeCount(Integer likeCount) {
    this.likeCount = likeCount;
    this.re=false;
}
```

**수정 후**
```java
@Transient
private boolean refreshUpdatedAt = true;

@PreUpdate
protected void onUpdate() {
    if (refreshUpdatedAt) {
        updatedAt = LocalDateTime.now();
    }
    refreshUpdatedAt = true;
}

public void setLikeCount(Integer likeCount) {
    this.likeCount = likeCount;
    this.refreshUpdatedAt = false;
}
```

**학습 포인트**
- `@Transient` 필드는 DB에 저장되지 않아 엔티티 상태 관리에 활용 가능
- 변수명은 의미가 명확해야 함 (`re`보다 `refreshUpdatedAt`)
- JPA 라이프사이클 훅(`@PreUpdate`) 동작 이해가 중요

---

### 1.2 PostService.java - Repository 호출 수정

**문제**
- 주간/월간 좋아요 순 정렬이 제대로 동작하지 않음
- 원인: 복사-붙여넣기 실수로 모두 `findAllOrderByDayLikeCount()` 호출

**수정 전**
```java
public List<PostDTO> getAllOrderByWeekLikeCount(Long boardId) {
    List<Post> posts = postRepository.findAllOrderByDayLikeCount(boardId);  // 오류
    return convertToPostDTOList(posts);
}

public List<PostDTO> getAllOrderByMonthLikeCount(Long boardId) {
    List<Post> posts = postRepository.findAllOrderByDayLikeCount(boardId);  // 오류
    return convertToPostDTOList(posts);
}
```

**수정 후**
```java
public List<PostDTO> getAllOrderByWeekLikeCount(Long boardId) {
    List<Post> posts = postRepository.findAllOrderByWeekLikeCount(boardId);
    return convertToPostDTOList(posts);
}

public List<PostDTO> getAllOrderByMonthLikeCount(Long boardId) {
    List<Post> posts = postRepository.findAllOrderByMonthLikeCount(boardId);
    return convertToPostDTOList(posts);
}
```

**학습 포인트**
- 복사-붙여넣기 후 반드시 내부 호출도 확인할 것
- IDE의 "Find Usages" 기능으로 메서드가 올바르게 사용되는지 검증

---

### 1.3 PostController.java - dislike 엔드포인트 추가

**문제**
- 싫어요 버튼이 동작하지 않음
- 원인: Service에 메서드는 있지만 Controller에 엔드포인트가 없음

**추가 코드**
```java
// POST /api/posts/{id}/dislike - Increment dislike count with IP tracking
@PostMapping("/{id}/dislike")
public ResponseEntity<Void> incrementDislikeCount(
        @PathVariable Long id,
        @RequestParam(required = false) Long userId,
        HttpServletRequest request) {

    String ipAddress = getClientIpAddress(request);
    postService.incrementDislikeCount(id, userId, ipAddress);
    return ResponseEntity.ok().build();
}
```

**학습 포인트**
- REST API 설계 시 대칭성 유지 (like ↔ dislike)
- Service와 Controller 구현이 맞춰져 있는지 확인

---

## 2. 확인필요 이슈 검토

### 2.1 no.18: 유저 추가 기능 불량

**문제 분석**
- `UsersPage.js`가 `name`, `email`만 수집
- `User` 엔티티는 `username`, `password`가 필수(NOT NULL)

**해결책**
- `/api/auth/register` API를 사용하도록 변경
- 폼에 `username`, `password` 필드 추가

**수정 파일**: `frontend/src/pages/Users/UsersPage.js`

---

### 2.2 no.30: 채팅 익명 닉네임 충돌

**문제 분석**
Race Condition으로 인한 동시 접속 시 문제:
1. 같은 닉네임이 여러 사용자에게 할당됨
2. 유저 수 카운트 불일치
3. 연타 시 카운트 증가

**해결책**: 낙관적 잠금(Optimistic Locking) 구현

**ChatRoom.java - @Version 필드 추가**
```java
@Version
@Column(name = "version")
private Long version;
```

**ChatService.java - 재시도 로직 추가**
```java
public String assignNickname(Long roomId) {
    int maxRetries = 3;
    for (int attempt = 0; attempt < maxRetries; attempt++) {
        try {
            return assignNicknameInternal(roomId);
        } catch (OptimisticLockException e) {
            if (attempt == maxRetries - 1) {
                throw new RuntimeException("닉네임 할당 실패. 다시 시도해주세요.", e);
            }
        }
    }
    return "匿名0";
}
```

**학습 포인트**
- **낙관적 잠금**: `@Version` 필드로 자동 버전 관리
- 동시 수정 시 `OptimisticLockException` 발생
- 재시도 로직으로 충돌 해결 (최대 3회)
- 비관적 잠금보다 성능이 좋고 웹 애플리케이션에 적합

---

### 2.3 no.42: Spring Security 인증 URL 정의

**수정 전 상태**
- `POST /api/posts/**`만 인증 필요
- 나머지는 `anyRequest().permitAll()`로 전체 공개

**수정 후 SecurityConfig.java**
```java
.authorizeHttpRequests(authz -> authz
    .antMatchers(OPTIONS, "/**").permitAll()
    .antMatchers("/", "/resources/**", "/api/auth/**").permitAll()

    // 공개 API (인증 불필요)
    .antMatchers(GET, "/api/posts/**").permitAll()
    .antMatchers(GET, "/api/comments/**").permitAll()
    .antMatchers(GET, "/api/boards/**").permitAll()
    .antMatchers(GET, "/api/users").permitAll()
    .antMatchers(GET, "/api/chat/**").permitAll()
    .antMatchers("/ws/**").permitAll()

    // 인증 필요 API - 게시글 관련
    .antMatchers(POST, "/api/posts").authenticated()
    .antMatchers(POST, "/api/posts/*/view").permitAll()
    .antMatchers(POST, "/api/posts/*/like").authenticated()
    .antMatchers(POST, "/api/posts/*/dislike").authenticated()
    .antMatchers(PUT, "/api/posts/**").authenticated()
    .antMatchers(DELETE, "/api/posts/**").authenticated()

    // 인증 필요 API - 댓글 관련
    .antMatchers(POST, "/api/comments").authenticated()
    .antMatchers(POST, "/api/comments/*/like").authenticated()
    .antMatchers(PUT, "/api/comments/**").authenticated()
    .antMatchers(DELETE, "/api/comments/**").authenticated()

    // 인증 필요 API - 신고
    .antMatchers(POST, "/api/reports").authenticated()

    // 관리자 전용 API
    .antMatchers("/api/admin/**").authenticated()

    .anyRequest().permitAll()
)
```

**학습 포인트**
- HTTP 메서드별로 접근 제어 설정 가능
- `antMatchers` 순서가 중요 (먼저 매칭된 규칙 적용)
- 개발 시 `anyRequest().permitAll()`, 운영 환경에서는 명시적 제한

---

### 2.4 no.49: 서버/DB 시간 설정

**확인 결과**: docker-compose.yml에서 설정 완료

```yaml
services:
  mariadb:
    environment:
      TZ: Asia/Seoul

  backend:
    environment:
      TZ: Asia/Seoul
```

---

### 2.5 no.76: SUPER_ADMIN 권한 추가

**권한 체계**
```
SUPER_ADMIN (👑)
├── 게시판 관리 (CRUD)
├── 모든 유저 권한 변경
└── 신고/유저 관리

ADMIN
├── USER 권한 변경만 가능
└── 신고/유저 관리 (ADMIN/SUPER_ADMIN 제외)

USER
└── 일반 사용자 기능
```

**수정 파일**

1. **User.java** - 기본 권한을 ADMIN으로 변경
```java
private String role = "ADMIN";  // SUPER_ADMIN, ADMIN, USER
```

2. **AdminController.java** - 권한 검증 메서드 추가
```java
private boolean isAdmin(HttpSession session) {
    User currentUser = (User) session.getAttribute("user");
    String role = currentUser.getRole();
    return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
}

private boolean isSuperAdmin(HttpSession session) {
    User currentUser = (User) session.getAttribute("user");
    return "SUPER_ADMIN".equals(currentUser.getRole());
}
```

3. **게시판 관리 API** - SUPER_ADMIN 전용으로 변경
```java
@GetMapping("/boards")
public ResponseEntity<?> getAllBoards(HttpSession session) {
    ResponseEntity<?> accessCheck = checkSuperAdminAccess(session);
    if (accessCheck != null) return accessCheck;
    // ...
}
```

4. **유저 권한 변경** - 단계적 권한 제어
```java
// SUPER_ADMIN 권한은 SUPER_ADMIN만 부여 가능
if (newRole.equals("SUPER_ADMIN") && !isSuperAdmin(session)) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}

// ADMIN은 USER만 변경 가능
if (!isSuperAdmin(session)) {
    String targetRole = targetUser.getRole();
    if ("ADMIN".equals(targetRole) || "SUPER_ADMIN".equals(targetRole)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
```

**학습 포인트**
- 최소 권한의 원칙(Principle of Least Privilege)
- 권한 체크는 Controller 레벨에서 구현
- 단계적 권한 에스컬레이션 방지

---

## 3. 수정 파일 목록

| 파일 | 수정 내용 |
|------|----------|
| `Comment.java` | `re` → `refreshUpdatedAt` 변수명 수정 |
| `PostService.java` | week/month Repository 호출 수정 |
| `PostController.java` | dislike 엔드포인트 추가 |
| `UsersPage.js` | username/password 필드 추가, API 엔드포인트 변경 |
| `ChatRoom.java` | `@Version` 필드 추가 |
| `ChatService.java` | 낙관적 잠금 + 재시도 로직 추가 |
| `SecurityConfig.java` | 인증 필요 URL 정의 |
| `User.java` | 기본 권한을 ADMIN으로 변경 |
| `AdminController.java` | SUPER_ADMIN 권한 검증 추가, 게시판 관리 API 제한 |
| `AdminService.java` | `getUserById()` 메서드 추가 |
| `App.js` | SUPER_ADMIN용 관리자 메뉴 표시 |
| `AdminUsersPage.js` | SUPER_ADMIN 선택 옵션 추가 |

---

## 4. 권한별 기능 요약

| 기능 | SUPER_ADMIN | ADMIN | USER |
|------|-------------|-------|------|
| 게시판 관리 | ✅ | ❌ | ❌ |
| USER 권한 변경 | ✅ | ✅ | ❌ |
| ADMIN 권한 변경 | ✅ | ❌ | ❌ |
| SUPER_ADMIN 권한 부여 | ✅ | ❌ | ❌ |
| 신고 관리 | ✅ | ✅ | ❌ |
| 유저 정지/해제 | ✅ | ✅ | ❌ |

---

## 5. 향후 검토 사항

1. **CSRF 보호**: 현재 개발용으로 비활성화. 운영 환경에서는 활성화 필요
2. **비밀번호 정책**: 현재 6자 이상만 검사. 더 엄격한 정책 검토
3. **감사 로그**: 관리자 작업 이력 기록 기능
4. **Rate Limiting**: API 호출 횟수 제한으로 DDoS 대응

---

## 6. 참고 명령어

```bash
# 백엔드 빌드
cd backend && mvn compile -q

# Docker 실행
docker-compose up --build

# MariaDB 접속
docker exec -it mariadb mysql -u appuser -papppassword appdb

# 관리자 유저 생성 SQL
UPDATE users SET role = 'SUPER_ADMIN' WHERE username = 'your_username';
```

---

*작성일: 2026-02-05*
