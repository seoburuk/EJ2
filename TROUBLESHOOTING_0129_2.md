# Troubleshooting Guide 0129_2 - 댓글 
좋아요 무한 클릭 문제 해결

## 문제 설명

**증상**: 댓글 좋아요 버튼을 무한히 클릭할 수 있어서 좋아요 수가 계속 증가함

**원인**: 기존 코드는 단순히 `likeCount`를 증가시키기만 하고, 사용자가 이미 좋아요를 눌렀는지 확인하지 않음

### 기존 문제 코드

```java
// CommentService.java (수정 전)
public void incrementLikeCount(Long id) {
    Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
    comment.setLikeCount(comment.getLikeCount() + 1);  // 무한 증가 가능!
    commentRepository.save(comment);
}
```

---

## 해결 방법

게시글 좋아요(`PostLikeLog`)와 동일한 패턴으로 `CommentLikeLog` 테이블을 생성하여 중복 좋아요를 방지

### 1. CommentLikeLog 엔티티 생성

**파일**: `backend/src/main/java/com/ej2/model/CommentLikeLog.java`

```java
package com.ej2.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comment_like_logs")
public class CommentLikeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "user_id")
    private Long userId;  // 로그인 사용자용

    @Column(name = "ip_address", length = 50)
    private String ipAddress;  // 비로그인 사용자용

    @Column(name = "liked_at")
    private LocalDateTime likedAt;

    @PrePersist
    protected void onCreate() {
        likedAt = LocalDateTime.now();
    }

    // Constructors, Getters, Setters...
}
```

### 2. CommentLikeLogRepository 생성

**파일**: `backend/src/main/java/com/ej2/repository/CommentLikeLogRepository.java`

```java
package com.ej2.repository;

import com.ej2.model.CommentLikeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeLogRepository extends JpaRepository<CommentLikeLog, Long> {

    // 로그인 사용자 중복 체크
    Optional<CommentLikeLog> findByCommentIdAndUserId(Long commentId, Long userId);

    // 비로그인 사용자(IP 기반) 중복 체크
    Optional<CommentLikeLog> findByCommentIdAndIpAddress(Long commentId, String ipAddress);
}
```

### 3. CommentService 수정

**파일**: `backend/src/main/java/com/ej2/service/CommentService.java`

```java
@Autowired
private CommentLikeLogRepository commentLikeLogRepository;

/**
 * Toggle like on a comment. Returns true if liked, false if unliked.
 */
public boolean toggleLike(Long commentId, Long userId, String ipAddress) {
    Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

    Optional<CommentLikeLog> existingLike;

    if (userId != null) {
        existingLike = commentLikeLogRepository.findByCommentIdAndUserId(commentId, userId);
    } else {
        existingLike = commentLikeLogRepository.findByCommentIdAndIpAddress(commentId, ipAddress);
    }

    if (existingLike.isPresent()) {
        // 이미 좋아요 -> 취소
        commentLikeLogRepository.delete(existingLike.get());
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentRepository.save(comment);
        return false;
    } else {
        // 좋아요 추가
        CommentLikeLog likeLog = new CommentLikeLog(commentId, userId, ipAddress);
        commentLikeLogRepository.save(likeLog);
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
        return true;
    }
}

/**
 * Check if user has liked a specific comment
 */
public boolean hasUserLiked(Long commentId, Long userId, String ipAddress) {
    if (userId != null) {
        return commentLikeLogRepository.findByCommentIdAndUserId(commentId, userId).isPresent();
    } else {
        return commentLikeLogRepository.findByCommentIdAndIpAddress(commentId, ipAddress).isPresent();
    }
}
```

### 4. CommentController 수정

**파일**: `backend/src/main/java/com/ej2/controller/CommentController.java`

```java
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

// POST /api/comments/{id}/like - Toggle like on comment
@PostMapping("/{id}/like")
public ResponseEntity<Map<String, Object>> toggleLike(
        @PathVariable Long id,
        @RequestParam(required = false) Long userId,
        HttpServletRequest request) {
    String ipAddress = getClientIpAddress(request);
    boolean liked = commentService.toggleLike(id, userId, ipAddress);

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("liked", liked);
    return ResponseEntity.ok(response);
}

// GET /api/comments/{id}/like/check - Check if user has liked
@GetMapping("/{id}/like/check")
public ResponseEntity<Map<String, Object>> checkLikeStatus(
        @PathVariable Long id,
        @RequestParam(required = false) Long userId,
        HttpServletRequest request) {
    String ipAddress = getClientIpAddress(request);
    boolean liked = commentService.hasUserLiked(id, userId, ipAddress);

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("liked", liked);
    return ResponseEntity.ok(response);
}

private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
}
```

### 5. Frontend 수정

**파일**: `frontend/src/pages/Board/CommentSection.js`

```javascript
const [likedComments, setLikedComments] = useState({});

const fetchComments = async () => {
  try {
    const response = await axios.get(`/api/comments/post/${postId}`);
    setComments(response.data);

    // 각 댓글의 좋아요 상태 확인
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const likeStatuses = {};
    for (const comment of response.data) {
      try {
        const likeCheck = await axios.get(`/api/comments/${comment.id}/like/check`, {
          params: { userId: user.id || null }
        });
        likeStatuses[comment.id] = likeCheck.data.liked;
      } catch (e) {
        likeStatuses[comment.id] = false;
      }
    }
    setLikedComments(likeStatuses);
    setLoading(false);
  } catch (error) {
    console.error('コメントの読み込みに失敗しました:', error);
    setComments([]);
    setLoading(false);
  }
};

const handleLikeComment = async (commentId) => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const response = await axios.post(`/api/comments/${commentId}/like`, null, {
      params: { userId: user.id || null }
    });

    // 로컬 상태 업데이트
    setLikedComments(prev => ({
      ...prev,
      [commentId]: response.data.liked
    }));

    // 좋아요 수 로컬 업데이트
    setComments(prev => prev.map(comment => {
      if (comment.id === commentId) {
        return {
          ...comment,
          likeCount: response.data.liked
            ? (comment.likeCount || 0) + 1
            : Math.max(0, (comment.likeCount || 0) - 1)
        };
      }
      return comment;
    }));
  } catch (error) {
    console.error('いいねに失敗しました:', error);
  }
};
```

### 6. CSS 스타일 추가

**파일**: `frontend/src/pages/Board/CommentSection.css`

```css
.comment-action-btn.liked {
  background: #e6f3ff;
  border-color: #0366d6;
  color: #0366d6;
}

.comment-action-btn.liked:hover {
  background: #cce5ff;
}
```

---

## 변경된 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `backend/src/main/java/com/ej2/model/CommentLikeLog.java` | 새로 생성 |
| `backend/src/main/java/com/ej2/repository/CommentLikeLogRepository.java` | 새로 생성 |
| `backend/src/main/java/com/ej2/service/CommentService.java` | toggleLike, hasUserLiked 메서드 추가 |
| `backend/src/main/java/com/ej2/controller/CommentController.java` | like API 수정, check API 추가 |
| `frontend/src/pages/Board/CommentSection.js` | 좋아요 상태 추적 로직 추가 |
| `frontend/src/pages/Board/CommentSection.css` | .liked 클래스 스타일 추가 |

---

## 데이터베이스 변경

Hibernate가 자동으로 `comment_like_logs` 테이블 생성:

```sql
CREATE TABLE comment_like_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT,
    ip_address VARCHAR(50),
    liked_at DATETIME
);
```

확인 명령어:
```bash
docker exec mariadb mysql -u appuser -papppassword -e "SHOW TABLES FROM appdb LIKE '%comment%';"
```

---

## 동작 방식

1. **첫 클릭**: 좋아요 추가 (버튼이 파란색으로 변경)
2. **다시 클릭**: 좋아요 취소 (버튼이 원래 색으로 복귀)
3. **중복 방지**:
   - 로그인 사용자: `userId`로 중복 체크
   - 비로그인 사용자: `ipAddress`로 중복 체크

---

## 테스트 방법

1. Docker 재시작: `docker-compose down && docker-compose up --build -d`
2. http://localhost:3000 접속
3. 게시글 상세 페이지로 이동
4. 댓글 좋아요 버튼 클릭
5. 버튼 색상 변경 확인 (파란색 = 좋아요됨)
6. 다시 클릭하여 좋아요 취소 확인
7. 무한 클릭 불가 확인

---

## 핵심 패턴

단순 카운터 증가 대신 **로그 테이블**을 사용하면:
- 누가 언제 좋아요를 눌렀는지 추적 가능
- 좋아요 취소 기능 구현 가능
- 좋아요한 사용자 목록 표시 가능
- 중복 좋아요 방지

이 패턴은 좋아요, 북마크, 팔로우 등 "사용자별 1회 액션" 기능에 공통적으로 적용됩니다.
