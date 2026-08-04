USE master;
GO

PRINT N'Backup database before applying UTF-8 reservation payment upgrade.';
PRINT N'Example command:';
PRINT N'BACKUP DATABASE RestaurantDB TO DISK = N''E:\DoAnTotNghiep\database\utf8-reservation-payment-upgrade\RestaurantDB_before_utf8_reservation_payment.bak'' WITH INIT, COMPRESSION;';
GO
