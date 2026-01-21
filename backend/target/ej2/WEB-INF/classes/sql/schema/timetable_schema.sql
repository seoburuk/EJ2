-- 時間割テーブル
CREATE TABLE IF NOT EXISTS timetables (
    timetable_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    year INT NOT NULL,
    semester VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_semester (user_id, year, semester),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 時間割科目テーブル
CREATE TABLE IF NOT EXISTS timetable_courses (
    course_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timetable_id BIGINT NOT NULL,
    course_name VARCHAR(200) NOT NULL,
    professor_name VARCHAR(100),
    classroom VARCHAR(50),
    day_of_week INT NOT NULL COMMENT '1=月, 2=火, 3=水, 4=木, 5=金',
    period_start INT NOT NULL COMMENT '1-7',
    period_end INT NOT NULL COMMENT '1-7',
    credits DOUBLE,
    color_code VARCHAR(7) DEFAULT '#FFB3BA',
    memo VARCHAR(500),
    FOREIGN KEY (timetable_id) REFERENCES timetables(timetable_id) ON DELETE CASCADE,
    INDEX idx_timetable_id (timetable_id),
    INDEX idx_day_period (day_of_week, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- サンプルデータ挿入（テスト用）
INSERT INTO timetables (user_id, year, semester, name)
VALUES (1, 2025, 'spring', '2025年春学期')
ON DUPLICATE KEY UPDATE name = name;

-- サンプル科目データ
INSERT INTO timetable_courses (timetable_id, course_name, professor_name, classroom, day_of_week, period_start, period_end, credits, color_code)
SELECT
    t.timetable_id,
    'データ構造とアルゴリズム',
    '山田太郎',
    'A101',
    1, -- 月曜日
    1, -- 1限
    1,
    3.0,
    '#FFB3BA'
FROM timetables t
WHERE t.user_id = 1 AND t.year = 2025 AND t.semester = 'spring'
ON DUPLICATE KEY UPDATE course_name = course_name;

INSERT INTO timetable_courses (timetable_id, course_name, professor_name, classroom, day_of_week, period_start, period_end, credits, color_code)
SELECT
    t.timetable_id,
    'データベース設計',
    '佐藤花子',
    'B202',
    2, -- 火曜日
    2, -- 2限
    3, -- 3限まで（連続2コマ）
    4.0,
    '#BAFFC9'
FROM timetables t
WHERE t.user_id = 1 AND t.year = 2025 AND t.semester = 'spring'
ON DUPLICATE KEY UPDATE course_name = course_name;

INSERT INTO timetable_courses (timetable_id, course_name, professor_name, classroom, day_of_week, period_start, period_end, credits, color_code)
SELECT
    t.timetable_id,
    'ソフトウェア工学',
    '鈴木一郎',
    'C303',
    4, -- 木曜日
    1,
    1,
    3.0,
    '#BAE1FF'
FROM timetables t
WHERE t.user_id = 1 AND t.year = 2025 AND t.semester = 'spring'
ON DUPLICATE KEY UPDATE course_name = course_name;
