-- 109-房屋区域实测档案与方案区域来源.sql
-- 目的：DEC-064 / OBJ-007 / ANS-BIZ-20261012-001 / T12-BIZ-20261012-001 第 2 期。
--       ① 新增房屋区域实测档案（区域实测面积/周长的唯一事实源，一次录入多方案复用）；
--       ② db_design_space 增加「值来源 / 来源房屋区域 / 房屋值快照 / 覆盖留痕」列，
--          支撑实例化继承、金额门禁（只有 area_source=Measured 可作基数）与有界联动。
-- 影响表：新增 db_property_region；db_design_space 增加 6 列。
-- 回滚：docs/design/migrations/rollback/109-房屋区域实测档案与方案区域来源-rollback.sql
-- 注意：面积/周长可空表示「未录入」，不得用 0 表达未录入（DEC-062）；
--       参考值与历史估算值不得作为工程量基数（数值来源仍由 area_source 判定）。

create table db_property_region
(
    region_id        bigint         not null comment '房屋区域',
    com_id           bigint         not null comment '所属企业',
    store_id         bigint         null comment '所属门店',
    team_id          bigint         null comment '所属团队',
    emp_id           bigint         not null comment '数据负责人',
    property_id      bigint         not null comment '所属房屋',
    room_key         varchar(32)    null comment '区域键（与模版区域对齐）',
    room_type        varchar(32)    not null default 'Bedroom' comment '区域类型',
    name             varchar(64)    not null comment '区域名称',
    length_m         decimal(10, 3) null comment '量房快照 开间/长（m，非工程量依据）',
    width            decimal(10, 3) null comment '量房快照 进深/宽（m，非工程量依据）',
    height           decimal(10, 3) null comment '层高（m）',
    area             decimal(12, 2) null comment '实测面积（㎡，可空=未录入）',
    perimeter        decimal(12, 2) null comment '实测周长（m，可空=未录入）',
    area_source      varchar(16)    null comment '面积口径来源(Measured/Estimated)',
    measured_by_id   bigint         null comment '实测录入人',
    measured_at      datetime(6)    null comment '实测录入时间',
    sort             int            not null default 0 comment '排序',
    remark           varchar(512)   null comment '备注',
    create_date      int            null,
    create_date_time bigint         null,
    create_day       int            null,
    create_hour      int            null,
    create_minute    int            null,
    create_month     int            null,
    create_second    int            null,
    create_time      int            null,
    create_week      tinyint        null,
    create_year      int            null,
    update_date      int            null,
    update_date_time bigint         null,
    update_day       int            null,
    update_hour      int            null,
    update_minute    int            null,
    update_month     int            null,
    update_second    int            null,
    update_time      int            null,
    update_week      tinyint        null,
    update_year      int            null,
    primary key (region_id)
) engine = InnoDB comment '房屋区域实测档案（DEC-064）';

create index idx_property_region_company on db_property_region (com_id, region_id);
create index idx_property_region_company_property on db_property_region (com_id, property_id, region_id);
create index idx_property_region_company_store on db_property_region (com_id, store_id, region_id);
create index idx_property_region_company_team on db_property_region (com_id, team_id, region_id);
create index idx_property_region_company_owner on db_property_region (com_id, emp_id, region_id);

ALTER TABLE db_design_space
    ADD COLUMN value_origin VARCHAR(24) NULL COMMENT '区域值来源(House/Override/TemplateReference)',
    ADD COLUMN house_region_id bigint NULL COMMENT '来源房屋区域(房屋区域实测档案)',
    ADD COLUMN inherited_area DECIMAL(12, 2) NULL COMMENT '最近同步的房屋实测面积(㎡)',
    ADD COLUMN inherited_perimeter DECIMAL(12, 2) NULL COMMENT '最近同步的房屋实测周长(m)',
    ADD COLUMN override_by_id bigint NULL COMMENT '覆盖录入人',
    ADD COLUMN override_at datetime(6) NULL COMMENT '覆盖录入时间';

-- 存量区域没有房屋档案关联，保持 value_origin 为空（未关联），不猜测来源。
