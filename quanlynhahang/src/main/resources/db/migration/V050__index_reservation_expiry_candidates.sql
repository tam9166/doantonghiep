IF OBJECT_ID(N'dbo.reservations', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'reservation_status') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'deposit_expires_at') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.reservations') AND name = N'IX_reservations_status_deposit_expiry')
BEGIN
    CREATE INDEX IX_reservations_status_deposit_expiry
        ON dbo.reservations(reservation_status, deposit_expires_at);
END;

IF OBJECT_ID(N'dbo.reservations', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'reservation_status') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'created_at') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'deposit_amount') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.reservations') AND name = N'IX_reservations_status_created_at')
BEGIN
    CREATE INDEX IX_reservations_status_created_at
        ON dbo.reservations(reservation_status, created_at)
        INCLUDE (deposit_amount);
END;

IF OBJECT_ID(N'dbo.reservations', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'reservation_status') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'reservation_date') IS NOT NULL
   AND COL_LENGTH(N'dbo.reservations', N'arrival_time') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.reservations') AND name = N'IX_reservations_status_arrival')
BEGIN
    CREATE INDEX IX_reservations_status_arrival
        ON dbo.reservations(reservation_status, reservation_date, arrival_time);
END;
