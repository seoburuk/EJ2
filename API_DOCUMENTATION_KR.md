# 📚 EJ2 시간표 관리 시스템 - 완전 초보자용 API 가이드

이 문서는 EJ2 프로젝트의 모든 API를 **처음 개발을 시작하는 초보자도 이해할 수 있도록** 자세히 설명합니다.

---

## 📑 목차

1. [API 기본 개념](#-api란-무엇인가요)
2. [인증 시스템 (회원가입/로그인)](#-1-인증-시스템-api)
3. [사용자 관리](#-2-사용자-관리-api)
4. [시간표 관리](#-3-시간표-관리-api)
5. [강의 검색](#-4-강의-검색-api)
6. [게시판 시스템](#-5-게시판-crud-api)
7. [댓글 시스템](#-6-댓글-관리-api)
8. [실시간 채팅](#-7-실시간-채팅-api)
9. [관리자 기능](#-8-관리자-기능-api)
10. [0부터 시작하는 개발 가이드](#-9-0부터-시작하는-개발-가이드-초보자-필독)

---

## 🎯 API란 무엇인가요?

**API (Application Programming Interface)**는 프론트엔드(사용자가 보는 화면)와 백엔드(데이터베이스와 비즈니스 로직) 사이의 **약속된 통신 방법**입니다.

### 비유로 이해하기
- **레스토랑**을 생각해보세요
  - **손님(프론트엔드)**: 음식을 주문하는 사람
  - **메뉴판(API 문서)**: 주문할 수 있는 것들의 목록
  - **웨이터(API)**: 주문을 받아서 주방에 전달
  - **주방(백엔드)**: 실제로 음식을 만드는 곳
  - **음식(응답 데이터)**: 손님이 받는 결과

---

## 🌐 기본 설정

### URL 구조 이해하기

```
http://localhost:8080/ej2/api/users/123
│      │         │    │   │   │      │
│      │         │    │   │   │      └─ 리소스 ID (특정 사용자)
│      │         │    │   │   └──────── 리소스 타입 (사용자들)
│      │         │    │   └──────────── API 경로
│      │         │    └──────────────── 컨텍스트 경로
│      │         └───────────────────── 포트 번호
│      └─────────────────────────────── 도메인
└────────────────────────────────────── 프로토콜
```

### 프로젝트 URL 정보
- **백엔드 기본 주소**: `http://localhost:8080/ej2/api`
- **프론트엔드 개발 서버**: `http://localhost:3000`
- **데이터 형식**: JSON (JavaScript Object Notation)

---

## 📖 HTTP 메서드 (동작) 이해하기

API는 다음과 같은 **동작(메서드)**를 사용합니다:

| 메서드 | 의미 | 비유 |
|--------|------|------|
| `GET` | 데이터 조회 | 책을 읽기만 함 |
| `POST` | 새 데이터 생성 | 새 책을 쓰기 |
| `PUT` | 데이터 전체 수정 | 책을 완전히 다시 쓰기 |
| `PATCH` | 데이터 일부 수정 | 책의 일부만 수정 |
| `DELETE` | 데이터 삭제 | 책을 버리기 |

---

## 📊 HTTP 응답 코드 이해하기

서버는 요청 결과를 **숫자 코드**로 알려줍니다:

| 코드 | 의미 | 설명 |
|------|------|------|
| `200` | OK | 성공! 요청한 데이터를 받았습니다 |
| `201` | Created | 성공! 새로운 데이터가 만들어졌습니다 |
| `204` | No Content | 성공! 하지만 돌려줄 데이터는 없습니다 (삭제 시) |
| `400` | Bad Request | 실패! 요청이 잘못되었습니다 (잘못된 데이터 형식) |
| `401` | Unauthorized | 실패! 로그인이 필요합니다 |
| `403` | Forbidden | 실패! 권한이 없습니다 |
| `404` | Not Found | 실패! 요청한 데이터를 찾을 수 없습니다 |
| `409` | Conflict | 실패! 데이터 충돌 (중복 등) |
| `500` | Server Error | 실패! 서버에 문제가 생겼습니다 |

---

## 🔐 1. 인증 시스템 API

사용자가 회원가입하고 로그인하는 기능입니다.

### 1.1 회원가입

**📌 언제 사용하나요?**
- 처음 서비스를 이용하는 사용자가 계정을 만들 때
- "회원가입" 버튼을 클릭했을 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/auth/register
Content-Type: application/json

{
  "username": "아이디",
  "password": "비밀번호",
  "email": "이메일",
  "full_name": "이름"
}
```

**📤 요청 예시**
```javascript
const registerData = {
  username: 'hong_gildong',
  password: 'secure1234!',
  email: 'hong@example.com',
  full_name: '홍길동'
};

axios.post('/ej2/api/auth/register', registerData)
  .then(response => {
    console.log('회원가입 성공!', response.data);
    alert('회원가입이 완료되었습니다. 로그인해주세요.');
    window.location.href = '/login';
  })
  .catch(error => {
    if (error.response?.status === 409) {
      alert('이미 사용 중인 아이디입니다.');
    } else {
      alert('회원가입 실패! 입력 내용을 확인하세요.');
    }
  });
```

**📥 성공 응답**
```json
{
  "id": 1,
  "username": "hong_gildong",
  "email": "hong@example.com",
  "full_name": "홍길동",
  "created_at": "2024-01-15T10:30:00"
}
```

**📋 입력 검증 규칙**
- `username`: 필수, 3-20자, 영문/숫자/언더스코어만, 중복 불가
- `password`: 필수, 8-100자, 영문+숫자 조합 권장
- `email`: 필수, 유효한 이메일 형식, 중복 불가
- `full_name`: 필수, 1-50자

---

### 1.2 로그인

**📌 언제 사용하나요?**
- 사용자가 서비스에 접속할 때
- "로그인" 버튼을 클릭했을 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/auth/login
Content-Type: application/json

{
  "username": "아이디",
  "password": "비밀번호"
}
```

**📤 요청 예시**
```javascript
const loginData = {
  username: 'hong_gildong',
  password: 'secure1234!'
};

axios.post('/ej2/api/auth/login', loginData)
  .then(response => {
    console.log('로그인 성공!', response.data);

    // 토큰 저장 (JWT 방식)
    localStorage.setItem('token', response.data.token);
    localStorage.setItem('userId', response.data.user.id);

    alert(`환영합니다, ${response.data.user.full_name}님!`);
    window.location.href = '/dashboard';
  })
  .catch(error => {
    if (error.response?.status === 401) {
      alert('아이디 또는 비밀번호가 올바르지 않습니다.');
    } else {
      alert('로그인 실패! 다시 시도해주세요.');
    }
  });
```

**📥 성공 응답**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "hong_gildong",
    "email": "hong@example.com",
    "full_name": "홍길동",
    "role": "USER"
  }
}
```

**💡 토큰 사용 방법**
```javascript
// 이후 모든 API 요청에 토큰 포함
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('token')}`;

// 또는 각 요청마다
axios.get('/ej2/api/users/me', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});
```

---

### 1.3 로그아웃

**📌 언제 사용하나요?**
- 사용자가 서비스에서 나갈 때
- "로그아웃" 버튼을 클릭했을 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/auth/logout
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
function logout() {
  axios.post('/ej2/api/auth/logout', {}, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(() => {
    // 로컬 스토리지 정리
    localStorage.removeItem('token');
    localStorage.removeItem('userId');

    alert('로그아웃되었습니다.');
    window.location.href = '/login';
  })
  .catch(error => {
    console.error('로그아웃 실패:', error);
  });
}
```

---

### 1.4 비밀번호 찾기 (이메일 인증)

**📌 언제 사용하나요?**
- 사용자가 비밀번호를 잊어버렸을 때
- "비밀번호 찾기" 링크를 클릭했을 때

**🔧 1단계: 비밀번호 재설정 요청**
```http
POST http://localhost:8080/ej2/api/auth/forgot-password
Content-Type: application/json

{
  "email": "hong@example.com"
}
```

**📤 요청 예시**
```javascript
const email = 'hong@example.com';

axios.post('/ej2/api/auth/forgot-password', { email })
  .then(response => {
    alert('비밀번호 재설정 링크가 이메일로 전송되었습니다.');
  })
  .catch(error => {
    if (error.response?.status === 404) {
      alert('등록되지 않은 이메일입니다.');
    } else {
      alert('요청 실패! 다시 시도해주세요.');
    }
  });
```

**📥 성공 응답**
```json
{
  "message": "비밀번호 재설정 이메일이 전송되었습니다.",
  "email": "hong@example.com"
}
```

**🔧 2단계: 비밀번호 재설정 (토큰 사용)**
```http
POST http://localhost:8080/ej2/api/auth/reset-password
Content-Type: application/json

{
  "token": "이메일에서_받은_토큰",
  "new_password": "새로운_비밀번호"
}
```

**📤 요청 예시**
```javascript
// URL에서 토큰 추출 (예: /reset-password?token=abc123)
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');

const newPassword = 'newSecure1234!';

axios.post('/ej2/api/auth/reset-password', {
  token: token,
  new_password: newPassword
})
.then(response => {
  alert('비밀번호가 변경되었습니다. 다시 로그인해주세요.');
  window.location.href = '/login';
})
.catch(error => {
  if (error.response?.status === 400) {
    alert('유효하지 않거나 만료된 링크입니다.');
  } else {
    alert('비밀번호 변경 실패!');
  }
});
```

---

### 1.5 현재 로그인한 사용자 정보 가져오기

**📌 언제 사용하나요?**
- 페이지 로드 시 로그인 상태를 확인할 때
- 마이페이지를 표시할 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/auth/me
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
axios.get('/ej2/api/auth/me', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  console.log('현재 사용자:', response.data);
  // UI에 사용자 정보 표시
})
.catch(error => {
  if (error.response?.status === 401) {
    // 로그인되지 않음 또는 토큰 만료
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
});
```

**📥 성공 응답**
```json
{
  "id": 1,
  "username": "hong_gildong",
  "email": "hong@example.com",
  "full_name": "홍길동",
  "role": "USER",
  "created_at": "2024-01-15T10:30:00"
}
```

---

## 👤 2. 사용자 관리 API

사용자(User)는 시간표를 만들고 관리하는 **사람**을 나타냅니다.

### 2.1 모든 사용자 목록 가져오기 (관리자용)

**📌 언제 사용하나요?**
- 관리자 페이지에서 모든 사용자를 보여줄 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/users
Authorization: Bearer {admin_token}
```

**📤 요청 예시**
```javascript
axios.get('/ej2/api/users', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  console.log('사용자 목록:', response.data);
})
.catch(error => {
  console.error('에러 발생:', error);
});
```

**📥 응답 예시**
```json
[
  {
    "id": 1,
    "username": "hong_gildong",
    "email": "hong@example.com",
    "full_name": "홍길동",
    "role": "USER",
    "created_at": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "username": "kim_chulsoo",
    "email": "kim@example.com",
    "full_name": "김철수",
    "role": "ADMIN",
    "created_at": "2024-01-16T11:20:00"
  }
]
```

---

### 2.2 특정 사용자 정보 가져오기

**📌 언제 사용하나요?**
- 다른 사용자의 프로필을 볼 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/users/{id}
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
const userId = 1;

axios.get(`/ej2/api/users/${userId}`, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  console.log('사용자 정보:', response.data);
});
```

---

### 2.3 사용자 정보 수정하기

**📌 언제 사용하나요?**
- 프로필 수정 페이지에서 정보를 업데이트할 때

**🔧 요청 방법**
```http
PUT http://localhost:8080/ej2/api/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "새로운_이메일@example.com",
  "full_name": "수정된_이름"
}
```

**📤 요청 예시**
```javascript
const userId = localStorage.getItem('userId');
const updates = {
  email: 'newemail@example.com',
  full_name: '홍길동'
};

axios.put(`/ej2/api/users/${userId}`, updates, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  alert('프로필이 수정되었습니다.');
})
.catch(error => {
  alert('수정 실패!');
});
```

---

### 2.4 비밀번호 변경하기

**📌 언제 사용하나요?**
- 사용자가 비밀번호를 변경하고 싶을 때

**🔧 요청 방법**
```http
PUT http://localhost:8080/ej2/api/users/{id}/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "current_password": "현재_비밀번호",
  "new_password": "새_비밀번호"
}
```

**📤 요청 예시**
```javascript
const userId = localStorage.getItem('userId');
const passwordData = {
  current_password: 'oldPassword123!',
  new_password: 'newPassword456!'
};

axios.put(`/ej2/api/users/${userId}/password`, passwordData, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  alert('비밀번호가 변경되었습니다. 다시 로그인해주세요.');
  // 자동 로그아웃
  logout();
})
.catch(error => {
  if (error.response?.status === 401) {
    alert('현재 비밀번호가 올바르지 않습니다.');
  } else {
    alert('비밀번호 변경 실패!');
  }
});
```

---

## 📅 3. 시간표 관리 API

시간표(Timetable)는 한 학기의 수업 스케줄을 담는 **컨테이너**입니다.

### 3.1 특정 사용자의 모든 시간표 가져오기

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/timetables/user/{userId}
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
const userId = localStorage.getItem('userId');

axios.get(`/ej2/api/timetables/user/${userId}`, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  console.log('내 시간표 목록:', response.data);
});
```

---

### 3.2 새로운 시간표 만들기

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/timetables
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "시간표 이름",
  "semester": "학기 정보"
}
```

**📤 요청 예시**
```javascript
const newTimetable = {
  name: '2024년 1학기',
  semester: '2024-1'
};

axios.post('/ej2/api/timetables', newTimetable, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  alert('시간표가 생성되었습니다!');
})
.catch(error => {
  alert('시간표 생성 실패!');
});
```

---

### 3.3 시간표에 강의 추가하기

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/timetables/{timetableId}/courses
Authorization: Bearer {token}
Content-Type: application/json

{
  "course_code": "CS101",
  "course_name": "프로그래밍 입문",
  "professor": "김교수",
  "credits": 3,
  "color": "#FF6B6B",
  "schedule": [
    {"day": 1, "start_time": 9, "end_time": 11}
  ]
}
```

---

## 🔍 4. 강의 검색 API

### 4.1 강의 검색하기

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/courses/search?keyword=검색어
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
axios.get('/ej2/api/courses/search', {
  params: { keyword: '프로그래밍' },
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  console.log('검색 결과:', response.data);
});
```

---

## 📝 5. 게시판 CRUD API

게시판은 사용자들이 글을 작성하고 공유하는 공간입니다.

### 5.1 게시글 목록 가져오기 (페이지네이션)

**📌 언제 사용하나요?**
- 게시판 메인 페이지를 표시할 때
- 무한 스크롤이나 페이지 번호를 구현할 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/posts?page=1&size=20&sort=created_at&order=desc
```

**💬 쿼리 파라미터**
- `page`: 페이지 번호 (기본값: 1)
- `size`: 한 페이지당 게시글 수 (기본값: 20)
- `sort`: 정렬 기준 (created_at, views, likes)
- `order`: 정렬 순서 (desc, asc)
- `category`: 카테고리 필터 (선택)
- `keyword`: 검색어 (선택)

**📤 요청 예시**
```javascript
function loadPosts(page = 1) {
  axios.get('/ej2/api/posts', {
    params: {
      page: page,
      size: 20,
      sort: 'created_at',
      order: 'desc'
    }
  })
  .then(response => {
    console.log('게시글 목록:', response.data);
    displayPosts(response.data.posts);
    displayPagination(response.data.pagination);
  });
}
```

**📥 응답 예시**
```json
{
  "posts": [
    {
      "id": 123,
      "title": "시간표 공유합니다",
      "content": "2024년 1학기 컴공 시간표 추천...",
      "author": {
        "id": 1,
        "username": "hong_gildong",
        "full_name": "홍길동"
      },
      "category": "시간표공유",
      "views": 150,
      "likes": 25,
      "comment_count": 8,
      "created_at": "2024-01-15T10:30:00",
      "updated_at": "2024-01-15T10:30:00"
    }
  ],
  "pagination": {
    "current_page": 1,
    "total_pages": 10,
    "total_posts": 200,
    "has_next": true,
    "has_prev": false
  }
}
```

---

### 5.2 게시글 상세 보기

**📌 언제 사용하나요?**
- 사용자가 게시글을 클릭했을 때
- 게시글 전체 내용을 표시할 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/posts/{postId}
```

**📤 요청 예시**
```javascript
const postId = 123;

axios.get(`/ej2/api/posts/${postId}`)
  .then(response => {
    console.log('게시글 상세:', response.data);
    displayPostDetail(response.data);
  })
  .catch(error => {
    if (error.response?.status === 404) {
      alert('삭제되었거나 존재하지 않는 게시글입니다.');
    }
  });
```

**📥 응답 예시**
```json
{
  "id": 123,
  "title": "시간표 공유합니다",
  "content": "2024년 1학기 컴공 시간표입니다.\n\n월: CS101 9시-11시\n화: ...",
  "author": {
    "id": 1,
    "username": "hong_gildong",
    "full_name": "홍길동"
  },
  "category": "시간표공유",
  "views": 151,
  "likes": 25,
  "is_liked": false,
  "comment_count": 8,
  "attachments": [
    {
      "id": 1,
      "filename": "timetable.png",
      "url": "/uploads/timetable.png",
      "size": 245678
    }
  ],
  "created_at": "2024-01-15T10:30:00",
  "updated_at": "2024-01-15T10:30:00"
}
```

---

### 5.3 게시글 작성하기

**📌 언제 사용하나요?**
- "글쓰기" 버튼을 클릭했을 때
- 새 게시글을 작성할 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/posts
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "제목",
  "content": "내용",
  "category": "카테고리"
}
```

**📤 요청 예시**
```javascript
function createPost() {
  const postData = {
    title: document.getElementById('title').value,
    content: document.getElementById('content').value,
    category: document.getElementById('category').value
  };

  axios.post('/ej2/api/posts', postData, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    alert('게시글이 작성되었습니다!');
    window.location.href = `/posts/${response.data.id}`;
  })
  .catch(error => {
    if (error.response?.status === 401) {
      alert('로그인이 필요합니다.');
      window.location.href = '/login';
    } else {
      alert('게시글 작성 실패!');
    }
  });
}
```

**📥 성공 응답**
```json
{
  "id": 124,
  "title": "시간표 공유합니다",
  "content": "...",
  "author": {
    "id": 1,
    "username": "hong_gildong",
    "full_name": "홍길동"
  },
  "category": "시간표공유",
  "views": 0,
  "likes": 0,
  "comment_count": 0,
  "created_at": "2024-01-16T14:20:00"
}
```

---

### 5.4 게시글 수정하기

**📌 언제 사용하나요?**
- 자신이 작성한 게시글을 수정할 때
- "수정" 버튼을 클릭했을 때

**🔧 요청 방법**
```http
PUT http://localhost:8080/ej2/api/posts/{postId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "category": "카테고리"
}
```

**📤 요청 예시**
```javascript
const postId = 124;
const updatedData = {
  title: '수정된 시간표 공유',
  content: '내용 수정...',
  category: '시간표공유'
};

axios.put(`/ej2/api/posts/${postId}`, updatedData, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  alert('게시글이 수정되었습니다.');
  window.location.reload();
})
.catch(error => {
  if (error.response?.status === 403) {
    alert('자신의 게시글만 수정할 수 있습니다.');
  } else {
    alert('수정 실패!');
  }
});
```

---

### 5.5 게시글 삭제하기

**📌 언제 사용하나요?**
- 자신이 작성한 게시글을 삭제할 때
- "삭제" 버튼을 클릭했을 때

**🔧 요청 방법**
```http
DELETE http://localhost:8080/ej2/api/posts/{postId}
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
function deletePost(postId) {
  if (!confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
    return;
  }

  axios.delete(`/ej2/api/posts/${postId}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(() => {
    alert('게시글이 삭제되었습니다.');
    window.location.href = '/posts';
  })
  .catch(error => {
    if (error.response?.status === 403) {
      alert('자신의 게시글만 삭제할 수 있습니다.');
    } else {
      alert('삭제 실패!');
    }
  });
}
```

---

### 5.6 게시글 좋아요/좋아요 취소

**📌 언제 사용하나요?**
- "좋아요" 버튼을 클릭했을 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/posts/{postId}/like
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
function toggleLike(postId) {
  axios.post(`/ej2/api/posts/${postId}/like`, {}, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    // UI 업데이트
    updateLikeButton(response.data.is_liked, response.data.likes);
  })
  .catch(error => {
    if (error.response?.status === 401) {
      alert('로그인이 필요합니다.');
    }
  });
}
```

**📥 응답 예시**
```json
{
  "is_liked": true,
  "likes": 26
}
```

---

## 💬 6. 댓글 관리 API

댓글은 게시글에 달리는 짧은 의견이나 답변입니다.

### 6.1 댓글 목록 가져오기

**📌 언제 사용하나요?**
- 게시글 상세 페이지에서 댓글을 표시할 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/posts/{postId}/comments
```

**📤 요청 예시**
```javascript
const postId = 123;

axios.get(`/ej2/api/posts/${postId}/comments`)
  .then(response => {
    console.log('댓글 목록:', response.data);
    displayComments(response.data);
  });
```

**📥 응답 예시**
```json
[
  {
    "id": 1,
    "content": "좋은 시간표네요! 참고하겠습니다.",
    "author": {
      "id": 2,
      "username": "kim_chulsoo",
      "full_name": "김철수"
    },
    "parent_id": null,
    "replies": [
      {
        "id": 2,
        "content": "감사합니다!",
        "author": {
          "id": 1,
          "username": "hong_gildong",
          "full_name": "홍길동"
        },
        "parent_id": 1,
        "created_at": "2024-01-15T11:00:00"
      }
    ],
    "created_at": "2024-01-15T10:45:00",
    "updated_at": "2024-01-15T10:45:00"
  }
]
```

---

### 6.2 댓글 작성하기

**📌 언제 사용하나요?**
- 게시글에 댓글을 달 때
- 댓글에 답글을 달 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/posts/{postId}/comments
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "댓글 내용",
  "parent_id": null
}
```

**💬 요청 데이터 설명**
- `content`: 댓글 내용 (필수)
- `parent_id`: 답글인 경우 부모 댓글 ID, 일반 댓글은 null

**📤 요청 예시**
```javascript
// 일반 댓글 작성
function postComment(postId, content) {
  axios.post(`/ej2/api/posts/${postId}/comments`, {
    content: content,
    parent_id: null
  }, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    alert('댓글이 작성되었습니다!');
    loadComments(postId);
  })
  .catch(error => {
    alert('댓글 작성 실패!');
  });
}

// 답글 작성
function postReply(postId, parentCommentId, content) {
  axios.post(`/ej2/api/posts/${postId}/comments`, {
    content: content,
    parent_id: parentCommentId
  }, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    alert('답글이 작성되었습니다!');
    loadComments(postId);
  });
}
```

**📥 성공 응답**
```json
{
  "id": 3,
  "content": "좋은 정보 감사합니다!",
  "author": {
    "id": 3,
    "username": "lee_younghee",
    "full_name": "이영희"
  },
  "parent_id": null,
  "created_at": "2024-01-16T09:15:00"
}
```

---

### 6.3 댓글 수정하기

**📌 언제 사용하나요?**
- 자신이 작성한 댓글을 수정할 때

**🔧 요청 방법**
```http
PUT http://localhost:8080/ej2/api/comments/{commentId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "수정된 댓글 내용"
}
```

**📤 요청 예시**
```javascript
const commentId = 3;
const updatedContent = '수정된 내용입니다.';

axios.put(`/ej2/api/comments/${commentId}`, {
  content: updatedContent
}, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  alert('댓글이 수정되었습니다.');
  loadComments(postId);
})
.catch(error => {
  if (error.response?.status === 403) {
    alert('자신의 댓글만 수정할 수 있습니다.');
  }
});
```

---

### 6.4 댓글 삭제하기

**📌 언제 사용하나요?**
- 자신이 작성한 댓글을 삭제할 때

**🔧 요청 방법**
```http
DELETE http://localhost:8080/ej2/api/comments/{commentId}
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
function deleteComment(commentId) {
  if (!confirm('정말로 이 댓글을 삭제하시겠습니까?')) {
    return;
  }

  axios.delete(`/ej2/api/comments/${commentId}`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(() => {
    alert('댓글이 삭제되었습니다.');
    loadComments(postId);
  })
  .catch(error => {
    alert('삭제 실패!');
  });
}
```

---

## 💬 7. 실시간 채팅 API

WebSocket을 사용한 실시간 채팅 기능입니다.

### 7.1 채팅방 목록 가져오기

**📌 언제 사용하나요?**
- 채팅 페이지에 진입했을 때
- 참여 중인 채팅방 목록을 표시할 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/chat/rooms
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
axios.get('/ej2/api/chat/rooms', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  console.log('채팅방 목록:', response.data);
  displayChatRooms(response.data);
});
```

**📥 응답 예시**
```json
[
  {
    "id": 1,
    "name": "컴공 24학번 단톡방",
    "type": "GROUP",
    "member_count": 25,
    "unread_count": 3,
    "last_message": {
      "content": "내일 시험 있나요?",
      "sender": "김철수",
      "sent_at": "2024-01-16T14:30:00"
    },
    "created_at": "2024-01-10T09:00:00"
  },
  {
    "id": 2,
    "name": "홍길동",
    "type": "DIRECT",
    "member_count": 2,
    "unread_count": 0,
    "last_message": {
      "content": "시간표 공유해줘서 고마워!",
      "sender": "홍길동",
      "sent_at": "2024-01-15T18:20:00"
    },
    "created_at": "2024-01-05T14:00:00"
  }
]
```

---

### 7.2 채팅방 생성하기

**📌 언제 사용하나요?**
- 새로운 채팅방을 만들 때
- 1:1 채팅을 시작할 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/chat/rooms
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "채팅방 이름",
  "type": "GROUP",
  "member_ids": [2, 3, 4]
}
```

**💬 요청 데이터 설명**
- `name`: 채팅방 이름 (GROUP일 때 필수)
- `type`: "GROUP" (그룹 채팅) 또는 "DIRECT" (1:1 채팅)
- `member_ids`: 초대할 사용자 ID 배열

**📤 요청 예시**
```javascript
// 그룹 채팅방 만들기
function createGroupChat() {
  const chatRoomData = {
    name: '컴공 24학번 단톡방',
    type: 'GROUP',
    member_ids: [2, 3, 4, 5]
  };

  axios.post('/ej2/api/chat/rooms', chatRoomData, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    alert('채팅방이 생성되었습니다!');
    window.location.href = `/chat/${response.data.id}`;
  });
}

// 1:1 채팅 시작하기
function startDirectChat(userId) {
  axios.post('/ej2/api/chat/rooms', {
    type: 'DIRECT',
    member_ids: [userId]
  }, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    window.location.href = `/chat/${response.data.id}`;
  });
}
```

---

### 7.3 WebSocket 연결 및 메시지 송수신

**📌 언제 사용하나요?**
- 채팅방에 입장했을 때
- 실시간으로 메시지를 주고받을 때

**🔧 WebSocket 연결**
```javascript
// WebSocket 연결 설정
const token = localStorage.getItem('token');
const roomId = 1;
const socket = new WebSocket(`ws://localhost:8080/ej2/ws/chat/${roomId}?token=${token}`);

// 연결 성공
socket.onopen = function(event) {
  console.log('채팅방에 연결되었습니다.');
};

// 메시지 수신
socket.onmessage = function(event) {
  const message = JSON.parse(event.data);
  console.log('새 메시지:', message);
  displayMessage(message);
};

// 연결 종료
socket.onclose = function(event) {
  console.log('채팅방 연결이 종료되었습니다.');
};

// 에러 발생
socket.onerror = function(error) {
  console.error('WebSocket 에러:', error);
};
```

**📤 메시지 전송**
```javascript
function sendMessage(content) {
  const message = {
    type: 'CHAT',
    content: content,
    room_id: roomId
  };

  socket.send(JSON.stringify(message));
}

// 사용 예시
document.getElementById('sendBtn').addEventListener('click', function() {
  const messageInput = document.getElementById('messageInput');
  const content = messageInput.value;

  if (content.trim()) {
    sendMessage(content);
    messageInput.value = '';
  }
});
```

**📥 수신 메시지 형식**
```json
{
  "id": 1234,
  "type": "CHAT",
  "content": "안녕하세요!",
  "sender": {
    "id": 2,
    "username": "kim_chulsoo",
    "full_name": "김철수"
  },
  "room_id": 1,
  "sent_at": "2024-01-16T15:30:00",
  "is_read": false
}
```

---

### 7.4 채팅 메시지 히스토리 가져오기

**📌 언제 사용하나요?**
- 채팅방에 처음 들어갔을 때
- 이전 메시지를 불러올 때 (스크롤 업)

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/chat/rooms/{roomId}/messages?before={messageId}&limit=50
Authorization: Bearer {token}
```

**💬 쿼리 파라미터**
- `before`: 이 메시지 ID 이전의 메시지들을 가져옴 (선택)
- `limit`: 가져올 메시지 수 (기본값: 50)

**📤 요청 예시**
```javascript
function loadMessages(roomId, beforeMessageId = null) {
  const params = { limit: 50 };
  if (beforeMessageId) {
    params.before = beforeMessageId;
  }

  axios.get(`/ej2/api/chat/rooms/${roomId}/messages`, {
    params: params,
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    console.log('메시지 히스토리:', response.data);
    displayMessages(response.data);
  });
}
```

**📥 응답 예시**
```json
[
  {
    "id": 1234,
    "content": "안녕하세요!",
    "sender": {
      "id": 2,
      "username": "kim_chulsoo",
      "full_name": "김철수"
    },
    "sent_at": "2024-01-16T15:30:00",
    "is_read": true
  },
  {
    "id": 1235,
    "content": "네 안녕하세요~",
    "sender": {
      "id": 1,
      "username": "hong_gildong",
      "full_name": "홍길동"
    },
    "sent_at": "2024-01-16T15:31:00",
    "is_read": true
  }
]
```

---

### 7.5 메시지 읽음 처리

**📌 언제 사용하나요?**
- 채팅방에서 메시지를 읽었을 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/chat/rooms/{roomId}/read
Authorization: Bearer {token}
```

**📤 요청 예시**
```javascript
function markAsRead(roomId) {
  axios.post(`/ej2/api/chat/rooms/${roomId}/read`, {}, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    console.log('읽음 처리 완료');
  });
}

// 채팅방 입장 시 자동 실행
window.addEventListener('focus', function() {
  markAsRead(currentRoomId);
});
```

---

## 👮 8. 관리자 기능 API

관리자가 신고를 처리하고 사용자를 관리하는 기능입니다.

### 8.1 신고하기

**📌 언제 사용하나요?**
- 부적절한 게시글이나 댓글을 발견했을 때
- "신고" 버튼을 클릭했을 때

**🔧 요청 방법**
```http
POST http://localhost:8080/ej2/api/reports
Authorization: Bearer {token}
Content-Type: application/json

{
  "target_type": "POST",
  "target_id": 123,
  "reason": "SPAM",
  "description": "상세 설명"
}
```

**💬 요청 데이터 설명**
- `target_type`: "POST" (게시글), "COMMENT" (댓글), "USER" (사용자)
- `target_id`: 신고 대상의 ID
- `reason`: "SPAM" (스팸), "ABUSE" (욕설), "INAPPROPRIATE" (부적절한 내용), "ETC" (기타)
- `description`: 신고 사유 상세 설명

**📤 요청 예시**
```javascript
function reportPost(postId) {
  const reason = prompt('신고 사유를 선택하세요:\n1. 스팸\n2. 욕설/비방\n3. 부적절한 내용\n4. 기타');
  const description = prompt('상세 내용을 입력해주세요:');

  const reasonMap = {
    '1': 'SPAM',
    '2': 'ABUSE',
    '3': 'INAPPROPRIATE',
    '4': 'ETC'
  };

  axios.post('/ej2/api/reports', {
    target_type: 'POST',
    target_id: postId,
    reason: reasonMap[reason],
    description: description
  }, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    alert('신고가 접수되었습니다. 빠른 시일 내에 처리하겠습니다.');
  })
  .catch(error => {
    alert('신고 접수 실패!');
  });
}
```

**📥 성공 응답**
```json
{
  "id": 45,
  "target_type": "POST",
  "target_id": 123,
  "reason": "SPAM",
  "description": "광고성 게시글입니다.",
  "status": "PENDING",
  "reporter": {
    "id": 5,
    "username": "user123"
  },
  "created_at": "2024-01-16T16:00:00"
}
```

---

### 8.2 신고 목록 조회 (관리자 전용)

**📌 언제 사용하나요?**
- 관리자가 신고 내역을 확인할 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/admin/reports?status=PENDING&page=1
Authorization: Bearer {admin_token}
```

**💬 쿼리 파라미터**
- `status`: "PENDING" (대기), "APPROVED" (승인), "REJECTED" (거부)
- `page`: 페이지 번호
- `size`: 한 페이지당 개수

**📤 요청 예시**
```javascript
function loadReports(status = 'PENDING', page = 1) {
  axios.get('/ej2/api/admin/reports', {
    params: { status, page, size: 20 },
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    console.log('신고 목록:', response.data);
    displayReports(response.data.reports);
  })
  .catch(error => {
    if (error.response?.status === 403) {
      alert('관리자 권한이 필요합니다.');
    }
  });
}
```

**📥 응답 예시**
```json
{
  "reports": [
    {
      "id": 45,
      "target_type": "POST",
      "target_id": 123,
      "target_preview": {
        "title": "시간표 공유합니다",
        "author": "hong_gildong"
      },
      "reason": "SPAM",
      "description": "광고성 게시글입니다.",
      "status": "PENDING",
      "reporter": {
        "id": 5,
        "username": "user123",
        "full_name": "신고자"
      },
      "created_at": "2024-01-16T16:00:00"
    }
  ],
  "pagination": {
    "current_page": 1,
    "total_pages": 3,
    "total_reports": 45
  }
}
```

---

### 8.3 신고 처리하기 (관리자 전용)

**📌 언제 사용하나요?**
- 관리자가 신고를 검토하고 조치를 취할 때

**🔧 요청 방법**
```http
PUT http://localhost:8080/ej2/api/admin/reports/{reportId}
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "status": "APPROVED",
  "action": "DELETE_CONTENT",
  "admin_note": "관리자 메모"
}
```

**💬 요청 데이터 설명**
- `status`: "APPROVED" (승인), "REJECTED" (거부)
- `action`: "DELETE_CONTENT" (콘텐츠 삭제), "WARN_USER" (경고), "BAN_USER" (계정 정지), "NONE" (조치 없음)
- `admin_note`: 관리자 메모 (선택)

**📤 요청 예시**
```javascript
function processReport(reportId, approve) {
  const action = approve
    ? prompt('조치를 선택하세요:\n1. 콘텐츠 삭제\n2. 사용자 경고\n3. 계정 정지')
    : 'NONE';

  const actionMap = {
    '1': 'DELETE_CONTENT',
    '2': 'WARN_USER',
    '3': 'BAN_USER'
  };

  axios.put(`/ej2/api/admin/reports/${reportId}`, {
    status: approve ? 'APPROVED' : 'REJECTED',
    action: approve ? actionMap[action] : 'NONE',
    admin_note: prompt('관리자 메모 (선택):') || ''
  }, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    alert('신고가 처리되었습니다.');
    loadReports();
  })
  .catch(error => {
    alert('처리 실패!');
  });
}
```

---

### 8.4 사용자 계정 정지/해제 (관리자 전용)

**📌 언제 사용하나요?**
- 관리자가 문제가 있는 사용자를 제재할 때

**🔧 요청 방법**
```http
PUT http://localhost:8080/ej2/api/admin/users/{userId}/ban
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "is_banned": true,
  "ban_reason": "스팸 게시글 반복 작성",
  "ban_until": "2024-02-16T00:00:00"
}
```

**📤 요청 예시**
```javascript
function banUser(userId) {
  const reason = prompt('정지 사유를 입력하세요:');
  const days = prompt('정지 기간(일)을 입력하세요:');

  const banUntil = new Date();
  banUntil.setDate(banUntil.getDate() + parseInt(days));

  axios.put(`/ej2/api/admin/users/${userId}/ban`, {
    is_banned: true,
    ban_reason: reason,
    ban_until: banUntil.toISOString()
  }, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    alert('사용자 계정이 정지되었습니다.');
  });
}

function unbanUser(userId) {
  axios.put(`/ej2/api/admin/users/${userId}/ban`, {
    is_banned: false
  }, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  .then(response => {
    alert('계정 정지가 해제되었습니다.');
  });
}
```

---

### 8.5 통계 대시보드 (관리자 전용)

**📌 언제 사용하나요?**
- 관리자가 서비스 현황을 파악할 때

**🔧 요청 방법**
```http
GET http://localhost:8080/ej2/api/admin/statistics
Authorization: Bearer {admin_token}
```

**📤 요청 예시**
```javascript
axios.get('/ej2/api/admin/statistics', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => {
  console.log('통계 데이터:', response.data);
  displayStatistics(response.data);
});
```

**📥 응답 예시**
```json
{
  "users": {
    "total": 1250,
    "new_today": 15,
    "new_this_week": 87,
    "active_today": 342
  },
  "posts": {
    "total": 3456,
    "new_today": 23,
    "new_this_week": 156
  },
  "reports": {
    "pending": 12,
    "total_this_week": 34
  },
  "chat": {
    "active_rooms": 45,
    "messages_today": 1234
  }
}
```

---

## 🚀 9. 0부터 시작하는 개발 가이드 (초보자 필독)

이 섹션은 **처음 개발을 시작하는 초보자**를 위한 단계별 가이드입니다.

---

## 📋 개발 순서 전체 로드맵

### 🎯 Phase 0: 개발 환경 준비 (1주차)

**해야 할 일**
1. ✅ 개발 도구 설치
   - Java 8 JDK 설치
   - IntelliJ IDEA 또는 Eclipse 설치
   - Node.js 16+ 설치
   - Visual Studio Code 설치
   - Docker Desktop 설치

2. ✅ 프로젝트 초기 설정
   - Git 저장소 생성
   - 백엔드 프로젝트 생성 (Spring Framework)
   - 프론트엔드 프로젝트 생성 (React)
   - 데이터베이스 설정 (MariaDB)

3. ✅ 기본 구조 만들기
   ```
   EJ2/
   ├── backend/
   │   ├── src/main/java/com/ej2/
   │   │   ├── config/
   │   │   ├── controller/
   │   │   ├── model/
   │   │   ├── repository/
   │   │   └── service/
   │   └── pom.xml
   ├── frontend/
   │   ├── src/
   │   │   ├── components/
   │   │   ├── pages/
   │   │   └── services/
   │   └── package.json
   └── docker-compose.yml
   ```

**학습 자료**
- Java 기초 문법
- Spring Framework 기본 개념
- React 기초
- REST API 개념
- Git 사용법

---

### 🎯 Phase 1: 인증 시스템 구현 (2-3주차)

**왜 인증부터?**
- 대부분의 기능이 로그인 후 사용 가능
- 보안의 기초가 되는 부분
- 다른 기능 개발 시 테스트용 계정 필요

**백엔드 구현 순서**

#### 1단계: User 엔티티 만들기
```java
// backend/src/main/java/com/ej2/model/User.java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;  // 암호화된 비밀번호

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;  // USER, ADMIN

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // getter, setter
}
```

#### 2단계: UserRepository 만들기
```java
// backend/src/main/java/com/ej2/repository/UserRepository.java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

#### 3단계: 비밀번호 암호화 설정
```java
// backend/src/main/java/com/ej2/config/SecurityConfig.java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### 4단계: JWT 토큰 유틸리티 만들기
```java
// backend/src/main/java/com/ej2/util/JwtUtil.java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 5단계: AuthService 만들기
```java
// backend/src/main/java/com/ej2/service/AuthService.java
@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public User register(String username, String password, String email, String fullName) {
        // 중복 체크
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        // 새 사용자 생성
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return jwtUtil.generateToken(username);
    }
}
```

#### 6단계: AuthController 만들기
```java
// backend/src/main/java/com/ej2/controller/AuthController.java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getFullName()
            );
            return ResponseEntity.status(201).body(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getUsername(), request.getPassword());
            User user = authService.getUserByUsername(request.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
```

**프론트엔드 구현 순서**

#### 1단계: API 서비스 만들기
```javascript
// frontend/src/services/authService.js
import axios from 'axios';

const API_URL = 'http://localhost:8080/ej2/api';

export const authService = {
  register: async (username, password, email, fullName) => {
    const response = await axios.post(`${API_URL}/auth/register`, {
      username,
      password,
      email,
      full_name: fullName
    });
    return response.data;
  },

  login: async (username, password) => {
    const response = await axios.post(`${API_URL}/auth/login`, {
      username,
      password
    });

    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('userId', response.data.user.id);
    }

    return response.data;
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
  },

  getCurrentUser: () => {
    const token = localStorage.getItem('token');
    if (!token) return null;

    // JWT 토큰 파싱 (간단한 방법)
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));

      return JSON.parse(jsonPayload);
    } catch (e) {
      return null;
    }
  },

  isAuthenticated: () => {
    return !!localStorage.getItem('token');
  }
};

// Axios 인터셉터 설정 (모든 요청에 토큰 자동 추가)
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);
```

#### 2단계: 회원가입 페이지 만들기
```javascript
// frontend/src/pages/RegisterPage.js
import React, { useState } from 'react';
import { authService } from '../services/authService';
import { useNavigate } from 'react-router-dom';

function RegisterPage() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    fullName: ''
  });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // 유효성 검사
    if (formData.password !== formData.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    if (formData.password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }

    try {
      await authService.register(
        formData.username,
        formData.password,
        formData.email,
        formData.fullName
      );
      alert('회원가입이 완료되었습니다!');
      navigate('/login');
    } catch (error) {
      setError(error.response?.data || '회원가입 실패!');
    }
  };

  return (
    <div className="register-page">
      <h1>회원가입</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label>아이디:</label>
          <input
            type="text"
            name="username"
            value={formData.username}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>비밀번호:</label>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>비밀번호 확인:</label>
          <input
            type="password"
            name="confirmPassword"
            value={formData.confirmPassword}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>이메일:</label>
          <input
            type="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>이름:</label>
          <input
            type="text"
            name="fullName"
            value={formData.fullName}
            onChange={handleChange}
            required
          />
        </div>

        {error && <div className="error">{error}</div>}

        <button type="submit">회원가입</button>
      </form>
    </div>
  );
}

export default RegisterPage;
```

#### 3단계: 로그인 페이지 만들기
```javascript
// frontend/src/pages/LoginPage.js
import React, { useState } from 'react';
import { authService } from '../services/authService';
import { useNavigate } from 'react-router-dom';

function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const response = await authService.login(username, password);
      alert(`환영합니다, ${response.user.full_name}님!`);
      navigate('/dashboard');
    } catch (error) {
      setError('아이디 또는 비밀번호가 올바르지 않습니다.');
    }
  };

  return (
    <div className="login-page">
      <h1>로그인</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label>아이디:</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div>
          <label>비밀번호:</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        {error && <div className="error">{error}</div>}

        <button type="submit">로그인</button>
      </form>

      <div>
        <a href="/register">회원가입</a>
        <a href="/forgot-password">비밀번호 찾기</a>
      </div>
    </div>
  );
}

export default LoginPage;
```

**테스트 방법**
1. 백엔드 서버 실행 확인
2. 프론트엔드 개발 서버 실행
3. 회원가입 페이지에서 계정 생성
4. 로그인 테스트
5. 브라우저 개발자 도구에서 토큰 확인

---

### 🎯 Phase 2: 시간표 관리 (4-5주차)

이전 섹션의 시간표 API를 참고하여 구현합니다.

**구현 순서**
1. Timetable 엔티티 생성
2. TimetableCourse 엔티티 생성
3. Repository, Service, Controller 생성
4. 프론트엔드 UI 구현
5. 시간표 시각화 구현

**핵심 포인트**
- 로그인한 사용자만 자신의 시간표 접근 가능
- 강의 시간 충돌 검사 로직 구현
- 시간표 공유 기능 (선택)

---

### 🎯 Phase 3: 게시판 시스템 (6-7주차)

**구현 순서**
1. Post 엔티티 생성
2. 게시글 CRUD API 구현
3. 페이지네이션 구현
4. 게시글 목록 페이지 구현
5. 게시글 상세 페이지 구현
6. 게시글 작성/수정 페이지 구현

**핵심 포인트**
- 작성자만 수정/삭제 가능
- 조회수 중복 방지 (쿠키 또는 세션)
- 좋아요 기능 구현

---

### 🎯 Phase 4: 댓글 시스템 (8주차)

**구현 순서**
1. Comment 엔티티 생성 (대댓글 지원)
2. 댓글 CRUD API 구현
3. 댓글 UI 컴포넌트 구현
4. 답글 기능 구현

**핵심 포인트**
- 계층형 댓글 구조 (parent_id 사용)
- 작성자만 수정/삭제 가능
- 게시글 삭제 시 댓글도 함께 삭제

---

### 🎯 Phase 5: 실시간 채팅 (9-10주차)

**구현 순서**
1. WebSocket 설정 (백엔드)
2. ChatRoom, ChatMessage 엔티티 생성
3. WebSocket 핸들러 구현
4. 채팅방 관리 API 구현
5. WebSocket 클라이언트 구현 (프론트엔드)
6. 채팅 UI 구현

**핵심 포인트**
- WebSocket 연결 관리
- 읽지 않은 메시지 카운트
- 메시지 히스토리 로딩

---

### 🎯 Phase 6: 관리자 기능 (11주차)

**구현 순서**
1. Report 엔티티 생성
2. 신고 API 구현
3. 관리자 페이지 UI 구현
4. 신고 처리 로직 구현
5. 통계 대시보드 구현

**핵심 포인트**
- 권한 체크 (ADMIN만 접근)
- 신고 상태 관리
- 사용자 제재 시스템

---

## 💡 초보자를 위한 팁

### 1. 에러 해결 방법
```javascript
// 에러가 발생하면:
// 1. 브라우저 콘솔 확인 (F12)
// 2. 네트워크 탭에서 API 요청/응답 확인
// 3. 백엔드 로그 확인
// 4. 에러 메시지를 구글에 검색

console.log('디버깅:', 변수명);  // 자주 사용하기
```

### 2. Git 사용 팁
```bash
# 작업 전 항상 최신 코드 받기
git pull

# 자주 커밋하기
git add .
git commit -m "기능명: 무엇을 했는지 간단히"
git push

# 새 기능은 브랜치로
git checkout -b feature/login
```

### 3. 코드 작성 팁
- 한 번에 하나의 기능만 구현
- 작은 단위로 테스트
- 주석 달기 (나중에 내가 읽을 코드)
- 변수명은 의미 있게

---

★ Insight ─────────────────────────────────────
**계층적 아키텍처의 중요성**: 백엔드를 Entity → Repository → Service → Controller 순서로 구현하는 이유는 각 계층이 하위 계층에 의존하기 때문입니다. 이는 "의존성 방향"을 일관되게 유지하여 코드의 안정성과 테스트 용이성을 높입니다.

**인증 우선 개발 전략**: 대부분의 웹 서비스가 인증부터 구현하는 이유는, 이후 모든 기능이 "누가 요청했는지"를 알아야 하기 때문입니다. 인증 시스템 없이 다른 기능을 만들면 나중에 전체를 다시 수정해야 하는 상황이 발생합니다.

**점진적 복잡도 증가**: 시간표 → 게시판 → 댓글 → 채팅 순서로 개발하는 것은 단순한 CRUD부터 실시간 통신까지 점진적으로 난이도를 높이는 전략입니다. 각 단계에서 배운 패턴을 다음 단계에 적용할 수 있습니다.
─────────────────────────────────────────────────

## 📚 추가 학습 자료

- **Java & Spring**: 백기선의 스프링 입문, 인프런 강의
- **React**: 리액트 공식 문서, Nomad Coders
- **REST API**: RESTful API 설계 가이드
- **데이터베이스**: SQL 기초 문법
- **Git**: Pro Git 책 (무료)

---

**이 문서로 처음부터 끝까지 완전한 시간표 관리 시스템을 만들 수 있습니다! 🎉**

궁금한 점이 있으면 언제든 질문하세요!
