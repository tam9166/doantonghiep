USE RestaurantDB;
GO

PRINT N'Rollback script for newly added payment/preorder tables only.';
PRINT N'It does not drop columns added to reservations to avoid data loss.';
GO

IF OBJECT_ID(N'dbo.payment_intents', N'U') IS NOT NULL
    DROP TABLE dbo.payment_intents;

IF OBJECT_ID(N'dbo.reservation_preorder_items', N'U') IS NOT NULL
    DROP TABLE dbo.reservation_preorder_items;
GO
