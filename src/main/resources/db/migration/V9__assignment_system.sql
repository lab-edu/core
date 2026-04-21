-- V9__assignment_system.sql
-- 创建作业系统表结构

-- 1. 创建assignments表
CREATE TABLE assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description TEXT,
    total_score DECIMAL(10,2) NOT NULL,
    start_at TIMESTAMP,
    due_at TIMESTAMP,
    required BOOLEAN NOT NULL DEFAULT false,
    sort_order INTEGER NOT NULL DEFAULT 0,
    published BOOLEAN NOT NULL DEFAULT false,
    notify_on_start BOOLEAN NOT NULL DEFAULT false,
    notify_before_due_24h BOOLEAN NOT NULL DEFAULT false,
    notify_on_due BOOLEAN NOT NULL DEFAULT false,
    auto_calculate_total BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_assignments_course_id ON assignments(course_id);
CREATE INDEX idx_assignments_created_by ON assignments(created_by);
CREATE INDEX idx_assignments_due_at ON assignments(due_at);
CREATE INDEX idx_assignments_published ON assignments(published) WHERE published = true;

-- 2. 创建assignment_task_items表
CREATE TABLE assignment_task_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    question VARCHAR(500) NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    options_json TEXT,
    reference_answer TEXT,
    max_score DECIMAL(10,2) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    original_task_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_assignment_task_items_assignment_id ON assignment_task_items(assignment_id);
CREATE INDEX idx_assignment_task_items_original_task_id ON assignment_task_items(original_task_id);

-- 3. 创建assignment_submissions表
CREATE TABLE assignment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    answers_json TEXT,
    total_score DECIMAL(10,2),
    feedback TEXT,
    graded_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    graded_at TIMESTAMP,
    latest BOOLEAN NOT NULL DEFAULT false,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_assignment_submissions_assignment_id ON assignment_submissions(assignment_id);
CREATE INDEX idx_assignment_submissions_user_id ON assignment_submissions(user_id);
CREATE INDEX idx_assignment_submissions_latest ON assignment_submissions(latest) WHERE latest = true;
CREATE INDEX idx_assignment_submissions_graded_at ON assignment_submissions(graded_at);

-- 4. 向course_learning_tasks表添加迁移字段
ALTER TABLE course_learning_tasks
ADD COLUMN IF NOT EXISTS migrated_to_assignment_id UUID,
ADD COLUMN IF NOT EXISTS migrated_to_item_id UUID;

-- 5. 迁移现有数据：将HOMEWORK+QUIZ类型的任务转换为作业
-- 注意：此迁移为可选步骤，可在后续手动执行
-- 这里创建函数以便后续调用
CREATE OR REPLACE FUNCTION migrate_homework_quiz_tasks() RETURNS void AS $$
DECLARE
    point_record RECORD;
    assignment_id UUID;
    item_id UUID;
    task_record RECORD;
BEGIN
    -- 按知识点分组迁移
    FOR point_record IN
        SELECT DISTINCT p.id, p.title, u.course_id, t.created_by
        FROM course_learning_tasks t
        JOIN course_learning_points p ON t.point_id = p.id
        JOIN course_learning_units u ON p.unit_id = u.id
        WHERE t.task_kind = 'HOMEWORK' AND t.task_type = 'QUIZ'
        AND t.migrated_to_assignment_id IS NULL
    LOOP
        -- 为每个知识点创建一个作业
        INSERT INTO assignments (
            id, course_id, created_by, title, description, total_score,
            start_at, due_at, required, sort_order, published,
            notify_on_start, notify_before_due_24h, notify_on_due,
            auto_calculate_total, created_at, updated_at
        )
        SELECT
            gen_random_uuid(),
            point_record.course_id,
            point_record.created_by,
            CONCAT(point_record.title, ' - 作业'),
            NULL,
            COALESCE(SUM(t.max_score), 0),
            MIN(t.start_at),
            MAX(t.due_at),
            true,
            0,
            true,
            bool_or(t.notify_on_start),
            bool_or(t.notify_before_due_24h),
            bool_or(t.notify_on_due),
            true,
            MIN(t.created_at),
            MIN(t.created_at)
        FROM course_learning_tasks t
        WHERE t.point_id = point_record.id
          AND t.task_kind = 'HOMEWORK' AND t.task_type = 'QUIZ'
        RETURNING id INTO assignment_id;

        -- 迁移该知识点下的所有任务
        FOR task_record IN
            SELECT * FROM course_learning_tasks t
            WHERE t.point_id = point_record.id
              AND t.task_kind = 'HOMEWORK' AND t.task_type = 'QUIZ'
            ORDER BY t.sort_order
        LOOP
            -- 创建题目项
            INSERT INTO assignment_task_items (
                id, assignment_id, question, question_type, options_json,
                reference_answer, max_score, sort_order, original_task_id,
                created_at, updated_at
            )
            VALUES (
                gen_random_uuid(),
                assignment_id,
                task_record.title,
                task_record.question_type,
                task_record.options_json,
                task_record.reference_answer,
                task_record.max_score,
                task_record.sort_order,
                task_record.id,
                task_record.created_at,
                task_record.updated_at
            )
            RETURNING id INTO item_id;

            -- 更新原任务的迁移标记
            UPDATE course_learning_tasks
            SET migrated_to_assignment_id = assignment_id,
                migrated_to_item_id = item_id
            WHERE id = task_record.id;
        END LOOP;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 6. 创建作业提醒事件表（如果需要）
-- 注意：homework_reminder_events表可能已存在，这里确保有assignment_id字段
ALTER TABLE homework_reminder_events
ADD COLUMN IF NOT EXISTS assignment_id UUID REFERENCES assignments(id) ON DELETE CASCADE;

-- 7. 更新inbox_notifications表（如果需要）
-- 作业相关通知可能需要更新action_path
-- 这里不修改表结构，仅注释说明

-- 迁移完成提示
COMMENT ON FUNCTION migrate_homework_quiz_tasks() IS '迁移HOMEWORK+QUIZ类型的任务到新作业系统';