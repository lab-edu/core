create table app_users (
    id uuid primary key,
    username varchar(64) not null unique,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(100),
    role varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table courses (
    id uuid primary key,
    title varchar(120) not null,
    description text,
    invite_code varchar(32) not null unique,
    owner_id uuid not null references app_users(id),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table course_members (
    id uuid primary key,
    course_id uuid not null references courses(id),
    user_id uuid not null references app_users(id),
    member_role varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_course_members_course_user unique (course_id, user_id)
);

create index idx_course_members_user_id on course_members(user_id);
create index idx_course_members_course_id on course_members(course_id);

create table experiments (
    id uuid primary key,
    course_id uuid not null references courses(id),
    title varchar(120) not null,
    description text,
    published_at timestamp not null,
    due_at timestamp,
    created_by uuid not null references app_users(id),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_experiments_course_id on experiments(course_id);

create table submissions (
    id uuid primary key,
    experiment_id uuid not null references experiments(id),
    user_id uuid not null references app_users(id),
    file_name varchar(255) not null,
    file_path varchar(500) not null,
    content_type varchar(255),
    note text,
    score numeric(10, 2),
    feedback text,
    latest boolean not null,
    submitted_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_submissions_experiment_id on submissions(experiment_id);
create index idx_submissions_user_id on submissions(user_id);
create index idx_submissions_latest on submissions(experiment_id, user_id, latest);