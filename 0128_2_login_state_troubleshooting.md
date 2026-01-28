# 로그인 상태 유지 버그 수정 + stompjs 빌드 에러 해결 - 트러블슈팅 & 학습 가이드 (0128_2)

## 목차
1. [문제 개요](#문제-개요)
2. [원인 분석: 왜 로그인 후 상태가 유지되지 않았나?](#원인-분석-왜-로그인-후-상태가-유지되지-않았나)
3. [해결 방법: CustomEvent 패턴](#해결-방법-customevent-패턴)
4. [수정한 파일 전체 정리](#수정한-파일-전체-정리)
5. [부가 문제: stompjs 빌드 에러](#부가-문제-stompjs-빌드-에러)
6. [디버깅 과정: 문제를 어떻게 찾았나?](#디버깅-과정-문제를-어떻게-찾았나)
7. [핵심 인사이트](#핵심-인사이트)
8. [Bash 명령어 설명](#bash-명령어-설명)
9. [TODO(human) 학습 정리](#todohuman-학습-정리)

---

## 문제 개요

### 증상
로그인 페이지에서 유저명/비밀번호를 입력하고 로그인하면, **메인페이지로 정상 이동**하지만 **NavBar에 여전히 "ログイン" 링크가 표시**됨. 즉, 로그인 자체는 성공하지만 로그인 상태가 UI에 반영되지 않음.

### 영향 범위
- 로그인 후 NavBar의 유저명/로그아웃 버튼이 표시되지 않음
- 회원가입 후에도 동일한 증상
- 페이지를 새로고침(F5)하면 로그인 상태가 정상 표시됨 ← 중요한 단서!

### 기대 동작
- 로그인 성공 → 메인페이지로 이동 → NavBar에 `👤 田中太郎` + `ログアウト` 버튼 표시
- 회원가입 성공 → 메인페이지로 이동 → 동일하게 로그인 상태 표시

---

## 원인 분석: 왜 로그인 후 상태가 유지되지 않았나?

### 핵심 원인: SPA에서 NavBar 컴포넌트가 리마운트되지 않는 문제

이 문제를 이해하려면 **SPA(Single Page Application)의 동작 방식**을 이해해야 합니다.

### SPA vs 전통적 웹페이지

```
[전통적 웹페이지 (MPA)]
로그인 → 서버가 새 HTML 전체를 반환 → 브라우저가 페이지 전체를 다시 로드
→ NavBar도 새로 생성됨 → localStorage 다시 읽음 → ✅ 정상 동작

[SPA (React)]
로그인 → JavaScript가 URL만 변경 → 페이지 전체를 다시 로드하지 않음
→ NavBar는 이미 마운트된 상태 → localStorage를 다시 읽지 않음 → ❌ 상태 갱신 안 됨
```

### 문제의 흐름을 단계별로 보면

**React의 컴포넌트 구조:**
```
<App>                      ← 최상위 컴포넌트 (1회 마운트)
  <Router>
    <NavBar />             ← 모든 페이지에서 공통 (1회 마운트, 리마운트 안 됨!)
    <Routes>
      <Route path="/" element={<MainPage />} />
      <Route path="/login" element={<LoginPage />} />
    </Routes>
  </Router>
</App>
```

**1단계 - 앱 시작 시:**
```javascript
// App.js - NavBar 컴포넌트
useEffect(() => {
  checkUserLogin();  // ← 마운트 시 1회만 실행
}, []);              // ← 의존성 배열이 비어있음 = 마운트 시에만

const checkUserLogin = () => {
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    setUser(JSON.parse(storedUser));  // localStorage에 user 있으면 상태 설정
  }
};
```
→ 앱 시작 시 localStorage에 user가 없으므로 `user = null` → "ログイン" 링크 표시

**2단계 - 로그인 성공 시:**
```javascript
// LoginPage.js
if (response.data.success) {
  localStorage.setItem('user', JSON.stringify(response.data.user));  // ① localStorage에 저장
  navigate('/');  // ② 메인페이지로 이동
}
```
→ `navigate('/')`는 **URL만 변경**하고 `<MainPage />`를 렌더링
→ **NavBar는 이미 마운트된 상태이므로 useEffect가 다시 실행되지 않음**
→ NavBar의 `user` state는 여전히 `null`

**3단계 - NavBar의 렌더링:**
```javascript
// App.js - NavBar JSX
{user ? (
  <div className="nav-user-info">
    <span>👤 {user.name}</span>           // ← user가 null이므로 이 부분 안 나옴
    <button onClick={handleLogout}>ログアウト</button>
  </div>
) : (
  <Link to="/login">ログイン</Link>       // ← 이 부분이 표시됨 (user === null)
)}
```

### 새로고침하면 되는 이유

```
브라우저 새로고침 → React 앱 전체가 처음부터 다시 시작
→ NavBar가 새로 마운트됨
→ useEffect 실행 → localStorage에서 user 읽음
→ user state 설정됨 → "👤 田中太郎" 표시 ✅
```

---

## 해결 방법: CustomEvent 패턴

### 개념

```
[수정 전]
LoginPage: localStorage에 user 저장 → navigate('/')
NavBar: (아무것도 모름, useEffect가 다시 실행되지 않음)

[수정 후]
LoginPage: localStorage에 user 저장 → "authChange" 이벤트 발행 → navigate('/')
NavBar: "authChange" 이벤트를 감지 → localStorage 다시 읽음 → user state 갱신
```

### CustomEvent란?

브라우저의 이벤트 시스템을 활용해서 컴포넌트 간에 메시지를 보내는 방법입니다.

```javascript
// 이벤트 보내기 (발행/publish)
window.dispatchEvent(new Event('authChange'));

// 이벤트 받기 (구독/subscribe)
window.addEventListener('authChange', () => {
  // 이벤트가 발생하면 실행될 코드
});
```

`click`, `scroll` 같은 브라우저 내장 이벤트와 동일한 메커니즘이지만, 이름을 직접 정의한 **커스텀 이벤트**입니다.

### 다른 해결 방법들과 비교

| 방법 | 복잡도 | 장점 | 단점 |
|------|:---:|------|------|
| **CustomEvent** (이번에 사용) | 낮음 | 코드 변경 최소, React 외부에서도 동작 | 이벤트 이름 관리 필요 |
| **React Context** | 중간 | React 방식에 맞음, 타입 안전 | Provider 구조 변경 필요, 앱 전체에 영향 |
| **상태 관리 라이브러리** (Zustand 등) | 높음 | 대규모 앱에 적합, DevTools 지원 | 라이브러리 추가 필요, 학습 비용 |
| **URL 파라미터** | 낮음 | 간단함 | URL이 지저분해짐, 보안 이슈 |
| **window.location.reload()** | 낮음 | 가장 간단 | 페이지 전체 리로드 → SPA의 장점 상실 |

이 프로젝트 규모에서는 **CustomEvent**가 가장 적절한 선택입니다. React Context나 상태 관리 라이브러리는 인증 관련 상태가 더 복잡해질 때(예: 토큰 갱신, 권한 관리 등) 도입하면 좋습니다.

---

## 수정한 파일 전체 정리

### 1. 수정: `App.js` (NavBar 컴포넌트)

**경로:** `frontend/src/App.js`

**변경 전:**
```javascript
useEffect(() => {
  fetchBoards();
  checkUserLogin();
}, []);

const checkUserLogin = () => {
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    setUser(JSON.parse(storedUser));
  }
};
```

**변경 후:**
```javascript
useEffect(() => {
  fetchBoards();
  checkUserLogin();

  // ログイン・ログアウト時にNavBarの状態を更新するイベントリスナー
  const handleAuthChange = () => {
    checkUserLogin();
  };
  window.addEventListener('authChange', handleAuthChange);

  return () => {
    window.removeEventListener('authChange', handleAuthChange);
  };
}, []);

const checkUserLogin = () => {
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    setUser(JSON.parse(storedUser));
  } else {
    setUser(null);  // ← 추가: 로그아웃 시 user를 null로 설정
  }
};
```

**포인트:**
- `window.addEventListener('authChange', ...)`: 커스텀 이벤트 리스너 등록
- `return () => { window.removeEventListener(...) }`: **클린업 함수** - 컴포넌트가 언마운트될 때 리스너 해제 (메모리 누수 방지)
- `else { setUser(null) }`: 로그아웃 후 상태를 null로 설정하는 방어 코드 추가

### 2. 수정: `LoginPage.js`

**경로:** `frontend/src/pages/Auth/LoginPage.js`

**변경 전:**
```javascript
if (response.data.success) {
  localStorage.setItem('user', JSON.stringify(response.data.user));
  navigate('/');
}
```

**변경 후:**
```javascript
if (response.data.success) {
  localStorage.setItem('user', JSON.stringify(response.data.user));

  // NavBarにログイン状態の変更を通知
  window.dispatchEvent(new Event('authChange'));

  navigate('/');
}
```

**포인트:** `navigate('/')` 전에 이벤트를 발행하여, 페이지 이동 전에 NavBar가 먼저 상태를 갱신하도록 함

### 3. 수정: `RegisterPage.js`

**경로:** `frontend/src/pages/Auth/RegisterPage.js`

LoginPage.js와 동일한 패턴으로 수정:
```javascript
if (response.data.success) {
  localStorage.setItem('user', JSON.stringify(response.data.user));

  // NavBarにログイン状態の変更を通知
  window.dispatchEvent(new Event('authChange'));

  navigate('/');
}
```

**포인트:** 회원가입 성공 시에도 자동 로그인되므로 같은 이벤트 발행 필요

---

## 부가 문제: stompjs 빌드 에러

### 증상

로그인 수정 후 `docker-compose up --build`를 실행하니 프론트엔드 빌드가 실패:

```
Module not found: Error: Can't resolve 'net' in '/app/node_modules/stompjs/lib'
```

### 원인: Webpack 5와 stompjs의 비호환

```
[라이브러리 구조]
stompjs/
├── index.js           ← 브라우저용 (Stomp.over 제공)
└── lib/
    ├── stomp.js       ← 핵심 로직
    └── stomp-node.js  ← Node.js용 (require('net') 포함!)
```

**문제 발생 흐름:**
```
import { Client } from 'stompjs'
→ Webpack이 stompjs 패키지 전체를 번들링
→ lib/stomp-node.js의 require('net') 발견
→ Webpack 5는 Node.js polyfill을 자동 제공하지 않음
→ "Can't resolve 'net'" 에러 발생!
```

**Webpack 4 vs 5의 차이:**
- **Webpack 4** (react-scripts 4.x): Node.js 코어 모듈(`net`, `fs` 등)의 브라우저 polyfill을 자동 포함
- **Webpack 5** (react-scripts 5.x): 보안과 번들 크기를 위해 polyfill을 제거. 필요하면 수동 설정 필요

### 해결: stompjs → @stomp/stompjs 마이그레이션

`stompjs`는 더 이상 관리되지 않는 구버전 라이브러리입니다. `@stomp/stompjs`는 브라우저 환경을 위해 설계된 현대적 버전입니다.

**설치:**
```bash
npm install @stomp/stompjs
npm uninstall stompjs
```

**import 변경:**
```javascript
// 수정 전
import SockJS from 'sockjs-client';
import { Client } from 'stompjs';      // ← Node.js 의존성 포함

// 수정 후
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs'; // ← 브라우저 전용
```

### API 변경점 정리

`@stomp/stompjs`는 API가 다릅니다:

| 기능 | stompjs (구버전) | @stomp/stompjs (새버전) |
|------|-----------------|----------------------|
| **클라이언트 생성** | `Client.over(socket)` | `new Client({ webSocketFactory: () => socket })` |
| **연결** | `client.connect({}, callback)` | `client.activate()` + `onConnect` 옵션 |
| **메시지 전송** | `client.send(dest, headers, body)` | `client.publish({ destination, body })` |
| **연결 해제** | `client.disconnect()` | `client.deactivate()` |
| **디버그 끄기** | `client.debug = null` | `debug: () => {}` |

**생성 + 연결 코드 비교:**

```javascript
// [수정 전 - stompjs]
const socket = new SockJS('/api/ws/chat');
const client = Client.over(socket);
client.debug = null;
client.connect({}, () => {
  // 연결 성공 콜백
  client.subscribe('/topic/chat/1', (msg) => { ... });
  client.send('/app/chat/1/join', {}, JSON.stringify({...}));
}, (error) => {
  // 에러 콜백
});

// [수정 후 - @stomp/stompjs]
const client = new Client({
  webSocketFactory: () => new SockJS('/api/ws/chat'),
  debug: () => {},
  onConnect: () => {
    // 연결 성공 콜백
    client.subscribe('/topic/chat/1', (msg) => { ... });
    client.publish({
      destination: '/app/chat/1/join',
      body: JSON.stringify({...})
    });
  },
  onStompError: (error) => {
    // 에러 콜백
  }
});
client.activate();  // ← 별도로 활성화 호출
```

**메시지 전송 비교:**
```javascript
// [수정 전]
client.send('/app/chat/1/send', {}, JSON.stringify({ content: 'hello' }));
//          destination       headers  body

// [수정 후]
client.publish({
  destination: '/app/chat/1/send',
  body: JSON.stringify({ content: 'hello' })
});
```

**연결 해제 비교:**
```javascript
// [수정 전]
client.disconnect();

// [수정 후]
client.deactivate();
```

---

## 디버깅 과정: 문제를 어떻게 찾았나?

### 1단계: 문제 범위 좁히기

"로그인이 안 된다"는 증상에서 **어디서 문제가 발생하는지** 확인:

```bash
# 백엔드 API가 정상인지 직접 테스트
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"password123"}'
```

결과:
```json
{"success":true,"message":"ログインに成功しました","user":{"id":1,"username":"testuser1",...}}
```
→ ✅ 백엔드 정상 → 문제는 프론트엔드

### 2단계: Nginx 프록시 확인

```bash
# Nginx를 통한 요청도 테스트
curl -s -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"password123"}'
```
→ ✅ Nginx 프록시도 정상

### 3단계: DB 확인

```bash
# DB에 유저가 존재하는지 확인
docker exec mariadb mysql -u appuser -papppassword -D appdb \
  -e "SELECT id, username, name FROM users;"
```
→ ✅ 유저 존재, BCrypt 해시도 정상

### 4단계: 코드 분석

백엔드는 정상 → 프론트엔드 코드 분석:
1. `LoginPage.js`: localStorage에 user 저장 후 `navigate('/')` → 정상
2. `App.js` (NavBar): `useEffect([], [])` 마운트 시 1회만 실행 → **문제 발견!**

### 5단계: 가설 검증

"새로고침하면 로그인 상태가 표시되나요?" → "예"
→ NavBar 리마운트 시에만 localStorage를 읽는다는 가설 확인

---

## 핵심 인사이트

### 1. SPA에서의 컴포넌트 라이프사이클

React Router에서 `navigate()`는 **URL만 변경**하고, 해당 Route의 컴포넌트만 교체합니다. Route 밖에 있는 컴포넌트(NavBar 등)는 리마운트되지 않습니다.

```
<Router>
  <NavBar />          ← navigate()해도 리마운트 안 됨!
  <Routes>
    <Route ... />     ← 이 안의 컴포넌트만 교체됨
  </Routes>
</Router>
```

**중요:** `useEffect(fn, [])`에서 빈 의존성 배열 `[]`은 "마운트 시 1회만 실행"을 의미합니다. 컴포넌트가 리마운트되지 않으면 다시 실행되지 않습니다.

### 2. 컴포넌트 간 통신 패턴

React에서 형제 컴포넌트(NavBar ↔ LoginPage)끼리 직접 통신하기 어렵습니다:

```
<App>
  ├── <NavBar />        ← 어떻게 LoginPage의 로그인 성공을 알 수 있나?
  └── <Routes>
       └── <LoginPage />  ← 로그인 성공 이벤트를 누구에게 알려야 하나?
```

**해결 방법의 계층:**
```
단순 ─────────────────────────────────────────────── 복잡
CustomEvent → props drilling → Context API → Zustand/Redux

이번 프로젝트                      대규모 프로젝트에서 사용
```

### 3. useEffect의 클린업 함수

```javascript
useEffect(() => {
  window.addEventListener('authChange', handleAuthChange);

  return () => {
    window.removeEventListener('authChange', handleAuthChange);  // ← 클린업
  };
}, []);
```

`return` 안의 함수는 **클린업 함수**로, 컴포넌트가 언마운트될 때 실행됩니다.
이벤트 리스너를 등록하고 해제하지 않으면 **메모리 누수**가 발생합니다:

```
컴포넌트 마운트   → addEventListener (리스너 1개)
컴포넌트 언마운트 → (해제 안 하면 리스너가 남음)
컴포넌트 재마운트 → addEventListener (리스너 2개!)
...반복 → 리스너가 계속 쌓임 → 메모리 누수 + 이벤트 중복 처리
```

### 4. Webpack 5의 Node.js polyfill 제거

Webpack 5부터 Node.js 코어 모듈의 브라우저 polyfill이 자동 포함되지 않습니다. 이는 **번들 크기 최적화**와 **보안**을 위한 의도적 변경입니다.

```
[Webpack 4]
import 'net' → webpack이 자동으로 브라우저용 polyfill 제공
              (실제로 동작하진 않지만 빌드는 성공)

[Webpack 5]
import 'net' → "Module not found: Can't resolve 'net'" 에러
              (명시적으로 polyfill 설정하거나, 해당 라이브러리 교체 필요)
```

영향받는 Node.js 모듈: `net`, `fs`, `path`, `crypto`, `stream`, `buffer`, `os` 등

### 5. 라이브러리 선택 시 주의점

| 체크 항목 | stompjs | @stomp/stompjs |
|-----------|---------|----------------|
| 최종 업데이트 | 2018년 (6년 전) | 2024년 (활발히 관리) |
| 브라우저 전용 | ❌ (Node.js 의존) | ✅ |
| TypeScript 지원 | ❌ | ✅ |
| Webpack 5 호환 | ❌ | ✅ |

npm 패키지를 선택할 때 확인할 것:
1. **최종 업데이트 날짜** - 1년 이상 업데이트 없으면 주의
2. **주간 다운로드 수** - 커뮤니티 크기 확인
3. **GitHub Issues** - 미해결 이슈가 많으면 주의
4. **번들 환경 호환** - Webpack 5, ESM 등 지원 여부

---

## Bash 명령어 설명

### API 테스트 명령어

| 명령어 | 설명 |
|--------|------|
| `curl -s -X POST <URL>` | POST 요청 전송 (`-s`: 진행률 숨김) |
| `-H "Content-Type: application/json"` | 요청 헤더 설정 (JSON 형식 명시) |
| `-d '{"key":"value"}'` | 요청 본문(body) 데이터 |
| `curl -v` | 상세(verbose) 모드 - 요청/응답 헤더 전체 표시 |
| `curl -c -` | 쿠키를 stdout에 출력 (세션 쿠키 확인용) |

### Docker 빌드 명령어

| 명령어 | 설명 |
|--------|------|
| `docker-compose up -d --build frontend` | frontend 서비스만 재빌드 후 백그라운드 실행 |
| `docker-compose ps` | 컨테이너 상태 확인 |
| `docker logs spring-backend --tail 50` | 백엔드 로그 마지막 50줄 확인 |

### npm 명령어

| 명령어 | 설명 |
|--------|------|
| `npm install @stomp/stompjs` | 새 패키지 설치 (dependencies에 추가) |
| `npm uninstall stompjs` | 기존 패키지 제거 (dependencies에서 삭제) |

### DB 확인 명령어

```bash
# MariaDB에 접속하여 유저 테이블 확인
docker exec mariadb mysql -u appuser -papppassword -D appdb \
  -e "SELECT id, username, name, email FROM users;"
```

`-e` 옵션: 인터랙티브 모드 없이 SQL 한 줄 실행 후 종료

---

## TODO(human) 학습 정리

이 섹션은 직접 정리해보세요. 이번 작업에서 배운 것들을 자신의 말로 적어보면 학습 효과가 높아집니다.

### 질문 가이드

1. **`window.dispatchEvent(new Event('authChange'))` 대신 React Context를 사용하면 어떤 코드 변경이 필요할까?**
   - App.js, LoginPage.js, RegisterPage.js에서 각각 어떻게 바뀌는지 생각해보세요
   - 답:

2. **현재 로그아웃 처리에서 `authChange` 이벤트를 발행하지 않는 이유는?**
   - `handleLogout` 함수의 코드를 읽고, 이벤트가 필요 없는 이유를 설명해보세요
   - 힌트: NavBar의 `handleLogout`은 어디에서 실행되나요?
   - 답:

3. **`@stomp/stompjs`의 `activate()`와 `deactivate()`는 비동기(async)입니다. 이것이 `beforeunload` 이벤트 핸들러에서 문제가 될 수 있을까요?**
   - `beforeunload`에서 비동기 작업이 어떻게 동작하는지 조사해보세요
   - 답:

4. **`useEffect`의 의존성 배열에 대해:**
   ```javascript
   // A: 마운트 시 1회만 실행
   useEffect(() => { ... }, []);

   // B: user가 변경될 때마다 실행
   useEffect(() => { ... }, [user]);

   // C: 매 렌더링마다 실행
   useEffect(() => { ... });
   ```
   - 이번 문제에서 NavBar의 useEffect가 `[]` (A 패턴)이었기 때문에 문제가 발생했습니다. 만약 B나 C 패턴으로 바꾸면 문제가 해결될까요? 왜 안 될까요?
   - 답:
