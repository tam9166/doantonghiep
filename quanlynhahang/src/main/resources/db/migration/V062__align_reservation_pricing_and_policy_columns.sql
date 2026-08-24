IF OBJECT_ID(N'dbo.reservations', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.reservations', N'preorder_enabled') IS NULL
        ALTER TABLE dbo.reservations ADD preorder_enabled BIT NOT NULL
            CONSTRAINT df_reservations_preorder_enabled DEFAULT 0 WITH VALUES;

    IF COL_LENGTH(N'dbo.reservations', N'table_amount') IS NULL
        ALTER TABLE dbo.reservations ADD table_amount DECIMAL(18,0) NOT NULL
            CONSTRAINT df_reservations_table_amount DEFAULT 0 WITH VALUES;

    IF COL_LENGTH(N'dbo.reservations', N'food_amount') IS NULL
        ALTER TABLE dbo.reservations ADD food_amount DECIMAL(18,0) NOT NULL
            CONSTRAINT df_reservations_food_amount DEFAULT 0 WITH VALUES;

    IF COL_LENGTH(N'dbo.reservations', N'payment_option') IS NULL
        ALTER TABLE dbo.reservations ADD payment_option VARCHAR(30) NOT NULL
            CONSTRAINT df_reservations_payment_option DEFAULT 'DEPOSIT_50' WITH VALUES;

    IF COL_LENGTH(N'dbo.reservations', N'deposit_policy_code') IS NULL
        ALTER TABLE dbo.reservations ADD deposit_policy_code VARCHAR(40) NULL;

    IF COL_LENGTH(N'dbo.reservations', N'deposit_policy_snapshot') IS NULL
        ALTER TABLE dbo.reservations ADD deposit_policy_snapshot NVARCHAR(MAX) NULL;
END;
