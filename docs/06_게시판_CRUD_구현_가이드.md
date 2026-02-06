# 06. 기본 게시판 CRUD 구현 가이드

> 이 문서는 EJ2 프로젝트의 **게시판(Board)과 게시글(Post) CRUD** 기능을 설명합니다.
> 게시판 구조, 게시글 생성/조회/수정/삭제, 좋아요/싫어요/조회수 시스템을 다룹니다.

---

## 목차

1. [전체 구조 이해하기](#1-전체-구조-이해하기)
2. [백엔드: 게시판(Board)](#2-백엔드-게시판board)
   - 2.1 [Board 엔티티](#21-board-엔티티)
   - 2.2 [BoardController](#22-boardcontroller)
3. [백엔드: 게시글(Post)](#3-백엔드-게시글post)
   - 3.1 [Post 엔티티](#31-post-엔티티)
   - 3.2 [PostDTO (데이터 변환)](#32-postdto-데이터-변환)
   - 3.3 [PostService (비즈니스 로직)](#33-postservice-비즈니스-로직)
   - 3.4 [PostController (API)](#34-postcontroller-api)
4. [프론트엔드: 게시글 목록](#4-프론트엔드-게시글-목록)
   - 4.1 [PostListPage 구조](#41-postlistpage-구조)
   - 4.2 [페이지네이션](#42-페이지네이션)
   - 4.3 [정렬 기능](#43-정렬-기능)
   - 4.4 [검색 기능](#44-검색-기능)
5. [프론트엔드: 게시글 작성](#5-프론트엔드-게시글-작성)
   - 5.1 [PostWritePage](#51-postwritepage)
   - 5.2 [이미지 업로드](#52-이미지-업로드)
6. [프론트엔드: 게시글 상세](#6-프론트엔드-게시글-상세)
   - 6.1 [PostDetailPage](#61-postdetailpage)
   - 6.2 [좋아요/싫어요 시스템](#62-좋아요싫어요-시스템)
   - 6.3 [조회수 중복 방지](#63-조회수-중복-방지)
7. [익명 게시판 시스템](#7-익명-게시판-시스템)
8. [자주 발생하는 에러와 해결법](#8-자주-발생하는-에러와-해결법)

---

## 1. 전체 구조 이해하기

### 게시판 시스템 구조

```
게시판(Board)
├── 자유게시판 (id:1, code:"free")
│   ├── 게시글 1
│   ├── 게시글 2
│   └── ...
├── 익명게시판 (id:2, code:"anonymous", isAnonymous:true)
│   ├── 게시글 3 (작성자: 匿名1)
│   └── ...
├── 이벤트 (id:3, code:"event")
├── 중고시장 (id:4, code:"market")
└── BEST (id:5, code:"best")
```

### URL 라우팅 구조

| URL | 컴포넌트 | 설명 |
|-----|---------|------|
| `/boards` | BoardListPage | 전체 게시판 목록 |
| `/boards/:boardId/posts` | PostListPage | 특정 게시판의 게시글 목록 |
| `/boards/:boardId/write` | PostWritePage | 게시글 작성 |
| `/boards/:boardId/posts/:postId` | PostDetailPage | 게시글 상세 |
| `/boards/:boardId/posts/:postId/edit` | PostEditPage | 게시글 수정 |

---

## 2. 백엔드: 게시판(Board)

### 2.1 Board 엔티티

> **파일 위치**: `backend/src/main/java/com/ej2/model/Board.java`

```java
@Entity
@Table(name = "boards")
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;          // 게시판 이름 ("자유게시판")

    @Column(nullable = false, unique = true, length = 50)
    private String code;          // 게시판 코드 ("free") - URL에서 사용

    @Column(columnDefinition = "TEXT")
    private String description;   // 게시판 설명

    private Long universityId;    // 소속 대학 ID

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;  // 익명 게시판 여부

    @Column(name = "require_admin")
    private Boolean requireAdmin = false; // 관리자만 작성 가능 여부
}
```

#### 주요 게시판 설정

| ID | 코드 | 이름 | isAnonymous | requireAdmin |
|----|------|------|-------------|-------------|
| 1 | free | 自由掲示板 | false | false |
| 2 | anonymous | 匿名掲示板 | **true** | false |
| 3 | event | イベント | false | false |
| 4 | market | 中古市場 | false | false |
| 5 | best | BEST | false | false |

### 2.2 BoardController

> **파일 위치**: `backend/src/main/java/com/ej2/controller/BoardController.java`

```java
@RestController
@RequestMapping("/api/boards")
public class BoardController {

    @GetMapping                          // GET /api/boards
    public List<Board> getAllBoards() { ... }

    @GetMapping("/{id}")                 // GET /api/boards/1
    public Board getBoardById(@PathVariable Long id) { ... }

    @GetMapping("/code/{code}")          // GET /api/boards/code/free
    public Board getBoardByCode(@PathVariable String code) { ... }

    @PostMapping                         // POST /api/boards (관리자)
    public Board createBoard(@RequestBody Board board) { ... }

    @PutMapping("/{id}")                 // PUT /api/boards/1 (관리자)
    public Board updateBoard(@PathVariable Long id, @RequestBody Board board) { ... }

    @DeleteMapping("/{id}")              // DELETE /api/boards/1 (SUPER_ADMIN)
    public void deleteBoard(@PathVariable Long id) { ... }
}
```

---

## 3. 백엔드: 게시글(Post)

### 3.1 Post 엔티티

> **파일 위치**: `backend/src/main/java/com/ej2/model/Post.java`

```java
@Entity
@Table(name = "posts")
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;            // 소속 게시판

    @Column(name = "user_id", nullable = false)
    private Long userId;             // 작성자

    @Column(nullable = false)
    private String title;            // 제목

    @Column(columnDefinition = "TEXT")
    private String content;          // 내용

    @Column(name = "anonymous_id")
    private String anonymousId;      // 익명 게시판에서의 익명 ID

    // 카운터들
    @Column(name = "view_count")
    private Integer viewCount = 0;       // 조회수
    @Column(name = "like_count")
    private Integer likeCount = 0;       // 좋아요
    @Column(name = "dislike_count")
    private Integer dislikeCount = 0;    // 싫어요
    @Column(name = "comment_count")
    private Integer commentCount = 0;    // 댓글 수

    // 관리 필드
    @Column(name = "is_notice")
    private Boolean isNotice = false;    // 공지사항 여부
    @Column(name = "is_blinded")
    private Boolean isBlinded = false;   // 블라인드 처리 여부

    @Column(name = "reported_count")
    private Integer reportedCount = 0;   // 신고 횟수

    // 스마트 업데이트 플래그
    @Transient
    private boolean refreshUpdatedAt = true;
}
```

#### @Transient와 refreshUpdatedAt

```java
@Transient  // DB에 저장되지 않는 필드
private boolean refreshUpdatedAt = true;

@PreUpdate
protected void onUpdate() {
    if (refreshUpdatedAt) {
        updatedAt = LocalDateTime.now();
    }
}
```

**왜 이런 구조인가?**
```
문제:
조회수가 1 증가할 때마다 updatedAt이 갱신되면
→ "5분 전에 수정됨"이 사실은 "5분 전에 누가 봤음"
→ 사용자에게 혼란

해결:
refreshUpdatedAt = false로 설정한 후 조회수만 변경
→ updatedAt은 그대로 유지
→ 실제 내용 수정 시에만 updatedAt 갱신
```

### 3.2 PostDTO (데이터 변환)

```java
public class PostDTO {
    private Long id;
    private Long boardId;
    private Long userId;
    private String title;
    private String content;
    private String authorName;    // ← 추가! 작성자 이름 또는 익명 ID
    private String anonymousId;
    private Integer viewCount;
    private Integer likeCount;
    // ...
}
```

**왜 엔티티 대신 DTO를 사용하나?**
```
Post 엔티티: userId = 5 (숫자만 있음)
→ 프론트엔드에서 "이 게시글 작성자가 누구지?" → 별도 API 호출 필요

PostDTO: authorName = "김철수" (이름 포함)
→ 한 번의 API 호출로 작성자 이름까지 바로 표시 가능

익명 게시판이면:
PostDTO: authorName = "匿名1" (익명 ID 표시)
```

### 3.3 PostService (비즈니스 로직)

> **파일 위치**: `backend/src/main/java/com/ej2/service/PostService.java`

#### 게시글 생성 (익명 ID 자동 부여)

```java
public Post createPost(Post post) {
    // 익명 게시판인지 확인
    Board board = boardRepository.findById(post.getBoardId());
    if (board != null && Boolean.TRUE.equals(board.getIsAnonymous())) {
        // 해당 게시판에서의 익명 ID 계산
        long anonymousCount = postRepository.countByBoardIdAndUserId(
            post.getBoardId(), post.getUserId()
        );

        if (anonymousCount == 0) {
            // 처음 작성하는 사용자 → 새 익명 번호 부여
            long totalUniqueUsers = postRepository.countDistinctUserIdByBoardId(post.getBoardId());
            post.setAnonymousId("匿名" + (totalUniqueUsers + 1));
        } else {
            // 이전에 작성한 적 있음 → 기존 익명 ID 재사용
            String existingAnonymousId = postRepository.findAnonymousIdByBoardIdAndUserId(
                post.getBoardId(), post.getUserId()
            );
            post.setAnonymousId(existingAnonymousId);
        }
    }

    return postRepository.save(post);
}
```

#### 조회수 중복 방지 (24시간)

```java
public void incrementViewCount(Long postId, Long userId, String ipAddress) {
    // 24시간 이내에 같은 사용자(또는 IP)가 조회한 적 있는지 확인
    LocalDateTime since = LocalDateTime.now().minusHours(24);

    boolean alreadyViewed;
    if (userId != null) {
        alreadyViewed = postViewLogRepository.existsByPostIdAndUserIdAndViewedAtAfter(
            postId, userId, since
        );
    } else {
        alreadyViewed = postViewLogRepository.existsByPostIdAndIpAddressAndViewedAtAfter(
            postId, ipAddress, since
        );
    }

    if (!alreadyViewed) {
        // 조회 기록 저장
        PostViewLog log = new PostViewLog(postId, userId, ipAddress);
        postViewLogRepository.save(log);

        // 조회수 +1
        Post post = postRepository.findById(postId);
        post.setRefreshUpdatedAt(false);  // updatedAt 갱신 방지!
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }
}
```

**조회수 중복 방지 시나리오**:
```
사용자 A가 게시글 5번을 열람:
09:00 → 조회수 +1 (기록: {postId:5, userId:A, time:09:00})
09:30 → 조회수 변화 없음 (24시간 이내 기록 존재)
15:00 → 조회수 변화 없음 (24시간 이내 기록 존재)

다음날 09:01 → 조회수 +1 (24시간 경과!)
```

#### 좋아요/싫어요 (중복 방지)

```java
public boolean incrementLikeCount(Long postId, Long userId) {
    // 이미 좋아요 했는지 확인
    if (postLikeLogRepository.existsByPostIdAndUserId(postId, userId)) {
        return false;  // 이미 좋아요함
    }

    // 좋아요 기록 저장
    PostLikeLog log = new PostLikeLog(postId, userId);
    postLikeLogRepository.save(log);

    // 좋아요 수 +1
    Post post = postRepository.findById(postId);
    post.setRefreshUpdatedAt(false);
    post.setLikeCount(post.getLikeCount() + 1);
    postRepository.save(post);

    return true;
}
```

### 3.4 PostController (API)

> **파일 위치**: `backend/src/main/java/com/ej2/controller/PostController.java`

#### 전체 API 목록

| 메서드 | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/posts` | 불필요 | 전체 게시글 목록 |
| GET | `/api/posts/{id}` | 불필요 | 게시글 상세 |
| POST | `/api/posts` | **필요** | 게시글 작성 |
| PUT | `/api/posts/{id}` | **필요** (본인만) | 게시글 수정 |
| DELETE | `/api/posts/{id}` | **필요** (본인만) | 게시글 삭제 |
| GET | `/api/posts/board/{boardId}` | 불필요 | 게시판별 목록 |
| GET | `/api/posts/board/{boardId}/{sortBy}` | 불필요 | 정렬된 목록 |
| GET | `/api/posts/search?keyword=xxx` | 불필요 | 제목 검색 |
| POST | `/api/posts/{id}/view` | 불필요 | 조회수 증가 |
| POST | `/api/posts/{id}/like` | **필요** | 좋아요 |
| POST | `/api/posts/{id}/dislike` | **필요** | 싫어요 |

#### 권한 검증 패턴

```java
@PutMapping("/{id}")
public ResponseEntity<?> updatePost(@PathVariable Long id,
                                     @RequestBody Post post,
                                     HttpSession session) {
    Long userId = (Long) session.getAttribute("userId");
    if (userId == null) {
        return ResponseEntity.status(401).body("ログインが必要です");
    }

    // 작성자 본인인지 확인
    Post existingPost = postService.getPostEntityById(id);
    if (!existingPost.getUserId().equals(userId)) {
        return ResponseEntity.status(403).body("自分の投稿のみ編集できます");
    }

    // 수정 실행
    Post updatedPost = postService.updatePost(id, post);
    return ResponseEntity.ok(updatedPost);
}
```

---

## 4. 프론트엔드: 게시글 목록

### 4.1 PostListPage 구조

> **파일 위치**: `frontend/src/pages/Board/PostListPage.js`

```
┌──────────────────────────────────────────┐
│ 자유게시판                    [글쓰기]    │
├──────────────────────────────────────────┤
│ [최신순] [좋아요순] [조회순]              │  ← 정렬 탭
├──────────────────────────────────────────┤
│ 📌 [공지] 게시판 이용규칙  admin  2/5    │  ← 공지사항
├──────────────────────────────────────────┤
│ 오늘 점심 뭐 먹지?          kim   12:30  │  ← 일반 게시글
│   👁 45  👍 3  💬 5                     │
│ 시험 언제예요?              lee   11:00  │
│   👁 120  👍 15  💬 23                  │
├──────────────────────────────────────────┤
│ [검색어 입력...] [검색]                   │  ← 검색
├──────────────────────────────────────────┤
│ [1] [2] [3] ... [10]                     │  ← 페이지네이션
└──────────────────────────────────────────┘
```

### 4.2 페이지네이션

```javascript
const POSTS_PER_PAGE = 10;  // 페이지당 10개

// 현재 페이지의 게시글만 추출
const currentPosts = posts.slice(
    (currentPage - 1) * POSTS_PER_PAGE,
    currentPage * POSTS_PER_PAGE
);

// 총 페이지 수 계산
const totalPages = Math.ceil(posts.length / POSTS_PER_PAGE);
```

**Math.ceil 설명**:
```
게시글 23개, 페이지당 10개
Math.ceil(23 / 10) = Math.ceil(2.3) = 3 (올림)
→ 3페이지 필요 (1페이지:10개, 2페이지:10개, 3페이지:3개)
```

**slice 설명**:
```javascript
// 2페이지를 보고 있을 때 (currentPage = 2)
posts.slice((2-1) * 10, 2 * 10)
= posts.slice(10, 20)
// → 인덱스 10~19의 게시글 (11번째~20번째)
```

### 4.3 정렬 기능

```javascript
const [sortBy, setSortBy] = useState('recent');

const loadPosts = async () => {
    let url = `/api/posts/board/${boardId}`;
    if (sortBy !== 'recent') {
        url = `/api/posts/board/${boardId}/${sortBy}`;
    }
    const response = await axios.get(url);
    setPosts(response.data);
};
```

| 정렬 키 | API URL | 의미 |
|---------|---------|------|
| recent | `/api/posts/board/1` | 최신순 (기본) |
| likes | `/api/posts/board/1/likes` | 좋아요 많은 순 |
| views | `/api/posts/board/1/views` | 조회수 많은 순 |

### 4.4 검색 기능

```javascript
const handleSearch = async () => {
    if (!searchKeyword.trim()) {
        loadPosts();  // 검색어 없으면 전체 목록
        return;
    }

    const response = await axios.get(`/api/posts/search`, {
        params: { keyword: searchKeyword }
    });
    setPosts(response.data);
};
```

---

## 5. 프론트엔드: 게시글 작성

### 5.1 PostWritePage

> **파일 위치**: `frontend/src/pages/Board/PostWritePage.js`

```javascript
function PostWritePage() {
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [images, setImages] = useState([]);      // 업로드할 이미지들
    const [previews, setPreviews] = useState([]);   // 이미지 미리보기 URL

    const handleSubmit = async (e) => {
        e.preventDefault();

        // 입력 검증
        if (!title.trim()) {
            alert('タイトルを入力してください');
            return;
        }

        const user = JSON.parse(localStorage.getItem('user'));
        if (!user) {
            navigate('/login');
            return;
        }

        // 게시글 데이터 구성
        const postData = {
            boardId: parseInt(boardId),
            userId: user.id,
            title: title.trim(),
            content: content.trim()
        };

        // 이미지가 있으면 FormData 사용
        if (images.length > 0) {
            const formData = new FormData();
            formData.append('post', JSON.stringify(postData));
            images.forEach(img => formData.append('images', img));

            await axios.post('/api/posts', formData, {
                headers: { 'Content-Type': 'multipart/form-data' },
                withCredentials: true
            });
        } else {
            await axios.post('/api/posts', postData, {
                withCredentials: true
            });
        }

        navigate(`/boards/${boardId}/posts`);
    };
}
```

### 5.2 이미지 업로드

```javascript
const handleImageChange = (e) => {
    const files = Array.from(e.target.files);

    // 검증: 최대 5장, 각 5MB
    if (images.length + files.length > 5) {
        alert('最大5枚まで');
        return;
    }

    const validFiles = files.filter(file => {
        if (file.size > 5 * 1024 * 1024) {  // 5MB
            alert(`${file.name}は5MBを超えています`);
            return false;
        }
        return true;
    });

    setImages(prev => [...prev, ...validFiles]);

    // 미리보기 URL 생성
    validFiles.forEach(file => {
        const reader = new FileReader();
        reader.onload = (e) => {
            setPreviews(prev => [...prev, e.target.result]);
        };
        reader.readAsDataURL(file);
    });
};
```

**FileReader.readAsDataURL 설명**:
```
파일 → Base64 문자열 → <img src="data:image/png;base64,..."> 로 미리보기

실제 서버에 업로드되기 전에 브라우저에서만 미리보기를 보여주는 기술
→ 사용자가 어떤 이미지를 올릴지 확인할 수 있음
```

---

## 6. 프론트엔드: 게시글 상세

### 6.1 PostDetailPage

> **파일 위치**: `frontend/src/pages/Board/PostDetailPage.js`

```javascript
function PostDetailPage() {
    const [post, setPost] = useState(null);

    useEffect(() => {
        loadPost();
        incrementView();
    }, [postId]);

    const loadPost = async () => {
        const response = await axios.get(`/api/posts/${postId}`);
        setPost(response.data);
    };

    const incrementView = async () => {
        await axios.post(`/api/posts/${postId}/view`, {}, {
            withCredentials: true
        });
    };
}
```

**useEffect의 의존성 배열 [postId]**:
```javascript
useEffect(() => {
    loadPost();
}, [postId]);
// postId가 변경될 때만 실행
// → 같은 게시글을 다시 열면 재로드하지 않음
// → 다른 게시글로 이동하면 새로 로드
```

### 6.2 좋아요/싫어요 시스템

```javascript
const handleLike = async () => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user) {
        alert('ログインが必要です');
        return;
    }

    try {
        await axios.post(`/api/posts/${postId}/like`, {}, {
            withCredentials: true
        });
        loadPost();  // 좋아요 수 갱신
    } catch (err) {
        if (err.response?.status === 409) {
            alert('既に「いいね」済みです');
        }
    }
};
```

**좋아요 흐름**:
```
[좋아요 버튼 클릭]
    ↓
POST /api/posts/5/like
    ↓
서버: postLikeLogRepository.existsByPostIdAndUserId(5, userId)
    → 이미 있으면: 409 Conflict 반환
    → 없으면: 기록 저장 + likeCount +1 + 200 OK
    ↓
프론트: loadPost()로 갱신 → 좋아요 숫자 업데이트
```

### 6.3 조회수 중복 방지

```
같은 사용자가 같은 게시글을 반복 조회해도 24시간 동안 1회만 카운트

구현 방식:
1. 로그인 사용자 → userId로 중복 체크
2. 비로그인 사용자 → IP 주소로 중복 체크
3. 24시간이 지나면 다시 카운트 가능
```

---

## 7. 익명 게시판 시스템

```
익명 게시판 (isAnonymous = true) 에서는:

1. 같은 사용자 = 같은 익명 ID
   사용자 A가 첫 글 작성 → "匿名1"
   사용자 A가 두 번째 글 작성 → "匿名1" (같은 ID 유지)

2. 다른 사용자 = 다른 익명 ID
   사용자 B가 첫 글 작성 → "匿名2"
   사용자 C가 첫 글 작성 → "匿名3"

3. 게시판마다 독립적인 번호 체계
   자유게시판에서 사용자 A → "김철수" (실명)
   익명게시판에서 사용자 A → "匿名1" (익명)

4. 프론트엔드 표시 규칙
   authorName: 일반 게시판 → 사용자 이름
   authorName: 익명 게시판 → 익명 ID
```

```jsx
// PostListPage에서 작성자 표시
<span className="post-author">
    {post.anonymousId ? (
        <span className="anonymous-badge">🎭 {post.anonymousId}</span>
    ) : (
        post.authorName
    )}
</span>
```

---

## 8. 자주 발생하는 에러와 해결법

### 에러 1: 게시글 작성 시 403 Forbidden

**원인**: SecurityConfig에서 POST /api/posts에 인증이 필요하도록 설정됨

**해결**: 로그인 상태 확인, `withCredentials: true` 옵션 확인

### 에러 2: 게시글 수정 시 "自分の投稿のみ編集できます"

**원인**: 세션의 userId와 게시글의 userId가 불일치

**확인**:
```javascript
const user = JSON.parse(localStorage.getItem('user'));
console.log('localStorage userId:', user.id);
// vs 게시글의 userId 비교
```

### 에러 3: 좋아요가 안 눌림

**원인**: 이미 좋아요한 상태 (409 Conflict)

**확인**: Network 탭에서 응답 상태 코드 확인

### 에러 4: 이미지 업로드 실패

```
MaxUploadSizeExceededException
```

**원인**: 파일 크기가 WebConfig에 설정된 최대값 초과

**확인**: `WebConfig.java`에서 최대 크기 설정 확인
```java
// 현재 설정: 개별 파일 10MB, 전체 50MB
```

### 에러 5: 페이지네이션에서 빈 페이지

**원인**: 게시글 삭제 후 마지막 페이지에 게시글이 없음

**해결**: 페이지가 비어있으면 이전 페이지로 자동 이동하는 로직 추가

---

> 이전 문서: [05. 채팅방 구현 가이드](./05_채팅방_구현_가이드.md)
> 다음 문서: [07. 댓글 기능 구현 가이드](./07_댓글_기능_구현_가이드.md)
