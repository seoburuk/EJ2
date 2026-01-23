# Database Schema Design - University Community Platform

## Overview
This document outlines the complete database schema for all 25 features of the EJ2 university community platform.

## Schema Diagram Structure

### 1. User & Authentication

#### users (Enhanced)
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,           -- University email (@waseda.jp)
    password VARCHAR(255) NOT NULL,                -- BCrypt hashed
    name VARCHAR(100) NOT NULL,
    student_id VARCHAR(50),                        -- 학번
    university_id BIGINT,                          -- FK to universities
    nickname VARCHAR(50) UNIQUE,                   -- For display
    profile_bio TEXT,                              -- 자기소개 (200자)
    profile_image_url VARCHAR(500),
    role VARCHAR(20) DEFAULT 'USER',               -- USER, ADMIN
    status VARCHAR(20) DEFAULT 'ACTIVE',           -- ACTIVE, SUSPENDED, BANNED
    suspension_until DATETIME,                     -- 정지 기간
    warning_count INT DEFAULT 0,                   -- 경고 누적
    email_verified BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at DATETIME,
    FOREIGN KEY (university_id) REFERENCES universities(id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_university ON users(university_id);
CREATE INDEX idx_users_status ON users(status);
```

#### universities
```sql
CREATE TABLE universities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(100) NOT NULL UNIQUE,           -- waseda.jp
    country VARCHAR(50) DEFAULT 'JP',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_universities_domain ON universities(domain);
```

#### email_verifications
```sql
CREATE TABLE email_verifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    verification_code VARCHAR(10) NOT NULL,
    expires_at DATETIME NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verifications_email ON email_verifications(email);
CREATE INDEX idx_email_verifications_code ON email_verifications(verification_code);
```

#### password_reset_tokens
```sql
CREATE TABLE password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
```

### 2. Board System

#### boards
```sql
CREATE TABLE boards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,                    -- 게시판명
    code VARCHAR(50) NOT NULL UNIQUE,              -- GENERAL, ANONYMOUS, EVENT, BEST, MARKET
    description TEXT,
    university_id BIGINT,                          -- NULL = 전체 공용
    is_anonymous BOOLEAN DEFAULT FALSE,
    require_admin BOOLEAN DEFAULT FALSE,           -- 관리자만 작성 가능
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (university_id) REFERENCES universities(id)
);

CREATE INDEX idx_boards_code ON boards(code);
CREATE INDEX idx_boards_university ON boards(university_id);
```

#### posts
```sql
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,                         -- 5000자 제한 (애플리케이션 레벨)
    anonymous_id VARCHAR(50),                      -- 익명1, 익명2 등 (익명 게시판용)
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    scrap_count INT DEFAULT 0,
    is_notice BOOLEAN DEFAULT FALSE,               -- 공지사항
    is_blinded BOOLEAN DEFAULT FALSE,              -- 블라인드 처리
    blind_reason TEXT,                             -- 블라인드 사유
    reported_count INT DEFAULT 0,                  -- 신고 횟수
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_posts_board ON posts(board_id);
CREATE INDEX idx_posts_user ON posts(user_id);
CREATE INDEX idx_posts_created ON posts(created_at DESC);
CREATE INDEX idx_posts_like_count ON posts(like_count DESC);
CREATE INDEX idx_posts_view_count ON posts(view_count DESC);
CREATE INDEX idx_posts_is_blinded ON posts(is_blinded);
```

#### post_images
```sql
CREATE TABLE post_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    image_order INT DEFAULT 0,                     -- 이미지 순서
    file_size BIGINT,                              -- bytes (5MB 제한)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_post_images_post ON post_images(post_id);
```

#### post_view_logs
```sql
CREATE TABLE post_view_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT,                                -- NULL = 비로그인
    ip_address VARCHAR(50),
    viewed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_post_view_logs_post_user ON post_view_logs(post_id, user_id);
CREATE INDEX idx_post_view_logs_post_ip ON post_view_logs(post_id, ip_address);
```

#### events (이벤트 게시판 확장)
```sql
CREATE TABLE events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL UNIQUE,
    thumbnail_url VARCHAR(500) NOT NULL,           -- 필수 썸네일
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    is_archived BOOLEAN DEFAULT FALSE,             -- 종료 후 자동 아카이빙
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_events_dates ON events(start_date, end_date);
CREATE INDEX idx_events_archived ON events(is_archived);
```

### 3. Comment System

#### comments
```sql
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_comment_id BIGINT,                      -- NULL = 최상위, NOT NULL = 답글
    content TEXT NOT NULL,                         -- 500자 제한
    anonymous_id VARCHAR(50),                      -- 익명 게시판용
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,              -- 삭제된 댓글
    deleted_at DATETIME,
    reported_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_post ON comments(post_id);
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_parent ON comments(parent_comment_id);
CREATE INDEX idx_comments_created ON comments(created_at);
```

### 4. Reactions (추천/비추천)

#### post_reactions
```sql
CREATE TABLE post_reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(20) NOT NULL,            -- LIKE, DISLIKE
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_post_user_reaction (post_id, user_id)
);

CREATE INDEX idx_post_reactions_post ON post_reactions(post_id);
CREATE INDEX idx_post_reactions_user ON post_reactions(user_id);
```

#### comment_reactions
```sql
CREATE TABLE comment_reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(20) NOT NULL,            -- LIKE, DISLIKE
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_comment_user_reaction (comment_id, user_id)
);

CREATE INDEX idx_comment_reactions_comment ON comment_reactions(comment_id);
CREATE INDEX idx_comment_reactions_user ON comment_reactions(user_id);
```

### 5. Scrap System

#### scraps
```sql
CREATE TABLE scraps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    folder_id BIGINT,                              -- NULL = 기본 폴더
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES scrap_folders(id) ON DELETE SET NULL,
    UNIQUE KEY uk_user_post_scrap (user_id, post_id)
);

CREATE INDEX idx_scraps_user ON scraps(user_id);
CREATE INDEX idx_scraps_post ON scraps(post_id);
CREATE INDEX idx_scraps_folder ON scraps(folder_id);
```

#### scrap_folders
```sql
CREATE TABLE scrap_folders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_scrap_folders_user ON scrap_folders(user_id);
```

### 6. Marketplace (중고장터)

#### marketplace_items
```sql
CREATE TABLE marketplace_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL UNIQUE,                -- posts 테이블 참조
    price DECIMAL(10, 2) NOT NULL,
    product_condition VARCHAR(1) NOT NULL,         -- S, A, B, F
    trade_method VARCHAR(100),                     -- 직거래, 택배 등
    trade_status VARCHAR(20) DEFAULT 'SELLING',    -- SELLING, RESERVED, SOLD
    reserved_user_id BIGINT,                       -- 예약자
    sold_user_id BIGINT,                           -- 구매자
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (reserved_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (sold_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_marketplace_items_status ON marketplace_items(trade_status);
CREATE INDEX idx_marketplace_items_price ON marketplace_items(price);
```

### 7. Study Groups & Club Promotion

#### study_groups
```sql
CREATE TABLE study_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,                 -- 어학, 자격증, 프로젝트 등
    max_members INT NOT NULL,
    current_members INT DEFAULT 1,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'RECRUITING',       -- RECRUITING, CLOSED, IN_PROGRESS, COMPLETED
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_study_groups_category ON study_groups(category);
CREATE INDEX idx_study_groups_status ON study_groups(status);
```

#### study_group_members
```sql
CREATE TABLE study_group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    study_group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER',             -- LEADER, MEMBER
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (study_group_id) REFERENCES study_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_study_group_user (study_group_id, user_id)
);

CREATE INDEX idx_study_group_members_group ON study_group_members(study_group_id);
CREATE INDEX idx_study_group_members_user ON study_group_members(user_id);
```

#### club_promotions
```sql
CREATE TABLE club_promotions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL UNIQUE,
    club_name VARCHAR(200) NOT NULL,
    club_type VARCHAR(20) NOT NULL,                -- UNIVERSITY, ALLIANCE
    contact_info VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE INDEX idx_club_promotions_type ON club_promotions(club_type);
```

### 8. Messaging System

#### direct_messages
```sql
CREATE TABLE direct_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    read_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_direct_messages_sender ON direct_messages(sender_id);
CREATE INDEX idx_direct_messages_receiver ON direct_messages(receiver_id);
CREATE INDEX idx_direct_messages_created ON direct_messages(created_at DESC);
```

#### chat_rooms
```sql
CREATE TABLE chat_rooms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    university_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (university_id) REFERENCES universities(id)
);

CREATE INDEX idx_chat_rooms_university ON chat_rooms(university_id);
```

#### chat_messages
```sql
CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    anonymous_id VARCHAR(50) NOT NULL,             -- 익명 채팅
    content TEXT NOT NULL,
    reported_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_room ON chat_messages(chat_room_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(created_at DESC);
```

### 9. Course Reviews (강의평)

#### courses
```sql
CREATE TABLE courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    university_id BIGINT NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    professor_name VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    credits INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (university_id) REFERENCES universities(id),
    UNIQUE KEY uk_university_course_code (university_id, course_code)
);

CREATE INDEX idx_courses_university ON courses(university_id);
CREATE INDEX idx_courses_professor ON courses(professor_name);
CREATE INDEX idx_courses_name ON courses(course_name);
```

#### course_reviews
```sql
CREATE TABLE course_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    difficulty_rating INT NOT NULL,                -- 1-5
    workload_rating INT NOT NULL,                  -- 1-5 (과제량)
    satisfaction_rating INT NOT NULL,              -- 1-5
    content TEXT NOT NULL,
    like_count INT DEFAULT 0,
    dislike_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_course_user_review (course_id, user_id)
);

CREATE INDEX idx_course_reviews_course ON course_reviews(course_id);
CREATE INDEX idx_course_reviews_user ON course_reviews(user_id);
```

#### course_review_reactions
```sql
CREATE TABLE course_review_reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(20) NOT NULL,            -- HELPFUL, NOT_HELPFUL
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES course_reviews(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_review_user_reaction (review_id, user_id)
);

CREATE INDEX idx_course_review_reactions_review ON course_review_reactions(review_id);
```

### 10. Timetable (Existing - Keep as is)
- `timetables` (already exists)
- `timetable_courses` (already exists)

### 11. Admin & Moderation

#### reports
```sql
CREATE TABLE reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,              -- POST, COMMENT, CHAT_MESSAGE, USER
    target_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,                   -- SPAM, INAPPROPRIATE, HARASSMENT, etc.
    description TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',          -- PENDING, REVIEWED, ACCEPTED, REJECTED
    reviewed_by BIGINT,                            -- 관리자 ID
    reviewed_at DATETIME,
    action_taken VARCHAR(100),                     -- 조치 내역
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_reports_target ON reports(target_type, target_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
```

#### user_warnings
```sql
CREATE TABLE user_warnings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    report_id BIGINT,                              -- 관련 신고
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE SET NULL
);

CREATE INDEX idx_user_warnings_user ON user_warnings(user_id);
CREATE INDEX idx_user_warnings_created ON user_warnings(created_at DESC);
```

#### admin_action_logs
```sql
CREATE TABLE admin_action_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,              -- BLIND_POST, DELETE_COMMENT, SUSPEND_USER, etc.
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reason TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_admin_action_logs_admin ON admin_action_logs(admin_id);
CREATE INDEX idx_admin_action_logs_created ON admin_action_logs(created_at DESC);
```

## Summary

### Total Tables: 31

**Authentication & User (5 tables)**
- users, universities, email_verifications, password_reset_tokens, user_warnings

**Board System (5 tables)**
- boards, posts, post_images, post_view_logs, events

**Community Features (7 tables)**
- comments, post_reactions, comment_reactions, scraps, scrap_folders, reports, admin_action_logs

**Marketplace (1 table)**
- marketplace_items

**Study & Clubs (3 tables)**
- study_groups, study_group_members, club_promotions

**Messaging (3 tables)**
- direct_messages, chat_rooms, chat_messages

**Courses (3 tables)**
- courses, course_reviews, course_review_reactions

**Timetable (2 tables - existing)**
- timetables, timetable_courses

**Admin (2 tables)**
- reports, admin_action_logs
