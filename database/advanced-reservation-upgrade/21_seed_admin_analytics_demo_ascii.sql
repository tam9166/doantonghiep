SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    PRINT N'Ensure ASCII demo data exists for Admin Analytics...';

    IF OBJECT_ID(N'dbo.restaurant_table', N'U') IS NULL
        THROW 52100, N'Missing dbo.restaurant_table. Run the base schema first.', 1;

    IF OBJECT_ID(N'dbo.categories', N'U') IS NULL
        THROW 52101, N'Missing dbo.categories. Run the base schema first.', 1;

    IF OBJECT_ID(N'dbo.products', N'U') IS NULL
        THROW 52102, N'Missing dbo.products. Run the base schema first.', 1;

    IF OBJECT_ID(N'dbo.orders', N'U') IS NULL OR OBJECT_ID(N'dbo.order_details', N'U') IS NULL
        THROW 52103, N'Missing order tables. Run the base schema first.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.restaurant_table)
    BEGIN
        INSERT INTO dbo.restaurant_table (name, floor, is_occupied, has_view, reserved_time, capacity, view_type)
        VALUES
            (N'Demo T1-01', N'Tang 1', 0, 0, NULL, 4, N'Standard'),
            (N'Demo T1-02', N'Tang 1', 0, 0, NULL, 4, N'Standard'),
            (N'Demo T2-01', N'Tang 2', 0, 1, NULL, 6, N'Window'),
            (N'Demo T2-02', N'Tang 2', 0, 1, NULL, 6, N'Window'),
            (N'Demo VIP-01', N'VIP', 0, 1, NULL, 8, N'Private'),
            (N'Demo Rooftop-01', N'Rooftop', 0, 1, NULL, 4, N'Rooftop');
    END;

    IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Demo Analytics')
        INSERT INTO dbo.categories (name) VALUES (N'Demo Analytics');

    DECLARE @CategoryId int = (SELECT TOP 1 id FROM dbo.categories WHERE name = N'Demo Analytics' ORDER BY id);

    DECLARE @Products TABLE (
        name nvarchar(200) NOT NULL,
        price decimal(18, 2) NOT NULL,
        image varchar(255) NOT NULL,
        description nvarchar(500) NOT NULL
    );

    INSERT INTO @Products (name, price, image, description)
    VALUES
        (N'Demo Pho Bo', 89000, 'demo-pho-bo.jpg', N'ASCII demo product for revenue charts.'),
        (N'Demo Com Ga', 79000, 'demo-com-ga.jpg', N'ASCII demo product for revenue charts.'),
        (N'Demo Lau Thai', 269000, 'demo-lau-thai.jpg', N'ASCII demo product for revenue charts.'),
        (N'Demo Bo Nuong', 159000, 'demo-bo-nuong.jpg', N'ASCII demo product for revenue charts.'),
        (N'Demo Goi Cuon', 59000, 'demo-goi-cuon.jpg', N'ASCII demo product for revenue charts.'),
        (N'Demo Cha Gio', 69000, 'demo-cha-gio.jpg', N'ASCII demo product for revenue charts.'),
        (N'Demo Tra Dao', 45000, 'demo-tra-dao.jpg', N'ASCII demo product for revenue charts.'),
        (N'Demo Nuoc Ep', 39000, 'demo-nuoc-ep.jpg', N'ASCII demo product for revenue charts.');

    INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
    SELECT p.name, p.price, 8, p.image, p.description, GETDATE(), 1, 1, @CategoryId
    FROM @Products p
    WHERE NOT EXISTS (SELECT 1 FROM dbo.products existing WHERE existing.name = p.name);

    IF EXISTS (SELECT 1 FROM dbo.orders WHERE CHARINDEX(N'[DEMO_ANALYTICS_ASCII]', address) > 0)
    BEGIN
        PRINT N'ASCII analytics demo orders already exist. Skip duplicate insert.';
    END
    ELSE
    BEGIN
        DECLARE @DemoProducts TABLE (
            row_no int IDENTITY(1,1) PRIMARY KEY,
            product_id int NOT NULL,
            unit_price decimal(18, 2) NOT NULL
        );

        INSERT INTO @DemoProducts (product_id, unit_price)
        SELECT id, price
        FROM dbo.products
        WHERE name LIKE N'Demo %'
        ORDER BY id;

        DECLARE @TableIds TABLE (row_no int IDENTITY(1,1) PRIMARY KEY, table_id int NOT NULL);
        INSERT INTO @TableIds (table_id)
        SELECT id FROM dbo.restaurant_table ORDER BY id;

        DECLARE @ProductCount int = (SELECT COUNT(*) FROM @DemoProducts);
        DECLARE @TableCount int = (SELECT COUNT(*) FROM @TableIds);

        IF @ProductCount = 0 OR @TableCount = 0
            THROW 52104, N'Cannot seed analytics demo data because products or tables are missing.', 1;

        DECLARE @i int = 1;

        WHILE @i <= 72
        BEGIN
            DECLARE @DaysAgo int = (@i * 2) % 120;
            DECLARE @OrderDate datetime2 = DATEADD(hour, 10 + (@i % 12), DATEADD(day, -@DaysAgo, CAST(CAST(GETDATE() AS date) AS datetime2)));
            DECLARE @TableId int = (SELECT table_id FROM @TableIds WHERE row_no = ((@i - 1) % @TableCount) + 1);
            DECLARE @Username varchar(50) = CASE WHEN OBJECT_ID(N'dbo.accounts', N'U') IS NOT NULL AND EXISTS (SELECT 1 FROM dbo.accounts WHERE username = 'customer') THEN 'customer' ELSE NULL END;

            INSERT INTO dbo.orders (create_date, username, table_id, status, address, sub_total, tax_amount, total_amount, deposit, is_paid)
            VALUES (@OrderDate, @Username, @TableId, 4, N'[DEMO_ANALYTICS_ASCII] Completed demo order #' + CAST(@i AS nvarchar(10)), 0, 0, 0, 0, 1);

            DECLARE @OrderId int = CAST(SCOPE_IDENTITY() AS int);
            DECLARE @LineCount int = 2 + (@i % 4);
            DECLARE @j int = 0;
            DECLARE @SubTotal decimal(18, 2) = 0;
            DECLARE @TaxTotal decimal(18, 2) = 0;

            WHILE @j < @LineCount
            BEGIN
                DECLARE @ProductIndex int = ((@i + (@j * 3)) % @ProductCount) + 1;
                DECLARE @ProductId int;
                DECLARE @UnitPrice decimal(18, 2);
                DECLARE @Qty int = 1 + ((@i + @j) % 3);
                DECLARE @LineSubTotal decimal(18, 2);
                DECLARE @LineTax decimal(18, 2);

                SELECT @ProductId = product_id, @UnitPrice = unit_price
                FROM @DemoProducts
                WHERE row_no = @ProductIndex;

                SET @LineSubTotal = @UnitPrice * @Qty;
                SET @LineTax = ROUND(@LineSubTotal * 0.08, 0);

                INSERT INTO dbo.order_details (order_id, product_id, price, quantity, tax_rate, tax_amount, status)
                VALUES (@OrderId, @ProductId, @LineSubTotal, @Qty, 8, @LineTax, 2);

                SET @SubTotal += @LineSubTotal;
                SET @TaxTotal += @LineTax;
                SET @j += 1;
            END;

            UPDATE dbo.orders
            SET sub_total = @SubTotal,
                tax_amount = @TaxTotal,
                total_amount = @SubTotal + @TaxTotal
            WHERE id = @OrderId;

            SET @i += 1;
        END;
    END;

    COMMIT TRANSACTION;
    PRINT N'Done: ASCII admin analytics demo data is ready.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    DECLARE @ErrorMessage nvarchar(4000) = ERROR_MESSAGE();
    THROW 52199, @ErrorMessage, 1;
END CATCH;
