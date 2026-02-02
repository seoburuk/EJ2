# 변경사항 0202_1 - 관리자 대시보드 개선

## 개요
관리자 페이지의 "ユーザー分布" (유저 분포) 차트를 **게시판별 게시글 수 + 1주간 증가수** 차트로 변경하고, 전체 색상을 눈에 편한 하늘색/무채색 계열로 수정했습니다.

---

## 1. 백엔드 변경사항

### 1.1 새 DTO 생성
**파일:** `backend/src/main/java/com/ej2/dto/BoardPostStatsDTO.java`

게시판별 통계를 담는 새로운 DTO 클래스입니다.

```java
public class BoardPostStatsDTO {
    private Long boardId;      // 게시판 ID
    private String boardName;  // 게시판 이름
    private Long totalPosts;   // 총 게시글 수
    private Long weeklyIncrease; // 1주간 증가수
}
```

### 1.2 MyBatis Mapper 인터페이스 수정
**파일:** `backend/src/main/java/com/ej2/mapper/AdminMapper.java`

새 메서드 추가:
```java
List<BoardPostStatsDTO> selectBoardPostStats();
```

### 1.3 MyBatis XML 쿼리 추가
**파일:** `backend/src/main/resources/mappers/AdminMapper.xml`

게시판별 통계를 조회하는 SQL 쿼리:
```xml
<select id="selectBoardPostStats" resultType="com.ej2.dto.BoardPostStatsDTO">
    SELECT
        b.id AS boardId,
        b.name AS boardName,
        COALESCE(total.cnt, 0) AS totalPosts,
        COALESCE(weekly.cnt, 0) AS weeklyIncrease
    FROM boards b
    LEFT JOIN (
        SELECT board_id, COUNT(*) AS cnt
        FROM posts
        GROUP BY board_id
    ) total ON b.id = total.board_id
    LEFT JOIN (
        SELECT board_id, COUNT(*) AS cnt
        FROM posts
        WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
        GROUP BY board_id
    ) weekly ON b.id = weekly.board_id
    ORDER BY totalPosts DESC
    LIMIT 10
</select>
```

**쿼리 설명:**
- `LEFT JOIN`: 게시글이 없는 게시판도 포함
- `COALESCE`: NULL을 0으로 변환
- `DATE_SUB(NOW(), INTERVAL 7 DAY)`: 최근 7일간 데이터
- `ORDER BY totalPosts DESC LIMIT 10`: 게시글 많은 순으로 상위 10개

### 1.4 Service 메서드 추가
**파일:** `backend/src/main/java/com/ej2/service/AdminService.java`

```java
public List<Map<String, Object>> getBoardPostStats() {
    List<BoardPostStatsDTO> stats = adminMapper.selectBoardPostStats();
    List<Map<String, Object>> result = new ArrayList<>();

    for (BoardPostStatsDTO stat : stats) {
        Map<String, Object> boardData = new HashMap<>();
        boardData.put("boardId", stat.getBoardId());
        boardData.put("name", stat.getBoardName());
        boardData.put("totalPosts", stat.getTotalPosts());
        boardData.put("weeklyIncrease", stat.getWeeklyIncrease());
        result.add(boardData);
    }
    return result;
}
```

### 1.5 Controller 엔드포인트 추가
**파일:** `backend/src/main/java/com/ej2/controller/AdminController.java`

```java
@GetMapping("/dashboard/board-stats")
public ResponseEntity<?> getBoardPostStats(HttpSession session) {
    ResponseEntity<?> accessCheck = checkAdminAccess(session);
    if (accessCheck != null) return accessCheck;

    List<Map<String, Object>> boardStats = adminService.getBoardPostStats();
    return ResponseEntity.ok(boardStats);
}
```

**API 정보:**
- **URL:** `GET /api/admin/dashboard/board-stats`
- **인증:** 관리자 권한 필요
- **응답 예시:**
```json
[
  {"boardId": 1, "name": "자유게시판", "totalPosts": 150, "weeklyIncrease": 23},
  {"boardId": 2, "name": "질문게시판", "totalPosts": 89, "weeklyIncrease": 12}
]
```

---

## 2. 프론트엔드 변경사항

### 2.1 AdminPage.js 수정
**파일:** `frontend/src/pages/Admin/AdminPage.js`

#### Import 변경
```javascript
// Before
import { PieChart, Pie, Cell } from 'recharts';

// After
import { BarChart, Bar, Legend } from 'recharts';
```

#### State 추가
```javascript
const [boardStats, setBoardStats] = useState([]);
```

#### API 호출 추가
```javascript
const [statsRes, weeklyRes, activityRes, boardStatsRes] = await Promise.all([
  axios.get('/api/admin/dashboard', { withCredentials: true }),
  axios.get('/api/admin/dashboard/weekly', { withCredentials: true }),
  axios.get('/api/admin/dashboard/activity', { withCredentials: true }),
  axios.get('/api/admin/dashboard/board-stats', { withCredentials: true })  // 추가
]);
setBoardStats(boardStatsRes.data);
```

#### 차트 변경 (PieChart → BarChart)
```jsx
<BarChart data={boardStats} layout="vertical" margin={{ left: 20, right: 20 }}>
  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
  <XAxis type="number" stroke="rgba(255,255,255,0.7)" />
  <YAxis type="category" dataKey="name" stroke="rgba(255,255,255,0.7)" width={80} />
  <Tooltip ... />
  <Legend ... />
  <Bar dataKey="totalPosts" fill="#4fc3f7" name="totalPosts" radius={[0, 4, 4, 0]} />
  <Bar dataKey="weeklyIncrease" fill="#90a4ae" name="weeklyIncrease" radius={[0, 4, 4, 0]} />
</BarChart>
```

### 2.2 색상 변경
**파일:** `frontend/src/pages/Admin/AdminPages.css`

#### CSS 변수 변경
```css
/* Before (보라색 계열) */
--admin-primary: #667eea;
--admin-secondary: #764ba2;
--admin-purple: #9D50BB;

/* After (하늘색/무채색 계열) */
--admin-primary: #4a90a4;
--admin-secondary: #2c3e50;
--admin-purple: #546e7a;
```

#### 배경색 & 패딩 변경
```css
/* Before */
.admin-dashboard {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
}

/* After */
.admin-dashboard {
  background: linear-gradient(135deg, #37474f 0%, #263238 100%);
  padding: 32px 40px;
}
```

#### 통계 카드 색상 (AdminPage.js)
| 항목 | Before | After |
|------|--------|-------|
| 총 유저수 | `#667eea` (보라) | `#4fc3f7` (하늘색) |
| 관리자수 | `#f6d365` (노랑) | `#78909c` (블루그레이) |
| 게시판수 | `#11998e` (초록) | `#26a69a` (청록색) |
| 투고수 | `#9D50BB` (자주) | `#90a4ae` (라이트그레이) |

#### 차트 색상
| 항목 | Before | After |
|------|--------|-------|
| 등록 | `#667eea` (보라) | `#4fc3f7` (하늘색) |
| 투고 | `#11998e` (초록) | `#26a69a` (청록색) |
| 코멘트 | `#f6d365` (노랑) | `#90a4ae` (무채색) |

---

## 3. 변경된 파일 목록

### 백엔드 (5개)
1. `backend/src/main/java/com/ej2/dto/BoardPostStatsDTO.java` - **새 파일**
2. `backend/src/main/java/com/ej2/mapper/AdminMapper.java` - 수정
3. `backend/src/main/resources/mappers/AdminMapper.xml` - 수정
4. `backend/src/main/java/com/ej2/service/AdminService.java` - 수정
5. `backend/src/main/java/com/ej2/controller/AdminController.java` - 수정

### 프론트엔드 (2개)
1. `frontend/src/pages/Admin/AdminPage.js` - 수정
2. `frontend/src/pages/Admin/AdminPages.css` - 수정

---

## 4. 적용 방법

### 백엔드 재빌드
```bash
cd backend
mvn clean package
```

### 프론트엔드 재시작
```bash
cd frontend
npm start
```

### Docker 사용 시
```bash
docker-compose down
docker-compose up --build
```

---

## 5. 결과 화면

### Before
- 도넛 차트로 관리자/일반 유저 비율 표시
- 보라색 계열 배경

### After
- 수평 막대 차트로 게시판별 게시글 수 + 주간 증가수 표시
- 하늘색/무채색 계열의 눈에 편한 배경
- 좌우 패딩 증가로 여유로운 레이아웃

---

## 6. 핵심 개념 정리 (초보자용)

### MyBatis LEFT JOIN 이해하기
```sql
FROM boards b
LEFT JOIN (...) total ON b.id = total.board_id
```
- `LEFT JOIN`: 왼쪽 테이블(boards)의 모든 행을 포함
- 게시글이 없는 게시판도 결과에 포함됨
- `INNER JOIN`을 쓰면 게시글 있는 게시판만 나옴

### COALESCE 함수
```sql
COALESCE(total.cnt, 0) AS totalPosts
```
- 첫 번째 인자가 NULL이면 두 번째 인자 반환
- 게시글이 없으면 NULL 대신 0으로 표시

### Recharts BarChart layout="vertical"
```jsx
<BarChart layout="vertical">
```
- `layout="vertical"`: 막대가 가로로 표시됨
- 게시판 이름이 Y축에, 숫자가 X축에 표시
- 긴 이름의 게시판도 가독성 좋음
