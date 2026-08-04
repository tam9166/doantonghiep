USE RestaurantDB;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_reservations_code' AND object_id = OBJECT_ID('dbo.reservations'))
    CREATE UNIQUE INDEX UX_reservations_code ON dbo.reservations(reservation_code);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_reservations_table_time' AND object_id = OBJECT_ID('dbo.reservations'))
    CREATE INDEX IX_reservations_table_time ON dbo.reservations(table_id, reservation_date, arrival_time, reservation_status);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_reservations_phone_code' AND object_id = OBJECT_ID('dbo.reservations'))
    CREATE INDEX IX_reservations_phone_code ON dbo.reservations(customer_phone, reservation_code);
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_reservations_table')
    ALTER TABLE dbo.reservations ADD CONSTRAINT FK_reservations_table FOREIGN KEY(table_id) REFERENCES dbo.restaurant_table(id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_reservations_area')
    ALTER TABLE dbo.reservations ADD CONSTRAINT FK_reservations_area FOREIGN KEY(area_id) REFERENCES dbo.table_areas(id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_reservation_status_history_reservation')
    ALTER TABLE dbo.reservation_status_history ADD CONSTRAINT FK_reservation_status_history_reservation FOREIGN KEY(reservation_id) REFERENCES dbo.reservations(id) ON DELETE CASCADE;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_reservation_images_area')
    ALTER TABLE dbo.reservation_images ADD CONSTRAINT FK_reservation_images_area FOREIGN KEY(area_id) REFERENCES dbo.table_areas(id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_reservation_images_table')
    ALTER TABLE dbo.reservation_images ADD CONSTRAINT FK_reservation_images_table FOREIGN KEY(table_id) REFERENCES dbo.restaurant_table(id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_reservations_guest_count')
    ALTER TABLE dbo.reservations ADD CONSTRAINT CK_reservations_guest_count CHECK (guest_count >= 1);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_reservations_deposit_rate')
    ALTER TABLE dbo.reservations ADD CONSTRAINT CK_reservations_deposit_rate CHECK (deposit_rate >= 0 AND deposit_rate <= 1);
GO
