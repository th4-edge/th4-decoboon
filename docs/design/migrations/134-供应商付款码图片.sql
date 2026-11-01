-- 134 · 供应商付款码图片
-- 用途：付款前核对供应商收款主体与付款码；只保存私有文件对象键，访问必须经 file/private/grant。
-- 口径：BR-019 / BR-045；维护角色 BOSS、店长、财务。

CREATE TABLE IF NOT EXISTS db_partner_payment_code_image (
    partner_id BIGINT NOT NULL COMMENT '供应商/合作方主键',
    url VARCHAR(512) NOT NULL COMMENT '私有文件对象键',
    sort_no INT NOT NULL COMMENT '展示顺序',
    KEY idx_partner_payment_code_image (partner_id, sort_no),
    CONSTRAINT fk_partner_payment_code_image_partner
        FOREIGN KEY (partner_id) REFERENCES db_partner (partner_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='供应商付款码图片';
