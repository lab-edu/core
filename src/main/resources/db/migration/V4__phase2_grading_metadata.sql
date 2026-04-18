alter table submissions
    add column if not exists graded_by uuid;

alter table submissions
    add column if not exists graded_at timestamp;

alter table submissions
    add constraint fk_submissions_graded_by foreign key (graded_by) references app_users(id);

create index if not exists idx_submissions_graded_by on submissions(graded_by);
create index if not exists idx_submissions_experiment_latest on submissions(experiment_id, latest, submitted_at desc);
