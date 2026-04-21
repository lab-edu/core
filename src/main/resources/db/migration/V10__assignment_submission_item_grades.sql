ALTER TABLE assignment_submissions
ADD COLUMN IF NOT EXISTS item_grades_json TEXT;
