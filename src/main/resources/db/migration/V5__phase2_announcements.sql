create table course_announcements (
    id uuid primary key,
    course_id uuid not null references courses(id),
    created_by uuid not null references app_users(id),
    title varchar(120) not null,
    content text not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_course_announcements_course_created_at on course_announcements(course_id, created_at desc);

alter table course_announcements
    add constraint chk_course_announcements_title_not_blank check (char_length(trim(title)) > 0);
