/* Payment QR and realtime scopes are stored together, exceeding the legacy 30-character column. */
ALTER TABLE dbo.reservations ALTER COLUMN payment_capability_scope VARCHAR(80) NULL;
GO
