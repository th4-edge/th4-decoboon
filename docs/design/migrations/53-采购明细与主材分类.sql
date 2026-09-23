-- 53-采购明细与主材分类.sql
-- 依据：DEC-MP-03（供应商由财务决定，主材与供应商关联落材料域）、
--       DEC-MP-09（损耗率在企業主材分类上设置，计划行继承）、
--       DEC-MP-02 延伸（退货余款优先冲抵同供应商其他应付，不足转供应商往来余额）。
-- 适用：MySQL 8.0+

-- 1) 采购单明细行：一张采购单一个供应商，可按供应商合并多条材料需求；
--    同一条材料需求也可拆给多个供应商，由多条明细承载。
CREATE TABLE IF NOT EXISTS db_purchase_order_item (
  purchase_item_id BIGINT        NOT NULL COMMENT '采购明细索引',
  create_date        INT         NULL,
  create_date_time   BIGINT      NULL,
  create_day         INT         NULL,
  create_hour        INT         NULL,
  create_minute      INT         NULL,
  create_month       INT         NULL,
  create_second      INT         NULL,
  create_time        INT         NULL,
  create_week        TINYINT     NULL,
  create_year        INT         NULL,
  update_date        INT         NULL,
  update_date_time   BIGINT      NULL,
  update_day         INT         NULL,
  update_hour        INT         NULL,
  update_minute      INT         NULL,
  update_month       INT         NULL,
  update_second      INT         NULL,
  update_time        INT         NULL,
  update_week        TINYINT     NULL,
  update_year        INT         NULL,
  amount             DECIMAL(14,2) NOT NULL COMMENT '采购金额',
  price_source       VARCHAR(32)  NULL COMMENT '价格来源',
  quantity           DECIMAL(12,3) NOT NULL COMMENT '采购数量',
  received_quantity  DECIMAL(12,3) NOT NULL COMMENT '已到场数量',
  remark             VARCHAR(500) NULL COMMENT '调整原因',
  status             ENUM('Draft','Submitted','Approved','Ordered','PartiallyReceived','Received',
                          'Rejected','Cancelled','Closed') NOT NULL COMMENT '明细状态',
  unit               ENUM('Area','Block','Centimeter','Count','Grid','Hole','House','Item',
                          'Metre','Pcs','Place','Plate','Root','Set','Sheet','Step','Stick') NOT NULL COMMENT '单位',
  unit_price         DECIMAL(12,2) NOT NULL COMMENT '采购单价',
  com_id             BIGINT       NOT NULL COMMENT '所属企业',
  contract_id        BIGINT       NULL COMMENT '所属合同',
  material_plan_id   BIGINT       NOT NULL COMMENT '来源材料计划',
  owner_id           BIGINT       NULL COMMENT '经营负责人',
  project_id         BIGINT       NOT NULL COMMENT '所属项目',
  property_id        BIGINT       NOT NULL COMMENT '所属房屋',
  purchase_order_id  BIGINT       NOT NULL COMMENT '所属采购单',
  store_id           BIGINT       NULL COMMENT '所属门店',
  team_id            BIGINT       NULL COMMENT '所属团队',
  PRIMARY KEY (purchase_item_id),
  KEY idx_purchase_item_company (com_id, purchase_item_id),
  KEY idx_purchase_item_company_order (com_id, purchase_order_id, purchase_item_id),
  KEY idx_purchase_item_company_plan (com_id, material_plan_id, purchase_item_id),
  KEY idx_purchase_item_company_store (com_id, store_id, purchase_item_id),
  KEY idx_purchase_item_company_team (com_id, team_id, purchase_item_id),
  KEY idx_purchase_item_company_owner (com_id, owner_id, purchase_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单明细';

-- 存量采购单补一条 1:1 明细，保证历史数据口径一致
INSERT INTO db_purchase_order_item (
  purchase_item_id, amount, quantity, received_quantity, status, unit, unit_price,
  com_id, contract_id, material_plan_id, owner_id, project_id, property_id, purchase_order_id,
  store_id, team_id, create_date_time)
SELECT po.purchase_id, po.amount, po.quantity, po.received_quantity, po.status, po.unit, po.unit_price,
       po.com_id, po.contract_id, po.material_plan_id, po.owner_id, po.project_id, po.property_id,
       po.purchase_id, po.store_id, po.team_id, po.create_date_time
FROM db_purchase_order po
WHERE NOT EXISTS (SELECT 1 FROM db_purchase_order_item i WHERE i.purchase_order_id = po.purchase_id);

-- 采购明细关联到场验收
ALTER TABLE db_goods_receipt
  ADD COLUMN purchase_item_id BIGINT NULL COMMENT '采购明细' AFTER material_plan_id,
  ADD KEY idx_goods_receipt_company_item (com_id, purchase_item_id, receipt_id);

-- 2) 企业主材分类（损耗率来源）
CREATE TABLE IF NOT EXISTS db_material_category (
  material_category_id BIGINT       NOT NULL COMMENT '主材分类索引',
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
  default_loss_rate    DECIMAL(8,4) NOT NULL COMMENT '默认损耗率（百分比，默认0）',
  name                 VARCHAR(64)  NOT NULL COMMENT '分类名称',
  remark               VARCHAR(500) NULL COMMENT '备注',
  sort                 INT          NOT NULL COMMENT '排序',
  com_id               BIGINT       NOT NULL COMMENT '所属企业',
  creator_id           BIGINT       NOT NULL COMMENT '创建人',
  PRIMARY KEY (material_category_id),
  UNIQUE KEY uk_material_category_company_name (com_id, name),
  KEY idx_material_category_company (com_id, material_category_id),
  KEY idx_material_category_company_name (com_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主材分类';

-- 3) 供应商往来余额（退货余款无处冲抵时形成）
CREATE TABLE IF NOT EXISTS db_finance_partner_credit (
  partner_credit_id  BIGINT        NOT NULL COMMENT '往来余额索引',
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
  amount             DECIMAL(14,2) NOT NULL COMMENT '往来余额（可抵扣金额）',
  effective_date     DATE          NULL COMMENT '生效日期',
  remark             VARCHAR(500)  NULL COMMENT '说明',
  source_no          VARCHAR(64)   NULL COMMENT '来源单号（退货单号）',
  com_id             BIGINT        NOT NULL COMMENT '所属企业',
  creator_id         BIGINT        NOT NULL COMMENT '创建人',
  partner_id         BIGINT        NOT NULL COMMENT '供应商',
  PRIMARY KEY (partner_credit_id),
  KEY idx_partner_credit_company (com_id, partner_credit_id),
  KEY idx_partner_credit_company_partner (com_id, partner_id, partner_credit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商往来余额';

-- 回滚（人工确认后执行）：
-- ALTER TABLE db_goods_receipt DROP KEY idx_goods_receipt_company_item, DROP COLUMN purchase_item_id;
-- DROP TABLE IF EXISTS db_finance_partner_credit;
-- DROP TABLE IF EXISTS db_material_category;
-- DROP TABLE IF EXISTS db_purchase_order_item;
