-- 47-阶段验收整改轮次.sql
-- 依据用户 2026-09-21 裁决：整改对象为阶段级、不合格复用同一阶段与验收单、允许多次循环不限次数、
-- 原验收记录保留并追加结论、只做质量事实不碰账。轮次表为 append-only，历史轮次不可修改。
-- 产品设计：docs/product 与 App docs/product/2026-09-21-阶段验收整改闭环业务流程与交互设计.md

create table if not exists db_acceptance_round (
    round_id bigint not null,
    com_id bigint not null,
    store_id bigint null,
    team_id bigint null,
    owner_id bigint null,
    acceptance_id bigint not null,
    project_id bigint not null,
    property_id bigint not null,
    stage_id bigint not null,
    round_no int not null,
    result varchar(16) not null,
    rectification_note longtext null,
    conclusion varchar(1024) null,
    submitted_by_id bigint null,
    submitted_at datetime(6) null,
    confirmed_by_id bigint null,
    confirmed_at datetime(6) null,
    rectification_owner_id bigint null,
    rectification_due_date date null,
    create_date_time bigint null,
    create_date int null,
    create_time int null,
    create_year int null,
    create_month int null,
    create_day int null,
    create_hour int null,
    create_minute int null,
    create_second int null,
    create_week int null,
    update_date_time bigint null,
    update_date int null,
    update_time int null,
    update_year int null,
    update_month int null,
    update_day int null,
    update_hour int null,
    update_minute int null,
    update_second int null,
    update_week int null,
    primary key (round_id),
    unique key uk_acceptance_round_no (acceptance_id, round_no),
    key idx_acceptance_round_company (com_id, round_id),
    key idx_acceptance_round_company_acceptance (com_id, acceptance_id, round_id),
    key idx_acceptance_round_company_project (com_id, project_id, round_id),
    key idx_acceptance_round_company_stage (com_id, stage_id, round_id),
    key idx_acceptance_round_company_store (com_id, store_id, round_id),
    key idx_acceptance_round_company_team (com_id, team_id, round_id),
    key idx_acceptance_round_company_owner (com_id, owner_id, round_id),
    constraint fk_acceptance_round_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_acceptance_round_acceptance foreign key (acceptance_id) references db_acceptance (acceptance_id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists db_acceptance_round_issue (
    round_id bigint not null,
    issue varchar(512) null,
    key idx_acceptance_round_issue (round_id),
    constraint fk_acceptance_round_issue_round foreign key (round_id) references db_acceptance_round (round_id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists db_acceptance_round_evidence (
    round_id bigint not null,
    url varchar(512) null,
    key idx_acceptance_round_evidence (round_id),
    constraint fk_acceptance_round_evidence_round foreign key (round_id) references db_acceptance_round (round_id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists db_acceptance_round_rectification_evidence (
    round_id bigint not null,
    url varchar(512) null,
    key idx_acceptance_round_rectification_evidence (round_id),
    constraint fk_acceptance_round_rectification_evidence_round
        foreign key (round_id) references db_acceptance_round (round_id)
) engine=InnoDB default charset=utf8mb4;

-- 存量回填：每张历史 Acceptance 补建第 1 轮，保留原状态、结论、问题、证据与整改要求。
-- 使用 acceptance_id 作为历史轮次主键；后续新轮次仍由雪花 ID 生成。重复执行不会重复插入。
insert into db_acceptance_round (
    round_id, com_id, store_id, team_id, owner_id, acceptance_id, project_id, property_id, stage_id,
    round_no, result, rectification_note, conclusion, submitted_by_id, submitted_at,
    confirmed_by_id, confirmed_at, rectification_owner_id, rectification_due_date,
    create_date_time, create_date, create_time, create_year, create_month, create_day,
    create_hour, create_minute, create_second, create_week,
    update_date_time, update_date, update_time, update_year, update_month, update_day,
    update_hour, update_minute, update_second, update_week
)
select
    a.acceptance_id, a.com_id, a.store_id, a.team_id, a.owner_id, a.acceptance_id,
    a.project_id, a.property_id, a.stage_id, 1, a.status, null, a.conclusion, a.emp_id,
    case
        when a.create_date is not null
            then str_to_date(concat(a.create_date, lpad(coalesce(a.create_time, 0), 6, '0')),
                             '%Y%m%d%H%i%s')
        else a.confirmed_at
    end,
    a.confirmed_by, a.confirmed_at, a.rectification_owner_id, a.rectification_due_date,
    a.create_date_time, a.create_date, a.create_time, a.create_year, a.create_month, a.create_day,
    a.create_hour, a.create_minute, a.create_second, a.create_week,
    a.update_date_time, a.update_date, a.update_time, a.update_year, a.update_month, a.update_day,
    a.update_hour, a.update_minute, a.update_second, a.update_week
from db_acceptance a
where not exists (
    select 1
    from db_acceptance_round r
    where r.acceptance_id = a.acceptance_id and r.round_no = 1
);

insert into db_acceptance_round_issue (round_id, issue)
select distinct r.round_id, ai.issue
from db_acceptance a
join db_acceptance_round r on r.acceptance_id = a.acceptance_id and r.round_no = 1
join db_acceptance_issue ai on ai.acceptance_id = a.acceptance_id
where ai.issue is not null
  and not exists (
      select 1
      from db_acceptance_round_issue ri
      where ri.round_id = r.round_id and ri.issue = ai.issue
  );

insert into db_acceptance_round_evidence (round_id, url)
select distinct r.round_id, ae.url
from db_acceptance a
join db_acceptance_round r on r.acceptance_id = a.acceptance_id and r.round_no = 1
join db_acceptance_evidence ae on ae.acceptance_id = a.acceptance_id
where ae.url is not null
  and not exists (
      select 1
      from db_acceptance_round_evidence re
      where re.round_id = r.round_id and re.url = ae.url
  );

-- 回滚（人工确认后执行）：
-- DROP TABLE IF EXISTS db_acceptance_round_rectification_evidence;
-- DROP TABLE IF EXISTS db_acceptance_round_evidence;
-- DROP TABLE IF EXISTS db_acceptance_round_issue;
-- DROP TABLE IF EXISTS db_acceptance_round;
