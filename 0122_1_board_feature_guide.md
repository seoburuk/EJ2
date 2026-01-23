# 게시판 기능 구현 가이드 (2026-01-22)

## 📋 목차
1. [개요](#개요)
2. [학습 목표](#학습-목표)
3. [구현된 기능](#구현된-기능)
4. [전체 파일 변경사항](#전체-파일-변경사항)
5. [백엔드 구조](#백엔드-구조)
6. [프론트엔드 구조](#프론트엔드-구조)
7. [핵심 개념 설명](#핵심-개념-설명)
8. [코드 실행 방법](#코드-실행-방법)
9. [트러블슈팅](#트러블슈팅)

---

## 개요

이 문서는 EJ2 프로젝트에 **게시판 시스템**을 구현한 내용을 초보자도 이해할 수 있도록 정리한 가이드입니다.

### 주요 변경사항
- 게시판 목록 및 게시글 CRUD 기능 완성
- 댓글/대댓글 시스템 구현
- 좋아요 및 조회수 기능 추가
- 에브리타임 스타일 UI 적용 (2열 레이아웃)

---

## 학습 목표

이 문서를 통해 다음을 배울 수 있습니다:

1. **Spring Framework의 3계층 아키텍처** (Controller → Service → Repository)
2. **JPA를 이용한 데이터베이스 연동**
3. **React 컴포넌트 설계 및 상태 관리**
4. **RESTful API 설계 원칙**
5. **계층 구조 데이터 모델링** (댓글-대댓글)
6. **CSS Grid를 이용한 반응형 레이아웃**

---

## 구현된 기능

### ✅ 완성된 기능 목록

#### 백엔드 (Java/Spring)
- [x] Board (게시판) 엔티티 및 CRUD API
- [x] Post (게시글) 엔티티 및 CRUD API
- [x] Comment (댓글) 엔티티 및 CRUD API
- [x] 게시판별 게시글 조회
- [x] 조회수 증가 API
- [x] 좋아요 기능 API
- [x] 댓글/대댓글 계층 구조
- [x] Soft Delete (논리 삭제)

#### 프론트엔드 (React)
- [x] 메인 페이지 (2열 게시판 레이아웃)
- [x] 게시판별 게시글 목록 페이지
- [x] 게시글 상세 페이지
- [x] 댓글 섹션 컴포넌트
- [x] 게시글 작성 폼
- [x] 반응형 디자인 (모바일 대응)

---

## 전체 파일 변경사항

### 📁 신규 생성 파일

#### 백엔드 (Backend)
```
backend/src/main/java/com/ej2/
├── model/
│   ├── Board.java              ✨ 새로 생성
│   ├── Post.java               ✨ 새로 생성
│   └── Comment.java            ✨ 새로 생성
├── repository/
│   ├── BoardRepository.java    ✨ 새로 생성
│   ├── PostRepository.java     ✨ 새로 생성
│   └── CommentRepository.java  ✨ 새로 생성
├── service/
│   ├── BoardService.java       ✨ 새로 생성
│   ├── PostService.java        ✨ 새로 생성
│   └── CommentService.java     ✨ 새로 생성
└── controller/
    ├── BoardController.java    ✨ 새로 생성
    ├── PostController.java     ✨ 새로 생성
    └── CommentController.java  ✨ 새로 생성
```

#### 프론트엔드 (Frontend)
```
frontend/src/pages/
├── Main/
│   ├── MainPage.js             ✨ 새로 생성
│   └── MainPage.css            ✨ 새로 생성
└── Board/
    ├── PostListPage.js         ✨ 새로 생성
    ├── PostListPage.css        ✨ 새로 생성
    ├── PostDetailPage.js       ✨ 새로 생성
    ├── PostDetailPage.css      ✨ 새로 생성
    ├── PostForm.js             ✨ 새로 생성
    ├── PostForm.css            ✨ 새로 생성
    ├── CommentSection.js       ✨ 새로 생성
    └── CommentSection.css      ✨ 새로 생성
```

---

### 🔧 수정된 파일

#### 1. PostService.java
**위치**: `backend/src/main/java/com/ej2/service/PostService.java`

**변경 내용**:
- ❌ **제거**: `post.setAuthor()` (존재하지 않는 필드)
- ✅ **추가**: 게시판별 게시글 조회 기능
- ✅ **추가**: 조회수 증가 메서드
- ✅ **추가**: 좋아요 증가 메서드

```java
// 수정 전 (오류)
public Post updatePost(Long id, Post postDetails) {
    post.setAuthor(postDetails.getAuthor());  // ❌ author 필드 없음
}

// 수정 후 (정상)
public Post updatePost(Long id, Post postDetails) {
    post.setTitle(postDetails.getTitle());
    post.setContent(postDetails.getContent());
}

// 새로 추가된 메서드
public List<Post> getPostsByBoardId(Long boardId) {
    return postRepository.findByBoardIdOrderByCreatedAtDesc(boardId);
}

public void incrementViewCount(Long id) {
    Post post = postRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Post not found"));
    post.setViewCount(post.getViewCount() + 1);
    postRepository.save(post);
}

public void incrementLikeCount(Long id) {
    Post post = postRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Post not found"));
    post.setLikeCount(post.getLikeCount() + 1);
    postRepository.save(post);
}
```

---

#### 2. PostRepository.java
**위치**: `backend/src/main/java/com/ej2/repository/PostRepository.java`

**변경 내용**:
- ✅ **추가**: 게시판별 게시글 조회 쿼리 메서드

```java
// 추가된 메서드
List<Post> findByBoardIdOrderByCreatedAtDesc(Long boardId);
```

**생성되는 SQL**:
```sql
SELECT * FROM posts
WHERE board_id = ?
ORDER BY created_at DESC;
```

---

#### 3. PostController.java
**위치**: `backend/src/main/java/com/ej2/controller/PostController.java`

**변경 내용**:
- ✅ **추가**: 게시판별 게시글 조회 엔드포인트
- ✅ **추가**: 조회수 증가 엔드포인트
- ✅ **추가**: 좋아요 증가 엔드포인트

```java
// GET /api/posts/board/{boardId}
@GetMapping("/board/{boardId}")
public ResponseEntity<List<Post>> getPostsByBoardId(@PathVariable Long boardId) {
    List<Post> posts = postService.getPostsByBoardId(boardId);
    return ResponseEntity.ok(posts);
}

// POST /api/posts/{id}/view
@PostMapping("/{id}/view")
public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
    postService.incrementViewCount(id);
    return ResponseEntity.ok().build();
}

// POST /api/posts/{id}/like
@PostMapping("/{id}/like")
public ResponseEntity<Void> incrementLikeCount(@PathVariable Long id) {
    postService.incrementLikeCount(id);
    return ResponseEntity.ok().build();
}
```

---

#### 4. PostListPage.js
**위치**: `frontend/src/pages/Board/PostListPage.js`

**변경 내용**:
- ❌ **제거**: TODO 주석 및 불필요한 코드

```javascript
// 수정 전
// TODO(human): ソート変更のロジックを実装してください
const handleSortChange = (newSortBy) => {
  // TODO(human): ここにソート変更のロジックを記述してください
  setSortBy(newSortBy);
  setCurrentPage(0);
};

// 수정 후 (간결하게)
const handleSortChange = (newSortBy) => {
  setSortBy(newSortBy);
  setCurrentPage(0);
};
```

**핵심 기능**:
- useEffect가 sortBy 변경을 자동 감지하여 fetchPosts 실행
- 별도의 추가 로직 불필요

---

#### 5. PostDetailPage.js
**위치**: `frontend/src/pages/Board/PostDetailPage.js`

**변경 내용**:
- ✅ **추가**: CommentSection 컴포넌트 import 및 통합
- ✅ **추가**: 조회수 자동 증가 기능
- ✅ **추가**: 게시글 좋아요 기능

```javascript
// Import 추가
import CommentSection from './CommentSection';

// useEffect에 조회수 증가 추가
useEffect(() => {
  fetchPost();
  incrementViewCount();  // ✨ 새로 추가
}, [postId]);

// 조회수 증가 함수
const incrementViewCount = async () => {
  try {
    await axios.post(`/ej2/api/posts/${postId}/view`);
  } catch (error) {
    console.error('閲覧数の更新に失敗しました:', error);
  }
};

// 좋아요 함수
const handleLikePost = async () => {
  try {
    await axios.post(`/ej2/api/posts/${postId}/like`);
    fetchPost();  // 좋아요 후 데이터 새로고침
  } catch (error) {
    console.error('いいねに失敗しました:', error);
  }
};

// 댓글 섹션 통합 (기존 코드 대체)
<CommentSection
  postId={postId}
  boardId={boardId}
  isAnonymous={board?.isAnonymous || false}
/>
```

**변경 전후 비교**:
```javascript
// 변경 전: 정적 댓글 섹션
<div className="comments-section">
  <div className="comments-header">
    <h3>コメント {post.commentCount}</h3>
  </div>
  <div className="no-comments">
    まだコメントがありません。
  </div>
</div>

// 변경 후: 동적 댓글 컴포넌트
<CommentSection
  postId={postId}
  boardId={boardId}
  isAnonymous={board?.isAnonymous || false}
/>
```

---

#### 6. MainPage.css
**위치**: `frontend/src/pages/Main/MainPage.css`

**변경 내용**:
- ✅ **수정**: 메인 페이지 패딩 증가 (30px → 45px, 80px → 120px)
- ✅ **수정**: 게시판 레이아웃 2열 그리드로 변경
- ✅ **추가**: 반응형 미디어 쿼리

```css
/* 수정 전 */
.main-page {
  padding: 20px;
}

.boards-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 수정 후 */
.main-page {
  padding: 45px 120px;  /* 패딩 1.5배 증가 */
}

.boards-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);  /* 2열 그리드 */
  gap: 20px;
}

/* 반응형: 900px 이하에서 1열로 전환 */
@media (max-width: 900px) {
  .boards-content {
    grid-template-columns: 1fr;
  }
}

/* 반응형: 1100px 이하에서 패딩 조정 */
@media (max-width: 1100px) {
  .main-page {
    padding: 30px 60px;
  }
}
```

**레이아웃 변화**:
```
변경 전 (1열):          변경 후 (2열):
┌─────────────┐        ┌──────┬──────┐
│ 게시판 1    │        │ 게시판1│게시판2│
├─────────────┤        ├──────┼──────┤
│ 게시판 2    │        │ 게시판3│게시판4│
├─────────────┤        └──────┴──────┘
│ 게시판 3    │
├─────────────┤
│ 게시판 4    │
└─────────────┘
```

---

### 📊 API 엔드포인트 추가

#### 새로 추가된 REST API

| 메서드 | 엔드포인트 | 설명 | 요청 본문 |
|--------|-----------|------|----------|
| GET | `/api/boards` | 게시판 목록 조회 | - |
| GET | `/api/boards/{id}` | 게시판 상세 조회 | - |
| POST | `/api/boards` | 게시판 생성 | Board JSON |
| GET | `/api/posts/board/{boardId}` | 게시판별 게시글 목록 | - |
| POST | `/api/posts/{id}/view` | 조회수 증가 | - |
| POST | `/api/posts/{id}/like` | 좋아요 증가 | - |
| GET | `/api/comments/post/{postId}` | 게시글별 댓글 목록 | - |
| POST | `/api/comments` | 댓글 작성 | Comment JSON |
| POST | `/api/comments/{id}/like` | 댓글 좋아요 | - |
| DELETE | `/api/comments/{id}` | 댓글 삭제 (Soft) | - |

---

### 🗄️ 데이터베이스 스키마

#### 새로 생성되는 테이블

**1. boards 테이블**
```sql
CREATE TABLE boards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    university_id BIGINT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    require_admin BOOLEAN DEFAULT FALSE,
    created_at DATETIME
);
```

**2. posts 테이블**
```sql
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    anonymous_id VARCHAR(50),
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    scrap_count INT DEFAULT 0,
    is_notice BOOLEAN DEFAULT FALSE,
    is_blinded BOOLEAN DEFAULT FALSE,
    blind_reason TEXT,
    reported_count INT DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (board_id) REFERENCES boards(id)
);
```

**3. comments 테이블**
```sql
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    anonymous_id VARCHAR(50),
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (parent_id) REFERENCES comments(id)
);
```

**관계도**:
```
boards (1) ─────< (N) posts (1) ─────< (N) comments
                                              │
                                              │ (self-reference)
                                              └─< parent_id
```

---

### 🎨 UI/UX 개선사항

#### 1. 메인 페이지
- **레이아웃**: 1열 → 2열 그리드
- **패딩**: 좌우 80px → 120px (더 넓은 여백)
- **반응형**: 900px 이하에서 1열로 자동 전환

#### 2. 게시글 목록
- **정렬**: 최신순, 인기순, 조회순
- **검색**: 제목 검색 기능
- **페이지네이션**: 20개씩 표시

#### 3. 게시글 상세
- **조회수**: 페이지 진입 시 자동 증가
- **좋아요**: 클릭 시 즉시 반영
- **댓글**: 실시간 작성/조회

#### 4. 댓글 시스템
- **계층 구조**: 댓글 → 대댓글 (2단계)
- **인덴트**: 대댓글은 왼쪽으로 40px 들여쓰기
- **Soft Delete**: 삭제 시 "削除されたコメントです" 표시

---

## 백엔드 구조

### 1. 엔티티 (Entity) - 데이터 모델

#### Board.java - 게시판 엔티티
```
boards 테이블
├── id (PK)
├── name (게시판 이름)
├── code (게시판 코드, 고유값)
├── description (설명)
├── is_anonymous (익명 여부)
├── require_admin (관리자 전용 여부)
└── created_at (생성일시)
```

**주요 필드 설명:**
- `code`: 게시판을 구분하는 고유 코드 (예: GENERAL, ANONYMOUS)
- `isAnonymous`: true면 작성자가 익명으로 표시됨

#### Post.java - 게시글 엔티티
```
posts 테이블
├── id (PK)
├── board_id (FK → boards)
├── user_id (작성자 ID)
├── title (제목)
├── content (내용)
├── anonymous_id (익명 ID)
├── view_count (조회수)
├── like_count (좋아요 수)
├── dislike_count (싫어요 수)
├── comment_count (댓글 수)
├── is_notice (공지 여부)
├── is_blinded (블라인드 여부)
├── created_at (작성일시)
└── updated_at (수정일시)
```

**주요 기능:**
- `@PrePersist`: 엔티티 저장 전에 자동으로 `createdAt` 설정
- `@PreUpdate`: 엔티티 수정 전에 자동으로 `updatedAt` 설정

#### Comment.java - 댓글 엔티티
```
comments 테이블
├── id (PK)
├── post_id (FK → posts)
├── user_id (작성자 ID)
├── parent_id (FK → comments, 대댓글용)
├── content (내용)
├── anonymous_id (익명 ID)
├── like_count (좋아요 수)
├── is_deleted (삭제 여부)
├── created_at (작성일시)
└── updated_at (수정일시)
```

**계층 구조 설명:**
- `parent_id`가 `null`: 최상위 댓글
- `parent_id`에 값이 있음: 대댓글 (해당 댓글의 답글)

---

### 2. 리포지토리 (Repository) - 데이터 접근 계층

Spring Data JPA를 사용하면 **인터페이스만 정의**하면 자동으로 구현됩니다.

#### PostRepository.java 예시
```java
public interface PostRepository extends JpaRepository<Post, Long> {
    // 메서드 이름으로 쿼리 자동 생성
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByTitleContaining(String keyword);
    List<Post> findByBoardIdOrderByCreatedAtDesc(Long boardId);
}
```

**명명 규칙:**
- `findBy`: SELECT 쿼리
- `OrderBy`: 정렬
- `Containing`: LIKE 검색
- `Desc`: 내림차순

**실제 생성되는 SQL 예시:**
```sql
-- findByBoardIdOrderByCreatedAtDesc(1L)
SELECT * FROM posts
WHERE board_id = 1
ORDER BY created_at DESC;
```

---

### 3. 서비스 (Service) - 비즈니스 로직 계층

#### PostService.java 주요 메서드

```java
@Service
@Transactional  // 트랜잭션 관리
public class PostService {

    @Autowired
    private PostRepository postRepository;

    // 게시글 생성
    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    // 조회수 증가
    public void incrementViewCount(Long id) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }
}
```

**핵심 개념:**
- `@Transactional`: 메서드 실행 중 오류 발생 시 자동으로 롤백
- `orElseThrow()`: Optional에서 값이 없으면 예외 발생

---

### 4. 컨트롤러 (Controller) - API 엔드포인트

#### PostController.java 예시

```java
@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    @Autowired
    private PostService postService;

    // GET /api/posts/board/{boardId}
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Post>> getPostsByBoardId(
        @PathVariable Long boardId
    ) {
        List<Post> posts = postService.getPostsByBoardId(boardId);
        return ResponseEntity.ok(posts);
    }

    // POST /api/posts/{id}/like
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> incrementLikeCount(
        @PathVariable Long id
    ) {
        postService.incrementLikeCount(id);
        return ResponseEntity.ok().build();
    }
}
```

**어노테이션 설명:**
- `@RestController`: JSON 응답 자동 변환
- `@RequestMapping`: 기본 URL 경로 설정
- `@GetMapping`: HTTP GET 요청 처리
- `@PostMapping`: HTTP POST 요청 처리
- `@PathVariable`: URL 경로의 변수 추출
- `@CrossOrigin`: CORS 허용 (프론트엔드와 통신)

---

## 프론트엔드 구조

### 1. 페이지 구조

```
frontend/src/pages/
├── Main/
│   ├── MainPage.js          # 메인 화면 (게시판 2열 레이아웃)
│   └── MainPage.css
├── Board/
│   ├── PostListPage.js      # 게시글 목록
│   ├── PostListPage.css
│   ├── PostDetailPage.js    # 게시글 상세
│   ├── PostDetailPage.css
│   ├── PostForm.js          # 게시글 작성 폼
│   ├── PostForm.css
│   ├── CommentSection.js    # 댓글 섹션
│   └── CommentSection.css
```

---

### 2. React 컴포넌트 분석

#### MainPage.js - 메인 화면

**주요 기능:**
1. 게시판 목록 불러오기
2. 각 게시판의 최신 게시글 5개 표시
3. 인기 게시글 TOP 10 사이드바 표시

**핵심 코드:**
```javascript
const [boards, setBoards] = useState([]);
const [boardPosts, setBoardPosts] = useState({});

// 데이터 가져오기
const fetchBoardsAndPosts = async () => {
  const boardsResponse = await axios.get('/ej2/api/boards');
  const boardsData = boardsResponse.data;
  setBoards(boardsData);

  // 각 게시판의 게시글 가져오기
  for (const board of boardsData) {
    const postsResponse = await axios.get(
      `/ej2/api/posts/board/${board.id}`
    );
    postsData[board.id] = postsResponse.data;
  }
};
```

**useState 설명:**
- `useState([])`: 빈 배열로 초기화된 상태 변수
- `setBoards(data)`: 상태 업데이트 → 화면 자동 리렌더링

---

#### PostListPage.js - 게시글 목록

**주요 기능:**
1. 게시판별 게시글 목록 표시
2. 정렬 기능 (최신순, 인기순, 조회순)
3. 검색 기능
4. 페이지네이션

**상태 관리:**
```javascript
const [posts, setPosts] = useState([]);
const [currentPage, setCurrentPage] = useState(0);
const [sortBy, setSortBy] = useState('recent');
const [searchKeyword, setSearchKeyword] = useState('');
```

**useEffect 활용:**
```javascript
// sortBy나 currentPage가 변경되면 자동으로 게시글 다시 로드
useEffect(() => {
  fetchPosts();
}, [boardId, currentPage, sortBy]);
```

---

#### PostDetailPage.js - 게시글 상세

**주요 기능:**
1. 게시글 상세 내용 표시
2. 조회수 자동 증가
3. 좋아요 버튼
4. 댓글 섹션 통합

**조회수 증가 로직:**
```javascript
useEffect(() => {
  fetchPost();
  incrementViewCount();  // 페이지 진입 시 조회수 +1
}, [postId]);

const incrementViewCount = async () => {
  await axios.post(`/ej2/api/posts/${postId}/view`);
};
```

---

#### CommentSection.js - 댓글 컴포넌트

**주요 기능:**
1. 댓글 목록 표시
2. 댓글 작성
3. 대댓글 (답글) 작성
4. 댓글 좋아요
5. 댓글 삭제

**계층 구조 처리:**
```javascript
// 최상위 댓글만 필터링
const topLevelComments = comments.filter(c => !c.parentId);

// 특정 댓글의 답글 가져오기
const getReplies = (parentId) =>
  comments.filter(c => c.parentId === parentId);

// 렌더링
topLevelComments.map(comment => (
  <div>
    {comment.content}
    {getReplies(comment.id).map(reply => (
      <div className="reply">{reply.content}</div>
    ))}
  </div>
))
```

---

### 3. CSS Grid 레이아웃

#### 메인 페이지 2열 레이아웃

```css
/* 게시판 섹션을 2열로 배치 */
.boards-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

/* 반응형: 900px 이하에서 1열로 전환 */
@media (max-width: 900px) {
  .boards-content {
    grid-template-columns: 1fr;
  }
}
```

**핵심 개념:**
- `grid-template-columns: repeat(2, 1fr)`: 동일한 너비의 2개 열
- `1fr`: 사용 가능한 공간을 균등 분할
- `gap`: 그리드 아이템 간 간격

---

## 핵심 개념 설명

### 1. RESTful API 설계

**REST 원칙:**
- 리소스를 URL로 표현
- HTTP 메서드로 행위 표현
- 상태 없는(Stateless) 통신

**예시:**
```
GET    /api/boards              → 게시판 목록 조회
GET    /api/boards/1            → 1번 게시판 조회
POST   /api/boards              → 새 게시판 생성
PUT    /api/boards/1            → 1번 게시판 수정
DELETE /api/boards/1            → 1번 게시판 삭제

GET    /api/posts/board/1       → 1번 게시판의 게시글 목록
POST   /api/posts/1/like        → 1번 게시글 좋아요
POST   /api/posts/1/view        → 1번 게시글 조회수 증가
```

---

### 2. JPA 어노테이션

#### @Entity
- 이 클래스가 데이터베이스 테이블과 매핑됨을 표시
- 클래스 이름 = 테이블 이름 (기본값)

#### @Table(name = "posts")
- 테이블 이름 직접 지정

#### @Id
- 기본 키(Primary Key)

#### @GeneratedValue(strategy = GenerationType.IDENTITY)
- 자동 증가(Auto Increment)

#### @Column
- 컬럼 속성 지정
```java
@Column(nullable = false)        // NOT NULL
@Column(unique = true)           // UNIQUE
@Column(columnDefinition = "TEXT") // 타입 지정
@Column(name = "user_id")        // 컬럼명 지정
```

#### @PrePersist / @PreUpdate
- 엔티티 저장/수정 전에 자동 실행되는 메서드

---

### 3. Spring Data JPA 쿼리 메서드

**메서드 이름으로 쿼리 자동 생성:**

```java
// SELECT * FROM posts WHERE board_id = ?
findByBoardId(Long boardId)

// SELECT * FROM posts WHERE board_id = ? ORDER BY created_at DESC
findByBoardIdOrderByCreatedAtDesc(Long boardId)

// SELECT * FROM posts WHERE title LIKE %?%
findByTitleContaining(String keyword)

// SELECT COUNT(*) FROM comments WHERE post_id = ?
countByPostId(Long postId)
```

---

### 4. React Hooks

#### useState
- 컴포넌트의 상태 관리
```javascript
const [count, setCount] = useState(0);
setCount(count + 1);  // 상태 업데이트 → 리렌더링
```

#### useEffect
- 사이드 이펙트 처리 (API 호출, 이벤트 리스너 등)
```javascript
// 컴포넌트 마운트 시 1회 실행
useEffect(() => {
  fetchData();
}, []);

// postId 변경될 때마다 실행
useEffect(() => {
  fetchPost();
}, [postId]);
```

#### useNavigate
- 프로그래밍 방식 페이지 이동
```javascript
const navigate = useNavigate();
navigate('/boards/1/posts');
navigate('/boards/1/posts/5', { state: { board } });
```

#### useParams
- URL 파라미터 추출
```javascript
// URL: /boards/1/posts/5
const { boardId, postId } = useParams();
// boardId = "1", postId = "5"
```

---

### 5. Soft Delete (논리 삭제)

**개념:**
- 실제로 데이터를 삭제하지 않고 플래그만 변경
- 데이터 복구 가능
- 관계 유지 (댓글 트리 구조)

**구현:**
```java
public void deleteComment(Long id) {
    Comment comment = commentRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Not found"));

    // 실제 삭제 X
    comment.setIsDeleted(true);
    comment.setContent("削除されたコメントです。");
    commentRepository.save(comment);
}
```

---

### 6. 계층 구조 데이터 모델링

**Self-Referencing (자기 참조) 관계:**

```
댓글 테이블
id | parent_id | content
1  | null      | "좋은 글이네요"         ← 최상위 댓글
2  | 1         | "감사합니다"            ← 1번의 답글
3  | 1         | "저도 그렇게 생각해요"  ← 1번의 답글
4  | null      | "궁금한 점이 있어요"    ← 최상위 댓글
5  | 4         | "무엇이 궁금하신가요?"  ← 4번의 답글
```

**조회 로직:**
```java
// 최상위 댓글만 조회
findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId)

// 특정 댓글의 답글 조회
findByParentIdOrderByCreatedAtAsc(Long parentId)
```

---

## 코드 실행 방법

### 1. 사전 준비

**필요한 소프트웨어:**
- Java 8
- Maven
- Node.js & npm
- MariaDB 10.6
- Apache Tomcat 9

---

### 2. Docker Compose로 실행 (권장)

```bash
# 프로젝트 루트에서
docker-compose up --build

# 접속
# 프론트엔드: http://localhost:3000
# 백엔드: http://localhost:8080/ej2/api
```

---

### 3. 로컬 개발 환경 실행

#### 백엔드 실행

```bash
cd backend

# Maven 빌드
mvn clean package

# WAR 파일 생성 확인
ls target/ej2.war

# Tomcat에 배포
cp target/ej2.war $TOMCAT_HOME/webapps/

# Tomcat 시작
$TOMCAT_HOME/bin/startup.sh

# 로그 확인
tail -f $TOMCAT_HOME/logs/catalina.out
```

#### 프론트엔드 실행

```bash
cd frontend

# 의존성 설치 (최초 1회)
npm install

# 개발 서버 시작
npm start

# 브라우저 자동 열림: http://localhost:3000
```

---

### 4. API 테스트

**curl 명령어:**

```bash
# 게시판 목록 조회
curl http://localhost:8080/ej2/api/boards

# 게시글 목록 조회
curl http://localhost:8080/ej2/api/posts/board/1

# 게시글 상세 조회
curl http://localhost:8080/ej2/api/posts/1

# 댓글 목록 조회
curl http://localhost:8080/ej2/api/comments/post/1

# 게시글 생성
curl -X POST http://localhost:8080/ej2/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "boardId": 1,
    "userId": 1,
    "title": "テスト投稿",
    "content": "これはテストです。"
  }'

# 좋아요
curl -X POST http://localhost:8080/ej2/api/posts/1/like
```

---

## 트러블슈팅

### 문제 1: CORS 에러

**증상:**
```
Access to XMLHttpRequest has been blocked by CORS policy
```

**원인:**
- 백엔드와 프론트엔드의 도메인이 다름
- 브라우저 보안 정책

**해결:**
```java
// Controller에 추가
@CrossOrigin(origins = "http://localhost:3000")
```

---

### 문제 2: 404 Not Found (API 호출 실패)

**확인 사항:**
1. Tomcat이 정상 실행 중인가?
2. WAR 파일이 배포되었는가?
3. URL이 정확한가? (`/ej2/api/...`)

**로그 확인:**
```bash
tail -f $TOMCAT_HOME/logs/catalina.out
```

---

### 문제 3: 데이터베이스 연결 실패

**증상:**
```
Could not open JPA EntityManager for transaction
```

**확인 사항:**
1. MariaDB가 실행 중인가?
```bash
docker ps | grep mariadb
```

2. 연결 정보가 정확한가?
```java
// RootConfig.java
dataSource.setJdbcUrl("jdbc:mariadb://localhost:3306/appdb");
dataSource.setUsername("appuser");
dataSource.setPassword("apppassword");
```

---

### 문제 4: 프론트엔드 빌드 에러

**증상:**
```
Module not found: Can't resolve './CommentSection'
```

**해결:**
1. 파일 경로 확인
2. import 문 확인
3. 파일 이름 대소문자 확인

---

### 문제 5: Java 버전 불일치

**증상:**
```
Unsupported class file major version
```

**해결:**
```bash
# Java 버전 확인
java -version

# Java 8 사용
export JAVA_HOME=/path/to/java8
```

---

## 다음 단계

### 추가 학습 주제
1. **인증/인가**: Spring Security 적용
2. **파일 업로드**: 게시글에 이미지 첨부
3. **검색 최적화**: Elasticsearch 연동
4. **실시간 알림**: WebSocket 구현
5. **성능 최적화**: 캐싱 (Redis)

### 개선 아이디어
- [ ] 게시글 스크랩 기능
- [ ] 신고 기능
- [ ] 관리자 페이지
- [ ] 이메일 알림
- [ ] 마크다운 에디터

---

## 참고 자료

### 공식 문서
- [Spring Framework](https://spring.io/projects/spring-framework)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [React 공식 문서](https://react.dev/)
- [Axios](https://axios-http.com/)

### 추천 학습 자료
- [백기선의 Spring 완전 정복](https://www.inflearn.com/roadmaps/373)
- [생활코딩 - React](https://opentutorials.org/module/4058)
- [TCP School - SQL](http://www.tcpschool.com/mysql/intro)

---

## 결론

이 문서를 통해 다음을 배웠습니다:

1. ✅ **Spring 3계층 아키텍처** 이해
2. ✅ **JPA 엔티티 설계** 및 관계 매핑
3. ✅ **RESTful API** 설계 및 구현
4. ✅ **React Hooks** 활용한 상태 관리
5. ✅ **계층 구조 데이터** 모델링
6. ✅ **CSS Grid** 레이아웃

게시판 시스템은 웹 개발의 핵심 기능을 모두 포함하고 있어, 이를 마스터하면 대부분의 웹 애플리케이션을 개발할 수 있는 기초가 완성됩니다.

**다음 스텝**: 실제로 코드를 수정하고, 새로운 기능을 추가하며 학습을 이어가세요! 🚀

---

**작성일**: 2026-01-22
**작성자**: Claude AI
**버전**: 1.0
