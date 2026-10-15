-- 117-主材下单作业单
-- 日期：2026-10-15
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 依据（业务事实源：装得清业务智能体）：
--   · SOL-014 主材下单作业单与行业标准单据体系方案（approved）
--   · OBJ-008 主材下单作业单与单据模板（approved）
--   · BR-044 主材下单作业单与行业标准基线规则（approved）
--   · BR-045 采购价、无价下单与结算价成本口径规则（approved）
--   · SCN-021 主材下单作业单成单、到货与结算（approved）
--   · DEC-072（无价下单形成暂估应付；合同外/标配外走增减项；Excel 导出；成本偏差为正式能力；结算价不沉淀）
--   · DEC-073（作业单与采购单＝两张单；结算优先级＝互斥 + 逐单优先 + 月结兜底）
--   · T12-BIZ-20261015-003（P0 瓷砖打样研发交接）
--
-- ⚠ 发布耦合（重要）：本文件包含**既有表的加列**（db_purchase_order / db_purchase_order_item /
--   db_finance_payable）与实体新增字段。生产 `ddl-auto=validate`，**代码与本迁移必须同批发布**，
--   发布前必须把 scripts/build-release.sh 的 MIGRATION_END 由 116 上调到 117；
--   否则生产启动会因「missing column」失败。
--
-- 回滚：见 docs/design/migrations/rollback/117-主材下单作业单-rollback.sql

-- ============================================================================
-- 1) 企业级：主材下单单据模板
-- ============================================================================
CREATE TABLE IF NOT EXISTS db_material_sheet_template (
  sheet_template_id    BIGINT        NOT NULL COMMENT '模板索引',
  create_date          INT           NULL,
  create_date_time     BIGINT        NULL,
  create_day           INT           NULL,
  create_hour          INT           NULL,
  create_minute        INT           NULL,
  create_month         INT           NULL,
  create_second        INT           NULL,
  create_time          INT           NULL,
  create_week          TINYINT       NULL,
  create_year          INT           NULL,
  update_date          INT           NULL,
  update_date_time     BIGINT        NULL,
  update_day           INT           NULL,
  update_hour          INT           NULL,
  update_minute        INT           NULL,
  update_month         INT           NULL,
  update_second        INT           NULL,
  update_time          INT           NULL,
  update_week          TINYINT       NULL,
  update_year          INT           NULL,
  name                 VARCHAR(128)  NOT NULL COMMENT '模板名称',
  sheet_type           VARCHAR(64)   NOT NULL COMMENT '单据类型编码（Tile 等）',
  scope                ENUM('Company','Platform') NOT NULL COMMENT '模板范围：平台行业基线/企业自定义',
  baseline_version     INT           NOT NULL COMMENT '继承基线版本',
  version              INT           NOT NULL COMMENT '模板版本',
  published_version    INT           NOT NULL COMMENT '当前发布版本，0=未发布',
  field_config         LONGTEXT      NOT NULL COMMENT '字段配置JSON（受控白名单）',
  row_template_config  LONGTEXT      NULL COMMENT '标准行骨架JSON',
  rule_config          LONGTEXT      NULL COMMENT '企业默认规则JSON',
  signature_config     LONGTEXT      NULL COMMENT '签名位JSON',
  number_rule          VARCHAR(128)  NULL COMMENT '单据编号规则',
  status               ENUM('Active','Disabled') NOT NULL COMMENT '模板状态',
  com_id               BIGINT        NOT NULL COMMENT '所属企业',
  material_category_id BIGINT        NULL COMMENT '关联主材分类',
  print_template_id    BIGINT        NULL COMMENT '关联 DocumentTemplate',
  creator_id           BIGINT        NOT NULL COMMENT '创建人',
  PRIMARY KEY (sheet_template_id),
  UNIQUE KEY uk_mst_company_type_version (com_id, sheet_type, version),
  KEY idx_mst_company (com_id, sheet_template_id),
  KEY idx_mst_company_scope (com_id, scope, sheet_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业主材下单单据模板';

-- ============================================================================
-- 2) 项目级：主材下单作业单（对厂家：不带价格与账期）
-- ============================================================================
CREATE TABLE IF NOT EXISTS db_material_order_sheet (
  sheet_id             BIGINT        NOT NULL COMMENT '作业单索引',
  create_date          INT           NULL,
  create_date_time     BIGINT        NULL,
  create_day           INT           NULL,
  create_hour          INT           NULL,
  create_minute        INT           NULL,
  create_month         INT           NULL,
  create_second        INT           NULL,
  create_time          INT           NULL,
  create_week          TINYINT       NULL,
  create_year          INT           NULL,
  update_date          INT           NULL,
  update_date_time     BIGINT        NULL,
  update_day           INT           NULL,
  update_hour          INT           NULL,
  update_minute        INT           NULL,
  update_month         INT           NULL,
  update_second        INT           NULL,
  update_time          INT           NULL,
  update_week          TINYINT       NULL,
  update_year          INT           NULL,
  sheet_no             VARCHAR(64)   NOT NULL COMMENT '单据编号',
  sheet_type           VARCHAR(64)   NOT NULL COMMENT '单据类型编码',
  manufacturer_name    VARCHAR(128)  NULL COMMENT '厂家名称文本',
  measure_at           DATE          NULL COMMENT '测量时间',
  group_chat_at        DATE          NULL COMMENT '拉群时间',
  deliver_at           DATE          NULL COMMENT '配送时间',
  install_at           DATE          NULL COMMENT '安装/送货时间',
  template_version     INT           NULL COMMENT '生成时冻结的模板版本',
  status               ENUM('Arrived','Cancelled','Closed','Draft','Measured','Measuring','Ordered',
                            'PartiallyArrived','Pricing','Settled','Submitted') NOT NULL COMMENT '作业单状态',
  price_state          ENUM('Mixed','Priced','Settled','Unpriced') NOT NULL COMMENT '价格状态（汇总态，行级为准）',
  signatures           LONGTEXT      NULL COMMENT '签名位确认人与时间JSON',
  remark               VARCHAR(500)  NULL COMMENT '备注',
  com_id               BIGINT        NOT NULL COMMENT '所属企业',
  contract_id          BIGINT        NOT NULL COMMENT '所属合同',
  creator_id           BIGINT        NOT NULL COMMENT '创建人',
  material_category_id BIGINT        NULL COMMENT '主材分类',
  owner_id             BIGINT        NULL COMMENT '经营负责人',
  project_id           BIGINT        NOT NULL COMMENT '所属项目',
  property_id          BIGINT        NOT NULL COMMENT '所属房屋',
  store_id             BIGINT        NULL COMMENT '所属门店',
  supplier_id          BIGINT        NULL COMMENT '供应商（设计师阶段可空）',
  team_id              BIGINT        NULL COMMENT '所属团队',
  template_id          BIGINT        NULL COMMENT '所用单据模板',
  PRIMARY KEY (sheet_id),
  UNIQUE KEY uk_mos_company_no (com_id, sheet_no),
  KEY idx_mos_company (com_id, sheet_id),
  KEY idx_mos_company_project (com_id, project_id, sheet_id),
  KEY idx_mos_company_project_status (com_id, project_id, status, sheet_id),
  KEY idx_mos_company_store (com_id, store_id, sheet_id),
  KEY idx_mos_company_team (com_id, team_id, sheet_id),
  KEY idx_mos_company_owner (com_id, owner_id, sheet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目主材下单作业单';

CREATE TABLE IF NOT EXISTS db_material_order_sheet_attachments (
  sheet_id BIGINT       NOT NULL,
  url      VARCHAR(512) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业单附件';

-- ============================================================================
-- 3) 作业单行（行来源只有 Plan：合同选材确认 / 增减项新增）
-- ============================================================================
CREATE TABLE IF NOT EXISTS db_material_order_sheet_item (
  item_id            BIGINT        NOT NULL COMMENT '作业单行索引',
  create_date        INT           NULL,
  create_date_time   BIGINT        NULL,
  create_day         INT           NULL,
  create_hour        INT           NULL,
  create_minute      INT           NULL,
  create_month       INT           NULL,
  create_second      INT           NULL,
  create_time        INT           NULL,
  create_week        TINYINT       NULL,
  create_year        INT           NULL,
  update_date        INT           NULL,
  update_date_time   BIGINT        NULL,
  update_day         INT           NULL,
  update_hour        INT           NULL,
  update_minute      INT           NULL,
  update_month       INT           NULL,
  update_second      INT           NULL,
  update_time        INT           NULL,
  update_week        TINYINT       NULL,
  update_year        INT           NULL,
  source_kind        ENUM('Plan')  NOT NULL COMMENT '行来源（仅 Plan；合同外走增减项）',
  standard_included  BIT           NOT NULL COMMENT '是否套餐/合同标配含（标配不额外计价）',
  space_name         VARCHAR(128)  NULL COMMENT '空间',
  location_name      VARCHAR(128)  NULL COMMENT '安装位置',
  material_name      VARCHAR(128)  NULL COMMENT '材料名称',
  brand              VARCHAR(128)  NULL COMMENT '品牌',
  model              VARCHAR(128)  NULL COMMENT '型号',
  spec               VARCHAR(256)  NULL COMMENT '规格',
  color              VARCHAR(128)  NULL COMMENT '颜色/花色',
  size               VARCHAR(128)  NULL COMMENT '尺寸',
  craft              VARCHAR(128)  NULL COMMENT '工艺',
  glass              VARCHAR(128)  NULL COMMENT '玻璃',
  quantity           DECIMAL(12,2) NOT NULL COMMENT '数量',
  unit               ENUM('Area','Block','Centimeter','Count','Grid','Hole','House','Item',
                          'Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL COMMENT '单位',
  loss_rate          DECIMAL(8,4)  NOT NULL COMMENT '损耗率（继承主材分类，默认0）',
  loss_quantity      DECIMAL(12,2) NOT NULL COMMENT '损耗量',
  quote_cost_price   DECIMAL(12,2) NULL COMMENT '报价成本价快照（只读，用于预计成本对照）',
  purchase_price     DECIMAL(12,2) NULL COMMENT '采购价（可为空，禁止用0冒充）',
  settlement_price   DECIMAL(12,2) NULL COMMENT '结算价',
  price_state        ENUM('Mixed','Priced','Settled','Unpriced') NOT NULL COMMENT '价格状态',
  sort               INT           NOT NULL COMMENT '排序',
  remark             VARCHAR(500)  NULL COMMENT '备注',
  com_id             BIGINT        NOT NULL COMMENT '所属企业',
  material_plan_id   BIGINT        NOT NULL COMMENT '来源材料计划行',
  owner_id           BIGINT        NULL COMMENT '经营负责人',
  project_id         BIGINT        NOT NULL COMMENT '所属项目',
  property_id        BIGINT        NOT NULL COMMENT '所属房屋',
  sheet_id           BIGINT        NOT NULL COMMENT '所属作业单',
  store_id           BIGINT        NULL COMMENT '所属门店',
  team_id            BIGINT        NULL COMMENT '所属团队',
  PRIMARY KEY (item_id),
  KEY idx_mosi_company_sheet (com_id, sheet_id, sort),
  KEY idx_mosi_company_plan (com_id, material_plan_id),
  KEY idx_mosi_company_project (com_id, project_id, item_id),
  KEY idx_mosi_company (com_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目主材下单作业单行';

CREATE TABLE IF NOT EXISTS db_material_order_sheet_item_attachments (
  item_id BIGINT       NOT NULL,
  url     VARCHAR(512) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业单行附件';

-- ============================================================================
-- 4) 既有表加列：价格状态与暂估应付（BR-045 / DEC-072）
-- ============================================================================
-- 采购单：价格状态（存量数据默认 Priced，不改变历史口径）
ALTER TABLE db_purchase_order
  ADD COLUMN price_state ENUM('Mixed','Priced','Settled','Unpriced') NOT NULL DEFAULT 'Priced' COMMENT '价格状态：有价/无价（待结算）';

-- 采购明细：价格状态 + 结算价（结算价是实际成本的唯一来源，P3 > P2）
ALTER TABLE db_purchase_order_item
  ADD COLUMN price_state ENUM('Mixed','Priced','Settled','Unpriced') NOT NULL DEFAULT 'Priced' COMMENT '价格状态',
  ADD COLUMN settlement_price DECIMAL(12,2) NULL COMMENT '结算价（对账结算确认的真实价）';

-- 应付：暂估标记 + 结算确定金额
-- 说明：amount 保持 NOT NULL（既有约束），暂估期 amount=0 且 estimated=1；
--       结算后回填 settled_amount，修正留痕，不覆盖历史。
ALTER TABLE db_finance_payable
  ADD COLUMN estimated BIT NOT NULL DEFAULT 0 COMMENT '是否暂估应付（无价下单）',
  ADD COLUMN settled_amount DECIMAL(14,2) NULL COMMENT '结算确定金额';

-- ============================================================================
-- 5) 结算记录（P3）：互斥 + 逐单优先 + 月结兜底（DEC-073）
--    P0 只落采购/应付的价格状态与结算价字段；结算单实体在 P3 实现，本迁移不建结算表。
-- ============================================================================
