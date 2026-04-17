create index if not exists idx_courses_owner_created_at on courses(owner_id, created_at desc);
create index if not exists idx_experiments_course_due_at on experiments(course_id, due_at);
create index if not exists idx_submissions_experiment_user_submitted_at on submissions(experiment_id, user_id, submitted_at desc);

alter table courses
    add constraint chk_courses_title_not_blank check (char_length(trim(title)) > 0);

alter table experiments
    add constraint chk_experiments_title_not_blank check (char_length(trim(title)) > 0);

alter table submissions
    add constraint chk_submissions_score_range check (score is null or (score >= 0 and score <= 100));
