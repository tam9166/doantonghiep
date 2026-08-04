USE master;
GO

DECLARE @BackupPath NVARCHAR(4000) =
    N'E:\DoAnTotNghiep\database\upgrade-reservation\RestaurantDB_before_reservation_upgrade.bak';

BACKUP DATABASE RestaurantDB
TO DISK = @BackupPath
WITH INIT, COMPRESSION, CHECKSUM, STATS = 10;
GO

PRINT N'Backup completed for RestaurantDB.';
GO
