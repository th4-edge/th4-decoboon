-- 109-量房原始区域与方案区域来源.sql
-- 目的：DEC-066 / OBJ-007（用户 2026-10-12 裁决）：
--       ① 量房测的是**房屋原始结构**（布局基本固定），原始区域挂在**量房单**下（db_measure_region）；
--          量房完成后冻结，需修正只能重新量房（复尺）；
--       ② 新建方案/实例化可**继承**原始区域作为起点（复制实测值），但**不联动**；
--          方案区域以方案为准（可新增/改尺寸/拆除），db_design_space 只增加「值来源 + 来源区域标识」两列。
-- 影响表：新增 db_measure_region；db_design_space 增加 value_origin / source_measure_region_id。
-- 回滚：docs/design/migrations/rollback/109-量房原始区域与方案区域来源-rollback.sql
-- 注意：面积/周长可空表示「未录入」，不得用 0 表达未录入（DEC-062）；只有 area_source=Measured 可作工程量基数。

create table db_measure_region
(
    region_id        bigint         not null comment '量房原始区域',
    com_id           bigint         not null comment '所属企业',
    store_id         bigint         null comment '所属门店',
    team_id          bigint         null comment '所属团队',
    emp_id           bigint         not null comment '数据负责人',
    measure_id       bigint         not null comment '所属量房记录',
    property_id      bigint         not null comment '所属房屋',
    room_key         varchar(32)    null comment '区域键（与模版区域对齐）',
    room_type        varchar(32)    not null default 'Bedroom' comment '区域类型',
    name             varchar(64)    not null comment '区域名称',
    length_m         decimal(10, 3) null comment '量房快照 开间/长（m，非工程量依据）',
    width            decimal(10, 3) null comment '量房快照 进深/宽（m，非工程量依据）',
    height           decimal(10, 3) null comment '层高（m）',
    area             decimal(12, 2) null comment '原始结构实测面积（㎡，可空=未录入）',
    perimeter        decimal(12, 2) null comment '原始结构实测周长（m，可空=未录入）',
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
) engine = InnoDB comment '量房原始区域（原始结构实测，DEC-066）';

create index idx_measure_region_company on db_measure_region (com_id, region_id);
create index idx_measure_region_company_measure on db_measure_region (com_id, measure_id, region_id);
create index idx_measure_region_company_property on db_measure_region (com_id, property_id, region_id);
create index idx_measure_region_company_store on db_measure_region (com_id, store_id, region_id);
create index idx_measure_region_company_owner on db_measure_region (com_id, emp_id, region_id);

-- 方案区域：只记录取值来源与来源原始区域（仅用于展示来源），不做联动、不保存房屋值快照。
ALTER TABLE db_design_space
    ADD COLUMN value_origin VARCHAR(24) NULL COMMENT '区域值来源(Measure/Override/TemplateReference)',
    ADD COLUMN source_measure_region_id bigint NULL COMMENT '来源量房原始区域(仅来源标识,不联动)';

-- 存量区域没有量房原始区域关联，保持 value_origin 为空（未继承），不猜测来源。
