IF COL_LENGTH('reservations', 'kitchen_order_id') IS NULL
BEGIN
    EXEC('ALTER TABLE reservations ADD kitchen_order_id INT NULL;');
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_reservations_kitchen_order_id')
BEGIN
    EXEC('CREATE UNIQUE INDEX UX_reservations_kitchen_order_id
          ON reservations(kitchen_order_id)
          WHERE kitchen_order_id IS NOT NULL;');
END;
