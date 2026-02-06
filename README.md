# EJ2 🎓

**EJ2**는 대학교 시간표 관리 및 커뮤니티 플랫폼입니다. Java 8 기반의 Spring Framework와 React를 사용한 풀스택 웹 애플리케이션입니다.

## 📋 목차

- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [시작하기](#-시작하기)
- [API 엔드포인트](#-api-엔드포인트)
- [개발 가이드](#-개발-가이드)
- [트러블슈팅](#-트러블슈팅)

## ✨ 주요 기능

### 🗓️ 시간표 관리
- 개인별 맞춤 시간표 생성 및 관리
- 과목 검색 및 추가
- 시간표 시각화 (주간 그리드 뷰)
- 시간표 스크린샷/내보내기 기능

### 👥 사용자 관리
- 회원가입 및 로그인
- 프로필 관리
- 비밀번호 찾기/재설정
- 사용자 랭킹 시스템

### 💬 커뮤니티
- 게시판 (자유게시판, 학과별 게시판 등)
- 게시글 작성, 수정, 삭제
- 댓글 시스템
- 실시간 채팅방
- 신고 시스템

### 🔒 관리자 기능
- 사용자 관리
- 게시글 및 댓글 관리
- 신고 처리
- 시스템 통계

## 🛠 기술 스택

### Backend
| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 8 | 프로그래밍 언어 |
| Spring Framework | 5.3.30 | 백엔드 프레임워크 (Spring Boot 아님) |
| Hibernate | 5.6.15 | ORM (JPA 구현체) |
| Apache Tomcat | 9 | 서블릿 컨테이너 |
| Maven | - | 빌드 도구 |
| HikariCP | - | 커넥션 풀 |

### Frontend
| 기술 | 버전 | 용도 |
|------|------|------|
| React | 18.2.0 | UI 라이브러리 |
| React Router | 7.12.0 | 라우팅 |
| Axios | 1.6.0 | HTTP 클라이언트 |
| html2canvas | 1.4.1 | 스크린샷 기능 |

### Database
| 기술 | 버전 | 용도 |
|------|------|------|
| MariaDB | 10.6 | RDBMS |

### Infrastructure
| 기술 | 용도 |
|------|------|
| Docker | 컨테이너화 |
| Docker Compose | 멀티 컨테이너 오케스트레이션 |
| Nginx | 프론트엔드 웹 서버 (프로덕션) |

## 📁 프로젝트 구조

```
EJ2/
├── backend/                          # Spring Framework 백엔드
│   ├── src/main/
│   │   ├── java/com/ej2/
│   │   │   ├── config/               # Spring 설정 클래스
│   │   │   │   ├── RootConfig.java       # DB, Hibernate, 트랜잭션
│   │   │   │   ├── WebConfig.java        # MVC, CORS, JSON 설정
│   │   │   │   └── SecurityConfig.java   # Spring Security 설정
│   │   │   ├── controller/           # REST API 컨트롤러
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── TimetableController.java
│   │   │   │   ├── CourseSearchController.java
│   │   │   │   ├── BoardController.java
│   │   │   │   ├── PostController.java
│   │   │   │   ├── CommentController.java
│   │   │   │   ├── ChatController.java
│   │   │   │   ├── RankingController.java
│   │   │   │   ├── ReportController.java
│   │   │   │   ├── AdminController.java
│   │   │   │   └── FileUploadController.java
│   │   │   ├── model/                # JPA 엔티티
│   │   │   │   ├── User.java
│   │   │   │   ├── Timetable.java
│   │   │   │   ├── TimetableCourse.java
│   │   │   │   ├── Board.java
│   │   │   │   ├── Post.java
│   │   │   │   ├── Comment.java
│   │   │   │   ├── ChatRoom.java
│   │   │   │   └── Report.java
│   │   │   ├── repository/           # JPA 리포지토리
│   │   │   ├── service/              # 비즈니스 로직
│   │   │   ├── dto/                  # 데이터 전송 객체
│   │   │   └── converter/            # JPA 커스텀 컨버터
│   │   ├── resources/
│   │   └── webapp/WEB-INF/
│   │       └── web.xml               # 서블릿 설정
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                         # React 프론트엔드
│   ├── public/
│   ├── src/
│   │   ├── index.js                  # 진입점
│   │   ├── App.js                    # 메인 앱 컴포넌트 & 라우팅
│   │   └── pages/
│   │       ├── Auth/                 # 인증 페이지
│   │       │   ├── LoginPage.js
│   │       │   ├── RegisterPage.js
│   │       │   ├── FindAccountPage.js
│   │       │   └── PasswordResetPage.js
│   │       ├── Main/                 # 메인 페이지
│   │       ├── Board/                # 게시판 페이지
│   │       │   ├── BoardPage.js
│   │       │   ├── BoardListPage.js
│   │       │   ├── PostListPage.js
│   │       │   ├── PostDetailPage.js
│   │       │   ├── PostWritePage.js
│   │       │   ├── PostEditPage.js
│   │       │   └── CommentSection.js
│   │       ├── Chat/                 # 채팅 페이지
│   │       ├── Users/                # 사용자 페이지
│   │       └── Admin/                # 관리자 페이지
│   │           ├── AdminPage.js
│   │           ├── AdminUsersPage.js
│   │           ├── AdminBoardsPage.js
│   │           └── AdminReportsPage.js
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docker-compose.yml                # Docker Compose 설정
├── README.md                         # 프로젝트 문서
├── CLAUDE.md                         # AI 어시스턴트용 컨텍스트
└── docs/                             # 추가 문서
```

## 🚀 시작하기

### 필수 요구사항

- **Docker** (v20.10 이상)
- **Docker Compose** (v2.0 이상)

개별 실행 시:
- **Java 8**
- **Apache Tomcat 9**
- **Maven 3.6+**
- **Node.js 16+**
- **MariaDB 10.6**

### 빠른 시작 (Docker Compose 사용)

1. **프로젝트 클론**
```bash
git clone <repository-url>
cd EJ2
```

2. **모든 서비스 실행**
```bash
docker-compose up --build
```

3. **애플리케이션 접속**
   - 🌐 **Frontend**: http://localhost:3000
   - 🔧 **Backend API**: http://localhost:8080/ej2/api
   - 🗄️ **MariaDB**: localhost:3306

4. **서비스 중지**
```bash
# 컨테이너 중지
docker-compose down

# 볼륨까지 삭제 (데이터베이스 초기화)
docker-compose down -v
```

### 개별 서비스 실행 (개발 모드)

#### 1️⃣ MariaDB
```bash
docker run -d \
  --name mariadb \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=appdb \
  -e MYSQL_USER=appuser \
  -e MYSQL_PASSWORD=apppassword \
  -p 3306:3306 \
  mariadb:10.6
```

#### 2️⃣ Backend
```bash
cd backend
mvn clean package
# WAR 파일을 Tomcat의 webapps 디렉토리에 복사
cp target/ej2.war $TOMCAT_HOME/webapps/
# Tomcat 시작
$TOMCAT_HOME/bin/catalina.sh run
```

#### 3️⃣ Frontend
```bash
cd frontend
npm install
npm start
```

## 📡 API 엔드포인트

### 인증 API (`/api/auth`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/register` | 회원가입 |
| POST | `/api/auth/logout` | 로그아웃 |
| POST | `/api/auth/reset-password` | 비밀번호 재설정 |

### 사용자 API (`/api/users`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/users` | 모든 사용자 조회 |
| GET | `/api/users/{id}` | 특정 사용자 조회 |
| POST | `/api/users` | 사용자 생성 |
| PUT | `/api/users/{id}` | 사용자 수정 |
| DELETE | `/api/users/{id}` | 사용자 삭제 |

### 시간표 API (`/api/timetables`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/timetables/user/{userId}` | 사용자의 시간표 목록 |
| GET | `/api/timetables/{id}` | 특정 시간표 조회 |
| POST | `/api/timetables` | 시간표 생성 |
| PUT | `/api/timetables/{id}` | 시간표 수정 |
| DELETE | `/api/timetables/{id}` | 시간표 삭제 |

### 과목 검색 API (`/api/courses`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/courses/search` | 과목 검색 |
| GET | `/api/courses/{id}` | 과목 상세 정보 |

### 게시판 API (`/api/boards`, `/api/posts`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/boards` | 게시판 목록 |
| GET | `/api/boards/{id}` | 특정 게시판 조회 |
| GET | `/api/posts` | 게시글 목록 |
| GET | `/api/posts/{id}` | 게시글 상세 |
| POST | `/api/posts` | 게시글 작성 |
| PUT | `/api/posts/{id}` | 게시글 수정 |
| DELETE | `/api/posts/{id}` | 게시글 삭제 |

### 댓글 API (`/api/comments`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/comments/post/{postId}` | 게시글의 댓글 목록 |
| POST | `/api/comments` | 댓글 작성 |
| PUT | `/api/comments/{id}` | 댓글 수정 |
| DELETE | `/api/comments/{id}` | 댓글 삭제 |

### 채팅 API (`/api/chat`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/chat/rooms` | 채팅방 목록 |
| POST | `/api/chat/rooms` | 채팅방 생성 |
| GET | `/api/chat/rooms/{id}/messages` | 채팅 메시지 조회 |

### 랭킹 API (`/api/ranking`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/ranking/users` | 사용자 랭킹 |

### 신고 API (`/api/reports`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/reports` | 신고 목록 |
| POST | `/api/reports` | 신고 등록 |
| PUT | `/api/reports/{id}` | 신고 처리 |

### 관리자 API (`/api/admin`)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/admin/users` | 사용자 관리 |
| GET | `/api/admin/posts` | 게시글 관리 |
| GET | `/api/admin/reports` | 신고 관리 |
| GET | `/api/admin/stats` | 통계 조회 |

## 🔧 개발 가이드

### 환경 설정

#### Backend 설정
- **Context Path**: `/ej2` (web.xml에 설정)
- **API Base Path**: `/api` (WebConfig에 설정)
- **Full API URL**: `http://localhost:8080/ej2/api/*`
- **CORS**: `http://localhost:3000` 허용 (개발 환경)
- **JSON**: Jackson을 사용한 camelCase ↔ snake_case 자동 변환
- **트랜잭션**: `@Transactional` 어노테이션 기반

#### Frontend 설정
- **Dev Server**: 포트 3000
- **Proxy**: `/ej2` 경로를 백엔드로 프록시
- **Production**: Nginx가 정적 파일 제공 및 API 프록시

#### Database 설정
- **Database**: `appdb`
- **User**: `appuser`
- **Password**: `apppassword`
- **Root Password**: `rootpassword`
- **Schema**: Hibernate가 자동 생성 (`hbm2ddl.auto=update`)

### 새로운 기능 추가하기

#### 1. 새로운 엔티티 추가
```java
// 1. model 패키지에 엔티티 생성
@Entity
@Table(name = "your_table")
public class YourEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ... 필드 및 관계 정의
}

// 2. Repository 인터페이스 생성
public interface YourRepository extends JpaRepository<YourEntity, Long> {
    // 커스텀 쿼리 메서드
}

// 3. Service 클래스 생성
@Service
@Transactional
public class YourService {
    // 비즈니스 로직 구현
}

// 4. Controller 생성
@RestController
@RequestMapping("/api/your-resource")
public class YourController {
    // REST API 엔드포인트 구현
}
```

#### 2. 새로운 API 엔드포인트 추가
```java
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @GetMapping("/{id}")
    public ResponseEntity<ResourceDTO> getResource(@PathVariable Long id) {
        // 구현
        return ResponseEntity.ok(resource);
    }

    @PostMapping
    public ResponseEntity<ResourceDTO> createResource(@RequestBody ResourceDTO dto) {
        // 구현
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

#### 3. Frontend API 통합
```javascript
// API 호출 예시
import axios from 'axios';

const fetchData = async () => {
  try {
    // proxy 설정으로 /ej2 자동 추가됨
    const response = await axios.get('/api/resource');
    setData(response.data);
  } catch (error) {
    console.error('Error:', error);
  }
};
```

### 코딩 컨벤션

#### Backend (Java)
- Java 8 호환 코드 작성 (람다, Stream API 제한적 사용)
- Spring Framework 5.3 (Spring Boot 아님)
- `@Service`, `@Repository`, `@Controller` 어노테이션 사용
- `@Transactional`로 트랜잭션 관리
- RESTful API 설계 원칙 준수

#### Frontend (React)
- 함수형 컴포넌트 사용
- React Hooks 활용
- 명확한 컴포넌트 명명
- PropTypes 또는 TypeScript (선택적)

### 데이터베이스 접근

```bash
# Docker 컨테이너의 MariaDB 접속
docker exec -it mariadb mysql -u appuser -p
# Password: apppassword

# 또는 root로 접속
docker exec -it mariadb mysql -u root -p
# Password: rootpassword
```

```sql
-- 데이터베이스 선택
USE appdb;

-- 테이블 목록 확인
SHOW TABLES;

-- 사용자 조회
SELECT * FROM users;
```

## 🐛 트러블슈팅

### 자주 발생하는 문제

#### 1. 백엔드 연결 실패
```bash
# 컨테이너 상태 확인
docker-compose ps

# 백엔드 로그 확인
docker-compose logs backend

# MariaDB 연결 확인
docker exec -it mariadb mysql -u appuser -p -e "SELECT 1"
```

#### 2. CORS 오류
- `WebConfig.java`에서 CORS 설정 확인
- 프론트엔드 URL이 허용 목록에 있는지 확인

#### 3. 데이터베이스 초기화
```bash
# 볼륨 포함 전체 삭제
docker-compose down -v

# 재시작
docker-compose up --build
```

#### 4. 포트 충돌
```bash
# 포트 사용 확인
lsof -i :3000  # Frontend
lsof -i :8080  # Backend
lsof -i :3306  # MariaDB

# 프로세스 종료
kill -9 <PID>
```

### 추가 문서
- 📄 `docs/0119_1_troubleshooting_guide.md` - 상세 트러블슈팅 가이드
- 📄 `docs/0118_1_BEGINNER_GUIDE.md` - 초보자를 위한 상세 가이드
- 📄 `CLAUDE.md` - AI 어시스턴트용 프로젝트 컨텍스트

## 📝 라이센스

이 프로젝트는 교육 목적으로 제작되었습니다.

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📮 문의

프로젝트에 대한 질문이나 제안사항이 있으시면 이슈를 등록해주세요.

---

**Made with ❤️ for better education**
