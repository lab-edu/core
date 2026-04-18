create table resources (
    id uuid primary key,
    course_id uuid not null references courses(id),
    uploaded_by uuid not null references app_users(id),
    name varchar(120) not null,
    type varchar(20) not null,
    category varchar(64),
    file_name varchar(255),
    file_path varchar(500),
    content_type varchar(255),
    external_url varchar(500),
    uploaded_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_resources_course_uploaded_at on resources(course_id, uploaded_at desc);
create index idx_resources_type on resources(type);

alter table resources
    add constraint chk_resources_name_not_blank check (char_length(trim(name)) > 0);

alter table resources
    add constraint chk_resources_type_valid check (type in ('FILE', 'VIDEO', 'LINK'));

alter table resources
    add constraint chk_resources_file_payload check (
        (type = 'FILE' and file_path is not null and file_name is not null and external_url is null)
        or
        (type in ('VIDEO', 'LINK') and external_url is not null and file_path is null)
    );
