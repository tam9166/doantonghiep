USE RestaurantDB;
GO

/*
Rollback is intentionally conservative. It removes only additive reservation objects
created by this upgrade package. Existing restaurant_table data is preserved.
Run only after restoring/validating a backup if production data has already used
the reservation feature.
*/

IF OBJECT_ID(N'dbo.reservation_status_history', N'U') IS NOT NULL DROP TABLE dbo.reservation_status_history;
IF OBJECT_ID(N'dbo.reservation_images', N'U') IS NOT NULL DROP TABLE dbo.reservation_images;
IF OBJECT_ID(N'dbo.reservations', N'U') IS NOT NULL DROP TABLE dbo.reservations;
IF OBJECT_ID(N'dbo.table_areas', N'U') IS NOT NULL DROP TABLE dbo.table_areas;
GO

PRINT N'Reservation upgrade objects rolled back. restaurant_table additive columns were preserved to avoid data loss.';
GO
