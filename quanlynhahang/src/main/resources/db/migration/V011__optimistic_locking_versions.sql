IF COL_LENGTH('dbo.Orders', 'version') IS NULL
    ALTER TABLE dbo.Orders ADD version BIGINT NOT NULL
        CONSTRAINT df_orders_version DEFAULT 0 WITH VALUES;

IF COL_LENGTH('dbo.reservations', 'version') IS NULL
    ALTER TABLE dbo.reservations ADD version BIGINT NOT NULL
        CONSTRAINT df_reservations_version DEFAULT 0 WITH VALUES;

IF COL_LENGTH('dbo.restaurant_table', 'version') IS NULL
    ALTER TABLE dbo.restaurant_table ADD version BIGINT NOT NULL
        CONSTRAINT df_restaurant_table_version DEFAULT 0 WITH VALUES;

IF COL_LENGTH('dbo.vouchers', 'version') IS NULL
    ALTER TABLE dbo.vouchers ADD version BIGINT NOT NULL
        CONSTRAINT df_vouchers_version DEFAULT 0 WITH VALUES;

IF COL_LENGTH('dbo.Accounts', 'version') IS NULL
    ALTER TABLE dbo.Accounts ADD version BIGINT NOT NULL
        CONSTRAINT df_accounts_version DEFAULT 0 WITH VALUES;

IF COL_LENGTH('dbo.ingredient_batches', 'version') IS NULL
    ALTER TABLE dbo.ingredient_batches ADD version BIGINT NOT NULL
        CONSTRAINT df_ingredient_batches_version DEFAULT 0 WITH VALUES;

IF COL_LENGTH('dbo.payment_intents', 'version') IS NULL
    ALTER TABLE dbo.payment_intents ADD version BIGINT NOT NULL
        CONSTRAINT df_payment_intents_version DEFAULT 0 WITH VALUES;
