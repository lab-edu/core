alter table course_learning_tasks
    add column if not exists task_kind varchar(20) not null default 'LEARNING';

alter table course_learning_tasks
    add column if not exists start_at timestamp;

alter table course_learning_tasks
    add column if not exists due_at timestamp;

alter table course_learning_tasks
    add column if not exists notify_on_start boolean not null default true;

alter table course_learning_tasks
    add column if not exists notify_before_due_24h boolean not null default true;

alter table course_learning_tasks
    add column if not exists notify_on_due boolean not null default true;

alter table course_learning_tasks
    add constraint chk_course_learning_tasks_kind_valid
    check (task_kind in ('LEARNING', 'HOMEWORK'));

alter table course_learning_tasks
    add constraint chk_course_learning_tasks_time_valid
    check (start_at is null or due_at is null or start_at <= due_at);

create index if not exists idx_course_learning_tasks_kind_due
    on course_learning_tasks(task_kind, due_at asc);

create table if not exists course_workspace_modules (
    id uuid primary key,
    course_id uuid not null references courses(id),
    module_key varchar(40) not null,
    enabled boolean not null default true,
    sort_order integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_course_workspace_modules_course_key unique (course_id, module_key)
);

create index if not exists idx_course_workspace_modules_course_order
    on course_workspace_modules(course_id, sort_order asc, created_at asc);

create table if not exists inbox_notifications (
    id uuid primary key,
    user_id uuid not null references app_users(id),
    notification_type varchar(40) not null,
    title varchar(120) not null,
    content text not null,
    action_path varchar(500),
    delivered_at timestamp not null,
    read_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_inbox_notifications_user_delivered
    on inbox_notifications(user_id, delivered_at desc);

create index if not exists idx_inbox_notifications_user_unread
    on inbox_notifications(user_id, read_at);

create table if not exists homework_reminder_events (
    id uuid primary key,
    task_id uuid not null references course_learning_tasks(id),
    target_user_id uuid not null references app_users(id),
    trigger_type varchar(30) not null,
    scheduled_at timestamp not null,
    sent_at timestamp,
    canceled boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_homework_reminder_events_task_user_trigger unique (task_id, target_user_id, trigger_type)
);

create index if not exists idx_homework_reminder_events_pending
    on homework_reminder_events(canceled, sent_at, scheduled_at asc);

alter table homework_reminder_events
    add constraint chk_homework_reminder_events_trigger
    check (trigger_type in ('START', 'BEFORE_DUE_24H', 'DUE'));