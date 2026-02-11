# Troubleshooting Guide 0209_1 - 채팅 익명 닉네임 초기화 광클 오류

## 문제 설명

**증상**: 채팅 버튼을 빠르게 여러 번 클릭(광클)하면 다음과 같은 오류 발생

```
1. 닉네임 번호가 비정상적으로 증가 (匿名1 → 匿名2 → 匿名3... 전부 같은 유저)
2. currentUsers 카운트 불일치
3. OptimisticLockException 500 에러 가능성
4. 이전 WebSocket 연결이 정리되기 전에 새 연결이 생성되어 JOIN/LEAVE 메시지 꼬임
```

**재현 방법**: NavBar의 「チャット」 버튼을 0.5초 이내에 3~5회 빠르게 클릭

### 에러 발생 흐름 (수정 전)

```
1. 사용자가 チャット 버튼 클릭 → window.open('/chat', 'ej2-chat', ...) 호출
2. 기존 팝업 창이 /chat으로 다시 네비게이션 → React ChatPage 언마운트/리마운트
3. 리마운트마다 useEffect → initChat() 실행
4. initChat()에서 POST /api/chat/rooms/1/nickname → nicknameCounter 증가
5. 광클 3회 → 匿名1, 匿名2, 匿名3 할당 (실제 유저는 1명)
6. 버려진 WebSocket 연결이 나중에 disconnect → userLeave() 호출 → currentUsers 꼬임
```

### 근본 원인

`window.open(url, name)`의 두 번째 파라미터가 같은 이름(`'ej2-chat'`)이면 기존 창을 **재사용**하지만, URL로 **재네비게이션**한다. 이로 인해 React 컴포넌트가 완전히 리마운트되면서 `initChat()`이 반복 호출됨.

---

## 수정 내용 — 3단계 방어 (Swiss Cheese Model)

### Layer 1: 팝업 중복 네비게이션 차단 (근본 원인 제거)

**파일**: `frontend/src/App.js`

**수정 전**:
```jsx
<a href="/chat" className="nav-link" onClick={(e) => {
  e.preventDefault();
  window.open('/chat', 'ej2-chat', 'width=500,height=700,scrollbars=yes,resizable=yes');
}}>チャット</a>
```

**수정 후**:
```jsx
// App.js 상단 (컴포넌트 외부)
let chatWindowRef = null;

// onClick 핸들러
onClick={(e) => {
  e.preventDefault();
  if (chatWindowRef && !chatWindowRef.closed) {
    chatWindowRef.focus();  // 기존 창에 포커스만 이동
    return;
  }
  chatWindowRef = window.open('/chat', 'ej2-chat', 'width=500,height=700,scrollbars=yes,resizable=yes');
}}
```

**원리**: `window.open()` 반환값을 저장하여, 이미 열린 창이 있으면 `focus()`만 호출. 재네비게이션 자체를 차단.

---

### Layer 2: initChat() 재진입 방지 + sessionStorage 캐싱

**파일**: `frontend/src/pages/Chat/ChatPage.js`

**2a. initCalledRef 가드** (React StrictMode 더블마운트 대응):
```jsx
const initCalledRef = useRef(false);

useEffect(() => {
  if (initCalledRef.current) return;
  initCalledRef.current = true;
  initChat();
  // ...
}, []);
```

**2b. disconnectSync() + 300ms 딜레이** (이전 연결 정리):
```jsx
const initChat = async () => {
  disconnectSync();
  await new Promise(function(resolve) { setTimeout(resolve, 300); });
  // ...
};
```

**2c. sessionStorage 닉네임 캐싱 + sessionToken**:
```jsx
let assignedNickname = sessionStorage.getItem('ej2-chat-nickname');

if (!assignedNickname) {
  let sessionToken = sessionStorage.getItem('ej2-chat-session-token');
  if (!sessionToken) {
    sessionToken = Date.now().toString(36) + Math.random().toString(36).substr(2, 9);
    sessionStorage.setItem('ej2-chat-session-token', sessionToken);
  }

  const nicknameRes = await axios.post(`/api/chat/rooms/${GLOBAL_ROOM_ID}/nickname`, {
    useAnonymous: true,
    sessionToken: sessionToken
  });
  assignedNickname = nicknameRes.data.nickname;
  sessionStorage.setItem('ej2-chat-nickname', assignedNickname);
}
```

**원리**: `sessionStorage`는 탭/창 단위로 격리됨. 팝업 창이 닫히면 자동 초기화, 재네비게이션되면 유지 → 닉네임 재사용.

---

### Layer 3: 백엔드 멱등성 캐시

**파일**: `backend/src/main/java/com/ej2/service/ChatService.java`

**3a. ConcurrentHashMap 캐시**:
```java
private final ConcurrentHashMap<String, String> sessionNicknameCache = new ConcurrentHashMap<>();
// Key: "roomId:sessionToken", Value: 할당된 닉네임
```

**3b. 세션 토큰 기반 멱등성 보장**:
```java
public String assignNickname(Long roomId, String sessionToken) {
    if (sessionToken != null && !sessionToken.trim().isEmpty()) {
        String cacheKey = roomId + ":" + sessionToken;
        String existing = sessionNicknameCache.get(cacheKey);
        if (existing != null) {
            return existing;  // 카운터 증가 없이 기존 닉네임 반환
        }
        String nickname = assignNickname(roomId);
        sessionNicknameCache.put(cacheKey, nickname);
        return nickname;
    }
    return assignNickname(roomId);
}
```

**3c. 방 비었을 때 캐시 자동 정리** (`userLeaveInternal` 내부):
```java
if (count <= 0) {
    room.setNicknameCounter(0);
    room.setCurrentUsers(0);

    String roomPrefix = roomId + ":";
    sessionNicknameCache.entrySet().removeIf(entry ->
            entry.getKey().startsWith(roomPrefix));
}
```

**파일**: `backend/src/main/java/com/ej2/controller/ChatController.java`

**3d. sessionToken 파라미터 수신**:
```java
Object token = body.get("sessionToken");
if (token instanceof String) {
    sessionToken = (String) token;
}
// ...
nickname = chatService.assignNickname(roomId, sessionToken);
```

---

## 수정 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `frontend/src/App.js` | `chatWindowRef` 추가, 채팅 버튼 onClick에 중복 방지 로직 |
| `frontend/src/pages/Chat/ChatPage.js` | `initCalledRef` 가드, `sessionStorage` 캐싱, `sessionToken` 전송, 300ms 딜레이 |
| `backend/.../service/ChatService.java` | `sessionNicknameCache`, `assignNickname(roomId, sessionToken)` 오버로드, 캐시 정리 |
| `backend/.../controller/ChatController.java` | `sessionToken` 파라미터 수신 및 서비스에 전달 |

---

## 검증 방법

### 테스트 1: 광클 방지 확인
1. NavBar에서 チャット 버튼을 빠르게 10회 연타
2. **기대 결과**: 팝업 1개만 열리고, 닉네임은 1개만 할당됨

### 테스트 2: 팝업 닫고 다시 열기
1. 채팅 팝업 닫기
2. チャット 버튼 다시 클릭
3. **기대 결과**: 새 닉네임 정상 할당 (sessionStorage 초기화됨)

### 테스트 3: DB 카운터 확인
```sql
SELECT id, nickname_counter, current_users FROM chat_rooms WHERE id = 1;
```
- 광클 후 `nickname_counter`가 불필요하게 증가하지 않는지 확인

### 테스트 4: 다중 사용자 동시 접속
1. 두 개의 다른 브라우저에서 채팅 접속
2. **기대 결과**: 각각 다른 닉네임 (匿名1, 匿名2) 정상 할당

---

## 기술 포인트

### window.open()과 같은 이름의 창
`window.open(url, windowName)`에서 `windowName`이 이미 존재하는 창 이름이면 기존 창을 재사용하지만, **URL로 재네비게이션**한다. 이는 SPA (Single Page Application)에서 `useEffect`가 재실행되는 원인이 됨.

### sessionStorage의 스코프
- 탭/창 단위로 격리됨
- 같은 창에서 페이지 네비게이션 시 유지
- 창이 닫히면 자동 삭제
- `window.open()`으로 열린 팝업은 독립된 `sessionStorage` 가짐

### ConcurrentHashMap vs synchronized
`ConcurrentHashMap`은 세그먼트 단위 잠금으로 읽기 성능이 우수. `removeIf()`는 원자적이지 않지만 개별 삭제 작업은 thread-safe. 방이 비었을 때만 호출되므로 실질적으로 안전.

### Swiss Cheese Model (스위스 치즈 모델)
각 방어층에 구멍이 있더라도 여러 겹이 겹치면 오류가 통과하지 못하는 다층 방어 패턴:
- Layer 1 (프론트 차단) 실패해도 → Layer 2 (컴포넌트 가드)가 잡음
- Layer 2 실패해도 → Layer 3 (서버 멱등성)이 잡음
