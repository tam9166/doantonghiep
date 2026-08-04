USE RestaurantDB;
GO

IF OBJECT_ID(N'dbo.reservation_preorder_items', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_preorder_items (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_reservation_preorder_items PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        product_id INT NOT NULL,
        product_name NVARCHAR(200) NOT NULL,
        product_image NVARCHAR(255) NULL,
        category_name NVARCHAR(150) NULL,
        unit_price DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservation_preorder_unit_price DEFAULT 0,
        quantity INT NOT NULL,
        note NVARCHAR(300) NULL,
        line_total DECIMAL(18,0) NOT NULL CONSTRAINT DF_reservation_preorder_line_total DEFAULT 0,
        status VARCHAR(30) NOT NULL CONSTRAINT DF_reservation_preorder_status DEFAULT 'REQUESTED',
        created_at DATETIME2 NOT NULL CONSTRAINT DF_reservation_preorder_created_at DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_reservation_preorder_items_reservations')
BEGIN
    ALTER TABLE dbo.reservation_preorder_items
    ADD CONSTRAINT FK_reservation_preorder_items_reservations
    FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id);
END;
GO
