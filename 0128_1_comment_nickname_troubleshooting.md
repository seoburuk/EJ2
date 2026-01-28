# 댓글 닉네임 버그 수정 - 트러블슈팅 & 학습 가이드 (0128_1)

## 목차
1. [문제 개요](#문제-개요)
2. [원인 분석: 왜 "user 6"이 표시되었나?](#원인-분석-왜-user-6이-표시되었나)
3. [해결 방법: DTO 패턴 적용](#해결-방법-dto-패턴-적용)
4. [수정한 파일 전체 정리](#수정한-파일-전체-정리)
5. [Docker 트러블슈팅](#docker-트러블슈팅)
6. [디버깅 방법: curl로 API 테스트](#디버깅-방법-curl로-api-테스트)
7. [핵심 인사이트](#핵심-인사이트)
8. [Bash 명령어 설명](#bash-명령어-설명)
9. [TODO(human) 학습 정리](#todohuman-학습-정리)

---

## 문제 개요

### 증상
게시판에서 댓글을 작성하면, 작성자 이름이 **닉네임(예: 佐藤花子)** 대신 **"ユーザー6"** (user 6)처럼 userId 숫자로 표시됨.

### 영향 범위
- 일반 게시판의 모든 댓글
- 댓글의 답글(reply)도 동일한 문제

### 기대 동작
- 일반 게시판: 사용자의 실제 닉네임(name 필드) 표시
- 익명 게시판: 익명 ID 표시 (이 부분은 정상 동작하고 있었음)

---

## 원인 분석: 왜 "user 6"이 표시되었나?

### 핵심 원인: 게시글(Post)과 댓글(Comment)의 처리 방식 차이

게시글은 잘 되는데 댓글만 안 되는 이유를 비교해보면:

```
[게시글 - 정상 동작]
DB에서 Post 조회 → PostService에서 User 조회 → PostDTO에 authorNickname 담기 → 프론트엔드에서 표시

[댓글 - 문제 발생]
DB에서 Comment 조회 → 그대로 반환 (User 조회 없음!) → 프론트엔드에 userId만 전달
```

### 문제의 흐름을 단계별로 보면

**1단계 - 백엔드 (CommentService.java) - 수정 전:**
```java
// userId만 포함된 Comment 엔티티를 그대로 반환
public List<Comment> getCommentsByPostId(Long postId) {
    return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
}
```
→ Comment 엔티티에는 `userId: 6`만 있고, 닉네임 정보가 없음

**2단계 - 백엔드 (CommentController.java) - 수정 전:**
```java
// Comment를 그대로 JSON으로 변환해서 반환
public ResponseEntity<List<Comment>> getCommentsByPostId(...) {
    List<Comment> comments = commentService.getCommentsByPostId(postId);
    return ResponseEntity.ok(comments);  // userId만 포함된 응답
}
```
→ API 응답에 `authorNickname` 필드 자체가 없음

**3단계 - 프론트엔드 (CommentSection.js) - 수정 전:**
```javascript
// 닉네임이 없으니 userId로 임시 표시
{isAnonymous ? comment.anonymousId || '匿名' : `ユーザー${comment.userId}`}
//                                                ^^^^^^^^^^^^^^^^^^^^^^^^
//                                                이 부분이 "ユーザー6"을 만듦
```

### 반면 게시글(Post)이 정상이었던 이유

PostService에는 이미 User를 조회하는 로직이 있었음:

```java
// PostService.java - 이미 구현되어 있던 코드
private String getAuthorNickname(Post post) {
    User user = userRepository.findById(post.getUserId());
    if (user != null) {
        return user.getName();  // ← "佐藤花子" 같은 실제 이름
    }
    return "Unknown User";
}
```

**결론:** Comment에는 이 과정이 빠져있었던 것이 버그의 원인

---

## 해결 방법: DTO 패턴 적용

### DTO란?

**DTO (Data Transfer Object)** = 데이터를 전달하기 위한 객체

```
┌──────────────┐                     ┌──────────────────┐
│   Comment    │    변환(convert)     │   CommentDTO     │
│   (엔티티)    │  ──────────────>    │   (전달 객체)     │
│              │                     │                  │
│ - id         │                     │ - id             │
│ - postId     │                     │ - postId         │
│ - userId ────│── User 조회 ──>     │ - userId         │
│ - content    │                     │ - content        │
│ - likeCount  │                     │ - likeCount      │
│              │                     │ - authorNickname │ ← 추가!
└──────────────┘                     └──────────────────┘
   DB 테이블과 1:1                     API 응답용 (추가 정보 포함)
```

**왜 엔티티를 직접 수정하지 않나?**
- `Comment` 엔티티는 DB 테이블(`comments`)과 1:1 매핑됨
- DB에 `author_nickname` 컬럼을 추가하면 데이터 중복 (사용자가 이름을 바꾸면 댓글도 전부 업데이트해야 함)
- DTO를 사용하면 DB 구조는 그대로 두고, API 응답에만 필요한 정보를 추가할 수 있음

---

## 수정한 파일 전체 정리

### 1. 새로 생성: `CommentDTO.java`

**경로:** `backend/src/main/java/com/ej2/dto/CommentDTO.java`

```java
public class CommentDTO {
    // Comment의 모든 필드 + authorNickname 추가
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private String content;
    private String anonymousId;
    private String authorNickname;  // ← 핵심: 닉네임 필드 추가
    private Integer likeCount;
    private Integer dislikeCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Comment 엔티티 + 닉네임을 받아서 DTO 생성
    public CommentDTO(Comment comment, String authorNickname) {
        this.id = comment.getId();
        this.authorNickname = authorNickname;
        // ... 나머지 필드 복사
    }
}
```

**포인트:** PostDTO와 동일한 구조. 프로젝트의 기존 패턴을 따름.

### 2. 수정: `CommentService.java`

**경로:** `backend/src/main/java/com/ej2/service/CommentService.java`

추가한 내용:
```java
@Autowired
private UserRepository userRepository;  // User 조회를 위해 추가

// 반환 타입이 List<Comment> → List<CommentDTO>로 변경
public List<CommentDTO> getCommentsByPostId(Long postId) {
    List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    return convertToCommentDTOList(comments);  // DTO 변환 추가
}

// Comment → CommentDTO 변환 헬퍼 메서드
private List<CommentDTO> convertToCommentDTOList(List<Comment> comments) {
    List<CommentDTO> dtoList = new ArrayList<CommentDTO>();
    for (Comment comment : comments) {
        String authorNickname = getAuthorNickname(comment);
        dtoList.add(new CommentDTO(comment, authorNickname));
    }
    return dtoList;
}

// userId로 User를 조회해서 닉네임 반환
private String getAuthorNickname(Comment comment) {
    User user = userRepository.findById(comment.getUserId());
    if (user != null) {
        return user.getName();
    }
    return "Unknown User";
}
```

**포인트:** PostService의 `getAuthorNickname()` + `convertToPostDTOList()`와 동일한 패턴

### 3. 수정: `CommentController.java`

**경로:** `backend/src/main/java/com/ej2/controller/CommentController.java`

변경 전 → 후:
```java
// 수정 전: Comment 엔티티 직접 반환
public ResponseEntity<List<Comment>> getCommentsByPostId(...)

// 수정 후: CommentDTO 반환
public ResponseEntity<List<CommentDTO>> getCommentsByPostId(...)
```

3개 엔드포인트 모두 동일하게 변경:
- `GET /api/comments/post/{postId}` (전체 댓글)
- `GET /api/comments/post/{postId}/top` (최상위 댓글)
- `GET /{id}/replies` (답글)

### 4. 수정: `CommentSection.js`

**경로:** `frontend/src/pages/Board/CommentSection.js`

```javascript
// 수정 전 (211행):
{isAnonymous ? comment.anonymousId || '匿名' : `ユーザー${comment.userId}`}

// 수정 후:
{isAnonymous ? comment.anonymousId || '匿名' : (comment.authorNickname || 'Unknown User')}
```

답글(289행)도 동일하게 수정:
```javascript
// 수정 전:
{isAnonymous ? reply.anonymousId || '匿名' : `ユーザー${reply.userId}`}

// 수정 후:
{isAnonymous ? reply.anonymousId || '匿名' : (reply.authorNickname || 'Unknown User')}
```

---

## Docker 트러블슈팅

### 발생한 문제: 컨테이너 이름 충돌

```bash
$ docker-compose up --build -d
```
```
Error response from daemon: Conflict. The container name "/e3245d973356_spring-backend"
is already in use by container "78c1a09..."
You have to remove (or rename) that container to be able to reuse that name.
```

### 원인
이전 실행에서 남아있던 컨테이너가 이름을 점유하고 있었음.
Docker Compose가 새 컨테이너를 만들려는데 같은 이름의 컨테이너가 이미 존재.

### 해결 방법

```bash
# 1. 모든 관련 컨테이너를 먼저 정지 및 제거
docker-compose down

# 2. 충돌하는 컨테이너가 남아있으면 수동 삭제
docker rm -f e3245d973356_spring-backend

# 3. 다시 빌드 및 실행
docker-compose up --build -d
```

### 예방법
- `docker-compose up` 전에 항상 `docker-compose down`으로 정리
- 또는 한 줄로: `docker-compose down && docker-compose up --build -d`

---

## 디버깅 방법: curl로 API 테스트

### 수정이 잘 적용되었는지 확인하는 방법

프론트엔드를 열지 않고도 백엔드 API를 직접 호출해서 확인할 수 있습니다.

### 1. 댓글 목록 조회

```bash
# postId=1인 게시글의 댓글 조회
curl -s http://localhost:8080/api/comments/post/1 | python3 -m json.tool
```

**수정 전 응답 (authorNickname 없음):**
```json
[
    {
        "id": 1,
        "postId": 1,
        "userId": 2,
        "content": "スクリーンショット機能が便利ですよね！"
    }
]
```

**수정 후 응답 (authorNickname 포함):**
```json
[
    {
        "id": 1,
        "postId": 1,
        "userId": 2,
        "authorNickname": "佐藤花子",
        "content": "スクリーンショット機能が便利ですよね！"
    }
]
```

### 2. 백엔드 로그 확인

```bash
# Spring 백엔드 컨테이너 로그 (최근 20줄)
docker logs spring-backend --tail 20

# 실시간 로그 추적 (Ctrl+C로 종료)
docker logs -f spring-backend
```

→ 서버 기동 실패 시 에러 메시지가 여기에 나옴
→ `Server startup in [xxxx] milliseconds` 가 나오면 정상 기동

### 3. 컨테이너 상태 확인

```bash
# 모든 컨테이너 상태 확인
docker-compose ps

# 기대 출력:
# NAME              STATUS
# mariadb           Up (healthy)
# spring-backend    Up
# react-frontend    Up
```

### 4. DB에서 직접 확인

```bash
# MariaDB에 접속
docker exec -it mariadb mysql -u appuser -papppassword appdb

# 사용자 목록 확인
SELECT id, username, name FROM users;

# 댓글과 사용자 조인 확인
SELECT c.id, c.user_id, u.name, c.content
FROM comments c
JOIN users u ON c.user_id = u.id
WHERE c.post_id = 1;
```

---

## 핵심 인사이트

### 1. 엔티티 vs DTO - 언제 분리하나?

| 상황 | 엔티티 직접 사용 | DTO 사용 |
|------|:---:|:---:|
| DB 구조와 API 응답이 동일 | O | - |
| 추가 정보가 필요 (닉네임 등) | - | O |
| 민감 정보 숨기기 필요 (비밀번호 등) | - | O |
| 여러 엔티티 조합 필요 | - | O |

이 프로젝트에서:
- `POST /api/comments` (생성)은 Comment 엔티티를 직접 받아도 됨
- `GET /api/comments/post/{id}` (조회)은 닉네임이 필요하므로 CommentDTO 사용

### 2. N+1 쿼리 문제

현재 코드는 댓글 10개 → DB 쿼리 11번 발생:
```
1. SELECT * FROM comments WHERE post_id = 1      -- 댓글 목록 (1번)
2. SELECT * FROM users WHERE id = 2              -- 댓글1의 사용자
3. SELECT * FROM users WHERE id = 3              -- 댓글2의 사용자
...
11. SELECT * FROM users WHERE id = 10            -- 댓글10의 사용자
```

PostService도 같은 문제가 있음. 댓글이 소규모(수십 개)인 커뮤니티 프로젝트에서는 괜찮지만, 대규모 서비스에서는 다음과 같이 최적화 가능:

```java
// 최적화 예시: 사용자 ID를 모아서 한 번에 조회
Set<Long> userIds = comments.stream()
    .map(Comment::getUserId)
    .collect(Collectors.toSet());
Map<Long, User> userMap = userRepository.findAllByIds(userIds);
```

### 3. 프론트엔드 방어적 코딩

```javascript
// authorNickname이 null/undefined일 때를 대비
comment.authorNickname || 'Unknown User'
```

`||` 연산자는 왼쪽이 falsy(null, undefined, 빈 문자열)이면 오른쪽 값을 반환. 서버 응답이 예상과 다를 때 화면이 깨지지 않도록 하는 방어적 코딩.

### 4. 같은 패턴 반복 시 기존 코드를 참고

이번 수정은 PostDTO/PostService의 기존 패턴을 그대로 Comment에 적용한 것. 프로젝트에서 새로운 기능을 만들 때:
1. **비슷한 기능이 이미 있는지** 먼저 확인
2. 있으면 그 **패턴을 따라서** 구현
3. 코드의 **일관성**이 유지됨 → 다른 개발자도 이해하기 쉬움

---

## Bash 명령어 설명

이번 작업에서 사용한 명령어들을 정리합니다.

### Docker 관련

| 명령어 | 설명 |
|--------|------|
| `docker-compose up --build -d` | 이미지 빌드 후 백그라운드 실행 (`--build`: 재빌드, `-d`: 백그라운드) |
| `docker-compose down` | 모든 컨테이너 정지 및 제거 |
| `docker-compose ps` | 컨테이너 상태 확인 |
| `docker logs spring-backend` | 백엔드 컨테이너 로그 확인 |
| `docker logs -f spring-backend` | 실시간 로그 추적 (`-f`: follow) |
| `docker logs --tail 20 spring-backend` | 마지막 20줄만 표시 |
| `docker rm -f <이름>` | 컨테이너 강제 삭제 (`-f`: force) |
| `docker exec -it mariadb mysql ...` | 실행 중인 컨테이너에 접속 (`-it`: 인터랙티브 터미널) |

### API 테스트 관련

| 명령어 | 설명 |
|--------|------|
| `curl -s <URL>` | HTTP GET 요청 (`-s`: 진행률 숨김) |
| `curl -s <URL> \| python3 -m json.tool` | JSON 응답을 보기 좋게 포맷팅 |
| `curl -X POST -H "Content-Type: application/json" -d '{"key":"value"}' <URL>` | POST 요청 |

### 파이프 (`|`)란?

```bash
curl -s http://localhost:8080/api/comments/post/1 | python3 -m json.tool
#    ^^^^^^^^ 왼쪽 명령의 출력을 ^^^^^^^^  오른쪽 명령의 입력으로 전달
```

파이프(`|`)는 앞 명령의 결과를 뒤 명령에 넘겨줍니다:
1. `curl`이 API 응답(JSON 문자열)을 받음
2. `|`가 그 문자열을 `python3 -m json.tool`에 전달
3. `python3 -m json.tool`이 JSON을 보기 좋게 들여쓰기

---

## TODO(human) 학습 정리

이 섹션은 직접 정리해보세요. 이번 작업에서 배운 것들을 자신의 말로 적어보면 학습 효과가 높아집니다.

### 질문 가이드

1. **DTO를 사용하지 않고 해결할 수 있는 다른 방법은?** (예: 프론트엔드에서 User API를 별도 호출)
   - 그 방법의 장단점은?
   - 답:

2. **N+1 쿼리 문제를 이 프로젝트에서 해결하려면 어떻게 해야 할까?**
   - 답:

3. **익명 게시판에서는 authorNickname이 표시되나, 무시되나?**
   - 코드에서 어느 부분이 이를 처리하는지 찾아보세요
   - 답:
