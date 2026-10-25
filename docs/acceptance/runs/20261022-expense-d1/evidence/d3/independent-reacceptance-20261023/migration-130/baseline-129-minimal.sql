CREATE TABLE db_voucher_record (
  voucher_id BIGINT NOT NULL PRIMARY KEY,
  com_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  voucher_no VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO db_voucher_record (voucher_id, com_id, project_id, voucher_no) VALUES
  (1, 10, NULL, 'PZ-BASE-1'),
  (2, 11, 200, 'PZ-BASE-2');
