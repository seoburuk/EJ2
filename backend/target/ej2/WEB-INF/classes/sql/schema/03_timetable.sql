-- 大学公式科目マスタテーブル
CREATE TABLE IF NOT EXISTS course_catalog (
    catalog_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    university_code VARCHAR(20) NOT NULL COMMENT '大学コード',
    course_code VARCHAR(20) NOT NULL COMMENT '科目コード',
    course_name VARCHAR(100) NOT NULL COMMENT '科目名',
    professor_name VARCHAR(50) COMMENT '担当教員',
    department VARCHAR(50) COMMENT '学部・学科',
    year_target VARCHAR(20) COMMENT '対象学年',
    semester VARCHAR(20) NOT NULL COMMENT '学期',
    day_of_week TINYINT COMMENT '曜日',
    period_start TINYINT COMMENT '開始時限',
    period_end TINYINT COMMENT '終了時限',
    classroom VARCHAR(50) COMMENT '教室',
    credits DECIMAL(2,1) COMMENT '単位数',
    capacity INT COMMENT '定員',
    syllabus_url VARCHAR(255) COMMENT 'シラバスURL',
    
    INDEX idx_university (university_code),
    INDEX idx_search (course_name, professor_name),
    INDEX idx_semester (semester, day_of_week),
    FULLTEXT idx_fulltext (course_name, professor_name) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ユーザーお気に入り科目
CREATE TABLE IF NOT EXISTS favorite_courses (
    favorite_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    catalog_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (catalog_id) REFERENCES course_catalog(catalog_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_course (user_id, catalog_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;