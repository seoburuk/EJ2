# EJ2

Java 8 + Spring Framework + React + MariaDB + Docker 풀스택 애플리케이션

## 기술 스택

- **Backend**: Java 8, Spring Framework 5.3.30, Hibernate 5.6.15, Apache Tomcat 9
- **Frontend**: React 18, Axios
- **Database**: MariaDB 10.6
- **Container**: Docker, Docker Compose

## 프로젝트 구조

```
EJ2/
├── backend/                # Spring Framework 백엔드
│   ├── src/
│   │   └── main/
│   │       ├── java/com/ej2/
│   │       │   ├── config/         # Spring 설정
│   │       │   │   ├── RootConfig.java
│   │       │   │   └── WebConfig.java
│   │       │   ├── controller/
│   │       │   ├── model/
│   │       │   ├── repository/
│   │       │   └── service/
│   │       ├── resources/
│   │       └── webapp/
│   │           └── WEB-INF/
│   │               └── web.xml
│   ├── Dockerfile
│   └── pom.xml
├── frontend/               # React 프론트엔드
│   ├── public/
│   ├── src/
│   ├── Dockerfileㅊ
│   ├── nginx.conf
│   └── package.json
└── docker-compose.yml
```

## 시작하기

### 필수 요구사항

- Docker
- Docker Compose

### 실행 방법

1. 프로젝트 클론 또는 다운로드

2. Docker Compose로 모든 서비스 실행:
```bash
docker-compose up --build
```

3. 애플리케이션 접속:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api
   - MariaDB: localhost:3306

### 개별 서비스 실행 (개발 모드)

#### Backend
```bash
cd backend
mvn clean package
# WAR 파일을 Tomcat에 배포
```

#### Frontend
```bash
cd frontend
npm install
npm start
```

#### MariaDB
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

## API 엔드포인트

- `GET /api/users` - 모든 사용자 조회
- `GET /api/users/{id}` - 특정 사용자 조회
- `POST /api/users` - 새 사용자 생성
- `PUT /api/users/{id}` - 사용자 정보 수정
- `DELETE /api/users/{id}` - 사용자 삭제

## 데이터베이스 설정

- Database: `appdb`
- Username: `appuser`
- Password: `apppassword`
- Root Password: `rootpassword`

## 중지 및 정리

```bash
# 컨테이너 중지
docker-compose down

# 볼륨까지 삭제
docker-compose down -v
```
# EJ2
