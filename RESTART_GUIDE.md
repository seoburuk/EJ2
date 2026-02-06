# EJ2 재시작 가이드

## 사전 조건
- Docker Desktop 실행 필요 (MariaDB, 백엔드는 Docker로 동작)
- Node.js 설치 필요 (프론트엔드 로컬 개발 서버)

---

## 1. 백엔드 + DB 실행 (Docker)

### 전체 시작
```bash
docker compose up mariadb backend -d
```

### 백엔드만 재시작 (코드 변경 후)
```bash
docker compose up backend --build -d
```

### 백엔드 로그 확인
```bash
docker compose logs -f backend
```

### 전체 중지
```bash
docker compose down
```

### DB 데이터 초기화 (주의)
```bash
docker compose down -v
```

---

## 2. 프론트엔드 실행 (로컬)

### 시작
```bash
cd frontend
npm start
```

### 재시작 (기존 프로세스 종료 후)
```bash
# 포트 3000 사용 중인 프로세스 확인 및 종료
lsof -ti:3000 | xargs kill -9
npm start
```

---

## 3. 전체 재시작 (한번에)

```bash
# 1) Docker 컨테이너 재시작
docker compose down
docker compose up mariadb backend --build -d

# 2) 프론트엔드 재시작
cd frontend
lsof -ti:3000 | xargs kill -9 2>/dev/null
npm start
```

---

## 접속 URL

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080/api |
| MariaDB | localhost:3306 (appuser / apppassword) |

---

## 트러블슈팅

### Docker가 안 켜져 있을 때
```
Cannot connect to the Docker daemon. Is the docker daemon running?
```
→ Docker Desktop을 먼저 실행

### 포트 충돌 (3306)
```
bind: address already in use
```
→ 로컬 MariaDB가 실행 중. 종료 후 재시도:
```bash
brew services stop mariadb
docker compose up mariadb backend -d
```

### 포트 충돌 (8080)
```bash
lsof -ti:8080 | xargs kill -9
docker compose up backend -d
```

### 백엔드 DB 연결 실패
→ MariaDB 컨테이너가 먼저 완전히 시작되어야 함. 백엔드만 재시작:
```bash
docker compose restart backend
```
