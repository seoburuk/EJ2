# AWS S3 이미지 업로드 설정 가이드

## 개요
이 문서는 EJ2 게시판의 AWS S3 이미지 업로드 기능을 설정하고 사용하는 방법을 설명합니다.

## 주요 기능
- ✅ 게시글당 최대 5개의 이미지 업로드 (각 5MB 제한)
- ✅ AWS S3 클라우드 스토리지에 이미지 저장
- ✅ 체계적인 폴더 구조: `posts/{postId}/{uuid}.ext`
- ✅ 자동 연쇄 삭제 (게시글 삭제 시 이미지도 함께 삭제)
- ✅ 게시글 상세 페이지에서 이미지 표시
- ✅ 지원 형식: JPG, JPEG, PNG, GIF, WEBP

## 아키텍처

### 2단계 업로드 전략
1. **1단계**: 게시글 생성 → 게시글 ID 획득 → 데이터베이스 커밋
2. **2단계**: 이미지를 S3에 업로드 → 이미지 메타데이터를 데이터베이스에 저장

**장점**:
- S3 업로드는 외부 I/O (데이터베이스 트랜잭션 내부에 있지 않음)
- S3 업로드 실패 시에도 게시글은 유지됨 (사용자가 이미지 업로드 재시도 가능)
- 트랜잭션 타임아웃 방지

### 데이터베이스 스키마

**테이블: `post_images`**
```sql
CREATE TABLE post_images (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  s3_key VARCHAR(500) NOT NULL,
  s3_url VARCHAR(1000) NOT NULL,
  original_filename VARCHAR(255),
  file_size BIGINT,
  content_type VARCHAR(50),
  display_order INT NOT NULL,
  uploaded_at DATETIME NOT NULL,
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);
```

**Hibernate**가 애플리케이션 시작 시 자동으로 이 테이블을 생성합니다.

## AWS S3 설정

### 1단계: S3 버킷 생성

1. AWS 콘솔 → S3 → **버킷 만들기**
2. **버킷 이름**: `ej2-post-images` (또는 원하는 이름)
3. **리전**: `ap-northeast-2` (서울) 또는 가장 가까운 리전
4. **퍼블릭 액세스 차단**: **체크 해제** (이미지를 브라우저에서 표시하기 위해 공개 읽기 필요) ⚠️
5. **버킷 버전 관리**: 비활성화
6. **암호화**: 활성화 (AES-256)
7. **버킷 만들기** 클릭

### 2단계: CORS 설정

1. 버킷 → **권한** 탭 → **CORS**
2. 다음 CORS 정책 추가:

```json
[
  {
    "AllowedOrigins": ["http://localhost:3000", "https://yourdomain.com"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
    "AllowedHeaders": ["*"],
    "MaxAgeSeconds": 3000
  }
]
```

3. `https://yourdomain.com`을 실제 프로덕션 도메인으로 변경
4. **변경 사항 저장** 클릭

### 3단계: 버킷 정책 설정 (공개 읽기)

1. **권한** 탭 → **버킷 정책**
2. 다음 정책 추가:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::ej2-post-images/*"
    }
  ]
}
```

3. `ej2-post-images`를 본인의 버킷 이름으로 변경
4. **변경 사항 저장** 클릭

### 4단계: IAM 사용자 생성

1. **IAM** → **사용자** → **사용자 생성**
2. **사용자 이름**: `ej2-s3-uploader`
3. **다음** 클릭
4. **직접 정책 연결** → **정책 생성**
5. 다음 JSON 정책 사용:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::ej2-post-images",
        "arn:aws:s3:::ej2-post-images/*"
      ]
    }
  ]
}
```

6. 정책 이름: `EJ2-S3-Upload-Policy`
7. 이 정책을 사용자에게 연결
8. **사용자 생성** 클릭

### 5단계: 액세스 키 생성

1. 생성한 사용자로 이동
2. **보안 자격 증명** 탭 클릭
3. **액세스 키** 섹션으로 스크롤 → **액세스 키 만들기**
4. **애플리케이션** 선택
5. **다음** → **액세스 키 만들기** 클릭
6. **⚠️ 중요**: **액세스 키 ID**와 **비밀 액세스 키** 복사
7. 다시 볼 수 없으니 안전한 곳에 저장하세요!

## 애플리케이션 설정

### 1단계: `.env` 파일 생성

1. `.env.example`을 `.env`로 복사:
   ```bash
   cp .env.example .env
   ```

2. `.env` 파일 편집하고 AWS 자격 증명 추가:
   ```
   AWS_S3_ACCESS_KEY=AKIA...
   AWS_S3_SECRET_KEY=본인의-비밀-키
   AWS_S3_BUCKET_NAME=ej2-post-images
   AWS_S3_REGION=ap-northeast-2
   ```

3. **⚠️ 절대 `.env` 파일을 Git에 커밋하지 마세요!** (이미 `.gitignore`에 포함됨)

### 2단계: 설정 파일 확인

**`backend/src/main/resources/application.properties`** (이미 설정됨):
```properties
aws.s3.access-key=${AWS_S3_ACCESS_KEY}
aws.s3.secret-key=${AWS_S3_SECRET_KEY}
aws.s3.bucket-name=${AWS_S3_BUCKET_NAME}
aws.s3.region=${AWS_S3_REGION:ap-northeast-2}
```

**`docker-compose.yml`** (이미 설정됨):
```yaml
backend:
  environment:
    - AWS_S3_ACCESS_KEY=${AWS_S3_ACCESS_KEY}
    - AWS_S3_SECRET_KEY=${AWS_S3_SECRET_KEY}
    - AWS_S3_BUCKET_NAME=${AWS_S3_BUCKET_NAME}
    - AWS_S3_REGION=${AWS_S3_REGION:-ap-northeast-2}
```

## 애플리케이션 실행

### Docker Compose 사용 (권장)

```bash
# .env 파일에 AWS 자격 증명이 있는지 확인
docker-compose up --build
```

백엔드가 자동으로:
1. .env 파일에서 환경 변수 읽기
2. AWS S3 클라이언트 초기화
3. MariaDB에 `post_images` 테이블 생성

### Docker 없이 로컬 개발

1. **백엔드**:
   ```bash
   cd backend
   export AWS_S3_ACCESS_KEY=AKIA...
   export AWS_S3_SECRET_KEY=본인의-비밀-키
   export AWS_S3_BUCKET_NAME=ej2-post-images
   export AWS_S3_REGION=ap-northeast-2
   mvn clean package
   # Tomcat에 배포
   ```

2. **프론트엔드**:
   ```bash
   cd frontend
   npm install
   npm start
   ```

## API 엔드포인트

### 이미지 업로드
```
POST /api/posts/{postId}/images
Content-Type: multipart/form-data
Body: images=[file1, file2, file3]

응답: List<PostImage>
[
  {
    "id": 1,
    "postId": 123,
    "s3Key": "posts/123/uuid.jpg",
    "s3Url": "https://ej2-post-images.s3.ap-northeast-2.amazonaws.com/posts/123/uuid.jpg",
    "originalFilename": "photo.jpg",
    "fileSize": 1234567,
    "contentType": "image/jpeg",
    "displayOrder": 0,
    "uploadedAt": "2026-02-10T12:00:00"
  }
]
```

### 게시글의 이미지 조회
```
GET /api/posts/{postId}/images

응답: List<PostImage>
```

### 단일 이미지 삭제
```
DELETE /api/posts/{postId}/images/{imageId}

응답: "이미지가 성공적으로 삭제되었습니다"
```

## 사용 흐름

### 1. 이미지와 함께 게시글 작성

**프론트엔드: PostWritePage.js**
1. 사용자가 제목과 내용 작성
2. 사용자가 최대 5개의 이미지 선택 (각 5MB 이하)
3. "게시" 버튼 클릭
4. 프론트엔드가 먼저 게시글 생성 → 게시글 ID 획득
5. 프론트엔드가 `/api/posts/{postId}/images`로 이미지 업로드
6. 게시글 상세 페이지로 이동

### 2. 이미지가 있는 게시글 보기

**프론트엔드: PostDetailPage.js**
1. 게시글 데이터 가져오기: `GET /api/posts/{postId}`
2. 이미지 가져오기: `GET /api/posts/{postId}/images`
3. 내용 아래에 갤러리 형태로 이미지 표시

### 3. 게시글 삭제

**백엔드: PostService.java**
1. 사용자가 삭제 버튼 클릭
2. 백엔드가 S3에서 모든 이미지 삭제
3. 백엔드가 데이터베이스에서 이미지 레코드 삭제
4. 백엔드가 게시글 레코드 삭제
5. 모든 이미지 제거됨 (연쇄 삭제)

## 문제 해결

### 이미지 업로드 안 됨

**확인 1: AWS 자격 증명**
```bash
# .env 파일 존재 및 올바른 값 확인
cat .env
```

**확인 2: S3 버킷 권한**
- 버킷 정책이 공개 읽기를 허용하는지 확인
- IAM 사용자에게 PutObject 권한이 있는지 확인

**확인 3: 백엔드 로그**
```bash
docker logs spring-backend
```
다음과 같은 오류 확인:
- `Access Denied` → IAM 권한 문제
- `Bucket not found` → 버킷 이름 오타
- `Invalid credentials` → 액세스 키/비밀 키 문제

### 이미지 표시 안 됨

**확인 1: CORS 설정**
- CORS 정책에 프론트엔드 도메인이 포함되어 있는지 확인
- 브라우저 콘솔에서 CORS 오류 확인

**확인 2: 버킷 퍼블릭 액세스**
- "모든 퍼블릭 액세스 차단"이 해제되어 있는지 확인
- 버킷 정책이 `s3:GetObject`를 허용하는지 확인

**확인 3: 이미지 URL**
```bash
# S3 URL을 브라우저에서 직접 테스트
https://ej2-post-images.s3.ap-northeast-2.amazonaws.com/posts/1/uuid.jpg
```

### 데이터베이스 오류

**오류: `post_images` 테이블을 찾을 수 없음**
- Hibernate가 테이블을 자동 생성할 때까지 대기
- 백엔드 로그에서 Hibernate 스키마 업데이트 확인
- 필요시 위의 SQL을 사용하여 수동으로 테이블 생성

## 비용 추정

**AWS S3 요금 (ap-northeast-2 리전)**:
- 스토리지: $0.025/GB/월
- PUT 요청: $0.005/1,000건
- GET 요청: $0.0004/1,000건
- 데이터 전송(다운로드): 첫 1GB 무료, 이후 $0.126/GB

**예시**:
- 1,000개 게시글 × 평균 3개 이미지 × 3MB = 9GB 스토리지
- 월간 비용: 9GB × $0.025 = **$0.225/월**
- 10,000회 조회 × 3개 이미지 = 30,000 GET 요청 = **$0.012/월**
- **총 비용**: 일반적인 사용량에서 월 ~$0.25 (약 350원)

## 보안 모범 사례

### ✅ 해야 할 것
- 자격 증명에 환경 변수 사용
- 최소 권한을 가진 IAM 사용자 사용 (관리자 권한 없음)
- 90일마다 액세스 키 교체
- S3 버킷 암호화 활성화
- 백엔드에서 파일 크기 및 유형 검증
- 공개 읽기만 사용 (공개 쓰기 금지)

### ❌ 하지 말아야 할 것
- `.env` 또는 자격 증명을 Git에 커밋
- IAM 사용자에게 전체 S3 액세스 권한 부여
- 버킷 암호화 비활성화
- 파일 검증 생략
- 인증 없이 익명 업로드 허용

## 향후 개선 사항

### 2단계 기능
1. **이미지 최적화**: 업로드 전 리사이징 (비용 절감)
2. **썸네일 생성**: 목록 보기용 썸네일 생성
3. **CloudFront CDN**: 전 세계 빠른 배포
4. **이미지 편집**: 자르기, 회전, 필터
5. **지연 로딩**: 스크롤 시 이미지 로드
6. **워터마크**: 저작권용 워터마크 추가
7. **정리 작업**: 고아 S3 파일 삭제 예약 작업
8. **분석**: 가장 많이 본 이미지, 사용자당 스토리지 사용량 추적

## 참고 자료

- [AWS S3 문서](https://docs.aws.amazon.com/s3/)
- [Java용 AWS SDK](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/)
- [S3 버킷 정책](https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucket-policies.html)
- [CORS 설정](https://docs.aws.amazon.com/AmazonS3/latest/userguide/cors.html)

---

**작성일**: 2026년 2월 10일
**버전**: 1.0
**관리자**: EJ2 개발팀
