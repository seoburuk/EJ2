# 시간표 애플리케이션 트러블슈팅 가이드 (0119_1)

## 📋 목차
1. [문제 개요](#문제-개요)
2. [발생한 오류들](#발생한-오류들)
3. [해결 과정](#해결-과정)
4. [사용한 Bash 명령어 설명](#사용한-bash-명령어-설명)
5. [최종 해결 방법](#최종-해결-방법)
6. [배운 점](#배운-점)

---

## 문제 개요

### 초기 증상
프론트엔드에서 시간표에 과목을 저장할 때 다음과 같은 오류가 발생했습니다:

```
POST http://localhost:3000/api/timetable/course 400 (Bad Request)
❌ エラー: 요일이 필요합니다
```

### 사용자 요구사항
각 요일마다 **독립적인 시간 설정**이 필요했습니다.
- 예시: 월요일 1-3교시, 금요일 4-6교시
- 기존에는 모든 요일이 같은 시간으로 강제 설정되는 문제가 있었습니다.

---

## 발생한 오류들

### 오류 1: 데이터 형식 불일치 (400 Bad Request)
**증상:**
```
POST http://localhost:3000/api/timetable/course 400 (Bad Request)
요일이 필요합니다
```

**원인:**
- 프론트엔드: `daySchedules` 배열 형식으로 데이터 전송
  ```javascript
  {
    daySchedules: [
      {day: 1, periodStart: 1, periodEnd: 3},
      {day: 5, periodStart: 4, periodEnd: 6}
    ]
  }
  ```
- 백엔드: `daysOfWeek` 또는 `dayOfWeek` 필드를 기대
  ```java
  if (requestData.get("dayOfWeek") == null &&
      requestData.get("daysOfWeek") == null) {
      return ResponseEntity.badRequest().body("요일이 필요합니다");
  }
  ```

### 오류 2: 백엔드 응답에 daySchedules 누락
**증상:**
```javascript
📥 백엔드에서 받은 courses: [
  {
    courseId: 8,
    daysOfWeek: [1, 5],
    daySchedules: []  // ← 비어있음!
  }
]
```

**원인:**
- Jackson JSON 직렬화 시 `@JsonProperty` 애노테이션 누락
- DaySchedule 클래스의 필드가 JSON에 포함되지 않음

### 오류 3: 백엔드 서버 연결 실패 (ECONNREFUSED)
**증상:**
```
GET http://localhost:3000/api/timetable 500 ECONNREFUSED
```

**원인:**
- 백엔드 서버가 실행되지 않음

### 오류 4: Spring Boot 플러그인 없음
**증상:**
```bash
[ERROR] No plugin found for prefix 'spring-boot' in the current project
```

**원인:**
- 이 프로젝트는 Spring Boot가 아니라 전통적인 Spring Framework + Tomcat WAR 배포 방식
- `mvn spring-boot:run` 명령어를 사용할 수 없음

### 오류 5: Tomcat 버전 호환성 문제
**증상:**
```
java.lang.NoClassDefFoundError: javax/servlet/ServletContextListener
```

**원인:**
- Tomcat 11은 Jakarta EE (`jakarta.servlet.*`) 사용
- 우리 애플리케이션은 Java EE (`javax.servlet.*`) 사용
- 네임스페이스 불일치로 클래스를 찾을 수 없음

---

## 해결 과정

### 1단계: 프론트엔드 데이터 형식 변환

**수정 파일:** `frontend/src/pages/Timetable/TimetablePage.tsx`

**변경 내용:**
```typescript
const handleSaveCourse = async (course: TimetableCourse) => {
  // daySchedules를 백엔드 호환 형식으로 변환
  const daysOfWeek = course.daySchedules.map(s => s.day);
  const periodStart = course.daySchedules.length > 0
    ? course.daySchedules[0].periodStart : 1;
  const periodEnd = course.daySchedules.length > 0
    ? course.daySchedules[0].periodEnd : 1;

  const dataToSend = {
    ...course,
    timetableId: course.courseId ? undefined : timetable.timetableId,
    daysOfWeek,      // 기존 형식
    periodStart,     // 기존 형식
    periodEnd,       // 기존 형식
    daySchedules: course.daySchedules  // 새로운 형식
  };

  // 백엔드로 전송
  await axios.post('/api/timetable/course', dataToSend);
}
```

### 2단계: 백엔드 daySchedules 파싱 구현

**수정 파일:** `backend/src/main/java/com/ej2/controller/TimetableController.java`

**추가 임포트:**
```java
import com.ej2.model.DaySchedule;
import java.util.ArrayList;
```

**변경 내용:**
```java
@PostMapping("/course")
public ResponseEntity<?> addCourse(@RequestBody Map<String, Object> requestData) {
    // daySchedules 처리 (새로운 구조 우선)
    Object daySchedulesObj = requestData.get("daySchedules");
    if (daySchedulesObj != null && daySchedulesObj instanceof List) {
        List<?> schedulesList = (List<?>) daySchedulesObj;
        List<DaySchedule> daySchedules = new ArrayList<>();
        List<Integer> daysOfWeek = new ArrayList<>();

        for (Object item : schedulesList) {
            if (item instanceof Map) {
                Map<?, ?> scheduleMap = (Map<?, ?>) item;
                Integer day = Integer.valueOf(scheduleMap.get("day").toString());
                Integer periodStart = Integer.valueOf(scheduleMap.get("periodStart").toString());
                Integer periodEnd = Integer.valueOf(scheduleMap.get("periodEnd").toString());

                daySchedules.add(new DaySchedule(day, periodStart, periodEnd));
                daysOfWeek.add(day);
            }
        }

        course.setDaySchedules(daySchedules);
        course.setDaysOfWeek(daysOfWeek);
        course.setDayOfWeek(daysOfWeek.get(0));
    }
    // ... 나머지 코드
}
```

### 3단계: JSON 직렬화 설정

**수정 파일:** `backend/src/main/java/com/ej2/model/DaySchedule.java`

**변경 내용:**
```java
import com.fasterxml.jackson.annotation.JsonProperty;

public class DaySchedule {
    @JsonProperty("day")
    private Integer day;

    @JsonProperty("periodStart")
    private Integer periodStart;

    @JsonProperty("periodEnd")
    private Integer periodEnd;

    // 생성자, getter, setter...
}
```

### 4단계: 백엔드 빌드 및 배포

#### 4-1. WAR 파일 빌드
```bash
cd /Users/yunsu-in/Downloads/EJ2/backend
mvn clean package
```

**출력:**
```
[INFO] Building war: /Users/yunsu-in/Downloads/EJ2/backend/target/ej2.war
[INFO] BUILD SUCCESS
```

#### 4-2. Tomcat 버전 확인
```bash
# 실행 중인 Tomcat 프로세스 확인
ps aux | grep -i tomcat | grep -v grep
```

**발견된 문제:**
- Tomcat 11이 실행 중 (`/opt/homebrew/Cellar/tomcat/11.0.15`)
- Tomcat 11은 Jakarta EE를 사용하므로 호환되지 않음

#### 4-3. Tomcat 9 사용으로 전환
```bash
# Tomcat 11 중지
pkill -f "org.apache.catalina.startup.Bootstrap"

# 2초 대기
sleep 2

# WAR 파일을 Tomcat 9에 배포
cp /Users/yunsu-in/Downloads/EJ2/backend/target/ej2.war \
   /opt/homebrew/opt/tomcat@9/libexec/webapps/

# Tomcat 9 시작
/opt/homebrew/opt/tomcat@9/bin/catalina start

# 배포 완료 대기
sleep 10
```

#### 4-4. 배포 확인
```bash
# DaySchedule 클래스가 배포되었는지 확인
find /opt/homebrew/opt/tomcat@9/libexec/webapps/ej2/WEB-INF/classes \
     -name "DaySchedule*"
```

**출력:**
```
/opt/homebrew/opt/tomcat@9/libexec/webapps/ej2/WEB-INF/classes/com/ej2/model/DaySchedule.class
```

#### 4-5. API 테스트
```bash
# 시간표 데이터 조회
curl -s 'http://localhost:8080/ej2/api/timetable?semester=spring&year=2026&userId=1'
```

**성공 응답 예시:**
```json
{
  "timetable": {...},
  "courses": [
    {
      "courseId": 8,
      "courseName": "1",
      "daysOfWeek": [1, 5],
      "daySchedules": [],
      "periodStart": 1,
      "periodEnd": 1
    }
  ]
}
```

---

## 사용한 Bash 명령어 설명

### 📁 파일 및 디렉토리 명령어

#### `ls` - 파일 목록 보기
```bash
# 기본 사용법
ls /path/to/directory

# 상세 정보 포함 (-l: long format)
ls -la /opt/homebrew/opt/tomcat@9/libexec/webapps/

# 설명:
# -l: 파일 권한, 소유자, 크기, 날짜 등 상세 정보
# -a: 숨김 파일(.으로 시작하는 파일)도 표시
```

**출력 예시:**
```
drwxr-x---  9 yunsu-in  admin   288 Jan 19 12:50 .
drwxr-xr-x 12 yunsu-in  admin   384 Dec  3 04:51 ..
drwxr-x---  4 yunsu-in  admin   128 Jan 19 12:50 ej2
-rw-r--r--  1 yunsu-in  admin 23389377 Jan 21 11:57 ej2.war
```

#### `find` - 파일 검색
```bash
# 특정 이름 패턴으로 파일 찾기
find /path/to/search -name "pattern"

# 예시: DaySchedule로 시작하는 모든 파일 찾기
find /opt/homebrew/opt/tomcat@9/libexec/webapps/ej2/WEB-INF/classes \
     -name "DaySchedule*"

# 에러 메시지 숨기기 (2>/dev/null)
find /some/path -name "*.class" 2>/dev/null
```

#### `cp` - 파일 복사
```bash
# 기본 사용법
cp source destination

# 예시: WAR 파일 배포
cp /Users/yunsu-in/Downloads/EJ2/backend/target/ej2.war \
   /opt/homebrew/opt/tomcat@9/libexec/webapps/
```

#### `rm` - 파일/디렉토리 삭제
```bash
# 파일 삭제
rm file.txt

# 디렉토리와 내용물 모두 삭제 (-r: recursive, -f: force)
rm -rf /path/to/directory

# 주의: 매우 강력한 명령어이므로 신중하게 사용!
```

### 🔍 프로세스 관리 명령어

#### `ps` - 실행 중인 프로세스 확인
```bash
# 모든 프로세스 확인
ps aux

# 특정 프로그램만 필터링
ps aux | grep tomcat

# 설명:
# a: 모든 사용자의 프로세스
# u: 사용자 친화적 형식
# x: 터미널 없이 실행 중인 프로세스도 포함
```

**출력 예시:**
```
USER    PID  %CPU %MEM      VSZ    RSS   TT  STAT STARTED      TIME COMMAND
yunsu-in 17952  0.0  1.6 417218576 267568 ??  S   12:00PM   0:03.08 /opt/homebrew/opt/openjdk/bin/java ... org.apache.catalina.startup.Bootstrap start
```

#### `grep` - 텍스트 필터링
```bash
# 특정 패턴이 포함된 줄만 출력
command | grep "pattern"

# 대소문자 구분 없이 검색 (-i)
ps aux | grep -i tomcat

# 패턴이 포함되지 않은 줄만 출력 (-v)
ps aux | grep tomcat | grep -v grep

# 설명: "tomcat"이 포함된 줄을 찾되, "grep" 명령어 자체는 제외
```

#### `pkill` - 프로세스 종료
```bash
# 프로세스 이름으로 종료
pkill process_name

# 정규표현식 패턴 매칭 (-f)
pkill -f "org.apache.catalina.startup.Bootstrap"

# 주의: 여러 프로세스가 매칭되면 모두 종료됨!
```

### 📊 로그 및 파일 내용 확인

#### `tail` - 파일 끝부분 보기
```bash
# 마지막 10줄 출력 (기본값)
tail filename

# 마지막 N줄 출력
tail -n 50 /path/to/logfile
tail -50 /path/to/logfile  # -n 생략 가능

# 실시간 로그 모니터링 (-f: follow)
tail -f /opt/homebrew/opt/tomcat@9/libexec/logs/catalina.out
```

#### `cat` - 파일 전체 내용 출력
```bash
# 파일 내용 출력
cat filename

# 여러 파일 연결하여 출력
cat file1.txt file2.txt
```

#### `head` - 파일 앞부분 보기
```bash
# 처음 10줄 출력 (기본값)
head filename

# 처음 N줄 출력
head -50 filename
```

### 🔗 파이프와 리다이렉션

#### `|` (파이프) - 명령어 연결
```bash
# 앞 명령어의 출력을 뒤 명령어의 입력으로 전달
command1 | command2

# 예시: 로그에서 에러만 필터링
tail -100 catalina.out | grep ERROR

# 여러 개 연결 가능
ps aux | grep tomcat | grep -v grep
```

#### `>` - 출력 리다이렉션
```bash
# 명령어 출력을 파일에 저장 (덮어쓰기)
command > output.txt

# 예시
echo "Hello" > test.txt

# 파일에 추가 (>>)
echo "World" >> test.txt
```

#### `/dev/null` - 블랙홀
```bash
# 출력 버리기
command > /dev/null

# 에러 메시지만 버리기 (2는 stderr)
command 2>/dev/null

# 모든 출력 버리기
command > /dev/null 2>&1
```

### 🌐 네트워크 명령어

#### `curl` - HTTP 요청
```bash
# 기본 GET 요청
curl http://localhost:8080/ej2/api/users

# 응답 본문만 (-s: silent, 진행 표시 숨김)
curl -s http://localhost:8080/api/endpoint

# HTTP 상태 코드만 확인 (-o: 본문 버림, -w: 형식 지정)
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000

# JSON 데이터 POST 요청
curl -X POST http://localhost:8080/api/endpoint \
     -H "Content-Type: application/json" \
     -d '{"key": "value"}'
```

### ⚙️ Maven 명령어

#### `mvn` - Maven 빌드 도구
```bash
# 프로젝트 클린 (기존 빌드 삭제)
mvn clean

# 컴파일 및 패키징
mvn package

# 클린 + 패키징 (권장)
mvn clean package

# 출력 예시:
# [INFO] Building war: /path/to/project/target/app.war
# [INFO] BUILD SUCCESS
```

### 🐱 Tomcat 명령어

#### `catalina` - Tomcat 제어
```bash
# Tomcat 시작
catalina start

# Tomcat 중지
catalina stop

# 특정 버전 Tomcat 실행
/opt/homebrew/opt/tomcat@9/bin/catalina start

# 로그 확인
tail -f /opt/homebrew/opt/tomcat@9/libexec/logs/catalina.out
```

### ⏱️ 기타 유틸리티

#### `sleep` - 대기
```bash
# N초 대기
sleep 5

# 사용 예시: 서버 시작 후 대기
catalina start && sleep 10
```

#### `which` - 명령어 위치 찾기
```bash
# 명령어의 전체 경로 출력
which catalina
which java
which mvn

# 출력 예시:
# /opt/homebrew/bin/catalina
```

#### `&&` - 순차 실행 (성공 시에만)
```bash
# 앞 명령어가 성공하면 다음 명령어 실행
command1 && command2 && command3

# 예시: 빌드 성공 시에만 배포
mvn clean package && cp target/app.war /path/to/tomcat/webapps/
```

#### `cd` - 디렉토리 이동
```bash
# 절대 경로로 이동
cd /Users/yunsu-in/Downloads/EJ2/backend

# 상대 경로로 이동
cd ../frontend

# 홈 디렉토리로 이동
cd ~

# 이전 디렉토리로 돌아가기
cd -
```

---

## 최종 해결 방법

### 전체 배포 프로세스

```bash
# 1. 백엔드 디렉토리로 이동
cd /Users/yunsu-in/Downloads/EJ2/backend

# 2. WAR 파일 빌드
mvn clean package

# 3. 기존 Tomcat 중지 (필요시)
pkill -f "org.apache.catalina.startup.Bootstrap"
sleep 2

# 4. WAR 파일 배포
cp target/ej2.war /opt/homebrew/opt/tomcat@9/libexec/webapps/

# 5. Tomcat 9 시작
/opt/homebrew/opt/tomcat@9/bin/catalina start

# 6. 배포 완료 대기 (약 10초)
sleep 10

# 7. API 테스트
curl -s 'http://localhost:8080/ej2/api/timetable?semester=spring&year=2026&userId=1' \
  | python3 -m json.tool | head -50
```

### 프론트엔드 실행 확인

```bash
# 프론트엔드 상태 확인
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000

# 200이 출력되면 정상 실행 중
```

---

## 배운 점

### 1. 프론트엔드-백엔드 데이터 형식 일치의 중요성
- API 요청/응답 형식이 양쪽에서 일치해야 함
- 하위 호환성을 유지하면서 새로운 기능 추가 가능

### 2. Tomcat 버전 호환성
| Tomcat 버전 | Servlet API | 비고 |
|------------|-------------|------|
| Tomcat 9.x | Java EE (javax.servlet.*) | 우리 프로젝트 호환 ✅ |
| Tomcat 10.x+ | Jakarta EE (jakarta.servlet.*) | 호환 안됨 ❌ |

### 3. Jackson JSON 직렬화
- `@JsonProperty` 애노테이션으로 필드 이름 명시
- 자동으로 getter/setter 메서드 사용하여 JSON 변환

### 4. Spring Framework vs Spring Boot
| 구분 | Spring Framework | Spring Boot |
|------|-----------------|-------------|
| 배포 방식 | WAR → Tomcat | 내장 서버 (JAR) |
| 실행 방법 | Tomcat에 배포 | `java -jar app.jar` |
| 설정 | XML/Java Config | Auto Configuration |
| 우리 프로젝트 | ✅ 해당 | ❌ 해당 없음 |

### 5. 디버깅 팁
1. **로그 확인**: `tail -f catalina.out`으로 실시간 로그 모니터링
2. **API 테스트**: `curl` 명령어로 직접 API 호출 테스트
3. **프로세스 확인**: `ps aux | grep tomcat`으로 실행 상태 확인
4. **단계별 검증**: 각 단계마다 결과 확인

### 6. 트러블슈팅 순서
1. **오류 메시지 분석** → 근본 원인 파악
2. **관련 코드 확인** → 프론트엔드/백엔드 양쪽 점검
3. **로그 확인** → 상세한 에러 정보 수집
4. **단계별 수정** → 한 번에 하나씩 변경
5. **테스트** → 각 단계마다 동작 확인

---

## 결과

✅ **성공:**
- 각 요일마다 독립적인 시간 설정 가능
- 예: 월요일 1-3교시, 금요일 4-6교시
- 데이터베이스에 `daySchedules` JSON으로 저장
- 프론트엔드에서 올바르게 표시

🎯 **테스트 방법:**
1. http://localhost:3000 접속
2. 시간표에서 빈 칸 클릭
3. 월요일, 금요일 선택
4. 월요일: 1-3교시, 금요일: 4-6교시 설정
5. 저장 후 시간표 확인

---

## 참고 자료

### 주요 파일 위치
```
프로젝트 구조:
/Users/yunsu-in/Downloads/EJ2/
├── backend/
│   ├── src/main/java/com/ej2/
│   │   ├── controller/TimetableController.java  # API 엔드포인트
│   │   ├── service/TimetableService.java        # 비즈니스 로직
│   │   ├── model/
│   │   │   ├── DaySchedule.java                 # 요일별 스케줄 모델
│   │   │   └── TimetableCourse.java             # 과목 엔티티
│   │   └── converter/DayScheduleListConverter.java
│   ├── pom.xml                                  # Maven 설정
│   └── target/ej2.war                          # 빌드된 WAR 파일
└── frontend/
    ├── src/pages/Timetable/
    │   ├── TimetablePage.tsx                    # 시간표 메인 페이지
    │   └── CourseModal.tsx                      # 과목 추가/수정 모달
    └── package.json                             # npm 설정

Tomcat 위치:
/opt/homebrew/opt/tomcat@9/
├── bin/catalina                                 # Tomcat 실행 스크립트
├── libexec/
│   ├── webapps/                                # WAR 배포 위치
│   │   └── ej2.war
│   └── logs/                                   # 로그 파일
│       ├── catalina.out                        # 메인 로그
│       └── localhost.2026-01-21.log           # 오류 상세 로그
```

### 유용한 명령어 모음
```bash
# 백엔드 빌드
cd /Users/yunsu-in/Downloads/EJ2/backend && mvn clean package

# Tomcat 재시작
pkill -f catalina && sleep 2 && \
cp /Users/yunsu-in/Downloads/EJ2/backend/target/ej2.war \
   /opt/homebrew/opt/tomcat@9/libexec/webapps/ && \
/opt/homebrew/opt/tomcat@9/bin/catalina start

# 로그 실시간 모니터링
tail -f /opt/homebrew/opt/tomcat@9/libexec/logs/catalina.out

# API 테스트
curl -s 'http://localhost:8080/ej2/api/timetable?semester=spring&year=2026&userId=1' \
  | python3 -m json.tool
```

---

**작성일:** 2026-01-21
**작성자:** AI Assistant
**문서 버전:** 1.0
