# 0205_2 트러블슈팅 및 학습내용 정리

## 개요
2026년 2월 5일 두 번째 세션에서 진행한 버그 수정 과정을 정리한 문서입니다.
프론트엔드 API 호출 오류(404), 권한 체크 불일치(403), 백엔드 DTO 매핑 오류(500) 총 3건을 수정했습니다.

---

## 1. 신고(Report) 제출 시 404 에러

### 증상
```
Failed to load resource: the server responded with a status of 404 ()
報告の提出に失敗しました: AxiosError
```
- 게시글 상세 페이지에서 신고 버튼 클릭 시 404 에러 발생
- 신고 모달에서 사유 선택 후 제출해도 항상 실패

### 원인 분석

**프론트엔드 API 호출 방식의 불일치**

같은 파일(`PostDetailPage.js`) 안에서 두 가지 URL 패턴이 혼재되어 있었다:

| 패턴 | 예시 | 동작 |
|------|------|------|
| 상대 경로 (정상) | `/api/posts/${postId}` | 프록시를 통해 백엔드로 전달 ✅ |
| 절대 경로 (오류) | `http://localhost:8080/api/reports` | 프록시 우회, 직접 요청 ❌ |

**왜 절대 경로가 문제인가:**

1. 개발 환경에서 React 프록시(`setupProxy.js`)가 `/api` 요청을 `http://localhost:8080`으로 전달
2. 절대 경로는 프록시를 건너뛰고 브라우저에서 직접 `localhost:8080`에 요청
3. 백엔드가 컨텍스트 경로(`/ej2`)를 사용하는 환경에서는 실제 엔드포인트가 `/ej2/api/reports`이므로 404 발생
4. 프로덕션(Nginx) 환경에서도 동일하게 실패

**요청 흐름 비교:**
```
[상대 경로 - 정상]
브라우저 → /api/reports → React 프록시(setupProxy.js) → http://localhost:8080/api/reports → 백엔드 ✅

[절대 경로 - 오류]
브라우저 → http://localhost:8080/api/reports → 백엔드 (프록시 우회) → 404 ❌
```

### 수정 내용

**파일: `frontend/src/pages/Board/PostDetailPage.js`**

```javascript
// 수정 전 (절대 경로 - 프록시 우회)
await axios.post(
  'http://localhost:8080/api/reports',
  {
    reportType: 'POST',
    entityId: parseInt(postId),
    reason: reportReason,
    description: reportDescription
  },
  { withCredentials: true }
);

// 수정 후 (상대 경로 - 프록시 경유)
await axios.post(
  '/api/reports',
  {
    reportType: 'POST',
    entityId: parseInt(postId),
    reason: reportReason,
    description: reportDescription
  },
  { withCredentials: true }
);
```

### 학습 포인트

- **상대 경로 vs 절대 경로**: Axios에서 상대 경로(`/api/...`)는 `baseURL` 또는 프록시를 통해 전달되므로 환경에 독립적
- **setupProxy.js의 역할**: 개발 서버에서 `/api`로 시작하는 요청을 백엔드로 전달하는 미들웨어
- **일관성**: 같은 프로젝트 내에서 API 호출 패턴을 통일해야 유지보수가 쉬움
- 이 프로젝트에서 절대 경로를 사용하는 파일이 추가로 존재함 (AdminUsersPage, AdminReportsPage, AdminPage 등) - 향후 수정 필요

---

## 2. 게시판 관리 페이지 403 에러 (권한 불일치)

### 증상
```
GET http://localhost:8080/api/admin/boards 403 (Forbidden)
AdminBoardsPage.js:41 Failed to fetch boards: AxiosError
```
- ADMIN 권한으로 로그인한 사용자가 게시판 관리 페이지에 접근하면 403 에러 발생
- 페이지 자체는 렌더링되지만, API 호출이 모두 거부됨

### 원인 분석

**프론트엔드/백엔드 간 권한 체크 불일치**

| 레이어 | 체크 방식 | ADMIN | SUPER_ADMIN |
|--------|----------|-------|-------------|
| 프론트엔드 (AdminBoardsPage.js) | `user.role !== 'ADMIN'` | 접근 허용 ✅ | 접근 거부 ❌ |
| 백엔드 (AdminController.java) | `checkSuperAdminAccess()` | 403 거부 ❌ | 접근 허용 ✅ |

**문제 1: 프론트엔드에서 ADMIN을 허용하지만 백엔드는 SUPER_ADMIN만 허용**
- 게시판 관리는 백엔드에서 `SUPER_ADMIN` 전용으로 설계됨
- 프론트엔드는 이를 반영하지 않아 ADMIN 유저가 페이지에 진입한 후 API 호출에서 403 발생

**문제 2: 엄격한 동등 비교(`!==`)로 인한 SUPER_ADMIN 잠김 현상**

```javascript
// 수정 전 - 모든 관리자 페이지에서 동일한 패턴
if (!user || user.role !== 'ADMIN') {
  navigate('/');  // SUPER_ADMIN도 여기서 리디렉트됨!
  return false;
}
```

이 코드는 `role`이 정확히 `'ADMIN'`인 경우만 허용하므로, `SUPER_ADMIN` 유저는 모든 관리자 페이지에서 홈으로 리디렉트됨.

백엔드의 `isAdmin()` 메서드와 비교:
```java
// 백엔드 - 올바른 권한 체크
private boolean isAdmin(HttpSession session) {
    String role = currentUser.getRole();
    return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);  // 둘 다 허용
}
```

### 수정 내용

**1. AdminBoardsPage.js - SUPER_ADMIN 전용으로 변경**
```javascript
// 수정 전
const checkAdminAccess = useCallback(() => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (!user || user.role !== 'ADMIN') {  // ADMIN만 허용
    navigate('/');
    return false;
  }
  return true;
}, [navigate]);

// 수정 후
const checkAdminAccess = useCallback(() => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (!user || user.role !== 'SUPER_ADMIN') {  // SUPER_ADMIN만 허용
    navigate('/');
    return false;
  }
  return true;
}, [navigate]);
```

**2. AdminPage.js, AdminReportsPage.js, AdminUsersPage.js - ADMIN + SUPER_ADMIN 허용**
```javascript
// 수정 전 (3개 파일 동일 패턴)
const checkAdminAccess = useCallback(() => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (!user || user.role !== 'ADMIN') {  // SUPER_ADMIN 접근 불가
    navigate('/');
    return false;
  }
  return true;
}, [navigate]);

// 수정 후 (3개 파일 동일 패턴)
const checkAdminAccess = useCallback(() => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (!user || (user.role !== 'ADMIN' && user.role !== 'SUPER_ADMIN')) {  // 둘 다 허용
    navigate('/');
    return false;
  }
  return true;
}, [navigate]);
```

**3. AdminPage.js - 게시판 관리 링크를 SUPER_ADMIN에게만 표시**
```jsx
// 수정 전 - 모든 관리자에게 표시
<Link to="/admin/boards" className="quick-menu-item">
  <FiLayout />
  <span>掲示板管理</span>
  <span className="menu-arrow">→</span>
</Link>

// 수정 후 - SUPER_ADMIN에게만 표시
{currentUser.role === 'SUPER_ADMIN' && (
  <Link to="/admin/boards" className="quick-menu-item">
    <FiLayout />
    <span>掲示板管理</span>
    <span className="menu-arrow">→</span>
  </Link>
)}
```

### 학습 포인트

- **권한 계층 (Role Hierarchy)**: `USER < ADMIN < SUPER_ADMIN` 구조에서 상위 권한은 하위 권한을 포함해야 함
- **엄격한 동등 비교의 함정**: `role !== 'ADMIN'`은 `SUPER_ADMIN`도 거부함. 역할 계층을 고려한 비교가 필요
- **프론트엔드/백엔드 권한 동기화**: 프론트엔드 권한 체크는 UX 가드일 뿐, 실제 보안은 백엔드에서 담당. 그러나 둘이 일치하지 않으면 사용자 혼란 발생
- **공통 헬퍼 패턴**: 권한 체크를 중앙화하면 불일치 방지 가능:
  ```javascript
  const isAdmin = (role) => ['ADMIN', 'SUPER_ADMIN'].includes(role);
  const isSuperAdmin = (role) => role === 'SUPER_ADMIN';
  ```

### 수정 파일 요약

| 파일 | 수정 내용 |
|------|----------|
| `AdminBoardsPage.js` | 권한 체크를 `SUPER_ADMIN`으로 변경 |
| `AdminPage.js` | 권한 체크에 `SUPER_ADMIN` 추가, 게시판 링크 조건부 렌더링 |
| `AdminReportsPage.js` | 권한 체크에 `SUPER_ADMIN` 추가 |
| `AdminUsersPage.js` | 권한 체크에 `SUPER_ADMIN` 추가 |

---

## 3. 관리자 신고 목록 500 에러 (MyBatis DTO 매핑 오류)

### 증상
```
GET http://localhost:8080/api/admin/reports?page=0&size=20&status=&reportType=&sortBy=date&sortOrder=DESC
500 (Internal Server Error)

AdminReportsPage.js:147 報告一覧取得エラー: AxiosError
```
- 관리자 신고 관리 페이지 접근 시 500 에러 발생
- 신고 통계(`/api/admin/reports/stats`)는 정상 동작하지만, 목록 조회만 실패

### 원인 분석

**MyBatis ResultMap과 DTO 클래스 간 필드 불일치**

`ReportMapper.xml`의 ResultMap이 매핑하는 컬럼:
```xml
<resultMap id="ReportResultMap" type="com.ej2.dto.ReportDTO">
    <id property="id" column="id"/>
    <result property="reportType" column="reportType"/>
    <result property="entityId" column="entityId"/>
    <result property="reporterId" column="reporterId"/>
    <result property="reporterName" column="reporterName"/>
    <result property="reason" column="reason"/>
    <result property="description" column="description"/>      <!-- ❌ DTO에 없음 -->
    <result property="status" column="status"/>
    <result property="createdAt" column="createdAt"/>
    <result property="updatedAt" column="updatedAt"/>           <!-- ❌ DTO에 없음 -->
</resultMap>
```

`ReportDTO.java`의 기존 필드:
```java
public class ReportDTO {
    private Long id;              // ✅
    private String reportType;    // ✅
    private Long entityId;        // ✅
    private Long reporterId;      // ✅
    private String reporterName;  // ✅
    private String reason;        // ✅
    // description               // ❌ 누락!
    private String status;        // ✅
    private LocalDateTime createdAt;  // ✅
    // updatedAt                 // ❌ 누락!
}
```

**`description`과 `updatedAt` 필드가 DTO에 존재하지 않아 MyBatis가 setter를 찾지 못함**

SQL 쿼리 자체는 정상 실행됨 (Docker 로그에서 확인):
```sql
SELECT r.id, r.report_type AS reportType, r.entity_id AS entityId,
       r.reporter_id AS reporterId, reporter.name AS reporterName,
       r.reason, r.description, r.status,
       r.created_at AS createdAt, r.updated_at AS updatedAt
FROM reports r
INNER JOIN users reporter ON r.reporter_id = reporter.id
ORDER BY r.created_at DESC
LIMIT 20 OFFSET 0
```

**Docker 로그 분석:**
```
[exec-6] selectReports ==> Preparing: SELECT ... (SQL 실행됨)
[exec-6] selectReports ==> Parameters: 20(Integer), 0(Integer)
[exec-6] Releasing transactional SqlSession
[exec-6] Participating transaction failed - marking existing transaction as rollback-only  ← 결과 매핑 실패
[exec-6] Rolling back JPA transaction
[exec-6] Writing [{success=false, message=報告一覧の取得に失敗しました}]
[exec-6] Completed 500 INTERNAL_SERVER_ERROR
```

**에러가 숨겨진 이유:** AdminController의 catch 블록이 예외 메시지를 로깅하지 않고 제네릭 에러만 반환:
```java
} catch (Exception e) {
    // e.getMessage()나 e.printStackTrace() 없이 제네릭 메시지만 반환
    error.put("message", "報告一覧の取得に失敗しました");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
}
```

### 수정 내용

**파일: `backend/src/main/java/com/ej2/dto/ReportDTO.java`**

```java
// 수정 전 - description, updatedAt 필드 누락
public class ReportDTO {
    private Long id;
    private String reportType;
    private Long entityId;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
    // ... getters/setters
}

// 수정 후 - 누락된 필드 추가
public class ReportDTO {
    private Long id;
    private String reportType;
    private Long entityId;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private String description;        // 추가
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;   // 추가

    // ... 기존 getters/setters ...

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

수정 후 Docker 컨테이너 재빌드 및 재시작 완료:
```bash
docker compose up --build backend -d
```

### 학습 포인트

- **MyBatis ResultMap ↔ DTO 동기화**: JPA와 달리 MyBatis는 매핑 정보(XML)와 자바 객체(DTO)가 별도 파일에 존재하므로 컴파일 타임에 불일치를 감지하지 못함
- **`callSettersOnNulls(true)` 설정의 영향**: `RootConfig.java`에서 이 설정을 활성화하면 NULL 값도 setter를 통해 주입 시도하므로, setter가 없으면 예외 발생
- **에러 로깅의 중요성**: catch 블록에서 `e.getMessage()`나 `e.printStackTrace()`를 로깅하지 않으면 원인 파악이 매우 어려움. 최소한 로그 레벨 ERROR로 기록하는 것을 권장:
  ```java
  } catch (Exception e) {
      logger.error("報告一覧の取得に失敗: ", e);  // 원인을 로깅
      error.put("message", "報告一覧の取得に失敗しました");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
  ```
- **Docker 로그 디버깅**: `docker compose logs backend --tail=300` 명령으로 로그를 확인하고, 스레드 이름(`exec-6` 등)으로 특정 요청의 흐름을 추적 가능

---

## 4. 수정 파일 전체 목록

| 파일 | 수정 내용 | 에러 |
|------|----------|------|
| `frontend/src/pages/Board/PostDetailPage.js` | 신고 API 절대 경로 → 상대 경로 변경 | 404 |
| `frontend/src/pages/Admin/AdminBoardsPage.js` | 권한 체크를 `SUPER_ADMIN`으로 변경 | 403 |
| `frontend/src/pages/Admin/AdminPage.js` | `SUPER_ADMIN` 권한 허용 + 게시판 링크 조건부 표시 | 403 |
| `frontend/src/pages/Admin/AdminReportsPage.js` | `SUPER_ADMIN` 권한 허용 | 403 |
| `frontend/src/pages/Admin/AdminUsersPage.js` | `SUPER_ADMIN` 권한 허용 | 403 |
| `backend/src/main/java/com/ej2/dto/ReportDTO.java` | `description`, `updatedAt` 필드 및 getter/setter 추가 | 500 |

---

## 5. 향후 검토 사항

1. **절대 경로 URL 정리**: `AdminUsersPage.js`, `AdminReportsPage.js`, `AdminPage.js`에 남아있는 `http://localhost:8080` 절대 경로를 상대 경로로 변경
2. **권한 체크 유틸 공통화**: 각 관리자 페이지마다 중복되는 권한 체크 로직을 공통 훅(hook)이나 HOC로 추출
   ```javascript
   // 예시: useAdminAccess 커스텀 훅
   const useAdminAccess = (requiredRole = 'ADMIN') => {
     const navigate = useNavigate();
     const user = JSON.parse(localStorage.getItem('user') || '{}');
     const roles = { USER: 0, ADMIN: 1, SUPER_ADMIN: 2 };
     if (!user || (roles[user.role] || 0) < (roles[requiredRole] || 0)) {
       navigate('/');
       return false;
     }
     return true;
   };
   ```
3. **에러 로깅 강화**: 백엔드 컨트롤러의 catch 블록에 예외 로깅 추가
4. **MyBatis DTO 검증 테스트**: ResultMap과 DTO 간 필드 불일치를 사전 감지하는 단위 테스트 작성

---

*작성일: 2026-02-05*
