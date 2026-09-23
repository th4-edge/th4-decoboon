-- 51-材料计划与供应商下单.sql
-- 依据 2026-09-23 用户裁决 DEC-MP-01～10：
--   DEC-MP-01 下单形成应付，到货回填账期，成本仍在到货确认
--   DEC-MP-02 已到货的扣减必须走退货单
--   DEC-MP-03 供应商由财务在下单时决定
--   DEC-MP-04 瓷砖等材料必须有损耗（采购损耗计入成本，施工损耗只流转库存）
--   DEC-MP-05 计划行与选材确认绑定，确认后才可下单
--   DEC-MP-06 预计成本以合同成本价
--   DEC-MP-07 到场确认 PM 或设计负责人任一方
--   DEC-MP-08 选材文本不参与供应关系匹配
--   DEC-MP-09 损耗率在企业主材分类中设置
--   DEC-MP-10 下单量不得超过净用量+损耗
-- 适用：MySQL 8.0+

-- 1) 材料计划：来源、选材快照、损耗、扣减引用
ALTER TABLE db_material_plan
  ADD COLUMN source_type VARCHAR(24) NULL COMMENT '材料来源：Selection/ChangeAdd/ChangeReduce' AFTER status,
  ADD COLUMN selection_id BIGINT NULL COMMENT '确认后的选材结果版本' AFTER source_type,
  ADD COLUMN selected_brand VARCHAR(128) NULL COMMENT '选材品牌（结构化）' AFTER selection_id,
  ADD COLUMN selected_spec VARCHAR(256) NULL COMMENT '选材规格（结构化）' AFTER selected_brand,
  ADD COLUMN selected_model VARCHAR(128) NULL COMMENT '选材型号（结构化）' AFTER selected_spec,
  ADD COLUMN material_key VARCHAR(512) NULL COMMENT '结构化材料标识（供应商匹配）' AFTER selected_model,
  ADD COLUMN net_quantity DECIMAL(12,2) NULL COMMENT '净用量' AFTER material_key,
  ADD COLUMN loss_rate DECIMAL(8,4) NULL COMMENT '损耗率（来源主材分类，默认0）' AFTER net_quantity,
  ADD COLUMN loss_quantity DECIMAL(12,2) NULL COMMENT '损耗量；计划数量=净用量+损耗量' AFTER loss_rate,
  ADD COLUMN reduce_of_plan_id BIGINT NULL COMMENT '扣减行指向的被冲减原计划' AFTER loss_quantity,
  ADD COLUMN remark VARCHAR(500) NULL COMMENT '数量/价格调整原因' AFTER reduce_of_plan_id;

-- 状态口径扩展：新增 可下单/部分下单/已取消
ALTER TABLE db_material_plan
  MODIFY COLUMN status ENUM('Planned','ReadyToOrder','Requested','Approved','PartiallyOrdered',
                            'Ordered','PartiallyArrived','Arrived','Closed','Cancelled') NOT NULL;

-- 历史口径归一：Requested/Approved 统一并入 ReadyToOrder（选材确认才可下单由应用层门禁保证）
UPDATE db_material_plan SET status = 'ReadyToOrder' WHERE status IN ('Requested','Approved');

ALTER TABLE db_material_plan
  ADD KEY idx_material_plan_company_project (com_id, project_id, plan_id),
  ADD KEY idx_material_plan_company_material_key (com_id, material_key, plan_id);

-- 2) 材料-供应商供应关系（主材与供应商关联沉淀在材料/供应链域，不落报价项目库）
CREATE TABLE IF NOT EXISTS db_material_supplier (
  material_supplier_id BIGINT       NOT NULL COMMENT '材料供应关系索引',
  create_date          INT          NULL,
  create_date_time     BIGINT       NULL,
  create_day           INT          NULL,
  create_hour          INT          NULL,
  create_minute        INT          NULL,
  create_month         INT          NULL,
  create_second        INT          NULL,
  create_time          INT          NULL,
  create_week          TINYINT      NULL,
  create_year          INT          NULL,
  update_date          INT          NULL,
  update_date_time     BIGINT       NULL,
  update_day           INT          NULL,
  update_hour          INT          NULL,
  update_minute        INT          NULL,
  update_month         INT          NULL,
  update_second        INT          NULL,
  update_time          INT          NULL,
  update_week          TINYINT      NULL,
  update_year          INT          NULL,
  active               BIT          NOT NULL COMMENT '是否启用',
  brand                VARCHAR(128) NULL COMMENT '品牌',
  effective_from       DATE         NULL COMMENT '生效日期',
  effective_to         DATE         NULL COMMENT '失效日期',
  lead_time_days       INT          NOT NULL COMMENT '交货天数',
  material_key         VARCHAR(512) NOT NULL COMMENT '结构化材料标识',
  material_name        VARCHAR(128) NULL COMMENT '材料名称',
  minimum_order        DECIMAL(12,3) NOT NULL COMMENT '最小起订量',
  model                VARCHAR(128) NULL COMMENT '型号',
  purchase_price       DECIMAL(12,2) NOT NULL COMMENT '参考采购价',
  remark               VARCHAR(500) NULL COMMENT '备注',
  settlement_days      INT          NULL COMMENT '账期天数（下单时快照到应付）',
  spec                 VARCHAR(256) NULL COMMENT '规格',
  unit                 VARCHAR(32)  NULL COMMENT '单位',
  catalog_item_id      BIGINT       NULL COMMENT '报价项目库来源（可空）',
  com_id               BIGINT       NOT NULL COMMENT '所属企业',
  creator_id           BIGINT       NOT NULL COMMENT '创建人',
  partner_id           BIGINT       NOT NULL COMMENT '供应商',
  PRIMARY KEY (material_supplier_id),
  KEY idx_material_supplier_company (com_id, material_supplier_id),
  KEY idx_material_supplier_company_key (com_id, material_key, material_supplier_id),
  KEY idx_material_supplier_company_partner (com_id, partner_id, material_supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='材料-供应商供应关系';

-- 3) 应付关联采购单（下单即形成应付；到货只回填账期）
ALTER TABLE db_finance_payable
  ADD COLUMN purchase_order_id BIGINT NULL COMMENT '来源采购单（下单即形成应付）' AFTER project_id,
  ADD KEY idx_fin_payable_purchase_order (com_id, purchase_order_id, payable_id);

-- 回滚（人工确认后执行）：
-- ALTER TABLE db_finance_payable DROP KEY idx_fin_payable_purchase_order, DROP COLUMN purchase_order_id;
-- DROP TABLE IF EXISTS db_material_supplier;
-- ALTER TABLE db_material_plan DROP KEY idx_material_plan_company_material_key, DROP KEY idx_material_plan_company_project;
-- ALTER TABLE db_material_plan
--   DROP COLUMN remark, DROP COLUMN reduce_of_plan_id, DROP COLUMN loss_quantity, DROP COLUMN loss_rate,
--   DROP COLUMN net_quantity, DROP COLUMN material_key, DROP COLUMN selected_model, DROP COLUMN selected_spec,
--   DROP COLUMN selected_brand, DROP COLUMN selection_id, DROP COLUMN source_type;
-- ALTER TABLE db_material_plan MODIFY COLUMN status ENUM('Planned','Requested','Approved','Ordered','PartiallyArrived','Arrived','Closed') NOT NULL;
