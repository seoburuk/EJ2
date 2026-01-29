# 채팅 기능 트러블슈팅 가이드 (2026.01.29)

## 1. 문제 상황

### 증상
```
Failed to init chat: AxiosError
Request failed with status code 404
/api/chat/rooms/1/nickname - 404 Not Found
```

채팅 페이지에서 닉네임 할당 API 호출 시 404 에러 발생.

---

## 2. 원인 분석

### Context Path 불일치 문제

Docker 환경에서 WAR 파일 배포 방식에 따라 Context Path가 달라집니다:

| 배포 파일명 | Context Path | API 경로 예시 |
|------------|--------------|---------------|
| `ROOT.war` | `/` | `/api/chat/rooms` |
| `ej2.war` | `/ej2` | `/ej2/api/chat/rooms` |

**이 프로젝트의 Dockerfile**:
```dockerfile
COPY --from=build /app/target/ej2.war /usr/local/tomcat/webapps/ROOT.war
```
→ `ROOT.war`로 배포하므로 Context Path는 `/`

**잘못된 nginx.conf 설정**:
```nginx
location /api {
    proxy_pass http://backend:8080/ej2/api;  # 잘못됨!
}
```
→ 존재하지 않는 `/ej2/api`로 프록시하여 404 발생

---

## 3. 해결 방법

### nginx.conf 수정
```nginx
# 변경 전 (잘못된 설정)
location /api {
    proxy_pass http://backend:8080/ej2/api;
}
location /ws {
    proxy_pass http://backend:8080/ej2/ws;
}

# 변경 후 (올바른 설정)
location /api {
    proxy_pass http://backend:8080/api;
}
location /ws {
    proxy_pass http://backend:8080/ws;
}
```

### ChatPage.js 경로 확인
```javascript
// 올바른 경로 (ROOT.war 배포 시)
axios.post(`/api/chat/rooms/${GLOBAL_ROOM_ID}/nickname`, {...})
axios.get(`/api/chat/rooms/${GLOBAL_ROOM_ID}/messages`)
new SockJS('/ws/chat')
```

---

## 4. 사용한 트러블슈팅 명령어

### Docker 컨테이너 상태 확인
```bash
# 실행 중인 컨테이너 목록
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### 백엔드 로그 확인
```bash
# 최근 로그 50줄 확인
docker logs spring-backend 2>&1 | tail -50

# 에러/경고 로그만 필터링
docker logs spring-backend 2>&1 | grep -E "(ERROR|WARN|Started)" | tail -30
```

### API 연결 테스트
```bash
# HTTP 상태 코드만 확인
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/chat/rooms

# 응답 내용 확인
curl -s http://localhost:8080/api/chat/rooms

# 응답 + HTTP 상태 코드 함께 확인
curl -s -w "\nHTTP Status: %{http_code}" http://localhost:8080/api/chat/rooms
```

### Docker 재빌드 및 재시작
```bash
# 전체 재빌드 (캐시 포함)
docker-compose down && docker-compose up --build -d

# 특정 서비스만 재빌드
docker-compose up --build -d frontend

# 백엔드 시작 대기 후 테스트 (20초 대기)
sleep 20 && curl -s http://localhost:8080/api/chat/rooms
```

---

## 5. 학습 내용

### 5.1 프록시 (Proxy) 개념

**프록시란?**
클라이언트와 서버 사이에서 요청을 중계하는 서버입니다.

```
[브라우저] ---> [Nginx 프록시] ---> [Spring 백엔드]
   :3000          :80               :8080
```

**왜 프록시를 사용하나요?**
1. **CORS 문제 해결**: 같은 도메인에서 요청하는 것처럼 보이게 함
2. **보안**: 백엔드 서버를 직접 노출하지 않음
3. **로드 밸런싱**: 여러 백엔드 서버로 요청 분산

**nginx.conf 프록시 설정 예시**:
```nginx
location /api {
    proxy_pass http://backend:8080/api;  # /api 요청을 백엔드로 전달
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

### 5.2 WebSocket 개념

**HTTP vs WebSocket**:
```
HTTP (단방향):
[클라이언트] --요청--> [서버]
[클라이언트] <--응답-- [서버]
(매번 새 연결)

WebSocket (양방향):
[클라이언트] <==연결 유지==> [서버]
(한번 연결 후 계속 통신)
```

**WebSocket이 필요한 경우**:
- 실시간 채팅
- 주식/코인 실시간 시세
- 온라인 게임
- 알림 시스템

**WebSocket 프록시 설정 (nginx)**:
```nginx
location /ws {
    proxy_pass http://backend:8080/ws;
    proxy_http_version 1.1;

    # WebSocket 업그레이드 헤더 (필수!)
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

    # 긴 연결 유지를 위한 타임아웃 설정
    proxy_read_timeout 86400s;   # 24시간
    proxy_send_timeout 86400s;
}
```

**Spring WebSocket + STOMP 구조**:
```
[React]                    [Spring]
   |                          |
   |-- SockJS 연결 ---------->| /ws/chat (WebSocket 엔드포인트)
   |                          |
   |-- STOMP SUBSCRIBE ------>| /topic/chat/{roomId} (메시지 수신)
   |                          |
   |-- STOMP SEND ----------->| /app/chat/{roomId}/send (메시지 발신)
   |                          |
   |<-- 브로드캐스트 ---------| (같은 방 모든 사용자에게 전달)
```

### 5.3 Context Path 이해

**Context Path란?**
웹 애플리케이션의 루트 URL 경로입니다.

```
전체 URL: http://localhost:8080/ej2/api/users
           \_____기본_____/ \__/ \___/
              호스트:포트   Context  API경로
                            Path
```

**Tomcat에서 Context Path 결정 방식**:
| WAR 파일명 | webapps 내 폴더 | Context Path |
|-----------|----------------|--------------|
| ROOT.war | ROOT/ | `/` |
| ej2.war | ej2/ | `/ej2` |
| myapp.war | myapp/ | `/myapp` |

### 5.4 Docker 네트워크

**Docker Compose 내부 통신**:
```yaml
services:
  frontend:
    # backend:8080으로 접근 가능 (서비스 이름 = 호스트명)
  backend:
    ports:
      - "8080:8080"  # 외부:내부
  mariadb:
    # backend에서 mariadb:3306으로 접근
```

**호스트에서 접근 vs 컨테이너에서 접근**:
```
호스트(맥/윈도우):  localhost:8080
프론트엔드 컨테이너: backend:8080
백엔드 컨테이너:     mariadb:3306
```

---

## 6. 체크리스트

채팅 기능이 안 될 때 확인할 사항:

- [ ] Docker 컨테이너가 모두 실행 중인가?
  ```bash
  docker ps
  ```

- [ ] 백엔드 로그에 에러가 있는가?
  ```bash
  docker logs spring-backend 2>&1 | tail -50
  ```

- [ ] API가 직접 응답하는가?
  ```bash
  curl http://localhost:8080/api/chat/rooms
  ```

- [ ] nginx 프록시 경로가 올바른가?
  - `nginx.conf`의 `proxy_pass` 확인

- [ ] 프론트엔드 API 경로가 올바른가?
  - ROOT.war 배포: `/api/...`
  - ej2.war 배포: `/ej2/api/...`

- [ ] WebSocket 연결 경로가 올바른가?
  - nginx에서 `/ws` → 백엔드 `/ws`로 프록시 확인

- [ ] 코드 변경 후 Docker 재빌드했는가?
  ```bash
  docker-compose up --build -d
  ```

---

## 7. 참고: 프로젝트 파일 구조

```
EJ2/
├── backend/
│   ├── src/main/java/com/ej2/
│   │   ├── controller/ChatController.java    # REST + WebSocket 핸들러
│   │   ├── service/ChatService.java          # 비즈니스 로직
│   │   ├── config/WebSocketConfig.java       # WebSocket 설정
│   │   └── model/ChatMessage.java            # 메시지 엔티티
│   └── Dockerfile
├── frontend/
│   ├── src/pages/Chat/ChatPage.js            # 채팅 UI + 로직
│   ├── nginx.conf                            # 프록시 설정
│   └── Dockerfile
└── docker-compose.yml
```

---

## 8. 관련 에러 메시지 해석

| 에러 메시지 | 원인 | 해결 |
|------------|------|------|
| `No mapping for GET /ej2/api/...` | Context Path 불일치 | nginx.conf 프록시 경로 확인 |
| `Connection refused` | 백엔드 미실행 | `docker ps`로 확인 후 재시작 |
| `WebSocket connection failed` | WS 프록시 설정 오류 | nginx의 Upgrade 헤더 확인 |
| `CORS error` | 다른 도메인 접근 | 프록시 사용 또는 CORS 설정 |

---

## 9. 익명 번호 리셋 & 이전 메시지 숨김 (2026.01.29 추가)

### 9.1 요구사항

| 요구사항 | 설명 |
|---------|------|
| 익명 번호 리셋 | 채팅방에 아무도 없으면 匿名1부터 다시 시작 |
| 이전 메시지 숨김 | 새로 입장한 사용자는 이전 대화 볼 수 없음 (DB에는 기록 유지) |

### 9.2 구현 내용

#### ChatService.java - userLeave() 수정
```java
// User leaves room - reset nickname counter when room becomes empty
public void userLeave(Long roomId) {
    Optional<ChatRoom> optRoom = chatRoomRepository.findById(roomId);
    if (optRoom.isPresent()) {
        ChatRoom room = optRoom.get();
        int count = room.getCurrentUsers() - 1;
        room.setCurrentUsers(Math.max(0, count));

        // 채팅방에 아무도 없으면 익명 번호 리셋
        if (count <= 0) {
            room.setNicknameCounter(0);
        }

        chatRoomRepository.save(room);
    }
}
```

#### ChatPage.js - 이전 메시지 로드 제거
```javascript
// 2. 이전 메시지 로드하지 않음 (새로 들어온 사용자는 이전 대화 볼 수 없음)
// const messagesRes = await axios.get(`/api/chat/rooms/${GLOBAL_ROOM_ID}/messages`);
// setMessages(messagesRes.data);
setMessages([]);
```

### 9.3 동작 흐름

```
[사용자 A 입장] → 匿名1 할당, currentUsers=1, nicknameCounter=1
[사용자 B 입장] → 匿名2 할당, currentUsers=2, nicknameCounter=2
[A, B 대화중] → 실시간 메시지 주고받음
[사용자 A 퇴장] → currentUsers=1
[사용자 B 퇴장] → currentUsers=0, nicknameCounter=0 (리셋!)
[사용자 C 입장] → 匿名1로 시작, 이전 대화 안 보임
```

### 9.4 알려진 문제: currentUsers 불일치

**문제 상황**:
브라우저 강제 종료, 네트워크 끊김 등으로 `beforeunload` 이벤트나 WebSocket Disconnect 이벤트가 정상 발생하지 않으면 `currentUsers`가 실제보다 높게 남아있을 수 있음.

**증상**:
- DB의 `current_users`가 0이 아니어서 익명 번호가 리셋되지 않음
- 실제로 아무도 없는데 匿名5, 匿名6부터 시작

**임시 해결 (수동 리셋)**:
```bash
docker exec mariadb mysql -u appuser -papppassword -e \
  "UPDATE appdb.chat_rooms SET current_users = 0, nickname_counter = 0 WHERE id = 1;"
```

**영구 해결 방안** (선택 가능):
1. 서버 시작 시 모든 방의 `currentUsers`를 0으로 초기화
2. 주기적인 스케줄러로 비정상 세션 정리
3. WebSocket 세션 목록을 직접 추적하여 실제 접속자 수 계산

### 9.5 DB 상태 확인 명령어

```bash
# 채팅방 상태 확인
docker exec mariadb mysql -u appuser -papppassword -e \
  "SELECT id, name, current_users, nickname_counter FROM appdb.chat_rooms;"

# 카운터 수동 리셋
docker exec mariadb mysql -u appuser -papppassword -e \
  "UPDATE appdb.chat_rooms SET current_users = 0, nickname_counter = 0 WHERE id = 1;"

# 채팅 메시지 확인 (최근 10개)
docker exec mariadb mysql -u appuser -papppassword -e \
  "SELECT id, sender_nickname, type, LEFT(content, 30) as content, created_at FROM appdb.chat_messages ORDER BY id DESC LIMIT 10;"
```

### 9.6 pom.xml 의존성 추가

`@PostConstruct` 사용 시 Java 8 + Spring Framework 환경에서 필요:
```xml
<!-- javax.annotation (for @PostConstruct) -->
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
</dependency>
```

---

## 10. 체크리스트 (익명 번호 관련)

익명 번호가 리셋되지 않을 때:

- [ ] DB의 `current_users` 값 확인
  ```bash
  docker exec mariadb mysql -u appuser -papppassword -e \
    "SELECT current_users, nickname_counter FROM appdb.chat_rooms WHERE id=1;"
  ```

- [ ] `current_users`가 0보다 크면 수동 리셋
  ```bash
  docker exec mariadb mysql -u appuser -papppassword -e \
    "UPDATE appdb.chat_rooms SET current_users=0, nickname_counter=0 WHERE id=1;"
  ```

- [ ] 백엔드 로그에서 disconnect 이벤트 확인
  ```bash
  docker logs spring-backend 2>&1 | grep -i "disconnect\|leave" | tail -10
  ```

- [ ] 코드 변경 후 Docker 재빌드
  ```bash
  docker-compose up --build -d
  ```
