USE RestaurantDB;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_payment_intents_payment_code' AND object_id = OBJECT_ID('dbo.payment_intents'))
    CREATE UNIQUE INDEX UX_payment_intents_payment_code ON dbo.payment_intents(payment_code);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_payment_intents_reservation_status' AND object_id = OBJECT_ID('dbo.payment_intents'))
    CREATE INDEX IX_payment_intents_reservation_status ON dbo.payment_intents(reservation_id, status, expires_at);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_reservation_preorder_items_reservation' AND object_id = OBJECT_ID('dbo.reservation_preorder_items'))
    CREATE INDEX IX_reservation_preorder_items_reservation ON dbo.reservation_preorder_items(reservation_id);
GO
