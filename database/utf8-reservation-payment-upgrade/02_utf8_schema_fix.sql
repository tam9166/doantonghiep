USE RestaurantDB;
GO

IF COL_LENGTH('dbo.reservations', 'preorder_enabled') IS NULL
    ALTER TABLE dbo.reservations ADD preorder_enabled BIT NOT NULL CONSTRAINT DF_reservations_preorder_enabled DEFAULT 0;

IF COL_LENGTH('dbo.reservations', 'table_amount') IS NULL
    ALTER TABLE dbo.reservations ADD table_amount DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservations_table_amount DEFAULT 0;

IF COL_LENGTH('dbo.reservations', 'food_amount') IS NULL
    ALTER TABLE dbo.reservations ADD food_amount DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservations_food_amount DEFAULT 0;

IF COL_LENGTH('dbo.reservations', 'payment_option') IS NULL
    ALTER TABLE dbo.reservations ADD payment_option VARCHAR(30) NOT NULL CONSTRAINT DF_reservations_payment_option DEFAULT 'DEPOSIT_50';

IF COL_LENGTH('dbo.reservations', 'payment_status') IS NULL
    ALTER TABLE dbo.reservations ADD payment_status VARCHAR(30) NOT NULL CONSTRAINT DF_reservations_payment_status DEFAULT 'PENDING';
GO

UPDATE dbo.reservations
SET
    table_amount = CASE WHEN table_amount = 0 THEN total_amount ELSE table_amount END,
    food_amount = COALESCE(food_amount, 0),
    preorder_enabled = COALESCE(preorder_enabled, 0),
    payment_option = COALESCE(payment_option, 'DEPOSIT_50'),
    payment_status = COALESCE(payment_status, 'PENDING');
GO

IF OBJECT_ID(N'dbo.Categories', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.Categories', 'name') IS NOT NULL
        ALTER TABLE dbo.Categories ALTER COLUMN name NVARCHAR(255) NULL;
END;
GO
