# Git 히스토리 정리 가이드

## 문제 상황

### 발생한 오류
```bash
remote: error: File frontend/node_modules/.cache/default-development/0.pack is 126.75 MB;
this exceeds GitHub's file size limit of 100.00 MB
remote: error: GH001: Large files detected.
error: failed to push some refs to 'https://github.com/seoburuk/EJ2.git'
```

### 원인 분석
1. **node_modules가 Git에 커밋됨**: `frontend/node_modules` 폴더 전체가 Git 추적 대상에 포함
2. **빌드 산출물이 커밋됨**: `backend/target` 폴더의 WAR 파일과 컴파일된 클래스 파일들이 포함
3. **.gitignore 미설정**: 프로젝트 초기에 `.gitignore`에 필수 제외 항목이 누락됨
4. **GitHub 파일 크기 제한**: 단일 파일 100MB 초과 시 push 거부

---

## 해결 과정

### 1단계: .gitignore 업데이트

#### 추가된 항목
```gitignore
### Node.js ###
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*

### Build artifacts ###
target/
*.war
*.jar
build/
dist/

### Cache ###
.cache/
*.log
```

#### 왜 이 항목들을 제외해야 하는가?
- **node_modules/**: npm이 자동 생성하는 의존성 폴더 (수만 개 파일, 수백 MB)
  - `package.json` + `package-lock.json`만 있으면 `npm install`로 재생성 가능
- **target/**: Maven이 생성하는 빌드 산출물 (WAR, 컴파일된 .class 파일)
  - `mvn clean package`로 언제든 재빌드 가능
- **.cache/**: webpack/babel 같은 빌드 도구의 임시 캐시
- **로그 파일**: 실행 시 생성되는 임시 파일

---

### 2단계: 현재 작업 디렉토리에서 파일 제거

```bash
# Git 인덱스에서만 제거 (실제 파일은 유지)
git rm -r --cached frontend/node_modules
git rm -r --cached backend/target
```

#### 명령어 설명
- `git rm`: Git 추적에서 파일 제거
- `-r`: 디렉토리 재귀적으로 처리
- `--cached`: 인덱스(스테이징 영역)에서만 제거, 로컬 파일은 유지

**⚠️ 주의**: 이 단계는 **현재 커밋**에서만 제거하며, **과거 히스토리는 그대로 남음**

---

### 3단계: Git 히스토리 전체 정리 (핵심)

```bash
git filter-branch --force --index-filter \
  'git rm -rf --cached --ignore-unmatch frontend/node_modules' \
  --prune-empty --tag-name-filter cat -- --all
```

#### 명령어 상세 분석

| 옵션 | 설명 |
|------|------|
| `filter-branch` | Git 히스토리를 재작성하는 도구 |
| `--force` | 이전 filter-branch 백업 무시하고 강제 실행 |
| `--index-filter` | 각 커밋의 인덱스(스테이징 영역)를 수정하는 필터 |
| `git rm -rf --cached --ignore-unmatch` | 파일이 없어도 오류 없이 제거 |
| `--prune-empty` | 변경사항이 없어진 빈 커밋 제거 |
| `--tag-name-filter cat` | 태그도 함께 재작성 |
| `-- --all` | 모든 브랜치와 태그에 적용 |

#### 동작 원리
```
기존 히스토리:
커밋 A (node_modules 포함)
  ↓
커밋 B (node_modules 포함)
  ↓
커밋 C (node_modules 포함)

↓ filter-branch 실행 후

새 히스토리:
커밋 A' (node_modules 제거됨)
  ↓
커밋 B' (node_modules 제거됨)
  ↓
커밋 C' (node_modules 제거됨)
```

#### 성능 최적화
- `--index-filter`는 파일 시스템을 건드리지 않고 인덱스만 수정 (빠름)
- 대안: `--tree-filter`는 실제 파일을 체크아웃하므로 느림

---

### 4단계: 강제 Push

```bash
git push --force
```

#### 왜 --force가 필요한가?
- 로컬 히스토리와 원격 히스토리가 **완전히 다른 커밋**이 됨 (커밋 해시 변경)
- Git은 기본적으로 히스토리 덮어쓰기를 거부 (데이터 손실 방지)
- `--force`로 원격 저장소를 로컬 히스토리로 강제 덮어씀

#### ⚠️ 주의사항
```bash
# 팀 작업 시 협업자에게 알리기
# 협업자는 다음 명령으로 동기화 필요:
git fetch origin
git reset --hard origin/main
```

---

## Git 내부 동작 원리

### .gitignore의 동작 방식
```
.gitignore 추가 전:
  node_modules/ → Git 추적 중 ✗

.gitignore 추가 후:
  node_modules/ → 여전히 추적 중 ✗ (이미 인덱스에 등록됨)

git rm --cached 실행 후:
  node_modules/ → 추적 해제 ✓ (현재 커밋부터)

filter-branch 실행 후:
  node_modules/ → 모든 히스토리에서 제거 ✓✓✓
```

### 커밋 해시가 변경되는 이유
```
커밋 해시 = SHA-1(
  트리 객체 해시 +
  부모 커밋 해시 +
  작성자 정보 +
  커밋 메시지 +
  타임스탬프
)
```

- `filter-branch`가 트리 객체를 수정 → 트리 해시 변경
- 트리 해시가 변하면 → 커밋 해시도 변경
- 부모 커밋 해시가 변하면 → 자식 커밋 해시도 연쇄적으로 변경

---

## 검증 방법

### 1. 로컬 저장소 크기 확인
```bash
# .git 폴더 크기 확인
du -sh .git

# 히스토리 정리 전: 150MB+
# 히스토리 정리 후: 5MB
```

### 2. 특정 파일이 히스토리에 남아있는지 확인
```bash
# node_modules가 과거 커밋에 있는지 검색
git log --all --full-history -- "frontend/node_modules/*"

# 결과가 없으면 성공적으로 제거된 것
```

### 3. 원격 저장소 Push 테스트
```bash
git push

# "Everything up-to-date" 또는 성공 메시지 확인
```

---

## 대안 도구: BFG Repo-Cleaner (추천)

`filter-branch`보다 빠르고 안전한 도구:

```bash
# 설치 (macOS)
brew install bfg

# 100MB 이상 파일 모두 제거
bfg --strip-blobs-bigger-than 100M

# 특정 폴더 제거
bfg --delete-folders node_modules

# 정리 및 Push
git reflog expire --expire=now --all
git gc --prune=now --aggressive
git push --force
```

---

## 예방 조치

### 1. 프로젝트 시작 시 .gitignore 필수 설정
```bash
# Node.js 프로젝트
curl -o .gitignore https://raw.githubusercontent.com/github/gitignore/main/Node.gitignore

# Java 프로젝트
curl -o .gitignore https://raw.githubusercontent.com/github/gitignore/main/Java.gitignore
```

### 2. Pre-commit Hook 설정
```bash
# .git/hooks/pre-commit
#!/bin/bash
if git diff --cached --name-only | grep -q "node_modules/\|target/"; then
  echo "❌ Error: node_modules or target detected in commit"
  exit 1
fi
```

### 3. Git LFS (Large File Storage) 사용
```bash
# 큰 바이너리 파일 관리
git lfs install
git lfs track "*.zip"
git lfs track "*.tar.gz"
```

---

## 요약

| 단계 | 명령어 | 효과 |
|------|--------|------|
| 1 | `.gitignore` 업데이트 | 앞으로 추적 안 함 |
| 2 | `git rm --cached` | 현재 커밋에서 제거 |
| 3 | `git filter-branch` | **모든 히스토리에서 제거** |
| 4 | `git push --force` | 원격 저장소에 반영 |

### 핵심 교훈
1. **node_modules는 절대 커밋하지 않기**
2. **빌드 산출물(target/)도 커밋하지 않기**
3. **프로젝트 시작 시 .gitignore 먼저 설정**
4. **이미 커밋된 파일은 filter-branch로 히스토리 정리 필요**

---

## 참고 자료
- [Git filter-branch 공식 문서](https://git-scm.com/docs/git-filter-branch)
- [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/)
- [GitHub 파일 크기 제한](https://docs.github.com/en/repositories/working-with-files/managing-large-files)
- [gitignore 템플릿 모음](https://github.com/github/gitignore)
