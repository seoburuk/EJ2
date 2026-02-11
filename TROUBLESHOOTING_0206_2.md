# Troubleshooting Guide 0206_2 - 게시글 조회수 증가 시 500 에러 (NonUniqueResultException)

## 문제 설명

**증상**: 게시글 상세 페이지 접속 시 브라우저 콘솔에 다음 에러 발생

```
POST http://localhost:8080/api/posts/27/view?userId=14 500 (Internal Server Error)
閲覧数の更新に失敗しました: AxiosError {message: 'Request failed with status code 500', ...}
```

**서버 에러 메시지**:
```
NonUniqueResultException: query did not return a unique result: 2
```

**원인**: `PostViewLogRepository`의 `findByPostIdAndUserIdAndViewedAtAfter()` 메서드가 `Optional<PostViewLog>`를 반환하도록 되어 있었는데, `post_view_logs` 테이블에 동일한 `postId + userId` 조합의 레코드가 2개 이상 존재하여 Spring Data JPA가 단일 결과를 기대했지만 복수 결과가 반환되면서 예외 발생

### 에러 발생 흐름

```
1. 사용자가 게시글 상세 페이지 접속
2. PostDetailPage.js에서 incrementViewCount() 호출
3. POST /api/posts/{id}/view?userId={userId} 요청
4. PostService.incrementViewCount() 실행
5. postViewLogRepository.findByPostIdAndUserIdAndViewedAtAfter() 호출
6. DB에 동일 조건의 레코드가 2개 이상 존재
7. Optional은 단일 결과만 허용하므로 NonUniqueResultException 발생
8. 500 Internal Server Error 반환
```

### 문제 코드 (수정 전)

**PostViewLogRepository.java**:
```java
// Optional은 결과가 0 또는 1개일 때만 사용 가능
Optional<PostViewLog> findByPostIdAndUserIdAndViewedAtAfter(
    Long postId, Long userId, LocalDateTime after
);
```

**PostService.java**:
```java
Optional<PostViewLog> userViewLog = postViewLogRepository
        .findByPostIdAndUserIdAndViewedAtAfter(postId, userId, oneDayAgo);
hasViewed = userViewLog.isPresent();  // 레코드가 2개 이상이면 여기서 예외 발생
```

---

## 해결 방법

Repository의 반환 타입을 `Optional` → `List`로 변경하고, Service에서 `isPresent()` → `!isEmpty()`로 변경

### 1. PostViewLogRepository 수정

**파일**: `backend/src/main/java/com/ej2/repository/PostViewLogRepository.java`

```java
// 수정 전: Optional<PostViewLog>
// 수정 후: List<PostViewLog>

// 로그인한 사용자가 최근(24시간 이내) 이 게시글을 조회했는지 확인
// List로 반환하여 중복 레코드 발생 시 NonUniqueResultException 방지
List<PostViewLog> findByPostIdAndUserIdAndViewedAtAfter(
    Long postId, Long userId, LocalDateTime after
);

// IP 주소 기반으로 비로그인 사용자가 최근 이 게시글을 조회했는지 확인
List<PostViewLog> findByPostIdAndIpAddressAndViewedAtAfter(
    Long postId, String ipAddress, LocalDateTime after
);
```

### 2. PostLikeLogRepository 수정

**파일**: `backend/src/main/java/com/ej2/repository/PostLikeLogRepository.java`

```java
// 로그인한 사용자가 최근 이 게시글에 좋아요를 눌렀는지 확인
List<PostLikeLog> findByPostIdAndUserIdAndLikedAtAfter(
    Long postId, Long userId, LocalDateTime after
);

// IP 주소 기반으로 비로그인 사용자가 최근 좋아요를 눌렀는지 확인
List<PostLikeLog> findByPostIdAndIpAddressAndLikedAtAfter(
    Long postId, String ipAddress, LocalDateTime after
);
```

### 3. PostDislikeLogRepository 수정

**파일**: `backend/src/main/java/com/ej2/repository/PostDislikeLogRepository.java`

```java
// 로그인한 사용자가 최근 이 게시글에 싫어요를 눌렀는지 확인
List<PostDislikeLog> findByPostIdAndUserIdAndDislikedAtAfter(
    Long postId, Long userId, LocalDateTime after
);

// IP 주소 기반으로 비로그인 사용자가 최근 싫어요를 눌렀는지 확인
List<PostDislikeLog> findByPostIdAndIpAddressAndDislikedAtAfter(
    Long postId, String ipAddress, LocalDateTime after
);
```

### 4. PostService 수정

**파일**: `backend/src/main/java/com/ej2/service/PostService.java`

조회수, 좋아요, 싫어요 모두 동일한 패턴으로 변경:

```java
// 수정 전
Optional<PostViewLog> userViewLog = postViewLogRepository
        .findByPostIdAndUserIdAndViewedAtAfter(postId, userId, oneDayAgo);
hasViewed = userViewLog.isPresent();

// 수정 후
List<PostViewLog> userViewLogs = postViewLogRepository
        .findByPostIdAndUserIdAndViewedAtAfter(postId, userId, oneDayAgo);
hasViewed = !userViewLogs.isEmpty();
```

---

## 변경된 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `backend/.../repository/PostViewLogRepository.java` | `Optional` → `List` 반환 타입 변경, 한국어 주석 추가 |
| `backend/.../repository/PostLikeLogRepository.java` | `Optional` → `List` 반환 타입 변경, 한국어 주석 추가 |
| `backend/.../repository/PostDislikeLogRepository.java` | `Optional` → `List` 반환 타입 변경, 한국어 주석 추가 |
| `backend/.../service/PostService.java` | `isPresent()` → `!isEmpty()` 변경, 한국어 주석 추가 |

---

## 테스트 방법

1. Docker 재빌드: `docker-compose up --build backend -d`
2. 서버 기동 대기 (약 15초)
3. curl로 확인:
   ```bash
   curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8080/api/posts/27/view?userId=14"
   # 기대 결과: 200
   ```
4. 브라우저에서 게시글 상세 페이지 접속 후 콘솔 에러 없음 확인

---

## 핵심 포인트

Spring Data JPA에서 `Optional` 반환 타입은 **결과가 반드시 0 또는 1개**임을 보장할 때만 사용해야 한다. DB에 unique constraint가 없는 경우, race condition이나 중복 데이터로 인해 복수 결과가 반환될 수 있으므로 `List`로 받아서 `isEmpty()`로 확인하는 것이 안전하다.

| 반환 타입 | 결과 0개 | 결과 1개 | 결과 2개 이상 |
|-----------|----------|----------|---------------|
| `Optional<T>` | `Optional.empty()` | `Optional.of(result)` | **NonUniqueResultException 발생** |
| `List<T>` | 빈 리스트 | 요소 1개 리스트 | 요소 N개 리스트 (정상 동작) |
