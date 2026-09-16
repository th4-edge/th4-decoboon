-- 用户密码登录迁移（存量库执行；新空库由 01-空库基线.sql 包含）
-- 仅补充凭证字段、凭证类型和安全日志动作，不写入任何明文密码。

ALTER TABLE db_tenant_credentials
    MODIFY COLUMN type ENUM('Wx', 'Password') NOT NULL,
    ADD COLUMN password_hash VARCHAR(100) NULL,
    ADD COLUMN password_updated_at DATETIME(6) NULL,
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME(6) NULL,
    ADD COLUMN must_change_password BIT NOT NULL DEFAULT b'0';

ALTER TABLE db_user_security_log
    MODIFY COLUMN action ENUM(
        'AccountLock',
        'AccountUnlock',
        'BossTransfer',
        'CompanyJoin',
        'CompanyResign',
        'CompanySwitch',
        'InviteAccept',
        'InviteReject',
        'LimitTrigger',
        'Login',
        'Logout',
        'PasswordChange',
        'PasswordSet',
        'PhoneBind',
        'Register'
    ) NOT NULL;
