-- 混合报价：记录套餐基础行与设计分项的差异类型和确认状态。

ALTER TABLE quote_line
    ADD COLUMN difference_type VARCHAR(24) NOT NULL DEFAULT 'None',
    ADD COLUMN difference_status VARCHAR(16) NOT NULL DEFAULT 'None';

CREATE INDEX idx_quote_line_company_template_difference
    ON quote_line (com_id, quote_id, difference_status, item_id);
