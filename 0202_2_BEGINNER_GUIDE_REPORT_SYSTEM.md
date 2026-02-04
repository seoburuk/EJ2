# 신고 시스템 및 유저 정지 기능 초보자 가이드

> 작성일: 2026-02-02
> 대상: Java/Spring 및 React를 처음 접하는 개발자

## 목차

1. [시스템 개요](#1-시스템-개요)
2. [핵심 개념 이해하기](#2-핵심-개념-이해하기)
3. [데이터베이스 구조](#3-데이터베이스-구조)
4. [백엔드 아키텍처](#4-백엔드-아키텍처)
5. [MyBatis 동적 쿼리 이해하기](#5-mybatis-동적-쿼리-이해하기)
6. [프론트엔드 구조](#6-프론트엔드-구조)
7. [주요 기능 흐름도](#7-주요-기능-흐름도)
8. [트러블슈팅 가이드](#8-트러블슈팅-가이드)
9. [실습 예제](#9-실습-예제)

---

## 1. 시스템 개요

### 1.1 무엇을 만들었나?

이 프로젝트는 **커뮤니티 게시판의 신고 관리 시스템**입니다. 다음 두 가지 주요 기능을 제공합니다:

1. **신고 시스템**: 일반 유저가 부적절한 게시글/댓글/유저를 신고할 수 있습니다
2. **정지 시스템**: 관리자가 문제가 있는 유저를 일시적 또는 영구적으로 정지시킬 수 있습니다

### 1.2 왜 이렇게 만들었나?

**문제**: 커뮤니티에 스팸, 욕설, 혐오 발언 등이 올라올 때 관리자가 이를 효율적으로 관리할 방법이 필요했습니다.

**해결책**:
- 유저들이 직접 문제가 있는 콘텐츠를 신고할 수 있게 함
- 관리자가 신고를 검토하고 조치를 취할 수 있는 대시보드 제공
- 반복적으로 문제를 일으키는 유저를 정지시킬 수 있는 기능 제공

### 1.3 기술 스택

| 영역 | 기술 | 설명 |
|------|------|------|
| **백엔드 프레임워크** | Spring Framework 5.3 | Java 기반 웹 애플리케이션 프레임워크 |
| **ORM (간단한 CRUD)** | JPA/Hibernate | 객체를 데이터베이스 테이블로 자동 매핑 |
| **SQL 매퍼 (복잡한 쿼리)** | MyBatis 3.5 | 복잡한 검색/필터링을 위한 동적 SQL |
| **데이터베이스** | MariaDB 10.6 | MySQL 호환 관계형 데이터베이스 |
| **프론트엔드** | React 18.2 | 사용자 인터페이스 라이브러리 |
| **HTTP 클라이언트** | Axios | 백엔드 API 호출 |

---

## 2. 핵심 개념 이해하기

### 2.1 JPA vs MyBatis (하이브리드 아키텍처)

이 프로젝트는 **JPA와 MyBatis를 동시에 사용**합니다. 왜 그럴까요?

#### JPA (Java Persistence API)
**언제 사용?** 간단한 CRUD 작업

```java
// 예: 신고 저장하기 (단순 작업)
Report report = new Report();
report.setReportType("POST");
report.setEntityId(123L);
reportRepository.save(report);  // ← JPA가 자동으로 INSERT 쿼리 생성
```

**장점**:
- 쿼리를 직접 작성하지 않아도 됨
- 객체 지향적으로 코드 작성 가능

**단점**:
- 복잡한 검색/필터링/조인이 어려움

#### MyBatis
**언제 사용?** 복잡한 검색, 동적 필터링, 통계 쿼리

```xml
<!-- 예: 동적 필터링 (사용자가 선택한 조건에 따라 WHERE 절이 달라짐) -->
<select id="selectReports">
  SELECT * FROM reports
  <where>
    <if test="status != null">AND status = #{status}</if>
    <if test="reportType != null">AND report_type = #{reportType}</if>
  </where>
</select>
```

**장점**:
- SQL을 완전히 제어할 수 있음
- 동적 쿼리 작성이 쉬움

**단점**:
- XML 파일에 SQL을 직접 작성해야 함

#### 언제 무엇을 사용할까?

| 작업 | 사용 기술 | 예시 |
|------|----------|------|
| 신고 생성 | JPA | `reportRepository.save(report)` |
| 신고 삭제 | JPA | `reportRepository.deleteById(id)` |
| 신고 검색 (필터링) | MyBatis | `reportMapper.selectReports(criteria)` |
| 신고 통계 | MyBatis | `reportMapper.selectReportStats()` |
| 유저 정지 | JPA | `userRepository.save(user)` |

### 2.2 폴리모픽 엔티티 참조 (Polymorphic Entity Reference)

**문제**: 신고 대상이 게시글일 수도 있고, 댓글일 수도 있고, 유저일 수도 있습니다. 각각 테이블을 따로 만들어야 할까요?

**해결책**: 하나의 `reports` 테이블에서 모든 타입을 처리합니다.

```java
@Entity
@Table(name = "reports")
public class Report {
    private String reportType;  // "POST", "COMMENT", "USER"
    private Long entityId;      // 신고 대상의 ID (게시글 ID, 댓글 ID, 유저 ID)
}
```

**예시**:

| id | report_type | entity_id | reporter_id | reason |
|----|-------------|-----------|-------------|--------|
| 1  | POST        | 42        | 123         | SPAM   |
| 2  | COMMENT     | 99        | 456         | HARASSMENT |
| 3  | USER        | 789       | 123         | HATE_SPEECH |

**장점**:
- 테이블 하나로 모든 신고 관리
- 코드 중복 없음

**MyBatis에서 타입별 데이터 조회**:

```xml
<!-- CASE 문으로 타입에 따라 다른 데이터 가져오기 -->
<select id="selectReportDetail">
  SELECT
    r.*,
    CASE
      WHEN r.report_type = 'POST' THEN p.title
      WHEN r.report_type = 'COMMENT' THEN CONCAT('Comment on: ', post_for_comment.title)
      WHEN r.report_type = 'USER' THEN reported_user.name
    END AS entityTitle
  FROM reports r
  LEFT JOIN posts p ON r.report_type = 'POST' AND r.entity_id = p.id
  LEFT JOIN comments c ON r.report_type = 'COMMENT' AND r.entity_id = c.id
  LEFT JOIN users reported_user ON r.report_type = 'USER' AND r.entity_id = reported_user.id
  WHERE r.id = #{reportId}
</select>
```

### 2.3 세션 기반 인증 (Session-based Authentication)

**질문**: 관리자만 신고를 관리할 수 있게 하려면 어떻게 할까요?

**답**: 로그인 시 사용자 정보를 **세션**에 저장하고, 요청마다 세션을 확인합니다.

```java
// 로그인 시 (UserController.java)
HttpSession session = request.getSession();
session.setAttribute("user", user);  // 세션에 유저 정보 저장

// 관리자 엔드포인트에서 (AdminController.java)
private boolean isAdmin(HttpSession session) {
    User currentUser = (User) session.getAttribute("user");
    return "ADMIN".equals(currentUser.getRole());
}
```

**프론트엔드에서 세션 쿠키 전송**:

```javascript
axios.get('/api/admin/reports', {
    withCredentials: true  // ← 세션 쿠키를 요청에 포함
});
```

---

## 3. 데이터베이스 구조

### 3.1 reports 테이블

신고 정보를 저장하는 핵심 테이블입니다.

```sql
CREATE TABLE reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_type VARCHAR(20) NOT NULL,          -- POST, COMMENT, USER
    entity_id BIGINT NOT NULL,                 -- 신고 대상의 ID
    reporter_id BIGINT NOT NULL,               -- 신고자 ID
    reason VARCHAR(50) NOT NULL,               -- SPAM, HARASSMENT, etc.
    description TEXT,                          -- 상세 설명
    status VARCHAR(20) DEFAULT 'PENDING',      -- PENDING, REVIEWING, RESOLVED, DISMISSED
    admin_note TEXT,                           -- 관리자 메모
    resolved_by BIGINT,                        -- 처리한 관리자 ID
    resolved_at DATETIME,                      -- 처리 일시
    resolution_action VARCHAR(50),             -- BLIND_POST, DELETE_POST, etc.
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 중복 신고 방지
    UNIQUE KEY unique_report (reporter_id, report_type, entity_id),

    -- 외래키
    FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**중요 포인트**:

1. **UNIQUE KEY**: 같은 사람이 같은 대상을 여러 번 신고할 수 없음
2. **status 필드**: 신고의 처리 상태를 추적
   - `PENDING`: 아직 처리 안 됨
   - `REVIEWING`: 관리자가 검토 중
   - `RESOLVED`: 조치 완료
   - `DISMISSED`: 신고 기각
3. **resolution_action**: 어떤 조치를 취했는지 기록

### 3.2 users 테이블 수정

유저 정지를 위해 3개의 컬럼을 추가했습니다.

```sql
ALTER TABLE users
ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE',           -- ACTIVE, SUSPENDED, BANNED
ADD COLUMN suspended_until DATETIME,                      -- 정지 만료일 (NULL = 영구)
ADD COLUMN suspension_reason TEXT;                        -- 정지 사유
```

**상태별 의미**:

| status | 의미 | suspended_until |
|--------|------|-----------------|
| ACTIVE | 정상 활동 가능 | NULL |
| SUSPENDED | 일시 정지 | 2026-02-09 (7일 정지 예시) |
| BANNED | 영구 정지 | NULL |

### 3.3 테이블 관계도

```
┌─────────┐          ┌─────────┐          ┌───────┐
│  users  │◄────────┤ reports │─────────►│ posts │
└─────────┘          └─────────┘          └───────┘
     ▲                    │
     │                    │
     │                    ▼
     │               ┌──────────┐
     └───────────────┤ comments │
                     └──────────┘
```

**설명**:
- `reports.reporter_id` → `users.id` (신고자)
- `reports.entity_id` → `posts.id` OR `comments.id` OR `users.id` (신고 대상)

---

## 4. 백엔드 아키텍처

### 4.1 레이어 구조

Spring 애플리케이션은 보통 **3계층 아키텍처**를 따릅니다:

```
┌──────────────────┐
│   Controller     │  ← HTTP 요청 받음, 응답 반환
│  (API 엔드포인트) │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│    Service       │  ← 비즈니스 로직 (유효성 검증, 트랜잭션)
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Repository      │  ← 데이터베이스 접근 (CRUD)
│  / Mapper        │
└──────────────────┘
```

### 4.2 파일 구조 설명

```
backend/src/main/java/com/ej2/
├── controller/
│   ├── ReportController.java          # 일반 유저 신고 API
│   └── AdminController.java           # 관리자 신고 관리 API (확장)
├── service/
│   ├── ReportService.java             # 신고 비즈니스 로직
│   └── AdminService.java              # 관리자 기능 (확장)
├── repository/
│   └── ReportRepository.java          # JPA 레포지토리 (간단한 CRUD)
├── mapper/
│   └── ReportMapper.java              # MyBatis 매퍼 인터페이스
├── model/
│   ├── Report.java                    # 신고 엔티티
│   └── User.java                      # 유저 엔티티 (수정)
└── dto/
    ├── ReportDTO.java                 # 신고 목록용
    ├── ReportDetailDTO.java           # 신고 상세용
    ├── ReportStatsDTO.java            # 통계용
    ├── SuspendUserRequest.java        # 정지 요청용
    └── ReportSearchCriteria.java      # 검색 조건용

backend/src/main/resources/
└── mappers/
    └── ReportMapper.xml               # MyBatis SQL 쿼리
```

### 4.3 주요 클래스 설명

#### ReportService.java (핵심 비즈니스 로직)

```java
@Service
@Transactional  // ← 모든 메서드가 트랜잭션 안에서 실행됨
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;  // JPA

    @Autowired
    private ReportMapper reportMapper;          // MyBatis

    @Autowired
    private PostRepository postRepository;

    // 신고 제출
    public Report submitReport(Long reporterId, String reportType,
                               Long entityId, String reason, String description) {

        // 1. 유효성 검증
        validateReportSubmission(reporterId, reportType, entityId);

        // 2. 중복 체크
        if (reportRepository.existsByReporterIdAndReportTypeAndEntityId(
                reporterId, reportType, entityId)) {
            throw new RuntimeException("이미 신고했습니다");
        }

        // 3. 신고 생성
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setReportType(reportType);
        report.setEntityId(entityId);
        report.setReason(reason);
        report.setDescription(description);

        // 4. 저장
        Report saved = reportRepository.save(report);

        // 5. 신고 횟수 증가
        if ("POST".equals(reportType)) {
            reportMapper.incrementPostReportedCount(entityId);
        }

        return saved;
    }

    // 신고 검색 (MyBatis 사용)
    public List<ReportDTO> searchReports(ReportSearchCriteria criteria,
                                         int page, int size) {
        int offset = page * size;
        return reportMapper.selectReports(criteria, offset, size);
    }

    // 모더레이션 액션 실행
    public void takeModerationAction(Long reportId, String action,
                                     String adminNote, Long adminId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("신고를 찾을 수 없습니다"));

        // 액션 실행
        switch (action) {
            case "BLIND_POST":
                reportMapper.blindPost(report.getEntityId(), adminNote);
                break;
            case "DELETE_POST":
                postRepository.deleteById(report.getEntityId());
                break;
            // ... 기타 액션
        }

        // 신고 상태 업데이트
        report.setStatus("RESOLVED");
        report.setResolutionAction(action);
        report.setResolvedBy(adminId);
        report.setResolvedAt(LocalDateTime.now());
        reportRepository.save(report);

        // 관련 신고 자동 해결
        reportMapper.resolveReportsForEntity(
            report.getReportType(), report.getEntityId(), "RESOLVED"
        );
    }
}
```

**트랜잭션 (@Transactional)**:
- 메서드 안의 모든 DB 작업이 **하나의 단위**로 실행됨
- 중간에 에러가 나면 모든 변경사항이 **자동 롤백**됨

**예시**:
```java
takeModerationAction() {
    1. 게시글 삭제
    2. 신고 상태 업데이트  ← 여기서 에러 발생
    3. 관련 신고 해결
}
// ↓ @Transactional 덕분에
// 1번 작업도 자동으로 취소됨!
```

---

## 5. MyBatis 동적 쿼리 이해하기

### 5.1 왜 동적 쿼리가 필요한가?

**문제**: 사용자가 필터를 선택적으로 적용할 수 있습니다.

예를 들어:
- 상태만 필터링: `SELECT * FROM reports WHERE status = 'PENDING'`
- 상태 + 유형 필터링: `SELECT * FROM reports WHERE status = 'PENDING' AND report_type = 'POST'`
- 필터 없음: `SELECT * FROM reports`

**일반 SQL로는 어려움**:

```java
// ❌ 나쁜 방법: 조건마다 쿼리를 따로 만들어야 함
if (status != null && reportType != null) {
    sql = "SELECT * FROM reports WHERE status = ? AND report_type = ?";
} else if (status != null) {
    sql = "SELECT * FROM reports WHERE status = ?";
} else if (reportType != null) {
    sql = "SELECT * FROM reports WHERE report_type = ?";
} else {
    sql = "SELECT * FROM reports";
}
```

**MyBatis 동적 쿼리로 해결**:

```xml
<!-- ✅ 좋은 방법: 하나의 쿼리로 모든 경우 처리 -->
<select id="selectReports">
  SELECT * FROM reports
  <where>
    <if test="criteria.status != null">
      AND status = #{criteria.status}
    </if>
    <if test="criteria.reportType != null">
      AND report_type = #{criteria.reportType}
    </if>
  </where>
</select>
```

### 5.2 MyBatis 태그 설명

#### `<where>` 태그

자동으로 WHERE 절을 관리합니다.

```xml
<where>
  <if test="status != null">AND status = #{status}</if>
  <if test="type != null">AND report_type = #{type}</if>
</where>
```

**결과**:
- 둘 다 있으면: `WHERE status = 'PENDING' AND report_type = 'POST'`
- status만: `WHERE status = 'PENDING'`
- type만: `WHERE report_type = 'POST'`
- 둘 다 없으면: (WHERE 절 자체가 사라짐)

**주의**: `<where>` 태그가 자동으로 맨 앞의 `AND`나 `OR`를 제거합니다!

#### `<if>` 태그

조건부로 SQL 조각을 포함합니다.

```xml
<if test="age != null and age > 18">
  AND age > #{age}
</if>
```

**test 속성**:
- `null` 체크: `test="name != null"`
- 비교: `test="age > 18"`
- 문자열 비교: `test="status == 'ACTIVE'"`
- AND/OR: `test="age != null and age > 18"`

#### `<choose>`, `<when>`, `<otherwise>` 태그

여러 조건 중 하나만 선택 (switch-case와 비슷)

```xml
<choose>
  <when test="sortBy == 'date'">
    ORDER BY created_at DESC
  </when>
  <when test="sortBy == 'status'">
    ORDER BY status
  </when>
  <otherwise>
    ORDER BY id DESC
  </otherwise>
</choose>
```

### 5.3 실전 예제: 복잡한 신고 검색 쿼리

```xml
<select id="selectReports" resultType="com.ej2.dto.ReportDTO">
  SELECT
    r.id,
    r.report_type AS reportType,
    r.entity_id AS entityId,
    r.reporter_id AS reporterId,
    reporter.name AS reporterName,
    r.reason,
    r.status,
    r.created_at AS createdAt
  FROM reports r
  INNER JOIN users reporter ON r.reporter_id = reporter.id

  <!-- 동적 WHERE 절 -->
  <where>
    <!-- 상태 필터 -->
    <if test="criteria.status != null and criteria.status != ''">
      AND r.status = #{criteria.status}
    </if>

    <!-- 유형 필터 -->
    <if test="criteria.reportType != null and criteria.reportType != ''">
      AND r.report_type = #{criteria.reportType}
    </if>

    <!-- 날짜 범위 필터 -->
    <if test="criteria.startDate != null">
      AND r.created_at >= #{criteria.startDate}
    </if>
    <if test="criteria.endDate != null">
      AND r.created_at &lt;= #{criteria.endDate}
    </if>
  </where>

  <!-- 동적 정렬 -->
  ORDER BY
  <choose>
    <when test="criteria.sortBy == 'status'">
      r.status, r.created_at DESC
    </when>
    <when test="criteria.sortBy == 'type'">
      r.report_type, r.created_at DESC
    </when>
    <otherwise>
      r.created_at DESC
    </otherwise>
  </choose>

  <!-- 페이지네이션 -->
  LIMIT #{limit} OFFSET #{offset}
</select>
```

**파라미터 바인딩**:
- `#{}`: PreparedStatement의 `?` 자리 표시자로 변환 (SQL 인젝션 방지)
- `${}`: 문자열 직접 치환 (위험! 사용 자제)

```xml
<!-- ✅ 안전 -->
<if test="status != null">
  AND status = #{status}
</if>
<!-- SQL: AND status = ? -->
<!-- 파라미터: "PENDING" -->

<!-- ❌ 위험 -->
<if test="tableName != null">
  FROM ${tableName}
</if>
<!-- SQL: FROM reports -->
<!-- 만약 tableName = "reports; DROP TABLE users"라면? -->
```

### 5.4 CASE 문으로 폴리모픽 데이터 가져오기

```xml
<select id="selectReportDetail" resultType="com.ej2.dto.ReportDetailDTO">
  SELECT
    r.*,
    reporter.name AS reporterName,
    reporter.email AS reporterEmail,

    <!-- 타입별로 다른 데이터 가져오기 -->
    CASE
      WHEN r.report_type = 'POST' THEN p.title
      WHEN r.report_type = 'COMMENT' THEN CONCAT('Comment on: ', post_for_comment.title)
      WHEN r.report_type = 'USER' THEN reported_user.name
    END AS entityTitle,

    CASE
      WHEN r.report_type = 'POST' THEN p.content
      WHEN r.report_type = 'COMMENT' THEN c.content
      WHEN r.report_type = 'USER' THEN reported_user.email
    END AS entityContent,

    CASE
      WHEN r.report_type = 'POST' THEN post_author.name
      WHEN r.report_type = 'COMMENT' THEN comment_author.name
      WHEN r.report_type = 'USER' THEN reported_user.name
    END AS entityAuthorName

  FROM reports r
  INNER JOIN users reporter ON r.reporter_id = reporter.id

  <!-- 타입별 테이블 조인 -->
  LEFT JOIN posts p ON r.report_type = 'POST' AND r.entity_id = p.id
  LEFT JOIN users post_author ON p.user_id = post_author.id

  LEFT JOIN comments c ON r.report_type = 'COMMENT' AND r.entity_id = c.id
  LEFT JOIN posts post_for_comment ON c.post_id = post_for_comment.id
  LEFT JOIN users comment_author ON c.user_id = comment_author.id

  LEFT JOIN users reported_user ON r.report_type = 'USER' AND r.entity_id = reported_user.id

  WHERE r.id = #{reportId}
</select>
```

**결과 예시**:

| report_type | entityTitle | entityContent | entityAuthorName |
|-------------|-------------|---------------|------------------|
| POST | "안녕하세요" | "게시글 내용..." | "홍길동" |
| COMMENT | "Comment on: 안녕하세요" | "댓글 내용..." | "김철수" |
| USER | "이영희" | "young@example.com" | "이영희" |

---

## 6. 프론트엔드 구조

### 6.1 React 컴포넌트 구조

```
frontend/src/
├── App.js                              # 라우팅 설정
├── pages/
│   ├── Admin/
│   │   ├── AdminPage.js                # 관리자 대시보드 (수정)
│   │   ├── AdminUsersPage.js           # 유저 관리 (수정)
│   │   ├── AdminReportsPage.js         # 신고 관리 (신규)
│   │   └── AdminPages.css              # 스타일
│   └── Board/
│       └── PostDetailPage.js           # 게시글 상세 (수정)
```

### 6.2 AdminReportsPage 컴포넌트 분석

```javascript
import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';

function AdminReportsPage() {
  // ===== 상태 관리 =====
  const [reports, setReports] = useState([]);           // 신고 목록
  const [stats, setStats] = useState(null);             // 통계
  const [selectedReport, setSelectedReport] = useState(null);  // 선택된 신고
  const [showDetailModal, setShowDetailModal] = useState(false); // 모달 표시 여부

  // 필터 상태
  const [filters, setFilters] = useState({
    status: '',
    reportType: '',
    sortBy: 'date'
  });

  // ===== 데이터 가져오기 =====
  const fetchReports = useCallback(async () => {
    try {
      const response = await axios.get(
        'http://localhost:8080/ej2/api/admin/reports',
        {
          params: { ...filters, page: 0, size: 20 },
          withCredentials: true  // 세션 쿠키 포함
        }
      );
      setReports(response.data.reports);
    } catch (err) {
      console.error('Failed to fetch reports:', err);
    }
  }, [filters]);  // filters가 바뀔 때마다 재실행

  // 컴포넌트 마운트 시 실행
  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  // ===== 이벤트 핸들러 =====
  const handleModerationAction = async (reportId, action) => {
    if (!window.confirm(`정말로 ${action}을 실행하시겠습니까?`)) {
      return;
    }

    try {
      await axios.post(
        `http://localhost:8080/ej2/api/admin/reports/${reportId}/actions`,
        { action, adminNote: '...' },
        { withCredentials: true }
      );
      alert('액션이 실행되었습니다');
      fetchReports();  // 목록 새로고침
    } catch (err) {
      alert('액션 실행 실패');
    }
  };

  // ===== 렌더링 =====
  return (
    <div className="admin-container">
      <h1>신고 관리</h1>

      {/* 필터 */}
      <div className="filter-section">
        <select
          value={filters.status}
          onChange={(e) => setFilters({...filters, status: e.target.value})}
        >
          <option value="">모든 상태</option>
          <option value="PENDING">대기 중</option>
          <option value="RESOLVED">해결됨</option>
        </select>
      </div>

      {/* 테이블 */}
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>유형</th>
            <th>신고자</th>
            <th>상태</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          {reports.map(report => (
            <tr key={report.id}>
              <td>{report.id}</td>
              <td>{report.reportType}</td>
              <td>{report.reporterName}</td>
              <td>{report.status}</td>
              <td>
                <button onClick={() => fetchReportDetail(report.id)}>
                  상세보기
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* 상세 모달 */}
      {showDetailModal && selectedReport && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>신고 상세</h2>
            <p>내용: {selectedReport.entityContent}</p>

            <button onClick={() => handleModerationAction(selectedReport.id, 'BLIND_POST')}>
              블라인드 처리
            </button>
            <button onClick={() => handleModerationAction(selectedReport.id, 'DELETE_POST')}>
              삭제
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
```

### 6.3 주요 React 개념

#### useState (상태 관리)

```javascript
const [count, setCount] = useState(0);  // 초기값: 0

// count 읽기
console.log(count);  // 0

// count 변경
setCount(5);  // count가 5로 바뀌고 컴포넌트가 다시 렌더링됨
```

#### useEffect (부수 효과)

컴포넌트가 **마운트**되거나 **상태가 변경**될 때 실행됩니다.

```javascript
// 1. 마운트 시 한 번만 실행
useEffect(() => {
  fetchData();
}, []);  // 의존성 배열이 비어있음

// 2. userId가 바뀔 때마다 실행
useEffect(() => {
  fetchUserData(userId);
}, [userId]);  // userId가 의존성

// 3. 매 렌더링마다 실행 (비추천)
useEffect(() => {
  console.log('렌더링됨');
});  // 의존성 배열 없음
```

#### useCallback (함수 메모이제이션)

함수를 캐싱하여 불필요한 재생성을 방지합니다.

```javascript
// ❌ 나쁜 예: 렌더링마다 새 함수 생성
const fetchReports = async () => {
  // ...
};

// ✅ 좋은 예: filters가 바뀔 때만 새 함수 생성
const fetchReports = useCallback(async () => {
  // ...
}, [filters]);
```

---

## 7. 주요 기능 흐름도

### 7.1 신고 제출 흐름

```
[일반 유저]
    │
    ▼
[게시글 상세 페이지]
    │
    ├── 🚨 "신고" 버튼 클릭
    │
    ▼
[신고 모달 열림]
    │
    ├── 신고 사유 선택 (SPAM, HARASSMENT, ...)
    ├── 상세 설명 입력
    └── "신고하기" 클릭
    │
    ▼
[POST /api/reports]
    │
    ▼
[ReportController.submitReport()]
    │
    ├── 세션에서 유저 확인
    │   └─ 로그인 안 했으면 401 에러
    │
    ▼
[ReportService.submitReport()]
    │
    ├── 본인 콘텐츠 신고 체크
    │   └─ 본인이면 에러 throw
    │
    ├── 중복 신고 체크
    │   └─ 이미 신고했으면 에러 throw
    │
    ├── Report 엔티티 생성 및 저장 (JPA)
    │
    └── Post의 reportedCount 증가 (MyBatis)
    │
    ▼
[200 OK: "신고가 접수되었습니다"]
    │
    ▼
[프론트엔드]
    └── alert("신고가 접수되었습니다")
```

**코드 추적**:

1. **프론트엔드** (`PostDetailPage.js`):
```javascript
const handleSubmitReport = async () => {
  await axios.post('http://localhost:8080/ej2/api/reports', {
    reportType: 'POST',
    entityId: parseInt(postId),
    reason: reportReason,
    description: reportDescription
  }, { withCredentials: true });
  alert('신고가 접수되었습니다');
};
```

2. **컨트롤러** (`ReportController.java`):
```java
@PostMapping
public ResponseEntity<?> submitReport(@RequestBody Map<String, Object> request,
                                      HttpSession session) {
    User currentUser = (User) session.getAttribute("user");
    if (currentUser == null) {
        return ResponseEntity.status(401).body("로그인이 필요합니다");
    }

    Report report = reportService.submitReport(
        currentUser.getId(),
        (String) request.get("reportType"),
        Long.valueOf(request.get("entityId").toString()),
        (String) request.get("reason"),
        (String) request.get("description")
    );

    return ResponseEntity.ok("신고가 접수되었습니다");
}
```

3. **서비스** (`ReportService.java`):
```java
@Transactional
public Report submitReport(Long reporterId, String reportType, ...) {
    // 중복 체크
    if (reportRepository.existsByReporterIdAndReportTypeAndEntityId(...)) {
        throw new RuntimeException("이미 신고했습니다");
    }

    // 저장
    Report report = new Report();
    report.setReporterId(reporterId);
    // ... 필드 설정

    Report saved = reportRepository.save(report);

    // 신고 횟수 증가
    reportMapper.incrementPostReportedCount(entityId);

    return saved;
}
```

### 7.2 관리자 신고 처리 흐름

```
[관리자]
    │
    ▼
[AdminReportsPage]
    │
    ├── 필터 선택 (상태: PENDING, 유형: POST)
    │
    ▼
[GET /api/admin/reports?status=PENDING&reportType=POST]
    │
    ▼
[AdminController.searchReports()]
    │
    ├── 관리자 권한 체크
    │   └─ 일반 유저면 403 에러
    │
    ▼
[AdminService.searchReports()]
    │
    └── MyBatis 동적 쿼리로 필터링된 신고 조회
    │
    ▼
[200 OK: { reports: [...], totalCount: 15 }]
    │
    ▼
[AdminReportsPage]
    │
    └── 테이블에 신고 목록 표시
    │
    ├── 신고 행 클릭
    │
    ▼
[GET /api/admin/reports/{id}]
    │
    ▼
[상세 모달 열림]
    │
    ├── 원본 콘텐츠 확인
    ├── "블라인드 처리" 버튼 클릭
    │
    ▼
[POST /api/admin/reports/{id}/actions]
    │
    ▼
[ReportService.takeModerationAction()]
    │
    ├── 게시글 블라인드 처리 (MyBatis UPDATE)
    ├── 신고 상태를 RESOLVED로 변경 (JPA)
    └── 같은 게시글의 다른 신고도 자동 해결 (MyBatis)
    │
    ▼
[200 OK: "액션이 실행되었습니다"]
```

### 7.3 유저 정지 흐름

```
[관리자]
    │
    ▼
[AdminUsersPage]
    │
    ├── 유저 목록 확인
    ├── 문제 유저의 "정지" 버튼 클릭
    │
    ▼
[정지 모달 열림]
    │
    ├── 정지 기간 선택 (7일)
    ├── 정지 사유 입력 ("스팸 반복")
    └── "정지하기" 클릭
    │
    ▼
[POST /api/admin/users/{userId}/suspend]
    │
    ▼
[AdminController.suspendUser()]
    │
    └── 사유가 비어있으면 400 에러
    │
    ▼
[AdminService.suspendUser()]
    │
    ├── User 조회 (JPA)
    ├── status = "SUSPENDED"
    ├── suspendedUntil = 현재 + 7일
    ├── suspensionReason = "스팸 반복"
    └── 저장 (JPA)
    │
    ▼
[200 OK: "유저가 정지되었습니다"]
    │
    ▼
[정지된 유저가 로그인 시도]
    │
    ▼
[로그인은 성공하지만...]
    │
    ▼
[게시글 작성 시도]
    │
    ▼
[백엔드에서 상태 체크]
    │
    └── AdminService.isUserSuspended() 호출
    │   ├── status == "SUSPENDED"?
    │   ├── suspendedUntil > 현재 시각?
    │   │   └─ Yes → 403 에러
    │   └── 만료됨 → 자동으로 ACTIVE로 변경
```

---

## 8. 트러블슈팅 가이드

### 8.1 백엔드 에러

#### 에러: "Table 'appdb.reports' doesn't exist"

**원인**: Hibernate가 테이블을 자동 생성하지 못했습니다.

**해결**:

1. `application.properties` 확인:
```properties
spring.jpa.hibernate.ddl-auto=update
```

2. 수동으로 테이블 생성:
```sql
CREATE TABLE reports (
    -- 위의 SQL 스키마 참고
);
```

#### 에러: "Cannot invoke 'com.ej2.mapper.ReportMapper.selectReports'"

**원인**: MyBatis 매퍼가 스캔되지 않았습니다.

**해결**:

`RootConfig.java` 확인:
```java
@Configuration
@MapperScan("com.ej2.mapper")  // ← 이 어노테이션이 있는지 확인
public class RootConfig {
    // ...
}
```

#### 에러: "Forbidden: 관리자 권한이 필요합니다"

**원인**: 세션에 유저 정보가 없거나, ADMIN 권한이 아닙니다.

**해결**:

1. 로그인 확인:
```sql
SELECT * FROM users WHERE username = 'admin';
-- role이 'ADMIN'인지 확인
```

2. 프론트엔드에서 `withCredentials: true` 확인:
```javascript
axios.get('/api/admin/reports', {
    withCredentials: true  // ← 이게 있어야 세션 쿠키가 전송됨
});
```

### 8.2 프론트엔드 에러

#### 에러: "CORS policy: No 'Access-Control-Allow-Origin' header"

**원인**: CORS 설정이 잘못되었습니다.

**해결**:

`WebConfig.java` 또는 컨트롤러에 CORS 설정:

```java
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    // ...
}
```

#### 에러: "Cannot read properties of undefined (reading 'reports')"

**원인**: API 응답 구조가 예상과 다릅니다.

**해결**:

1. 브라우저 개발자 도구 > Network 탭에서 실제 응답 확인
2. 코드 수정:
```javascript
// ❌ 잘못된 코드
setReports(response.data.reports);

// ✅ 실제 응답 구조에 맞게 수정
console.log(response.data);  // 먼저 구조 확인
setReports(response.data);    // 또는 response.data.content 등
```

### 8.3 MyBatis 쿼리 디버깅

#### MyBatis 쿼리 로그 활성화

`application.properties` (또는 logback.xml):

```properties
logging.level.com.ej2.mapper=DEBUG
```

이제 콘솔에 실행된 SQL이 출력됩니다:

```
DEBUG com.ej2.mapper.ReportMapper.selectReports - ==>  Preparing:
  SELECT * FROM reports WHERE status = ? AND report_type = ? ORDER BY created_at DESC LIMIT ? OFFSET ?
DEBUG com.ej2.mapper.ReportMapper.selectReports - ==> Parameters: PENDING(String), POST(String), 20(Integer), 0(Integer)
DEBUG com.ej2.mapper.ReportMapper.selectReports - <==      Total: 5
```

#### 동적 쿼리가 예상대로 생성되지 않을 때

```xml
<!-- 디버그용 출력 추가 -->
<select id="selectReports">
  <!-- 여기에 주석으로 어떤 조건이 들어올지 메모 -->
  SELECT * FROM reports
  <where>
    <if test="criteria.status != null">
      AND status = #{criteria.status}
      <!-- 디버그: status 조건 추가됨 -->
    </if>
  </where>
</select>
```

그리고 Java 코드에서 로그 출력:

```java
public List<ReportDTO> searchReports(ReportSearchCriteria criteria, ...) {
    System.out.println("Criteria: " + criteria);  // 파라미터 확인
    return reportMapper.selectReports(criteria, offset, limit);
}
```

---

## 9. 실습 예제

### 9.1 새로운 신고 사유 추가하기

**과제**: "COPYRIGHT" (저작권 침해) 사유를 추가하세요.

#### 1단계: 백엔드에서 사유 추가

`Report.java`:
```java
// 이미 "SPAM", "HARASSMENT" 등이 있음
// 특별히 enum을 만들지 않았으므로 그냥 문자열로 사용
```

#### 2단계: 프론트엔드 모달에 옵션 추가

`PostDetailPage.js`:
```javascript
<label className="radio-label">
  <input
    type="radio"
    value="COPYRIGHT"
    checked={reportReason === 'COPYRIGHT'}
    onChange={(e) => setReportReason(e.target.value)}
  />
  <span>저작권 침해</span>
</label>
```

#### 3단계: 관리자 페이지에서 표시

`AdminReportsPage.js`:
```javascript
const getReasonLabel = (reason) => {
  const reasonMap = {
    SPAM: 'スパム/広告',
    HARASSMENT: '嫌がらせ',
    INAPPROPRIATE: '不適切なコンテンツ',
    HATE_SPEECH: 'ヘイトスピーチ',
    COPYRIGHT: '著作権侵害',  // ← 추가
    OTHER: 'その他'
  };
  return reasonMap[reason] || reason;
};
```

**테스트**:
1. 게시글에서 신고 → COPYRIGHT 선택 → 제출
2. 관리자 페이지에서 "著作権侵害"로 표시되는지 확인

### 9.2 신고 통계에 새로운 항목 추가하기

**과제**: "오늘 처리된 신고 수" 통계를 추가하세요.

#### 1단계: DTO 수정

`ReportStatsDTO.java`:
```java
public class ReportStatsDTO {
    private Long totalReports;
    private Long pendingReports;
    private Long reviewingReports;
    private Long resolvedToday;
    private Long dismissedToday;
    private Long totalResolvedToday;  // ← 추가

    // getter/setter 추가
}
```

#### 2단계: MyBatis 쿼리 수정

`ReportMapper.xml`:
```xml
<select id="selectReportStats" resultType="com.ej2.dto.ReportStatsDTO">
  SELECT
    COUNT(*) AS totalReports,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pendingReports,
    SUM(CASE WHEN status = 'REVIEWING' THEN 1 ELSE 0 END) AS reviewingReports,
    SUM(CASE WHEN status = 'RESOLVED' AND DATE(resolved_at) = CURDATE() THEN 1 ELSE 0 END) AS resolvedToday,
    SUM(CASE WHEN status = 'DISMISSED' AND DATE(resolved_at) = CURDATE() THEN 1 ELSE 0 END) AS dismissedToday,

    <!-- 새로 추가 -->
    SUM(CASE WHEN (status = 'RESOLVED' OR status = 'DISMISSED')
                   AND DATE(resolved_at) = CURDATE() THEN 1 ELSE 0 END) AS totalResolvedToday
  FROM reports
</select>
```

#### 3단계: 프론트엔드에 표시

`AdminReportsPage.js`:
```javascript
<StatCard
  icon={FiCheckCircle}
  title="오늘 처리된 신고"
  value={stats.totalResolvedToday}
  color="linear-gradient(135deg, #10b981 0%, #059669 100%)"
/>
```

**테스트**:
1. 신고 몇 개 처리 (RESOLVED 또는 DISMISSED)
2. 대시보드에서 "오늘 처리된 신고" 숫자 확인

### 9.3 댓글 신고 기능 추가하기

**과제**: 현재는 게시글만 신고할 수 있습니다. 댓글도 신고할 수 있게 만드세요.

#### 1단계: CommentSection 컴포넌트에 신고 버튼 추가

`CommentSection.js` (가정):
```javascript
// 각 댓글 옆에 신고 버튼 추가
<div className="comment-actions">
  <button onClick={() => openReportModal(comment.id)}>
    🚨 신고
  </button>
</div>

// 신고 모달 추가 (PostDetailPage와 유사)
const handleSubmitCommentReport = async (commentId) => {
  await axios.post('http://localhost:8080/ej2/api/reports', {
    reportType: 'COMMENT',  // ← POST 대신 COMMENT
    entityId: commentId,
    reason: reportReason,
    description: reportDescription
  }, { withCredentials: true });
  alert('댓글이 신고되었습니다');
};
```

#### 2단계: 백엔드에서 댓글 신고 검증 추가

`ReportService.java`:
```java
private void validateReportSubmission(Long reporterId, String reportType, Long entityId) {
    if ("COMMENT".equals(reportType)) {
        // 댓글 존재 확인
        Comment comment = commentRepository.findById(entityId)
            .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다"));

        // 본인 댓글 신고 방지
        if (comment.getUserId().equals(reporterId)) {
            throw new RuntimeException("자신의 댓글은 신고할 수 없습니다");
        }
    }
}
```

#### 3단계: MyBatis에서 댓글 신고 횟수 증가

`ReportMapper.xml`:
```xml
<update id="incrementCommentReportedCount">
  UPDATE comments
  SET reported_count = reported_count + 1
  WHERE id = #{commentId}
</update>
```

`ReportService.java`:
```java
public Report submitReport(...) {
    // ... 기존 코드

    // 신고 횟수 증가
    if ("POST".equals(reportType)) {
        reportMapper.incrementPostReportedCount(entityId);
    } else if ("COMMENT".equals(reportType)) {
        reportMapper.incrementCommentReportedCount(entityId);  // ← 추가
    }

    return saved;
}
```

**테스트**:
1. 댓글에서 신고 버튼 클릭
2. 신고 제출
3. 관리자 페이지에서 COMMENT 타입 신고 확인
4. 댓글 삭제 액션 실행

---

## 부록

### A. 주요 SQL 쿼리 모음

#### 신고 통계 조회
```sql
SELECT
  COUNT(*) AS total_reports,
  SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending,
  SUM(CASE WHEN status = 'REVIEWING' THEN 1 ELSE 0 END) AS reviewing,
  SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved,
  SUM(CASE WHEN status = 'DISMISSED' THEN 1 ELSE 0 END) AS dismissed
FROM reports;
```

#### 신고가 가장 많은 게시글 찾기
```sql
SELECT
  p.id,
  p.title,
  p.reported_count,
  COUNT(r.id) AS actual_report_count
FROM posts p
LEFT JOIN reports r ON r.report_type = 'POST' AND r.entity_id = p.id
GROUP BY p.id, p.title, p.reported_count
HAVING actual_report_count > 0
ORDER BY actual_report_count DESC
LIMIT 10;
```

#### 정지된 유저 목록
```sql
SELECT
  id,
  username,
  name,
  status,
  suspended_until,
  suspension_reason,
  CASE
    WHEN status = 'BANNED' THEN '영구 정지'
    WHEN status = 'SUSPENDED' AND suspended_until > NOW() THEN CONCAT('정지 중 (~', DATE_FORMAT(suspended_until, '%Y-%m-%d'), ')')
    WHEN status = 'SUSPENDED' AND suspended_until <= NOW() THEN '만료 (자동 해제 대기)'
    ELSE '정상'
  END AS status_description
FROM users
WHERE status IN ('SUSPENDED', 'BANNED')
ORDER BY suspended_until ASC;
```

### B. 유용한 Git 명령어

```bash
# 변경사항 확인
git status

# 새 파일 추가
git add backend/src/main/java/com/ej2/model/Report.java

# 모든 변경사항 스테이징
git add .

# 커밋 (의미있는 메시지 작성)
git commit -m "feat: add report system with MyBatis dynamic queries"

# 푸시
git push origin main

# 변경사항 되돌리기 (아직 커밋 전)
git checkout -- filename.java

# 최근 커밋 확인
git log --oneline -10
```

### C. 추가 학습 자료

- **MyBatis 공식 문서**: https://mybatis.org/mybatis-3/
- **Spring Framework 가이드**: https://spring.io/guides
- **React 공식 튜토리얼**: https://react.dev/learn
- **JPA/Hibernate 튜토리얼**: https://www.baeldung.com/learn-jpa-hibernate

---

## 마치며

이 가이드를 통해 신고 시스템의 전체 구조를 이해하셨기를 바랍니다.

**핵심 개념 요약**:
1. ✅ **하이브리드 아키텍처**: JPA (간단한 CRUD) + MyBatis (복잡한 쿼리)
2. ✅ **폴리모픽 참조**: 하나의 테이블로 여러 타입 처리
3. ✅ **동적 쿼리**: MyBatis의 `<if>`, `<where>`, `<choose>` 태그
4. ✅ **세션 기반 인증**: HttpSession으로 관리자 권한 확인
5. ✅ **React 컴포넌트**: useState, useEffect, useCallback

**다음 단계**:
- 실습 예제를 직접 구현해보세요
- 에러가 나면 트러블슈팅 가이드를 참고하세요
- 궁금한 점이 있으면 공식 문서를 읽어보세요

Good luck! 🚀
