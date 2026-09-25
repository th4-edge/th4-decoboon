-- AI设计上下文与Prompt编排 P0.1
--
-- 范围：
-- 1) 新增AI设计生成任务、上下文快照和Prompt快照。
-- 2) 只保存新任务，不改写历史方案、报价、合同和财务事实。
--
-- 适用：MySQL 8.0+。执行前备份，生产执行前完成隔离库演练。

CREATE TABLE IF NOT EXISTS db_design_generation_task (
    generation_id BIGINT NOT NULL COMMENT '生成任务索引',
    no VARCHAR(64) NOT NULL COMMENT '任务编号',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    store_id BIGINT NULL COMMENT '所属门店',
    team_id BIGINT NULL COMMENT '所属团队',
    emp_id BIGINT NOT NULL COMMENT '负责设计师',
    customer_id BIGINT NOT NULL COMMENT '客户',
    property_id BIGINT NOT NULL COMMENT '所属房屋',
    design_id BIGINT NOT NULL COMMENT '设计方案',
    selected_render_id BIGINT NULL COMMENT '选中的效果图版本',
    mode VARCHAR(16) NOT NULL COMMENT 'Precise/Concept',
    status VARCHAR(32) NOT NULL COMMENT '生成任务状态',
    version INT NOT NULL DEFAULT 1 COMMENT '任务版本',
    source_digest VARCHAR(64) NULL COMMENT '来源摘要',
    model_name VARCHAR(128) NULL COMMENT '模型名称',
    failure_code VARCHAR(64) NULL COMMENT '失败编码',
    failure_message VARCHAR(512) NULL COMMENT '失败说明',
    built_at DATETIME(6) NULL COMMENT '构建时间',
    completed_at DATETIME(6) NULL COMMENT '完成时间',
    deliverable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否正式可交付',
    confirmed_by BIGINT NULL COMMENT '确认人',
    confirmed_at DATETIME(6) NULL COMMENT '确认时间',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (generation_id),
    UNIQUE KEY uk_design_generation_no (no),
    KEY idx_design_generation_company (com_id, generation_id),
    KEY idx_design_generation_company_store (com_id, store_id, generation_id),
    KEY idx_design_generation_company_team (com_id, team_id, generation_id),
    KEY idx_design_generation_company_owner (com_id, emp_id, generation_id),
    KEY idx_design_generation_company_design (com_id, design_id, generation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI设计生成任务';

CREATE TABLE IF NOT EXISTS db_design_context_snapshot (
    snapshot_id BIGINT NOT NULL COMMENT '上下文快照索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    store_id BIGINT NULL COMMENT '所属门店',
    team_id BIGINT NULL COMMENT '所属团队',
    emp_id BIGINT NOT NULL COMMENT '负责设计师',
    customer_id BIGINT NOT NULL COMMENT '客户',
    property_id BIGINT NOT NULL COMMENT '所属房屋',
    design_id BIGINT NOT NULL COMMENT '设计方案',
    generation_id BIGINT NOT NULL COMMENT '生成任务',
    context_json LONGTEXT NOT NULL COMMENT '结构化上下文JSON',
    source_digest VARCHAR(64) NOT NULL COMMENT '来源摘要',
    missing_fields_json LONGTEXT NULL COMMENT '缺失字段JSON',
    conflict_flags_json LONGTEXT NULL COMMENT '冲突标记JSON',
    confidence VARCHAR(16) NOT NULL COMMENT 'High/Medium/Low',
    captured_at DATETIME(6) NOT NULL COMMENT '采集时间',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (snapshot_id),
    KEY idx_design_context_company (com_id, snapshot_id),
    KEY idx_design_context_company_task (com_id, generation_id, snapshot_id),
    KEY idx_design_context_company_design (com_id, design_id, snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI设计上下文快照';

CREATE TABLE IF NOT EXISTS db_design_prompt_snapshot (
    prompt_id BIGINT NOT NULL COMMENT 'Prompt快照索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    store_id BIGINT NULL COMMENT '所属门店',
    team_id BIGINT NULL COMMENT '所属团队',
    emp_id BIGINT NOT NULL COMMENT '负责设计师',
    customer_id BIGINT NOT NULL COMMENT '客户',
    property_id BIGINT NOT NULL COMMENT '所属房屋',
    design_id BIGINT NOT NULL COMMENT '设计方案',
    generation_id BIGINT NOT NULL COMMENT '生成任务',
    snapshot_id BIGINT NOT NULL COMMENT '上下文快照',
    visual_prompt LONGTEXT NOT NULL COMMENT '视觉Prompt',
    talk_prompt LONGTEXT NOT NULL COMMENT '谈单Prompt',
    document_brief_json LONGTEXT NOT NULL COMMENT '方案书结构化内容JSON',
    template_version VARCHAR(32) NOT NULL COMMENT '模板版本',
    model_name VARCHAR(128) NULL COMMENT '模型名称',
    model_parameters_json LONGTEXT NULL COMMENT '模型参数JSON',
    rendered_at DATETIME(6) NOT NULL COMMENT '渲染时间',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (prompt_id),
    KEY idx_design_prompt_company (com_id, prompt_id),
    KEY idx_design_prompt_company_task (com_id, generation_id, prompt_id),
    KEY idx_design_prompt_company_design (com_id, design_id, prompt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI设计Prompt快照';

CREATE TABLE IF NOT EXISTS db_design_render_version (
    render_id BIGINT NOT NULL COMMENT '效果图版本索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    store_id BIGINT NULL COMMENT '所属门店',
    team_id BIGINT NULL COMMENT '所属团队',
    emp_id BIGINT NOT NULL COMMENT '负责设计师',
    customer_id BIGINT NOT NULL COMMENT '客户',
    property_id BIGINT NOT NULL COMMENT '所属房屋',
    design_id BIGINT NOT NULL COMMENT '设计方案',
    generation_id BIGINT NOT NULL COMMENT '生成任务',
    prompt_id BIGINT NOT NULL COMMENT 'Prompt快照',
    sequence INT NOT NULL COMMENT '版本序号',
    status VARCHAR(16) NOT NULL COMMENT 'Ready/Failed/Superseded',
    image_url VARCHAR(1024) NULL COMMENT '效果图地址',
    model_name VARCHAR(128) NULL COMMENT '模型名称',
    request_id VARCHAR(128) NULL COMMENT '模型请求号',
    input_image_count INT NOT NULL DEFAULT 0 COMMENT '输入图片数',
    output_image_count INT NOT NULL DEFAULT 0 COMMENT '输出图片数',
    usage_json LONGTEXT NULL COMMENT '模型用量JSON',
    cost_amount DECIMAL(12,4) NULL COMMENT '模型费用',
    cost_currency VARCHAR(8) NULL COMMENT '费用币种',
    started_at DATETIME(6) NULL COMMENT '开始时间',
    completed_at DATETIME(6) NULL COMMENT '完成时间',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (render_id),
    KEY idx_design_render_company (com_id, render_id),
    KEY idx_design_render_company_task (com_id, generation_id, render_id),
    KEY idx_design_render_company_design (com_id, design_id, render_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI效果图版本';

ALTER TABLE db_design_generation_task
    ADD CONSTRAINT fk_design_generation_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_design_generation_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    ADD CONSTRAINT fk_design_generation_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    ADD CONSTRAINT fk_design_generation_owner FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    ADD CONSTRAINT fk_design_generation_customer FOREIGN KEY (customer_id) REFERENCES cust_customer (cust_id),
    ADD CONSTRAINT fk_design_generation_property FOREIGN KEY (property_id) REFERENCES db_property (property_id),
    ADD CONSTRAINT fk_design_generation_design FOREIGN KEY (design_id) REFERENCES db_design (design_id);

ALTER TABLE db_design_context_snapshot
    ADD CONSTRAINT fk_design_context_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_design_context_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    ADD CONSTRAINT fk_design_context_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    ADD CONSTRAINT fk_design_context_owner FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    ADD CONSTRAINT fk_design_context_customer FOREIGN KEY (customer_id) REFERENCES cust_customer (cust_id),
    ADD CONSTRAINT fk_design_context_property FOREIGN KEY (property_id) REFERENCES db_property (property_id),
    ADD CONSTRAINT fk_design_context_design FOREIGN KEY (design_id) REFERENCES db_design (design_id),
    ADD CONSTRAINT fk_design_context_task FOREIGN KEY (generation_id) REFERENCES db_design_generation_task (generation_id);

ALTER TABLE db_design_prompt_snapshot
    ADD CONSTRAINT fk_design_prompt_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_design_prompt_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    ADD CONSTRAINT fk_design_prompt_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    ADD CONSTRAINT fk_design_prompt_owner FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    ADD CONSTRAINT fk_design_prompt_customer FOREIGN KEY (customer_id) REFERENCES cust_customer (cust_id),
    ADD CONSTRAINT fk_design_prompt_property FOREIGN KEY (property_id) REFERENCES db_property (property_id),
    ADD CONSTRAINT fk_design_prompt_design FOREIGN KEY (design_id) REFERENCES db_design (design_id),
    ADD CONSTRAINT fk_design_prompt_task FOREIGN KEY (generation_id) REFERENCES db_design_generation_task (generation_id),
    ADD CONSTRAINT fk_design_prompt_context FOREIGN KEY (snapshot_id) REFERENCES db_design_context_snapshot (snapshot_id);

ALTER TABLE db_design_render_version
    ADD CONSTRAINT fk_design_render_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_design_render_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    ADD CONSTRAINT fk_design_render_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    ADD CONSTRAINT fk_design_render_owner FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    ADD CONSTRAINT fk_design_render_customer FOREIGN KEY (customer_id) REFERENCES cust_customer (cust_id),
    ADD CONSTRAINT fk_design_render_property FOREIGN KEY (property_id) REFERENCES db_property (property_id),
    ADD CONSTRAINT fk_design_render_design FOREIGN KEY (design_id) REFERENCES db_design (design_id),
    ADD CONSTRAINT fk_design_render_task FOREIGN KEY (generation_id) REFERENCES db_design_generation_task (generation_id),
    ADD CONSTRAINT fk_design_render_prompt FOREIGN KEY (prompt_id) REFERENCES db_design_prompt_snapshot (prompt_id),
    ADD CONSTRAINT fk_design_render_confirmer FOREIGN KEY (confirmed_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE db_design_generation_task
    ADD CONSTRAINT fk_design_generation_selected_render FOREIGN KEY (selected_render_id) REFERENCES db_design_render_version (render_id);
