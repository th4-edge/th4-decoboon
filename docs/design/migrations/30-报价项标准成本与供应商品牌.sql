-- 报价项标准成本与供应商品牌关系。
--
-- 1) catalog_item.cost_price 已存在，作为企业标准成本的直接维护字段；
--    不再要求 PartnerPrice 逐项形成标准成本。
-- 2) 新增供应商-品牌关系，用于材料采购时按品牌匹配供应商。

CREATE TABLE IF NOT EXISTS db_partner_brand (
    partner_id BIGINT NOT NULL COMMENT '供应商/合作方',
    brand VARCHAR(128) NOT NULL COMMENT '可供应品牌',
    PRIMARY KEY (partner_id, brand),
    KEY idx_partner_brand_brand (brand, partner_id),
    CONSTRAINT fk_partner_brand_partner FOREIGN KEY (partner_id)
        REFERENCES db_partner (partner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商可供应品牌关系';
