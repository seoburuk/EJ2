# 0122_3 게시글 작성 페이지 이동 문제 트러블슈팅 가이드 (초보자용)

## 📋 문제 상황
게시판 목록 페이지에서 "投稿する" (게시글 작성) 버튼을 클릭해도 페이지 이동이 안 되는 문제가 발생했습니다.

## 🔍 문제 진단 과정

### 1단계: 버튼 클릭 이벤트 확인
**문제**: 버튼을 클릭해도 반응이 없음
**진단 방법**: console.log를 추가하여 클릭 이벤트가 실제로 발생하는지 확인

#### 사용한 코드
```javascript
// PostListPage.js - 138번째 줄 근처
<button
  className="write-post-button"
  onClick={() => {
    console.log('=== 投稿ボタンクリック ===');
    console.log('boardId:', boardId);
    console.log('board object:', board);
    console.log('Navigate to:', `/boards/${boardId}/write`);
    navigate(`/boards/${boardId}/write`, { state: { board } });
  }}
>
  投稿する
</button>
```

**학습 포인트**:
- `console.log()`는 가장 기본적인 디버깅 도구입니다
- 변수의 실제 값을 확인하여 예상과 다른 부분을 찾을 수 있습니다
- React의 onClick 핸들러가 실행되는지 먼저 확인해야 합니다

### 2단계: DOM에서 버튼 존재 확인
**문제**: console.log가 나오지 않음
**진단 방법**: 브라우저 개발자 도구에서 버튼이 실제로 렌더링되었는지 확인

#### 사용한 Bash 명령어
```bash
# 브라우저 콘솔에서 실행 (JavaScript)
document.querySelector('.write-post-button')
```

**결과 해석**:
- `<button class="write-post-button">...</button>` → 버튼 존재 ✅
- `null` → 버튼이 렌더링되지 않음 ❌

**학습 포인트**:
- `document.querySelector()`는 CSS 선택자로 DOM 요소를 찾습니다
- 버튼이 `null`이면 컴포넌트 렌더링 자체에 문제가 있는 것입니다
- 버튼이 존재하는데 클릭이 안 되면 이벤트 리스너 문제입니다

### 3단계: React 서버 상태 확인
**문제**: 버튼은 존재하지만 onClick이 작동하지 않음
**원인**: Hot Module Replacement가 제대로 작동하지 않아 코드 변경사항이 반영되지 않음

#### 사용한 Bash 명령어

**1. React 프로세스 찾기**
```bash
ps aux | grep "react-scripts start" | grep -v grep | awk '{print $2}'
```
- `ps aux`: 모든 실행 중인 프로세스 표시
- `grep "react-scripts start"`: "react-scripts start"를 포함한 프로세스 필터링
- `grep -v grep`: grep 명령어 자체는 제외
- `awk '{print $2}'`: 프로세스 ID(PID)만 출력

**출력 예시**: `4881` (프로세스 ID)

**2. 프로세스 종료**
```bash
kill 4881
```
- `kill [PID]`: 해당 프로세스를 종료합니다

**3. 포트 3000 사용 중인 프로세스 강제 종료**
```bash
lsof -ti:3000 | xargs kill -9
```
- `lsof -ti:3000`: 포트 3000을 사용 중인 프로세스의 PID 출력
  - `-t`: PID만 출력 (간결한 형식)
  - `-i:3000`: 포트 3000을 사용하는 프로세스 찾기
- `|`: 파이프 - 앞 명령의 출력을 뒤 명령의 입력으로 전달
- `xargs kill -9`: 받은 PID들에 대해 강제 종료
  - `kill -9`: SIGKILL 신호로 강제 종료 (프로세스가 무시할 수 없음)

**4. React 서버 재시작**
```bash
cd /Users/yunsu-in/Downloads/EJ2/frontend && npm start
```
- `cd [경로]`: 디렉토리 이동
- `&&`: 앞 명령이 성공하면 뒤 명령 실행
- `npm start`: package.json의 start 스크립트 실행 (React 개발 서버 시작)

**5. 백그라운드에서 실행**
```bash
cd /Users/yunsu-in/Downloads/EJ2/frontend && npm start &
```
- 마지막에 `&`를 붙이면 백그라운드에서 실행됩니다
- 터미널을 닫아도 서버가 계속 실행됩니다

**6. 서버 상태 확인**
```bash
curl -s http://localhost:3000 | head -20
```
- `curl -s [URL]`: URL에 HTTP 요청을 보내고 응답 받기
  - `-s`: silent 모드 (진행 상태 숨김)
- `|`: 파이프 - 출력을 다음 명령으로 전달
- `head -20`: 처음 20줄만 출력

**출력 예시**:
```html
<!DOCTYPE html>
<html lang="ko">
  <head>
    <meta charset="utf-8" />
    ...
```

**학습 포인트**:
- React 개발 서버가 제대로 작동하지 않으면 코드 변경사항이 반영되지 않습니다
- Hot Module Replacement는 편리하지만 때로는 완전한 재시작이 필요합니다
- 백그라운드에서 서버를 실행하면 터미널을 계속 사용할 수 있습니다

### 4단계: 브라우저 캐시 문제 해결
**문제**: 서버는 재시작했지만 여전히 이전 코드가 실행됨
**해결 방법**: 강력 새로고침으로 브라우저 캐시 무시

#### 브라우저 조작
- **Windows/Linux**: `Ctrl + Shift + R`
- **Mac**: `Cmd + Shift + R`

**일반 새로고침 vs 강력 새로고침**:
- **일반 새로고침** (`F5`, `Ctrl+R`): 캐시된 파일 사용
- **강력 새로고침** (`Ctrl+Shift+R`): 캐시 무시하고 서버에서 모든 파일 다시 다운로드

**학습 포인트**:
- 브라우저는 성능을 위해 JavaScript, CSS 파일을 캐시합니다
- 개발 중에는 강력 새로고침을 사용해야 최신 코드를 볼 수 있습니다
- 시크릿/프라이빗 모드를 사용하면 캐시 없이 테스트할 수 있습니다

## 🎯 최종 해결 방법

### 요약
1. **디버깅 로그 추가**: onClick 핸들러에 console.log 추가
2. **서버 재시작**: React 개발 서버 완전 재시작
3. **강력 새로고침**: 브라우저 캐시 무시하고 최신 코드 로드
4. **결과 확인**: 콘솔에서 로그 확인 및 페이지 이동 성공

### 성공 로그 예시
```
=== 投稿ボタンクリック ===
boardId: 1
board object: {id: 1, name: '自由掲示板', code: 'GENERAL', ...}
Navigate to: /boards/1/write
=== 投稿処理開始 ===
現在のユーザー: {id: 6, username: 'newuser1', ...}
```

## 🐛 추가로 발견된 문제: 백엔드 서버 연결 오류

### 에러 메시지
```
POST http://localhost:3000/api/posts 500 (Internal Server Error)
Proxy error: Could not proxy request /api/posts from localhost:3000 to http://localhost:8080 (ECONNREFUSED).
```

### 원인
백엔드 서버(Tomcat)가 실행되지 않아서 API 요청이 실패

### 해결 방법

#### 방법 1: Docker 사용 (권장)
```bash
# Docker 상태 확인
docker-compose ps

# Docker 컨테이너 시작
cd /Users/yunsu-in/Downloads/EJ2
docker-compose up -d

# 로그 확인
docker-compose logs -f backend
```

#### 방법 2: 로컬 Tomcat 사용
```bash
# 백엔드 빌드
cd /Users/yunsu-in/Downloads/EJ2/backend
mvn clean package

# WAR 파일이 생성됨: target/ej2.war
# 이 파일을 Tomcat의 webapps 폴더에 복사
```

## 📚 학습한 주요 개념

### 1. React 디버깅
- **console.log()**: 가장 기본적이고 효과적인 디버깅 도구
- **브라우저 개발자 도구**: DOM 검사, 콘솔 로그, 네트워크 요청 확인
- **React DevTools**: 컴포넌트 구조 및 state/props 확인

### 2. 프로세스 관리
- **ps**: 실행 중인 프로세스 확인
- **kill**: 프로세스 종료
- **lsof**: 파일/포트를 사용 중인 프로세스 찾기
- **백그라운드 실행**: `&`를 사용하여 터미널을 계속 사용

### 3. 네트워크 디버깅
- **curl**: HTTP 요청 테스트
- **프록시 에러**: 프론트엔드에서 백엔드로 요청 전달 실패
- **ECONNREFUSED**: 대상 서버가 실행되지 않거나 연결 거부

### 4. React 라우팅
- **useNavigate()**: React Router의 프로그래밍 방식 네비게이션
- **state 전달**: navigate() 시 state로 데이터 전달
- **URL 파라미터**: useParams()로 URL의 동적 세그먼트 접근

## 🔧 유용한 Bash 명령어 모음

### 프로세스 관리
```bash
# 포트 사용 중인 프로세스 찾기
lsof -ti:3000

# 특정 프로세스 찾기
ps aux | grep "react-scripts"

# 프로세스 강제 종료
kill -9 [PID]

# 포트 사용 프로세스 한 번에 종료
lsof -ti:3000 | xargs kill -9
```

### 파일 검색
```bash
# 파일 내용에서 텍스트 검색
grep -r "write-post-button" src/

# 특정 확장자만 검색
grep -r "write-post-button" src/ --include="*.css"

# 줄 번호와 함께 검색
grep -n "write-post-button" src/pages/Board/PostListPage.js
```

### 서버 상태 확인
```bash
# HTTP 요청 보내기
curl http://localhost:3000

# 헤더만 확인
curl -I http://localhost:3000

# 응답 시간 측정
curl -w "@-" -o /dev/null -s http://localhost:3000
```

### 로그 확인
```bash
# 파일의 마지막 20줄 보기
tail -20 /path/to/logfile

# 실시간 로그 감시
tail -f /path/to/logfile

# 처음 20줄 보기
head -20 /path/to/logfile
```

## 💡 트러블슈팅 체크리스트

React 페이지 이동 문제가 발생하면 다음 순서로 확인하세요:

- [ ] **1. 라우트 설정 확인**: App.js에서 Route가 올바르게 설정되었는지
- [ ] **2. onClick 이벤트 확인**: console.log로 클릭 이벤트가 발생하는지
- [ ] **3. DOM 렌더링 확인**: document.querySelector()로 요소가 존재하는지
- [ ] **4. 브라우저 캐시**: 강력 새로고침 (Ctrl+Shift+R)
- [ ] **5. React 서버 재시작**: 개발 서버를 완전히 재시작
- [ ] **6. 콘솔 에러 확인**: 브라우저 콘솔에 빨간색 에러가 있는지
- [ ] **7. 네트워크 요청 확인**: 개발자 도구의 Network 탭에서 API 요청 상태
- [ ] **8. 백엔드 서버 확인**: 백엔드가 실행 중인지 (Docker 또는 Tomcat)

## 🚨 자주 발생하는 실수

### 1. 상대 경로 vs 절대 경로
```javascript
// ❌ 잘못됨
navigate('boards/1/write')  // 현재 경로에 추가됨

// ✅ 올바름
navigate('/boards/1/write')  // 루트부터 시작
```

### 2. state 전달 누락
```javascript
// ❌ board 정보 없이 이동
navigate(`/boards/${boardId}/write`)

// ✅ board 정보와 함께 이동
navigate(`/boards/${boardId}/write`, { state: { board } })
```

### 3. useNavigate는 컴포넌트 내부에서만
```javascript
// ❌ 컴포넌트 외부
const navigate = useNavigate()  // Error!

function MyComponent() {
  // ✅ 컴포넌트 내부
  const navigate = useNavigate()
}
```

## 📝 다음 단계

이제 페이지 이동은 정상 작동합니다! 다음은:

1. **백엔드 서버 시작**: Docker Compose 또는 로컬 Tomcat
2. **데이터베이스 연결 확인**: MariaDB가 실행 중인지
3. **API 엔드포인트 테스트**: Postman 또는 curl로 API 테스트
4. **게시글 작성 기능 완성**: 이미지 업로드, 유효성 검사 등

## 🎓 배운 점 정리

1. **디버깅은 단계적으로**: 작은 부분부터 확인하여 문제를 좁혀갑니다
2. **console.log는 강력합니다**: 변수의 실제 값을 확인하는 가장 쉬운 방법
3. **캐시 문제를 간과하지 마세요**: 개발 중에는 강력 새로고침을 습관화
4. **프로세스 관리 중요**: 포트 충돌이나 좀비 프로세스를 정리하는 방법 숙지
5. **프론트엔드와 백엔드는 별개**: 프론트엔드가 작동해도 백엔드가 필요합니다

---

작성일: 2026-01-22
작성자: Claude (AI Assistant)
문제: 게시글 작성 페이지 이동 안 됨
해결: 서버 재시작 + 브라우저 캐시 클리어
