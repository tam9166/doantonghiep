-- Run before migration. Change path if needed.
DECLARE @file nvarchar(4000) =
    N'E:\DoAnTotNghiep\database\backup_RestaurantDB_' +
    CONVERT(nvarchar(8), GETDATE(), 112) + N'_' +
    REPLACE(CONVERT(nvarchar(8), GETDATE(), 108), ':', '') + N'.bak';

DECLARE @sql nvarchar(max) = N'BACKUP DATABASE [RestaurantDB] TO DISK = N''' + @file + N''' WITH INIT, COMPRESSION, STATS = 10';
PRINT @sql;
EXEC (@sql);
