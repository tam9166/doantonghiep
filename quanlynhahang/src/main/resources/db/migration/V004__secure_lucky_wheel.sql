IF OBJECT_ID(N'dbo.wheel_spin_history', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wheel_spin_history (
        id BIGINT IDENTITY(1,1) NOT NULL,
        username VARCHAR(50) NOT NULL,
        spin_date DATE NOT NULL,
        reward_type VARCHAR(30) NOT NULL,
        reward_value INT NOT NULL,
        voucher_code VARCHAR(80) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_wheel_spin_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT pk_wheel_spin_history PRIMARY KEY (id),
        CONSTRAINT uk_wheel_spin_account_date UNIQUE (username, spin_date),
        CONSTRAINT fk_wheel_spin_account FOREIGN KEY (username) REFERENCES dbo.Accounts(username)
    );

    CREATE INDEX ix_wheel_spin_created_at
        ON dbo.wheel_spin_history(created_at DESC);
END;
