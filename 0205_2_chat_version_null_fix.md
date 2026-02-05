# 채팅 기능 오류 수정: Version NULL 문제 (2026.02.05)

## 1. 문제 상황

### 오류 메시지
```
Failed to load resource: the server responded with a status of 500 ()
Failed to init chat: On 채팅방 오류
```

브라우저 콘솔에서 채팅 페이지 초기화 시 500 에러 발생.

---

## 2. 원인 분석

### 2.1 백엔드 로그 확인

```bash
docker logs spring-backend 2>&1 | grep -E "(ERROR|Exception|500)" | tail -30
```

**발견된 에러**:
```
org.springframework.transaction.TransactionSystemException: Could not commit JPA transaction
nested exception is javax.persistence.RollbackException: Error while committing the transaction
```

스택 트레이스를 통해 `ChatService.assignNickname()` 메서드에서 트랜잭션 롤백이 발생하는 것을 확인.

### 2.2 데이터베이스 상태 확인

```bash
docker exec mariadb mysql -u appuser -papppassword -e \
  "SELECT id, name, current_users, nickname_counter, version FROM appdb.chat_rooms;"
```

**결과**:
```
id  name  current_users  nickname_counter  version
1   111   1              24                NULL
```

**문제 발견**: `version` 컬럼이 `NULL`로 되어 있음!

### 2.3 근본 원인

| 항목 | 설명 |
|------|------|
| **JPA `@Version` 어노테이션** | 낙관적 락(Optimistic Locking)을 구현하기 위한 기능 |
| **낙관적 락의 동작 원리** | 엔티티 업데이트 시 버전 번호를 비교하여 충돌 감지 |
| **NULL 버전의 문제** | 버전이 NULL이면 비교가 불가능하여 트랜잭션 롤백 발생 |
| **발생 원인** | 기존 데이터 마이그레이션 시 또는 초기화 시 version이 설정되지 않음 |

**낙관적 락의 동작 흐름**:
```
[정상적인 경우]
1. 엔티티 읽기 (version=5)
2. 데이터 변경
3. 저장 시: WHERE id=1 AND version=5
4. version을 6으로 업데이트
5. 커밋 성공

[NULL인 경우]
1. 엔티티 읽기 (version=NULL)
2. 데이터 변경
3. 저장 시: WHERE id=1 AND version=NULL → 비교 실패
4. OptimisticLockException 발생
5. 트랜잭션 롤백 → 500 에러
```

---

## 3. 해결 방법

### 3.1 데이터베이스 기존 레코드 수정

**기존 데이터의 version 초기화**:
```bash
docker exec mariadb mysql -u appuser -papppassword -e \
  "UPDATE appdb.chat_rooms SET version = 0 WHERE version IS NULL;"
```

**확인**:
```bash
docker exec mariadb mysql -u appuser -papppassword -e \
  "SELECT id, name, current_users, nickname_counter, version FROM appdb.chat_rooms;"
```

**결과**:
```
id  name  current_users  nickname_counter  version
1   111   1              24                0
```

### 3.2 엔티티 클래스 수정

**파일**: `backend/src/main/java/com/ej2/model/ChatRoom.java`

**변경 전**:
```java
@Version
@Column(name = "version")
private Long version;
```

**변경 후**:
```java
@Version
@Column(name = "version")
private Long version = 0L;  // 기본값 설정
```

**변경 이유**:
- 향후 생성되는 새로운 ChatRoom 인스턴스에서 version이 NULL이 되지 않도록 방지
- JPA의 `@Version`은 자동으로 증가하지만, 초기값은 개발자가 설정해야 함

### 3.3 백엔드 재빌드

```bash
docker-compose up --build -d backend
```

**시작 확인** (15초 대기 후):
```bash
sleep 15 && docker logs spring-backend 2>&1 | tail -20
```

**정상 시작 시 로그**:
```
Server startup in [5825] milliseconds
```

---

## 4. 동작 확인

### 4.1 API 테스트

```bash
curl -s -w "\nHTTP Status: %{http_code}" \
  -X POST http://localhost:8080/api/chat/rooms/1/nickname \
  -H "Content-Type: application/json" \
  -d '{"useAnonymous": true}'
```

**예상 결과**:
```json
{"nickname":"匿名1"}
HTTP Status: 200
```

### 4.2 카운터 리셋 (테스트 후 정리)

```bash
# 카운터를 0으로 리셋
docker exec mariadb mysql -u appuser -papppassword -e \
  "UPDATE appdb.chat_rooms SET current_users = 0, nickname_counter = 0 WHERE id = 1;"

# 확인
docker exec mariadb mysql -u appuser -papppassword -e \
  "SELECT id, name, current_users, nickname_counter, version FROM appdb.chat_rooms;"
```

**결과**:
```
id  name  current_users  nickname_counter  version
1   111   0              0                 1
```

**참고**: version은 자동으로 1로 증가 (낙관적 락이 정상 동작하는 증거)

---

## 5. 기술 해설

### 5.1 낙관적 락 (Optimistic Locking)

**개념**:
데이터베이스 레코드 업데이트 시, 읽기 시점과 저장 시점에 데이터가 변경되지 않았는지를 버전 번호로 확인하는 동시성 제어 기법.

**비관적 락과의 비교**:
```
비관적 락 (Pessimistic):
- 읽기 시점에 락 획득 (SELECT ... FOR UPDATE)
- 다른 트랜잭션은 대기
- 데드락 위험 존재

낙관적 락 (Optimistic):
- 읽기 시점에는 락 없음
- 업데이트 시 버전 체크
- 충돌 시 재시도 (리트라이 로직)
```

**ChatService.java의 재시도 구현**:
```java
public String assignNickname(Long roomId) {
    int maxRetries = 3;
    for (int attempt = 0; attempt < maxRetries; attempt++) {
        try {
            return assignNicknameInternal(roomId);
        } catch (OptimisticLockException e) {
            if (attempt == maxRetries - 1) {
                throw new RuntimeException("닉네임 할당에 실패했습니다. 다시 시도해주세요.", e);
            }
            // 자동으로 다음 루프에서 재시도
        }
    }
    return "匿名0";
}
```

### 5.2 JPA `@Version` 어노테이션

**동작 흐름**:
```java
@Entity
public class ChatRoom {
    @Id
    private Long id;

    @Version
    private Long version = 0L;  // 초기값 필수!

    private Integer nicknameCounter;
}

// 사용 예시
ChatRoom room = chatRoomRepository.findById(1L).get();  // version=0
room.setNicknameCounter(5);
chatRoomRepository.save(room);  // version이 자동으로 1로 증가
```

**SQL 실행 내용**:
```sql
-- 읽기
SELECT * FROM chat_rooms WHERE id = 1;

-- 업데이트 (version 체크 포함)
UPDATE chat_rooms
SET nickname_counter = 5, version = 1, updated_at = NOW()
WHERE id = 1 AND version = 0;

-- 영향받은 행 수가 0이면 → OptimisticLockException
```

### 5.3 NULL 버전이 문제가 되는 이유

**SQL의 NULL 비교**:
```sql
-- version이 NULL인 경우
SELECT * FROM chat_rooms WHERE id = 1;  -- version = NULL

-- 업데이트 시도
UPDATE chat_rooms
SET version = 1
WHERE id = 1 AND version = NULL;  -- 이 조건은 항상 FALSE!

-- NULL과의 비교는 IS NULL을 사용해야 함
WHERE id = 1 AND version IS NULL;  -- 올바른 구문

-- 하지만 JPA는 "version = ?"로 바인딩하므로 NULL을 제대로 처리 불가
```

**Hibernate가 생성하는 SQL**:
```sql
-- Hibernate가 생성하는 UPDATE 문
UPDATE chat_rooms
SET nickname_counter=?, version=?, updated_at=?
WHERE id=? AND version=?

-- version이 NULL인 경우, 마지막 "version=?"가 NULL이 됨
-- SQL에서 "column = NULL"은 항상 FALSE이므로 업데이트 실패
```

---

## 6. 예방책

### 6.1 새 엔티티 생성 시

**기본값 설정**:
```java
@Entity
public class MyEntity {
    @Version
    private Long version = 0L;  // 반드시 기본값 설정!
}
```

### 6.2 데이터베이스 스키마 설계

**version 컬럼에 NOT NULL 제약 추가**:
```sql
ALTER TABLE chat_rooms
MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
```

### 6.3 데이터 마이그레이션

**기존 데이터 마이그레이션 시**:
```sql
-- 기존 레코드의 version 초기화
UPDATE chat_rooms SET version = 0 WHERE version IS NULL;

-- 제약 추가
ALTER TABLE chat_rooms
MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
```

---

## 7. 체크리스트

### 채팅이 작동하지 않을 때 확인 순서

- [ ] **Docker 컨테이너가 실행 중인가**
  ```bash
  docker ps
  ```

- [ ] **백엔드 로그에 에러가 있는가**
  ```bash
  docker logs spring-backend 2>&1 | tail -50
  ```

- [ ] **TransactionSystemException이 발생하는가**
  ```bash
  docker logs spring-backend 2>&1 | grep -i "transaction" | tail -20
  ```

- [ ] **데이터베이스의 version 컬럼 확인**
  ```bash
  docker exec mariadb mysql -u appuser -papppassword -e \
    "SELECT id, version FROM appdb.chat_rooms;"
  ```

- [ ] **version이 NULL인 레코드가 있으면 수정**
  ```bash
  docker exec mariadb mysql -u appuser -papppassword -e \
    "UPDATE appdb.chat_rooms SET version = 0 WHERE version IS NULL;"
  ```

- [ ] **엔티티 클래스에서 기본값이 설정되어 있는가**
  ```java
  // ChatRoom.java
  @Version
  private Long version = 0L;  // 이 초기화가 있는지 확인
  ```

- [ ] **코드 변경 후 Docker 재빌드했는가**
  ```bash
  docker-compose up --build -d backend
  ```

---

## 8. 관련 파일

```
EJ2/
├── backend/
│   └── src/main/java/com/ej2/
│       ├── model/
│       │   └── ChatRoom.java            # @Version 필드 정의
│       ├── service/
│       │   └── ChatService.java         # 낙관적 락 재시도 구현
│       └── controller/
│           └── ChatController.java      # /api/chat/rooms/{id}/nickname 엔드포인트
├── frontend/
│   └── src/pages/Chat/
│       └── ChatPage.js                  # 채팅 초기화 처리
└── docker-compose.yml
```

---

## 9. 참고: 낙관적 락 구현 패턴

### 패턴1: 단순한 버전 관리
```java
@Entity
public class Product {
    @Id
    private Long id;

    @Version
    private Long version = 0L;

    private Integer stock;
}

// 재고 업데이트 (충돌 시 자동 재시도 없음)
@Transactional
public void updateStock(Long productId, int quantity) {
    Product product = productRepository.findById(productId).get();
    product.setStock(product.getStock() - quantity);
    productRepository.save(product);  // 충돌 시 예외 발생
}
```

### 패턴2: 재시도 포함 (권장)
```java
@Service
public class ProductService {

    @Transactional
    public void updateStockWithRetry(Long productId, int quantity) {
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                updateStockInternal(productId, quantity);
                return;  // 성공하면 종료
            } catch (OptimisticLockException e) {
                if (attempt == maxRetries - 1) {
                    throw new RuntimeException("재고 업데이트 실패", e);
                }
                // 다음 루프에서 재시도
            }
        }
    }

    private void updateStockInternal(Long productId, int quantity) {
        Product product = productRepository.findById(productId).get();
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }
}
```

### 패턴3: 타임스탬프 기반
```java
@Entity
public class Document {
    @Id
    private Long id;

    @Version
    private LocalDateTime lastModified;  // Long 대신 타임스탬프

    private String content;
}
```

---

## 10. 요약

| 항목 | 내용 |
|------|------|
| **문제** | 채팅 초기화 시 500 에러 |
| **원인** | `chat_rooms.version` 컬럼이 NULL로 낙관적 락 실패 |
| **영향 범위** | `/api/chat/rooms/{id}/nickname` 엔드포인트 |
| **해결책1** | 기존 데이터의 version을 0으로 초기화 |
| **해결책2** | ChatRoom 엔티티에서 기본값 `0L` 설정 |
| **재발 방지** | NOT NULL 제약 추가, 초기값 필수 코딩 규약 |
| **학습 포인트** | JPA의 `@Version`은 NULL을 허용하지 않음, 초기화 필수 |

---

## 11. 참고 링크

### 관련 트러블슈팅 문서
- `0129_1_chat_troubleshooting.md` - 채팅 기능 전반 트러블슈팅
- Context Path, WebSocket, 프록시 설정 문제

### JPA 낙관적 락
- Spring Data JPA Documentation: [Optimistic Locking](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.locking)
- Hibernate Documentation: [Optimistic Locking](https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)

### SQL NULL 처리
- MySQL Documentation: [Working with NULL Values](https://dev.mysql.com/doc/refman/8.0/en/working-with-null.html)
- MariaDB Documentation: [NULL Values](https://mariadb.com/kb/en/null-values/)

---

## 부록: 유용한 명령어 모음

```bash
# 1. Docker 상태 확인
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 2. 백엔드 로그 확인 (에러만)
docker logs spring-backend 2>&1 | grep -E "(ERROR|Exception)" | tail -30

# 3. 채팅방 상태 확인
docker exec mariadb mysql -u appuser -papppassword -e \
  "SELECT * FROM appdb.chat_rooms WHERE id = 1\G"

# 4. version이 NULL인 레코드 검색
docker exec mariadb mysql -u appuser -papppassword -e \
  "SELECT id, name, version FROM appdb.chat_rooms WHERE version IS NULL;"

# 5. 모든 채팅방 리셋
docker exec mariadb mysql -u appuser -papppassword -e \
  "UPDATE appdb.chat_rooms SET current_users = 0, nickname_counter = 0, version = 0;"

# 6. 채팅 메시지 확인 (최근 10개)
docker exec mariadb mysql -u appuser -papppassword -e \
  "SELECT id, sender_nickname, type, LEFT(content, 50) as content, created_at
   FROM appdb.chat_messages ORDER BY id DESC LIMIT 10;"

# 7. API 테스트 (닉네임 할당)
curl -X POST http://localhost:8080/api/chat/rooms/1/nickname \
  -H "Content-Type: application/json" \
  -d '{"useAnonymous": true}' | jq .

# 8. API 테스트 (채팅방 목록)
curl -s http://localhost:8080/api/chat/rooms | jq .

# 9. 백엔드 재시작
docker-compose restart backend

# 10. 전체 서비스 재빌드
docker-compose down && docker-compose up --build -d
```
