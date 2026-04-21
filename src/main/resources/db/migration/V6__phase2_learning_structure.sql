create table course_learning_units (
    id uuid primary key,
    course_id uuid not null references courses(id),
    created_by uuid not null references app_users(id),
    title varchar(120) not null,
    description text,
    sort_order integer not null default 0,
    published boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_course_learning_units_course_order on course_learning_units(course_id, sort_order asc, created_at asc);

create table course_learning_points (
    id uuid primary key,
    unit_id uuid not null references course_learning_units(id),
    created_by uuid not null references app_users(id),
    title varchar(120) not null,
    summary text,
    estimated_minutes integer,
    sort_order integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_course_learning_points_unit_order on course_learning_points(unit_id, sort_order asc, created_at asc);

create table course_learning_tasks (
    id uuid primary key,
    point_id uuid not null references course_learning_points(id),
    created_by uuid not null references app_users(id),
    title varchar(120) not null,
    description text,
    task_type varchar(20) not null,
    material_type varchar(20),
    content_text text,
    media_url varchar(500),
    file_name varchar(255),
    file_path varchar(500),
    content_type varchar(255),
    question_type varchar(20),
    options_json text,
    reference_answer text,
    max_score numeric(10, 2) not null,
    required boolean not null default true,
    sort_order integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_course_learning_tasks_point_order on course_learning_tasks(point_id, sort_order asc, created_at asc);
create index idx_course_learning_tasks_type on course_learning_tasks(task_type);

create table course_learning_task_submissions (
    id uuid primary key,
    task_id uuid not null references course_learning_tasks(id),
    user_id uuid not null references app_users(id),
    answer_text text,
    file_name varchar(255),
    file_path varchar(500),
    content_type varchar(255),
    score numeric(10, 2),
    feedback text,
    graded_by uuid references app_users(id),
    graded_at timestamp,
    latest boolean not null,
    submitted_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_course_learning_task_submissions_task_user_latest on course_learning_task_submissions(task_id, user_id, latest);
create index idx_course_learning_task_submissions_task_submitted_at on course_learning_task_submissions(task_id, submitted_at desc);
create index idx_course_learning_task_submissions_graded_by on course_learning_task_submissions(graded_by);

alter table course_learning_units
    add constraint chk_course_learning_units_title_not_blank check (char_length(trim(title)) > 0);

alter table course_learning_points
    add constraint chk_course_learning_points_title_not_blank check (char_length(trim(title)) > 0);

alter table course_learning_tasks
    add constraint chk_course_learning_tasks_title_not_blank check (char_length(trim(title)) > 0);

alter table course_learning_tasks
    add constraint chk_course_learning_tasks_type_valid check (task_type in ('MEDIA'));

alter table course_learning_tasks
    add constraint chk_course_learning_tasks_material_type_valid check (material_type is null or material_type in ('FILE', 'LINK', 'TEXT'));

alter table course_learning_tasks
    add constraint chk_course_learning_tasks_question_type_valid check (question_type is null or question_type in ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SHORT_ANSWER'));

alter table course_learning_tasks
    add constraint chk_course_learning_tasks_score_range check (max_score >= 0);

alter table course_learning_task_submissions
    add constraint chk_course_learning_task_submissions_score_range check (score is null or (score >= 0 and score <= 100));