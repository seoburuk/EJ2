# 0127_2 변경 이력 - 로그인 페이지 UI 리디자인

## 뭘 바꿨나?

**변경 전**: 그래디언트 배경 + 그림자 카드 + 일본어 UI의 기존 로그인 페이지
**변경 후**: 미니멀 플랫 디자인 + 영문 UI + 둥근 버튼의 모던한 로그인 페이지

---

## 변경한 파일 목록 (2개)

| # | 파일 | 뭘 바꿨나? |
|---|------|-----------|
| 1 | `frontend/src/pages/Auth/AuthPages.css` | 전체 스타일 리디자인 (플랫 디자인으로 변경) |
| 2 | `frontend/src/pages/Auth/LoginPage.js` | 텍스트를 영문으로 변경, 레이아웃 구조 수정 |

---

## 각 파일 상세 설명

### 1. AuthPages.css (스타일 전면 리디자인)

**위치**: `frontend/src/pages/Auth/AuthPages.css`

#### ① 배경 변경
```css
/* 변경 전: 그래디언트 배경 */
background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);

/* 변경 후: 단색 배경 */
background: #f5f5f5;
```

#### ② 카드 스타일 변경
```css
/* 변경 전: 그림자 카드 */
.auth-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  max-width: 450px;
}

/* 변경 후: 테두리 카드 (플랫) */
.auth-card {
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 40px 30px;
  max-width: 420px;
}
```
- `box-shadow` 제거 → `border`로 변경하여 플랫한 느낌
- 카드 너비 축소 (450px → 420px)

#### ③ 입력 필드 스타일 변경
```css
/* 변경 전: 테두리 있는 입력란 */
.form-group input {
  border: 2px solid #e0e0e0;
  border-radius: 8px;
}
.form-group input:focus {
  border-color: #64748b;
  box-shadow: 0 0 0 3px rgba(100, 116, 139, 0.15);
}

/* 변경 후: 테두리 없는 배경색 입력란 */
.form-group input {
  border: none;
  border-radius: 12px;
  background: #e8e8e8;
  padding: 16px 20px;
}
.form-group input:focus {
  background: #e0e0e0;
  box-shadow: none;
}
```
- 라벨 숨김 (`display: none`) → placeholder만으로 안내
- 포커스 시 배경색만 살짝 변경 (border 효과 대신)

#### ④ 버튼 스타일 변경
```css
/* 변경 전: 그래디언트 사각형 버튼 */
.auth-button {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
  border-radius: 8px;
  font-weight: 600;
}

/* 변경 후: 단색 둥근 버튼 */
.auth-button {
  background: #1e293b;
  border-radius: 30px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
```
- `border-radius: 30px`으로 pill 형태의 둥근 버튼
- 그래디언트 제거, 진한 네이비(#1e293b) 단색

#### ⑤ 컨셉 링크 섹션 추가 (신규)
```css
.concept-links {
  margin-top: 40px;
  text-align: center;
  font-size: 14px;
  color: #888;
}
```
- 모바일/데스크톱 컨셉 페이지로 이동하는 링크 영역

---

### 2. LoginPage.js (텍스트 및 구조 변경)

**위치**: `frontend/src/pages/Auth/LoginPage.js`

#### ① 타이틀 변경
```jsx
// 변경 전:
<h2 className="auth-title">ログイン</h2>

// 변경 후:
<h2 className="auth-title">Welcome Back 👊</h2>
```

#### ② 입력 필드 placeholder 변경
```jsx
// 변경 전:
placeholder="ユーザー名を入力"
placeholder="パスワードを入力"

// 변경 후:
placeholder="Username"
placeholder="Password"
```

#### ③ 버튼 텍스트 변경
```jsx
// 변경 전:
{loading ? 'ログイン中...' : 'ログイン'}

// 변경 후:
{loading ? 'Signing In...' : 'Sign In'}
```

#### ④ 하단 링크 구조 변경
```jsx
// 변경 전: 비밀번호 재설정 + 회원가입 링크
<div className="auth-links">
  <a href="/password-reset">パスワードをお忘れですか？</a>
  <p>アカウントをお持ちでないですか？<a href="/register">会員登録</a></p>
</div>

// 변경 후: Sign Up 링크만 + 컨셉 링크 추가
<div className="auth-links">
  <a href="/register">Sign Up</a>
</div>
<div className="concept-links">
  view concept for <a href="/concept/mobile">mobile</a> or for <a href="/concept/desktop">desktop</a>
</div>
```
- 비밀번호 재설정 링크 삭제
- "아카운트를 갖고 있지 않습니까?" 문구 삭제
- 심플하게 Sign Up 링크만 남김
- 컨셉 페이지 링크 추가

---

## 디자인 변경 비교

| 요소 | 변경 전 | 변경 후 |
|------|---------|---------|
| 배경 | 그래디언트 (e2e8f0 → cbd5e1) | 단색 (#f5f5f5) |
| 카드 | box-shadow | border (1px solid #e0e0e0) |
| 입력란 | border + focus에 glow | 배경색만 (border 없음) |
| 라벨 | 표시 | 숨김 (placeholder만 사용) |
| 버튼 | 그래디언트 + 사각형 | 단색 + pill 형태 (둥근) |
| 색상 톤 | 회색 (64748b계열) | 네이비 (1e293b계열) |
| 언어 | 일본어 | 영어 |
| 하단 링크 | 비밀번호 재설정 + 회원가입 | Sign Up + 컨셉 링크 |

---

## 트러블슈팅

### 증상 1: CSS 변경이 반영되지 않음

**원인**: 브라우저 캐시 또는 React 개발 서버 HMR 문제

**해결 방법**:
1. 브라우저 강력 새로고침: `Cmd + Shift + R` (Mac) / `Ctrl + Shift + R` (Windows)
2. React 개발 서버 재시작:
```bash
# 포트 3000 프로세스 종료 후 재시작
lsof -ti:3000 | xargs kill -9
cd frontend && npm start
```
3. 그래도 안 되면 webpack 캐시 삭제:
```bash
rm -rf frontend/node_modules/.cache
cd frontend && npm start
```

### 증상 2: 카드가 배경에 녹아들어 구분이 안 됨

**원인**: `.auth-card`와 `.auth-container`의 배경색이 동일할 경우 발생

**해결 방법**: `.auth-card`에 `border` 추가 또는 `background`를 다른 색으로 설정
```css
.auth-card {
  background: #ffffff;      /* 컨테이너(#f5f5f5)와 다른 색 */
  border: 1px solid #e0e0e0; /* 테두리로 구분 */
}
```

---

## 디자인 의도

- **미니멀리즘**: 그래디언트, 그림자 등 장식 요소를 최소화
- **플랫 디자인**: border와 배경색 차이로 구분감 표현
- **모던 트렌드**: pill 버튼, placeholder만 사용하는 입력란
- **국제화 준비**: 일본어 → 영어로 전환하여 다국어 대응 기반 마련
