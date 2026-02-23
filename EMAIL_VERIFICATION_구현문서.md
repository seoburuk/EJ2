# 이메일 인증 시스템 구현 문서

## 📋 개요

EJ2 프로젝트에 이메일 인증 기능이 추가되었습니다. 사용자는 회원가입 후 이메일 인증을 완료해야만 로그인할 수 있으며, 향상된 UI/UX로 명확한 사용자 경험을 제공합니다.

**구현 날짜**: 2026년 2월 11일
**개발자**: Claude (Sonnet 4.5)
**주요 기능**: 이메일 인증, Gmail SMTP 통합, 애니메이션이 적용된 UI

---

## 🎯 주요 기능

### 1. 이메일 인증 플로우
- 회원가입 시 자동으로 인증 이메일 발송
- 24시간 유효한 인증 토큰 생성
- 이메일 링크 클릭으로 계정 활성화
- 인증 완료 후 자동 로그인 페이지 리다이렉트

### 2. 보안 강화
- 미인증 사용자의 로그인 차단
- UUID 기반 안전한 토큰 생성
- 토큰 만료 시간 검증
- 인증 완료 후 토큰 자동 삭제

### 3. 사용자 경험 개선
- 깔끔한 성공/오류 화면
- 부드러운 애니메이션 효과
- 5초 카운트다운과 자동 리다이렉트
- 인증 메일 재발송 기능
- 모바일 반응형 디자인

---

## 🏗️ 백엔드 구현

### 데이터베이스 스키마 변경

**User 테이블에 추가된 컬럼:**

```sql
ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN email_verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN email_verification_token_expiry DATETIME;
```

- `email_verified`: 이메일 인증 완료 여부 (기본값: false)
- `email_verification_token`: UUID 형식의 인증 토큰
- `email_verification_token_expiry`: 토큰 만료 시간 (24시간)

### 새로 생성된 파일

#### 1. EmailService.java
**위치**: `/backend/src/main/java/com/ej2/service/EmailService.java`

**기능**:
- Gmail SMTP를 사용한 이메일 발송 (JavaMail API)
- HTML 형식의 전문적인 이메일 템플릿
- Gmail 미설정 시 콘솔에 인증 URL 출력 (개발 모드)

**주요 메서드**:
```java
public void sendVerificationEmail(String email, String name, String token) throws MessagingException
```

**특징**:
- `@Value` 어노테이션으로 환경변수 주입 (mail.username, mail.password)
- SMTP 587 포트, STARTTLS 암호화 사용
- 프로덕션/개발 환경 자동 감지
- 에러 핸들링 및 로깅

#### 2. EmailVerificationRequest.java
**위치**: `/backend/src/main/java/com/ej2/dto/EmailVerificationRequest.java`

```java
public class EmailVerificationRequest {
    private String token;
    // getter, setter, 생성자
}
```

#### 3. ResendVerificationRequest.java
**위치**: `/backend/src/main/java/com/ej2/dto/ResendVerificationRequest.java`

```java
public class ResendVerificationRequest {
    private String email;
    // getter, setter, 생성자
}
```

### 수정된 파일

#### 1. User.java
**변경사항**: 3개의 필드 추가

```java
@Column(name = "email_verified")
private Boolean emailVerified = false;

@Column(name = "email_verification_token")
private String emailVerificationToken;

@Column(name = "email_verification_token_expiry")
private LocalDateTime emailVerificationTokenExpiry;
```

#### 2. AuthService.java
**변경사항**: 인증 로직 추가

**register() 메서드 수정**:
```java
// 인증 토큰 생성
String verificationToken = PasswordUtil.generateResetToken();
user.setEmailVerified(false);
user.setEmailVerificationToken(verificationToken);
user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

// 이메일 발송
emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);
```

**login() 메서드에 인증 체크 추가**:
```java
// 이메일 인증 확인
if (user.getEmailVerified() == null || !user.getEmailVerified()) {
    AuthResponse response = new AuthResponse(false, "EMAIL_NOT_VERIFIED", user.getEmail());
    response.setErrorCode("EMAIL_NOT_VERIFIED");
    return response;
}
```

**새로운 메서드**:
- `verifyEmail(String token)`: 토큰으로 이메일 인증
- `resendVerificationEmail(String email)`: 인증 이메일 재발송

#### 3. AuthController.java
**변경사항**: 2개의 엔드포인트 추가

```java
@PostMapping("/verify-email")
public ResponseEntity<AuthResponse> verifyEmail(@RequestBody EmailVerificationRequest request)

@PostMapping("/resend-verification")
public ResponseEntity<AuthResponse> resendVerification(@RequestBody ResendVerificationRequest request)
```

**register() 엔드포인트 수정**:
- 자동 세션 생성 제거 (이메일 인증 필수)

#### 4. UserRepository.java
**새로운 메서드 추가**:

```java
public User findByEmailVerificationToken(String emailVerificationToken)
```

#### 5. AuthResponse.java
**새로운 필드 추가**:

```java
private String errorCode;  // EMAIL_NOT_VERIFIED, TOKEN_EXPIRED 등
private String email;       // 재발송을 위한 이메일 주소
```

### 설정 파일

#### application.properties
```properties
# Gmail SMTP 설정
mail.username=${MAIL_USERNAME}
mail.password=${MAIL_PASSWORD}

# 프론트엔드 URL (이메일 링크용)
frontend.url=${FRONTEND_URL:http://localhost:3000}
```

#### docker-compose.yml
```yaml
backend:
  environment:
    MAIL_USERNAME: ${MAIL_USERNAME}
    MAIL_PASSWORD: ${MAIL_PASSWORD}
    FRONTEND_URL: ${FRONTEND_URL:-http://localhost:3000}
```

---

## 🎨 프론트엔드 구현

### 새로 생성된 파일

#### 1. EmailVerificationPage.js
**위치**: `/frontend/src/pages/Auth/EmailVerificationPage.js`

**기능**:
- URL 파라미터에서 토큰 추출
- 자동 인증 요청 실행
- 4가지 상태 처리:
  - `loading`: 인증 진행 중 (스피너 표시)
  - `success`: 인증 성공 (체크마크 애니메이션 + 5초 카운트다운)
  - `expired`: 토큰 만료 (경고 아이콘)
  - `error`: 인증 실패 (에러 아이콘)

**주요 코드**:
```javascript
const verifyEmail = async () => {
  const response = await axios.post('/api/auth/verify-email', { token });
  if (response.data.success) {
    setStatus('success');
    startCountdown(); // 5초 후 로그인 페이지로 이동
  }
};
```

### 수정된 파일

#### 1. RegisterPage.js
**변경사항**: 회원가입 성공 화면 변경

**기존**:
```javascript
// 자동 로그인 및 홈 페이지 리다이렉트
localStorage.setItem('user', JSON.stringify(response.data.user));
navigate('/');
```

**변경 후**:
```javascript
// 이메일 인증 안내 화면 표시
setRegisteredEmail(formData.email);
setRegistrationSuccess(true);
```

**새로운 UI 요소**:
- 성공 아이콘 (체크마크)
- 등록된 이메일 주소 표시
- 인증 메일 확인 안내 메시지
- "로그인 페이지로" 버튼

#### 2. LoginPage.js
**변경사항**: 미인증 에러 처리 추가

**새로운 상태**:
```javascript
const [showResend, setShowResend] = useState(false);
const [unverifiedEmail, setUnverifiedEmail] = useState('');
const [resendSuccess, setResendSuccess] = useState(false);
```

**에러 핸들링**:
```javascript
if (errorCode === 'EMAIL_NOT_VERIFIED') {
  setError('이메일 주소가 미인증입니다. 받은편지함을 확인해주세요.');
  setShowResend(true);
  setUnverifiedEmail(err.response.data.email);
}
```

**재발송 기능**:
```javascript
const handleResendVerification = async () => {
  await axios.post('/api/auth/resend-verification', { email: unverifiedEmail });
  setResendSuccess(true);
};
```

#### 3. App.js
**변경사항**: 라우트 추가

```javascript
import EmailVerificationPage from './pages/Auth/EmailVerificationPage';

<Route path="/verify-email" element={<EmailVerificationPage />} />
```

### CSS 스타일링

#### AuthPages.css
**추가된 스타일**:

**애니메이션**:
```css
@keyframes fadeInScale { /* 페이드인 + 스케일 */ }
@keyframes spin { /* 스피너 회전 */ }
@keyframes checkBounce { /* 체크마크 바운스 */ }
```

**컴포넌트**:
- `.loading-spinner`: 로딩 스피너
- `.verification-success`: 성공 화면
- `.success-icon`: 성공 아이콘 (초록색 원)
- `.verification-error`: 에러 화면
- `.error-icon`: 에러 아이콘 (빨간색 원)
- `.resend-button`: 재발송 버튼
- `.success-message`: 성공 메시지 배너
- `.countdown`: 카운트다운 텍스트

**반응형 디자인**:
- 모바일 (768px 이하)에서 아이콘 크기 조정
- 패딩 및 폰트 크기 최적화

---

## 🔧 API 명세

### 1. 이메일 인증
**엔드포인트**: `POST /api/auth/verify-email`

**요청 본문**:
```json
{
  "token": "123e4567-e89b-12d3-a456-426614174000"
}
```

**응답**:
```json
{
  "success": true,
  "message": "메일 인증이 완료되었습니다"
}
```

**에러 응답**:
```json
{
  "success": false,
  "message": "TOKEN_EXPIRED"
}
```

### 2. 인증 메일 재발송
**엔드포인트**: `POST /api/auth/resend-verification`

**요청 본문**:
```json
{
  "email": "user@example.com"
}
```

**응답**:
```json
{
  "success": true,
  "message": "인증 메일을 재발송했습니다"
}
```

### 3. 회원가입 (수정됨)
**엔드포인트**: `POST /api/auth/register`

**변경사항**:
- 더 이상 자동 세션 생성하지 않음
- 응답 메시지에 이메일 인증 안내 포함

**응답**:
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다. 메일을 확인하여 인증을 완료해주세요",
  "user": { ... }
}
```

### 4. 로그인 (수정됨)
**엔드포인트**: `POST /api/auth/login`

**새로운 에러 응답**:
```json
{
  "success": false,
  "message": "EMAIL_NOT_VERIFIED",
  "errorCode": "EMAIL_NOT_VERIFIED",
  "email": "user@example.com"
}
```

---

## 📧 이메일 템플릿

### HTML 이메일 구조

```
┌─────────────────────────────┐
│  EJ2 - エブリージャパン      │  ← 헤더 (파란색)
├─────────────────────────────┤
│  안녕하세요, [이름]님        │
│                             │
│  EJ2 회원가입 감사합니다.    │
│                             │
│  ┌─────────────────────┐   │
│  │  메일 주소 인증하기   │   │  ← 인증 버튼 (파란색)
│  └─────────────────────┘   │
│                             │
│  링크: https://...          │  ← 대체 링크
│                             │
│  주의사항:                  │
│  • 유효기간: 24시간          │
│  • 스팸 메일함 확인          │
├─────────────────────────────┤
│  © 2026 EJ2                 │  ← 푸터 (회색)
└─────────────────────────────┘
```

### 특징
- 인라인 CSS 사용 (이메일 클라이언트 호환성)
- 반응형 디자인 (모바일 최적화)
- 대체 텍스트 링크 제공
- 명확한 행동 유도 버튼

---

## 🚀 설치 및 실행

### 1. Gmail SMTP 설정

1. Gmail 계정 준비 (기존 계정 사용 가능)
2. [Google 계정](https://myaccount.google.com/) 접속
3. 보안 → 2단계 인증 활성화
4. 보안 → 앱 비밀번호 생성
   - 앱: "메일"
   - 기기: "기타 (맞춤 이름)" → "EJ2 Backend" 입력
5. 생성된 16자리 비밀번호 복사 (한 번만 표시됨)

### 2. 환경 변수 설정

프로젝트 루트에 `.env` 파일 생성:

```bash
# Gmail SMTP 설정
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-digit-app-password

# 기존 AWS S3 설정
AWS_S3_ACCESS_KEY=your_key
AWS_S3_SECRET_KEY=your_secret
AWS_S3_BUCKET_NAME=your_bucket
AWS_S3_REGION=ap-northeast-2

# 프론트엔드 URL (선택사항)
FRONTEND_URL=http://localhost:3000
```

**중요**:
- `MAIL_USERNAME`에는 Gmail 주소 입력
- `MAIL_PASSWORD`에는 앱 비밀번호 (16자리) 입력, 일반 Gmail 비밀번호 아님
- 하루 최대 500통 이메일 발송 가능 (Gmail 무료 계정 제한)

### 3. Docker로 실행

```bash
# 빌드 및 실행
docker-compose up --build

# 백그라운드 실행
docker-compose up -d
```

**접속 URL**:
- 프론트엔드: http://localhost:3000
- 백엔드 API: http://localhost:8080/ej2/api
- MariaDB: localhost:3306

### 4. 개발 모드 (Gmail 없이)

Gmail 설정 없이도 개발 가능합니다:
- 이메일 대신 콘솔에 인증 URL 출력
- 콘솔에서 URL 복사하여 브라우저에 붙여넣기

**콘솔 출력 예시**:
```
==================== メール認証情報 ====================
受信者: test@example.com (テスト ユーザー)
認証URL: http://localhost:3000/verify-email?token=abc123...
トークン: abc123-def456-ghi789
有効期限: 24時間
=========================================================
```

---

## 🧪 테스트 가이드

### 1. 회원가입 플로우 테스트

```bash
# 1. 회원가입 요청
curl -X POST http://localhost:8080/ej2/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "name": "테스트 유저",
    "email": "test@example.com",
    "password": "password123"
  }'

# 응답 확인
# ✓ success: true
# ✓ message: "회원가입이 완료되었습니다. 메일을 확인하여..."
```

```bash
# 2. 데이터베이스 확인
docker exec -it mariadb mysql -u appuser -papppassword -e \
  "USE appdb; SELECT username, email, email_verified, email_verification_token FROM users WHERE username='testuser';"

# 예상 결과:
# username   | email              | email_verified | email_verification_token
# testuser   | test@example.com   | 0              | abc123-def456-...
```

### 2. 로그인 차단 테스트

```bash
# 미인증 상태에서 로그인 시도
curl -X POST http://localhost:8080/ej2/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'

# 예상 응답:
# {
#   "success": false,
#   "message": "EMAIL_NOT_VERIFIED",
#   "errorCode": "EMAIL_NOT_VERIFIED",
#   "email": "test@example.com"
# }
```

### 3. 이메일 인증 테스트

```bash
# 토큰으로 인증
curl -X POST http://localhost:8080/ej2/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "abc123-def456-ghi789"
  }'

# 응답:
# {
#   "success": true,
#   "message": "메일 인증이 완료되었습니다"
# }
```

```bash
# 데이터베이스 재확인
docker exec -it mariadb mysql -u appuser -papppassword -e \
  "USE appdb; SELECT email_verified, email_verification_token FROM users WHERE username='testuser';"

# 예상 결과:
# email_verified | email_verification_token
# 1              | NULL
```

### 4. 인증 후 로그인 테스트

```bash
# 인증 완료 후 로그인 시도
curl -X POST http://localhost:8080/ej2/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }' \
  -c cookies.txt

# 예상 응답:
# {
#   "success": true,
#   "message": "로그인에 성공했습니다",
#   "user": { ... }
# }
```

### 5. 재발송 테스트

```bash
# 인증 메일 재발송
curl -X POST http://localhost:8080/ej2/api/auth/resend-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'

# 응답:
# {
#   "success": true,
#   "message": "인증 메일을 재발송했습니다"
# }
```

### 6. 토큰 만료 테스트

```bash
# 데이터베이스에서 만료 시간을 과거로 설정
docker exec -it mariadb mysql -u appuser -papppassword -e \
  "USE appdb; UPDATE users SET email_verification_token_expiry = DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE username='testuser';"

# 인증 시도
curl -X POST http://localhost:8080/ej2/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "expired_token"
  }'

# 예상 응답:
# {
#   "success": false,
#   "message": "TOKEN_EXPIRED"
# }
```

### 7. 프론트엔드 UI 테스트

**회원가입 플로우**:
1. http://localhost:3000/register 접속
2. 폼 작성 후 제출
3. ✅ 성공 화면 표시 확인
4. ✅ "메일을 확인하여..." 메시지 확인
5. ✅ 자동 리다이렉트가 되지 않는지 확인

**이메일 인증 플로우**:
1. 콘솔에서 인증 URL 복사
2. 브라우저에 붙여넣기
3. ✅ 로딩 스피너 표시 확인
4. ✅ 성공 체크마크 애니메이션 확인
5. ✅ 5초 카운트다운 확인
6. ✅ 자동 로그인 페이지 이동 확인

**로그인 에러 플로우**:
1. http://localhost:3000/login 접속
2. 미인증 계정으로 로그인 시도
3. ✅ "이메일 주소가 미인증입니다" 에러 메시지 확인
4. ✅ "인증 메일을 재발송" 버튼 표시 확인
5. 버튼 클릭
6. ✅ "인증 메일을 재발송했습니다" 성공 메시지 확인

---

## 🔄 기존 사용자 처리

### 문제점
기존 데이터베이스의 사용자는 `email_verified` 필드가 `NULL` 또는 `FALSE`이므로 로그인할 수 없습니다.

### 해결 방법

**옵션 1: 모든 기존 사용자 인증 처리**
```sql
UPDATE users
SET email_verified = TRUE
WHERE email_verified IS NULL OR email_verified = FALSE;
```

**옵션 2: 특정 사용자만 인증 처리**
```sql
UPDATE users
SET email_verified = TRUE
WHERE username IN ('admin', 'user1', 'user2');
```

**옵션 3: 생성 날짜 기준으로 인증 처리**
```sql
-- 2026년 2월 11일 이전 가입 사용자 모두 인증 처리
UPDATE users
SET email_verified = TRUE
WHERE created_at < '2026-02-11 00:00:00';
```

### 마이그레이션 스크립트

`migration.sql` 파일 생성:
```sql
-- 1. 기존 사용자 인증 처리
UPDATE users
SET email_verified = TRUE
WHERE created_at < NOW();

-- 2. 확인
SELECT
    COUNT(*) as total_users,
    SUM(CASE WHEN email_verified = TRUE THEN 1 ELSE 0 END) as verified_users,
    SUM(CASE WHEN email_verified = FALSE OR email_verified IS NULL THEN 1 ELSE 0 END) as unverified_users
FROM users;
```

실행:
```bash
docker exec -i mariadb mysql -u appuser -papppassword appdb < migration.sql
```

---

## 📊 데이터베이스 스키마

### users 테이블 (최종)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 사용자 ID (PK) |
| username | VARCHAR(50) | NO | - | 사용자명 (고유) |
| name | VARCHAR(100) | NO | - | 실명 |
| email | VARCHAR(255) | NO | - | 이메일 (고유) |
| password | VARCHAR(255) | NO | - | BCrypt 해시 |
| role | VARCHAR(20) | NO | 'ADMIN' | 역할 |
| status | VARCHAR(20) | NO | 'ACTIVE' | 계정 상태 |
| **email_verified** | **BOOLEAN** | **YES** | **FALSE** | **이메일 인증 여부** |
| **email_verification_token** | **VARCHAR(255)** | **YES** | **NULL** | **인증 토큰** |
| **email_verification_token_expiry** | **DATETIME** | **YES** | **NULL** | **토큰 만료 시간** |
| reset_token | VARCHAR(255) | YES | NULL | 비밀번호 리셋 토큰 |
| reset_token_expiry | DATETIME | YES | NULL | 리셋 토큰 만료 시간 |
| suspended_until | DATETIME | YES | NULL | 정지 종료 시간 |
| suspension_reason | TEXT | YES | NULL | 정지 사유 |
| created_at | DATETIME | NO | CURRENT_TIMESTAMP | 생성 시간 |
| updated_at | DATETIME | NO | CURRENT_TIMESTAMP | 수정 시간 |

### 인덱스
```sql
-- 기존 인덱스
CREATE UNIQUE INDEX idx_username ON users(username);
CREATE UNIQUE INDEX idx_email ON users(email);

-- 새로운 인덱스 (성능 최적화)
CREATE INDEX idx_email_verification_token ON users(email_verification_token);
CREATE INDEX idx_email_verified ON users(email_verified);
```

---

## 🛡️ 보안 고려사항

### 1. 토큰 보안
- ✅ UUID v4 사용 (예측 불가능)
- ✅ 24시간 자동 만료
- ✅ 일회용 (사용 후 삭제)
- ✅ HTTPS 권장 (프로덕션)

### 2. 타이밍 공격 방지
```java
// 사용자 존재 여부를 숨김
if (user == null) {
    PasswordUtil.verifyPassword("dummy", "$2a$12$...");
    return new AuthResponse(false, "사용자명 또는 비밀번호가 잘못되었습니다");
}
```

### 3. 레이트 리미팅
**현재 미구현** - 향후 추가 권장:
- 이메일 재발송: 60초에 1회
- 로그인 시도: 5분에 5회
- 인증 시도: 1시간에 10회

### 4. 이메일 스푸핑 방지
Gmail SMTP 사용 시 자동으로 적용됨:
- Gmail의 SPF, DKIM, DMARC 인증 활용
- 발신자 주소는 Gmail 계정으로 고정
- 스팸 필터링 자동 적용

---

## 🐛 알려진 이슈 및 제한사항

### 1. Gmail SMTP 제한사항
- **일일 발송 제한**: 무료 계정 500통/일
- **해결**: 개발 모드에서는 콘솔 출력으로 대체
- **프로덕션**: Gmail 앱 비밀번호 필수
- **대안**: 대량 발송이 필요한 경우 SendGrid, AWS SES 등 고려

### 2. 이메일 전송 실패 처리
- **현재**: 이메일 실패 시 콘솔 에러 로그만 출력
- **개선안**: 실패 이벤트 로깅, 재시도 큐 구현

### 3. 레이트 리미팅 미구현
- **위험**: 무제한 이메일 재발송 가능
- **개선안**: Redis 기반 레이트 리미터 추가

### 4. 이메일 변경 시 재인증 미구현
- **현재**: 이메일 변경 기능 없음
- **개선안**: 이메일 변경 시 재인증 플로우 추가

---

## 📈 향후 개선 사항

### Phase 1: 기본 개선
- [ ] 레이트 리미팅 구현
- [ ] 이메일 전송 실패 재시도 로직
- [ ] 관리자 대시보드에서 인증 상태 확인
- [ ] 이메일 템플릿 다국어 지원

### Phase 2: 고급 기능
- [ ] 2단계 인증 (2FA)
- [ ] 소셜 로그인 (Google, Kakao)
- [ ] 이메일 변경 및 재인증
- [ ] SMS 인증 옵션

### Phase 3: 모니터링
- [ ] 이메일 발송 성공률 추적
- [ ] 인증 완료율 분석
- [ ] 이상 징후 탐지 (봇 방지)

---

## 📚 참고 자료

### 기술 문서
- [JavaMail API 문서](https://javaee.github.io/javamail/)
- [Gmail SMTP 설정](https://support.google.com/mail/answer/7126229)
- [Spring Security 레퍼런스](https://docs.spring.io/spring-security/reference/)
- [React Router 문서](https://reactrouter.com/)

### 디자인 참고
- [Material Design - Authentication](https://material.io/design/communication/confirmation-acknowledgement.html)
- [Best Practices for Email Verification](https://www.nngroup.com/articles/email-verification/)

### 보안 가이드
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Google 2단계 인증 및 앱 비밀번호](https://support.google.com/accounts/answer/185833)

---

## 🤝 기여

이 기능에 대한 개선 제안이나 버그 리포트는 이슈로 등록해주세요.

**구현자**: Claude (Anthropic Sonnet 4.5)
**문서 작성일**: 2026년 2월 11일
**버전**: 1.0.0

---

## ✅ 체크리스트

### 개발 완료
- [x] 데이터베이스 스키마 변경
- [x] 백엔드 API 구현
- [x] 프론트엔드 UI 구현
- [x] Gmail SMTP 통합 (JavaMail API)
- [x] 에러 처리
- [x] 애니메이션 및 UX
- [x] 문서 작성

### 배포 전 확인사항
- [ ] Gmail 앱 비밀번호 생성
- [ ] 환경 변수 설정 (.env 파일)
- [ ] 기존 사용자 마이그레이션
- [ ] 이메일 템플릿 테스트
- [ ] 전체 플로우 테스트
- [ ] 프로덕션 URL 설정
- [ ] HTTPS 적용 확인

---

**© 2026 EJ2 Project. All rights reserved.**
