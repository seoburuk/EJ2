# MyBatis를 이용한 인기글 랭킹 알고리즘 구현

> 작성일: 2026-01-30
> 주제: Spring Framework + JPA 환경에 MyBatis 추가하여 복잡한 랭킹 쿼리 구현

---

## 1. 개요

### 1.1 배경
기존 프로젝트는 **Spring Data JPA + Hibernate**를 사용 중이었습니다. 하지만 인기글 랭킹처럼 복잡한 계산식과 동적 쿼리가 필요한 경우, **MyBatis**가 더 적합합니다.

### 1.2 왜 MyBatis를 추가했는가?

| 상황 | JPA 적합성 | MyBatis 적합성 |
|------|-----------|---------------|
| 단순 CRUD | ⭐⭐⭐ | ⭐⭐ |
| 복잡한 JOIN | ⭐⭐ | ⭐⭐⭐ |
| 동적 쿼리 (조건별 분기) | ⭐ | ⭐⭐⭐ |
| 수학 계산식 포함 쿼리 | ⭐ | ⭐⭐⭐ |
| 통계/집계 쿼리 | ⭐ | ⭐⭐⭐ |

**결론**: JPA는 기존 CRUD에 유지하고, 복잡한 랭킹 쿼리만 MyBatis로 처리 (공존 전략)

---

## 2. 인기도 점수 알고리즘

### 2.1 공식 (Hacker News + Reddit 하이브리드)

```
                (like × 3.0) + (comment × 2.0) + (scrap × 2.5) + (view × 0.1) - (dislike × 1.0)
popularity = ─────────────────────────────────────────────────────────────────────────────────────
                                        (hours_since_creation + 2) ^ gravity
```

### 2.2 가중치 설명

| 요소 | 가중치 | 이유 |
|------|--------|------|
| `like_count` | **3.0** | 좋아요는 적극적인 참여 지표 |
| `comment_count` | **2.0** | 댓글은 토론 활성화 의미 |
| `scrap_count` | **2.5** | 스크랩은 "나중에 다시 보고 싶다" = 고품질 |
| `view_count` | **0.1** | 조회는 수동적 참여 (낮은 가중치) |
| `dislike_count` | **-1.0** | 부정적 피드백은 감점 |

### 2.3 시간 감쇠 (Time Decay)

**Gravity 값에 따른 차이**:

| Gravity | 감쇠 강도 | 특징 |
|---------|----------|------|
| 1.0 (선택) | 약함 | 오래된 인기글도 상위 유지 가능 |
| 1.5 | 중간 | Hacker News 스타일 |
| 2.0 | 강함 | 최신글 극단적 우대 |

**예시 계산** (Gravity 1.0):
```
게시글 A: 좋아요 45, 댓글 10, 스크랩 5, 조회 1000, 작성 2시간 전
점수 = (45×3 + 10×2 + 5×2.5 + 1000×0.1) / (2+2)^1.0
     = (135 + 20 + 12.5 + 100) / 4
     = 267.5 / 4
     = 66.9점

게시글 B: 동일 반응, 작성 48시간 전
점수 = 267.5 / (48+2)^1.0 = 267.5 / 50 = 5.4점
```

---

## 3. 프로젝트 구조

### 3.1 추가/수정된 파일

```
backend/
├── pom.xml                                    # MyBatis 의존성 추가
├── src/main/java/com/ej2/
│   ├── config/
│   │   └── RootConfig.java                    # MyBatis 설정 추가
│   ├── mapper/                                # [새 디렉토리]
│   │   └── RankingMapper.java                 # Mapper 인터페이스
│   ├── dto/
│   │   └── PopularPostDTO.java                # 인기글 응답 DTO
│   ├── service/
│   │   └── RankingService.java                # 랭킹 비즈니스 로직
│   └── controller/
│       └── RankingController.java             # REST API
└── src/main/resources/
    └── mappers/                               # [새 디렉토리]
        └── RankingMapper.xml                  # SQL 매퍼
```

---

## 4. 구현 상세

### 4.1 Maven 의존성 (pom.xml)

```xml
<!-- MyBatis Core -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.13</version>
</dependency>

<!-- MyBatis-Spring 연동 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.1.1</version>
</dependency>
```

**버전 선택 근거**:
- MyBatis 3.5.x → Java 8 호환
- mybatis-spring 2.1.x → Spring 5.x 호환

---

### 4.2 Spring 설정 (RootConfig.java)

```java
// 기존 import에 추가
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.ej2.repository")  // JPA용
@MapperScan(basePackages = "com.ej2.mapper")                  // MyBatis용 (추가)
@ComponentScan(basePackages = {"com.ej2.service", "com.ej2.repository"})
public class RootConfig {

    // 기존 DataSource, EntityManagerFactory, TransactionManager 유지...

    // ========== MyBatis 설정 추가 ==========

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();

        // 기존 DataSource 공유 (JPA와 동일한 커넥션 풀 사용)
        sessionFactory.setDataSource(dataSource);

        // Mapper XML 위치 설정
        sessionFactory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mappers/**/*.xml")
        );

        // MyBatis 설정
        org.apache.ibatis.session.Configuration configuration =
            new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);  // snake_case → camelCase 자동 변환
        configuration.setCallSettersOnNulls(true);        // null도 setter 호출
        sessionFactory.setConfiguration(configuration);

        // 타입 별칭 (클래스명만으로 참조 가능)
        sessionFactory.setTypeAliasesPackage("com.ej2.model,com.ej2.dto");

        return sessionFactory.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
```

**핵심 포인트**:
1. `@MapperScan`: MyBatis Mapper 인터페이스 자동 스캔
2. 기존 DataSource 재사용 → 커넥션 풀 효율적 활용
3. `mapUnderscoreToCamelCase(true)` → DB의 `user_id`가 Java의 `userId`로 자동 매핑

---

### 4.3 Mapper 인터페이스 (RankingMapper.java)

```java
package com.ej2.mapper;

import com.ej2.dto.PopularPostDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RankingMapper {

    /**
     * 전체 인기글 조회
     */
    List<PopularPostDTO> selectPopularPosts(
        @Param("period") String period,   // daily, weekly, monthly, all
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 게시판별 인기글 조회
     */
    List<PopularPostDTO> selectPopularPostsByBoard(
        @Param("boardId") Long boardId,
        @Param("period") String period,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 인기글 총 개수 (페이징용)
     */
    int countPopularPosts(
        @Param("boardId") Long boardId,
        @Param("period") String period
    );
}
```

**`@Param` 어노테이션**: XML에서 `#{period}`로 참조 가능하게 함

---

### 4.4 SQL 매퍼 (RankingMapper.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.ej2.mapper.RankingMapper">

    <!-- ========== 재사용 가능한 SQL 조각 ========== -->

    <!-- 기간별 WHERE 조건 -->
    <sql id="periodCondition">
        <choose>
            <when test="period == 'daily'">
                AND p.created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
            </when>
            <when test="period == 'weekly'">
                AND p.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            </when>
            <when test="period == 'monthly'">
                AND p.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            </when>
            <!-- 'all'인 경우 조건 없음 -->
        </choose>
    </sql>

    <!-- 인기도 점수 계산식 -->
    <sql id="popularityScore">
        (
            (COALESCE(p.like_count, 0) * 3.0) +
            (COALESCE(p.comment_count, 0) * 2.0) +
            (COALESCE(p.scrap_count, 0) * 2.5) +
            (COALESCE(p.view_count, 0) * 0.1) -
            (COALESCE(p.dislike_count, 0) * 1.0)
        ) / POW(
            GREATEST(TIMESTAMPDIFF(HOUR, p.created_at, NOW()), 0) + 2,
            1.0
        )
    </sql>

    <!-- ========== 실제 쿼리 ========== -->

    <!-- 전체 인기글 조회 -->
    <select id="selectPopularPosts" resultType="com.ej2.dto.PopularPostDTO">
        SELECT
            p.id,
            p.board_id AS boardId,
            p.user_id AS userId,
            p.title,
            p.content,
            p.anonymous_id AS anonymousId,
            p.view_count AS viewCount,
            p.like_count AS likeCount,
            p.dislike_count AS dislikeCount,
            p.comment_count AS commentCount,
            p.scrap_count AS scrapCount,
            p.created_at AS createdAt,
            p.updated_at AS updatedAt,
            b.name AS boardName,
            u.name AS authorNickname,
            <include refid="popularityScore"/> AS popularityScore
        FROM posts p
        LEFT JOIN boards b ON p.board_id = b.id
        LEFT JOIN users u ON p.user_id = u.id
        WHERE p.is_blinded = FALSE
            AND p.is_notice = FALSE
            <include refid="periodCondition"/>
        ORDER BY popularityScore DESC, p.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <!-- 게시판별 인기글 조회 -->
    <select id="selectPopularPostsByBoard" resultType="com.ej2.dto.PopularPostDTO">
        SELECT
            p.id,
            p.board_id AS boardId,
            p.user_id AS userId,
            p.title,
            p.content,
            p.anonymous_id AS anonymousId,
            p.view_count AS viewCount,
            p.like_count AS likeCount,
            p.dislike_count AS dislikeCount,
            p.comment_count AS commentCount,
            p.scrap_count AS scrapCount,
            p.created_at AS createdAt,
            p.updated_at AS updatedAt,
            b.name AS boardName,
            u.name AS authorNickname,
            <include refid="popularityScore"/> AS popularityScore
        FROM posts p
        LEFT JOIN boards b ON p.board_id = b.id
        LEFT JOIN users u ON p.user_id = u.id
        WHERE p.is_blinded = FALSE
            AND p.is_notice = FALSE
            AND p.board_id = #{boardId}
            <include refid="periodCondition"/>
        ORDER BY popularityScore DESC, p.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <!-- 인기글 총 개수 -->
    <select id="countPopularPosts" resultType="int">
        SELECT COUNT(*)
        FROM posts p
        WHERE p.is_blinded = FALSE
            AND p.is_notice = FALSE
            <if test="boardId != null">
                AND p.board_id = #{boardId}
            </if>
            <include refid="periodCondition"/>
    </select>

</mapper>
```

**MyBatis XML 핵심 문법**:

| 태그 | 용도 | 예시 |
|------|------|------|
| `<sql id="...">` | SQL 조각 정의 | 재사용 가능한 코드 분리 |
| `<include refid="..."/>` | SQL 조각 삽입 | 반복 코드 제거 |
| `<choose>/<when>/<otherwise>` | switch-case 문 | 기간별 조건 분기 |
| `<if test="...">` | 조건문 | null 체크 |
| `#{param}` | 파라미터 바인딩 | SQL Injection 방지 |

---

### 4.5 DTO (PopularPostDTO.java)

```java
package com.ej2.dto;

import java.time.LocalDateTime;

public class PopularPostDTO {
    private Long id;
    private Long boardId;
    private Long userId;
    private String title;
    private String content;
    private String anonymousId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private Integer scrapCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String boardName;           // JOIN으로 가져온 게시판명
    private String authorNickname;      // JOIN으로 가져온 작성자명
    private Double popularityScore;     // 계산된 인기도 점수

    // 기본 생성자 + Getter/Setter (생략)
}
```

---

### 4.6 Service (RankingService.java)

```java
package com.ej2.service;

import com.ej2.dto.PopularPostDTO;
import com.ej2.mapper.RankingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)  // 읽기 전용 트랜잭션 (성능 최적화)
public class RankingService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final List<String> VALID_PERIODS =
        Arrays.asList("daily", "weekly", "monthly", "all");

    @Autowired
    private RankingMapper rankingMapper;

    public Map<String, Object> getPopularPosts(String period, int page, int size) {
        String validPeriod = validatePeriod(period);
        int validSize = validateSize(size);
        int validPage = Math.max(0, page);
        int offset = validPage * validSize;

        List<PopularPostDTO> posts = rankingMapper.selectPopularPosts(
            validPeriod, validSize, offset
        );
        int totalCount = rankingMapper.countPopularPosts(null, validPeriod);

        return buildResponse(posts, validPage, validSize, totalCount);
    }

    public Map<String, Object> getPopularPostsByBoard(
            Long boardId, String period, int page, int size) {
        String validPeriod = validatePeriod(period);
        int validSize = validateSize(size);
        int validPage = Math.max(0, page);
        int offset = validPage * validSize;

        List<PopularPostDTO> posts = rankingMapper.selectPopularPostsByBoard(
            boardId, validPeriod, validSize, offset
        );
        int totalCount = rankingMapper.countPopularPosts(boardId, validPeriod);

        return buildResponse(posts, validPage, validSize, totalCount);
    }

    // ========== 유효성 검증 메서드 ==========

    private String validatePeriod(String period) {
        if (period == null || !VALID_PERIODS.contains(period.toLowerCase())) {
            return "weekly";  // 기본값
        }
        return period.toLowerCase();
    }

    private int validateSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) return MAX_PAGE_SIZE;
        return size;
    }

    private Map<String, Object> buildResponse(
            List<PopularPostDTO> posts, int page, int size, int totalCount) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("posts", posts);
        response.put("page", page);
        response.put("size", size);
        response.put("totalCount", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / size));
        return response;
    }
}
```

---

### 4.7 Controller (RankingController.java)

```java
package com.ej2.controller;

import com.ej2.service.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ranking")
@CrossOrigin(origins = "http://localhost:3000")
public class RankingController {

    @Autowired
    private RankingService rankingService;

    /**
     * 전체 인기글 조회
     * GET /api/ranking/popular?period=weekly&page=0&size=20
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularPosts(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result = rankingService.getPopularPosts(period, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 게시판별 인기글 조회
     * GET /api/ranking/popular/board/{boardId}?period=weekly&page=0&size=20
     */
    @GetMapping("/popular/board/{boardId}")
    public ResponseEntity<Map<String, Object>> getPopularPostsByBoard(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result = rankingService.getPopularPostsByBoard(
            boardId, period, page, size
        );
        return ResponseEntity.ok(result);
    }
}
```

---

## 5. API 명세

### 5.1 전체 인기글 조회

```
GET /api/ranking/popular
```

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| period | String | N | weekly | daily, weekly, monthly, all |
| page | int | N | 0 | 페이지 번호 (0부터 시작) |
| size | int | N | 20 | 페이지당 개수 (최대 100) |

**Response**:

```json
{
    "posts": [
        {
            "id": 1,
            "boardId": 2,
            "userId": 5,
            "title": "인기 게시글 제목",
            "content": "게시글 내용...",
            "anonymousId": null,
            "viewCount": 1500,
            "likeCount": 45,
            "dislikeCount": 2,
            "commentCount": 23,
            "scrapCount": 12,
            "createdAt": "2026-01-28T14:30:00",
            "updatedAt": "2026-01-29T10:15:00",
            "boardName": "자유게시판",
            "authorNickname": "홍길동",
            "popularityScore": 66.4
        }
    ],
    "page": 0,
    "size": 20,
    "totalCount": 150,
    "totalPages": 8
}
```

### 5.2 게시판별 인기글 조회

```
GET /api/ranking/popular/board/{boardId}
```

**Path Variables**:

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| boardId | Long | 게시판 ID |

**Query Parameters**: 전체 인기글과 동일

---

## 6. 테스트 방법

### 6.1 빌드

```bash
cd backend
mvn clean compile
```

### 6.2 API 테스트

```bash
# 전체 인기글 (주간)
curl "http://localhost:8080/ej2/api/ranking/popular?period=weekly"

# 게시판별 인기글 (일간, 게시판 ID 1)
curl "http://localhost:8080/ej2/api/ranking/popular/board/1?period=daily"

# 페이징 테스트
curl "http://localhost:8080/ej2/api/ranking/popular?period=monthly&page=2&size=10"
```

---

## 7. JPA와 MyBatis 공존 구조

### 7.1 역할 분담

```
┌─────────────────────────────────────────────────────────────┐
│                      Spring Context                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   @EnableJpaRepositories          @MapperScan               │
│   (com.ej2.repository)            (com.ej2.mapper)          │
│           │                              │                   │
│           ▼                              ▼                   │
│   ┌───────────────┐              ┌───────────────┐          │
│   │ JpaRepository │              │ MyBatis Mapper│          │
│   │ (CRUD 용도)   │              │ (복잡쿼리용도)│          │
│   └───────┬───────┘              └───────┬───────┘          │
│           │                              │                   │
│           └──────────┬───────────────────┘                   │
│                      ▼                                       │
│              ┌──────────────┐                                │
│              │  DataSource  │  (공유)                        │
│              │  (DBCP2)     │                                │
│              └──────┬───────┘                                │
│                     ▼                                        │
│              ┌──────────────┐                                │
│              │   MariaDB    │                                │
│              └──────────────┘                                │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 트랜잭션 관리

- **JpaTransactionManager**가 MyBatis도 관리 가능
- 동일 DataSource 사용 시 트랜잭션 공유됨
- `@Transactional` 어노테이션 정상 동작

---

## 8. 성능 최적화 고려사항

### 8.1 인덱스 추가 권장

```sql
-- 인기글 조회 최적화 인덱스
CREATE INDEX idx_posts_ranking ON posts(
    is_blinded,
    is_notice,
    created_at,
    board_id
);
```

### 8.2 캐싱 전략

인기글은 실시간성이 덜 중요하므로 캐싱 가능:

```java
// 향후 Redis 캐싱 적용 시
@Cacheable(value = "popularPosts", key = "#period + '_' + #page")
public Map<String, Object> getPopularPosts(String period, int page, int size) {
    // ...
}
```

---

## 9. 확장 가능성

| 기능 | 구현 방법 |
|------|----------|
| 실시간 트렌딩 | 최근 1시간 내 급상승 게시글 별도 쿼리 |
| 카테고리별 인기글 | boardId 대신 categoryId 필터 추가 |
| 사용자 맞춤 추천 | 협업 필터링 알고리즘 + 별도 테이블 |
| 가중치 동적 변경 | 설정 테이블에서 가중치 읽어오기 |

---

## 10. 핵심 정리

### 10.1 새로 배운 개념

1. **JPA + MyBatis 공존**: 같은 DataSource 공유하면 두 ORM 동시 사용 가능
2. **`<sql>` 프래그먼트**: 반복되는 SQL 조각을 재사용
3. **동적 쿼리 (`<choose>`)**: 조건에 따라 다른 SQL 생성
4. **`mapUnderscoreToCamelCase`**: DB 컬럼명 자동 변환

### 10.2 파일 경로 요약

| 파일 | 경로 |
|------|------|
| pom.xml | `/backend/pom.xml` |
| RootConfig | `/backend/src/main/java/com/ej2/config/RootConfig.java` |
| RankingMapper.java | `/backend/src/main/java/com/ej2/mapper/RankingMapper.java` |
| RankingMapper.xml | `/backend/src/main/resources/mappers/RankingMapper.xml` |
| PopularPostDTO | `/backend/src/main/java/com/ej2/dto/PopularPostDTO.java` |
| RankingService | `/backend/src/main/java/com/ej2/service/RankingService.java` |
| RankingController | `/backend/src/main/java/com/ej2/controller/RankingController.java` |

---

## 참고 자료

- [MyBatis 공식 문서](https://mybatis.org/mybatis-3/ko/index.html)
- [Hacker News 랭킹 알고리즘](https://medium.com/hacking-and-gonzo/how-hacker-news-ranking-algorithm-works-1d9b0cf2c08d)
- [Reddit 랭킹 알고리즘](https://medium.com/hacking-and-gonzo/how-reddit-ranking-algorithms-work-ef111e33d0d9)
