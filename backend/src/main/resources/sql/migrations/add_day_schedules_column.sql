-- Add day_schedules column to support day-specific time ranges
-- This allows courses to have different time ranges for different days
-- Example: Monday 1-3, Wednesday 1-3 (same time) or Monday 1-2, Wednesday 3-4 (different times)

-- Add the new column
ALTER TABLE timetable_courses
ADD COLUMN day_schedules VARCHAR(1000) COMMENT 'JSON array of day schedules: [{day:1, periodStart:1, periodEnd:3}]';

-- Optional: Add days_of_week column if it doesn't exist (for backward compatibility)
ALTER TABLE timetable_courses
ADD COLUMN IF NOT EXISTS days_of_week VARCHAR(100) COMMENT 'JSON array of day numbers: [1, 3] for Mon+Wed';

-- Make period_start and period_end nullable (they're deprecated in favor of day_schedules)
ALTER TABLE timetable_courses
MODIFY COLUMN period_start INT NULL COMMENT '1-7 (deprecated, use day_schedules)',
MODIFY COLUMN period_end INT NULL COMMENT '1-7 (deprecated, use day_schedules)';

-- Optional: Migrate existing data from old format to new format
-- This converts single day courses to the new daySchedules format
UPDATE timetable_courses
SET day_schedules = JSON_ARRAY(
    JSON_OBJECT(
        'day', day_of_week,
        'periodStart', period_start,
        'periodEnd', period_end
    )
)
WHERE day_schedules IS NULL AND day_of_week IS NOT NULL;

-- Optional: Migrate multi-day courses (if days_of_week was being used)
-- Note: This assumes all days have the same time range
-- For custom migration logic, you may need to handle this separately
