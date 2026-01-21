-- 複数曜日対応のためのカラム追加
-- days_of_week: JSON配列形式で複数曜日を保存 (例: "[1,3,5]" = 月水金)

ALTER TABLE timetable_courses 
ADD COLUMN days_of_week VARCHAR(100) NULL COMMENT '曜日の配列 (JSON形式: [1,3,5] = 月水金)';

-- 既存データの移行: day_of_weekからdays_of_weekへコピー
UPDATE timetable_courses 
SET days_of_week = CONCAT('[', day_of_week, ']')
WHERE day_of_week IS NOT NULL;

-- day_of_weekカラムのNOT NULL制約を削除 (下位互換性のため残す)
ALTER TABLE timetable_courses 
MODIFY COLUMN day_of_week INT NULL COMMENT '曜日 (deprecated, use days_of_week)';

-- インデックス追加 (検索性能向上)
-- CREATE INDEX idx_days_of_week ON timetable_courses(days_of_week);
