-- 报价成本完整性：成本缺失或全部为 0 时不得据此形成有效利润口径。

ALTER TABLE quote
    ADD COLUMN cost_complete bit NOT NULL DEFAULT b'0';
