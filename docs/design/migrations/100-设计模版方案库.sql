-- 100-设计模版方案库.sql
-- 目的：DEC-050 / BR-040 设计模版（方案库）——区域骨架 + 报价分项 + 专业变量工程量公式。
-- 影响表：新增 design_template / design_template_version / design_template_room / design_template_item。
-- 前置：catalog_item、db_tenant_company / store / employee 已存在。
-- 回滚：drop table design_template_item; drop table design_template_room; drop table design_template_version; drop table design_template;
-- 说明：模版主档只存身份与治理（归属/范围/状态/当前版本指针），名称与房型等内容快照存 version 表，保证已发布版本不可变。
-- 说明：模版**不承载任何价格字段**（材料价/人工价/折扣率/金额），分项名称/单位/计价方式/价格一律读 catalog_item（用户 2026-10-09 裁决 A）。
-- 说明：公式只允许专业变量（L/W/H/DW/DA/WA/WR/N、A/P/WP/CP、SUM/COUNT/ROUND），禁止单元格引用。
-- 说明：com_id 始终最左建联合索引；发布后的版本不可修改，修改即新建版本。

create table design_template
(
    template_id     bigint                             not null comment '设计模版',
    com_id          bigint                             not null comment '所属企业',
    store_id        bigint                             null comment '所属门店（门店级模版）',
    owner_id        bigint                             not null comment '归属人',
    scope           enum ('Personal', 'Store', 'Company') not null default 'Personal' comment '归属范围',
    status          enum ('Draft', 'Published', 'Disabled') not null default 'Draft' comment '状态',
    current_version_id bigint                          null comment '当前版本（草稿或已发布）',
    source_template_id bigint                          null comment '派生来源模版',
    remark          varchar(512)                       null comment '备注',
    create_date     int             null,
    create_date_time bigint         null,
    create_day      int             null,
    create_hour     int             null,
    create_minute   int             null,
    create_month    int             null,
    create_second   int             null,
    create_time     int             null,
    create_week     tinyint         null,
    create_year     int             null,
    update_date     int             null,
    update_date_time bigint         null,
    update_day      int             null,
    update_hour     int             null,
    update_minute   int             null,
    update_month    int             null,
    update_second   int             null,
    update_time     int             null,
    update_week     tinyint         null,
    update_year     int             null,
    primary key (template_id),
    key idx_design_template_company_scope (com_id, scope, status, template_id),
    key idx_design_template_owner (com_id, owner_id, template_id)
) engine = innodb comment = '设计模版（方案库）';

create table design_template_version
(
    version_id      bigint                             not null comment '模版版本',
    com_id          bigint                             not null comment '所属企业',
    template_id     bigint                             not null comment '所属模版',
    version         int                                not null comment '版本号',
    status          enum ('Draft', 'Published', 'Disabled') not null default 'Draft' comment '状态',
    name            varchar(64)                        not null comment '模版名称（版本快照）',
    house_type      varchar(64)                        not null comment '房型标签（版本快照）',
    area_min        decimal(12, 2)                     null comment '适用套内面积下限',
    area_max        decimal(12, 2)                     null comment '适用套内面积上限',
    tags            varchar(256)                       null comment '风格/用途标签，逗号分隔',
    published_at    datetime(6)                        null comment '发布时间',
    published_by    bigint                             null comment '发布人',
    create_date     int             null,
    create_date_time bigint         null,
    create_day      int             null,
    create_hour     int             null,
    create_minute   int             null,
    create_month    int             null,
    create_second   int             null,
    create_time     int             null,
    create_week     tinyint         null,
    create_year     int             null,
    update_date     int             null,
    update_date_time bigint         null,
    update_day      int             null,
    update_hour     int             null,
    update_minute   int             null,
    update_month    int             null,
    update_second   int             null,
    update_time     int             null,
    update_week     tinyint         null,
    update_year     int             null,
    primary key (version_id),
    unique key uk_design_template_version (com_id, template_id, version),
    key idx_design_template_version_company (com_id, status, version_id)
) engine = innodb comment = '设计模版版本（发布后不可变）';

create table design_template_room
(
    room_id         bigint          not null comment '模版区域',
    com_id          bigint          not null comment '所属企业',
    version_id      bigint          not null comment '所属模版版本',
    room_key        varchar(32)     not null comment '模版内区域唯一键（公式锚点）',
    name            varchar(32)     not null comment '区域名称',
    room_type       enum ('Bedroom', 'Kitchen', 'LivingRoom', 'DiningRoom', 'Bathroom', 'Balcony') not null default 'Bedroom' comment '区域类型',
    sort_no         int             not null default 0 comment '展示顺序',
    length_m        decimal(10, 3)  null comment '开间/长（m）',
    width           decimal(10, 3)  null comment '进深/宽（m）',
    height          decimal(10, 3)  null comment '层高（m）',
    door_width_total  decimal(10, 3) null comment '门洞总宽（m）',
    door_area_total   decimal(10, 3) null comment '门洞总面积（m²）',
    window_area_total decimal(10, 3) null comment '窗洞总面积（m²）',
    wall_rise       decimal(10, 3)  null comment '上返高度（m）',
    room_count      int             not null default 1 comment '同型区域数量',
    remark          varchar(256)    null comment '备注',
    create_date     int             null,
    create_date_time bigint         null,
    create_day      int             null,
    create_hour     int             null,
    create_minute   int             null,
    create_month    int             null,
    create_second   int             null,
    create_time     int             null,
    create_week     tinyint         null,
    create_year     int             null,
    update_date     int             null,
    update_date_time bigint         null,
    update_day      int             null,
    update_hour     int             null,
    update_minute   int             null,
    update_month    int             null,
    update_second   int             null,
    update_time     int             null,
    update_week     tinyint         null,
    update_year     int             null,
    primary key (room_id),
    key idx_design_template_room_version (com_id, version_id, sort_no)
) engine = innodb comment = '设计模版区域骨架';

create table design_template_item
(
    item_id         bigint          not null comment '模版分项',
    com_id          bigint          not null comment '所属企业',
    version_id      bigint          not null comment '所属模版版本',
    room_key        varchar(32)     null comment '归属区域键；为空表示全屋共享项',
    catalog_item_id bigint          not null comment '引用报价项目库条目（价格来源）',
    quantity_formula varchar(512)   null comment '工程量公式（专业变量）',
    default_quantity decimal(12, 3) null comment '无公式时的固定数量',
    sort_no         int             not null default 0 comment '展示顺序',
    remark          varchar(256)    null comment '备注',
    create_date     int             null,
    create_date_time bigint         null,
    create_day      int             null,
    create_hour     int             null,
    create_minute   int             null,
    create_month    int             null,
    create_second   int             null,
    create_time     int             null,
    create_week     tinyint         null,
    create_year     int             null,
    update_date     int             null,
    update_date_time bigint         null,
    update_day      int             null,
    update_hour     int             null,
    update_minute   int             null,
    update_month    int             null,
    update_second   int             null,
    update_time     int             null,
    update_week     tinyint         null,
    update_year     int             null,
    primary key (item_id),
    unique key uk_design_template_item (com_id, version_id, room_key, item_id),
    key idx_design_template_item_version (com_id, version_id, sort_no),
    key idx_design_template_item_catalog (com_id, catalog_item_id)
) engine = innodb comment = '设计模版分项（不承载价格）';

-- 幂等列：同一房屋 + 同一模版版本只允许实例化一次（BR-040 §7）
alter table db_design
    add column design_template_version_id bigint null comment '由哪一版设计模版实例化（幂等键）';

create unique index uk_design_template_instance
    on db_design (com_id, property_id, design_template_version_id);

-- 回滚（本段）：
-- drop index uk_design_template_instance on db_design;
-- alter table db_design drop column design_template_version_id;
