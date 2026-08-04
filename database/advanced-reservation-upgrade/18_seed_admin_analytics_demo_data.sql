SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    PRINT N'Normalize legacy text order statuses for Java analytics endpoints...';

    UPDATE dbo.orders
    SET status = CASE status
        WHEN N'DA_THANH_TOAN' THEN N'4'
        WHEN N'CAN_THANH_TOAN' THEN N'2'
        WHEN N'DANG_PHUC_VU' THEN N'1'
        WHEN N'DANG_CHO' THEN N'0'
        WHEN N'DA_HUY' THEN N'3'
        ELSE N'0'
    END
    WHERE status IS NOT NULL
      AND TRY_CONVERT(int, status) IS NULL;

    PRINT N'Ensure demo categories and products exist...';

    IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Khai vị')
        INSERT INTO dbo.categories (name) VALUES (N'Khai vị');

    IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Lẩu & nướng')
        INSERT INTO dbo.categories (name) VALUES (N'Lẩu & nướng');

    IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Tráng miệng')
        INSERT INTO dbo.categories (name) VALUES (N'Tráng miệng');

    IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Món chính')
        INSERT INTO dbo.categories (name) VALUES (N'Món chính');

    IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE name = N'Đồ uống')
        INSERT INTO dbo.categories (name) VALUES (N'Đồ uống');

    DECLARE @CatMain int = (SELECT TOP 1 id FROM dbo.categories WHERE name IN (N'Món chính', N'Món ăn') ORDER BY id);
    DECLARE @CatDrink int = (SELECT TOP 1 id FROM dbo.categories WHERE name = N'Đồ uống' ORDER BY id);
    DECLARE @CatStarter int = (SELECT TOP 1 id FROM dbo.categories WHERE name = N'Khai vị' ORDER BY id);
    DECLARE @CatHotpot int = (SELECT TOP 1 id FROM dbo.categories WHERE name = N'Lẩu & nướng' ORDER BY id);
    DECLARE @CatDessert int = (SELECT TOP 1 id FROM dbo.categories WHERE name = N'Tráng miệng' ORDER BY id);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Gỏi cuốn tôm thịt')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Gỏi cuốn tôm thịt', 59000, 8, 'goi-cuon.jpg', N'Món khai vị thanh nhẹ dùng kèm nước chấm.', GETDATE(), 1, 1, @CatStarter);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Chả giò hải sản')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Chả giò hải sản', 79000, 8, 'cha-gio-hai-san.jpg', N'Chả giò giòn nhân hải sản.', GETDATE(), 1, 1, @CatStarter);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Lẩu Thái hải sản')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Lẩu Thái hải sản', 289000, 8, 'lau-thai-hai-san.jpg', N'Lẩu chua cay dùng cho nhóm 3-4 khách.', GETDATE(), 1, 1, @CatHotpot);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Bò nướng lá lốt')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Bò nướng lá lốt', 139000, 8, 'bo-nuong-la-lot.jpg', N'Bò nướng thơm dùng kèm rau sống.', GETDATE(), 1, 1, @CatHotpot);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Cá hồi sốt chanh dây')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Cá hồi sốt chanh dây', 219000, 8, 'ca-hoi-chanh-day.jpg', N'Cá hồi áp chảo sốt chanh dây.', GETDATE(), 1, 1, @CatMain);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Cơm gà Hội An')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Cơm gà Hội An', 89000, 8, 'com-ga-hoi-an.jpg', N'Cơm gà xé kiểu Hội An.', GETDATE(), 1, 1, @CatMain);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Mì Quảng đặc biệt')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Mì Quảng đặc biệt', 85000, 8, 'mi-quang-dac-biet.jpg', N'Mì Quảng tôm thịt trứng cút.', GETDATE(), 1, 1, @CatMain);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Nước ép dưa hấu')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Nước ép dưa hấu', 39000, 8, 'nuoc-ep-dua-hau.jpg', N'Nước ép tươi theo ngày.', GETDATE(), 1, 1, @CatDrink);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Trà đào cam sả')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Trà đào cam sả', 45000, 8, 'tra-dao-cam-sa.jpg', N'Trà trái cây mát lạnh.', GETDATE(), 1, 1, @CatDrink);

    IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE name = N'Chè khúc bạch')
        INSERT INTO dbo.products (name, price, tax_rate, image, description, create_date, available, status, category_id)
        VALUES (N'Chè khúc bạch', 49000, 8, 'che-khuc-bach.jpg', N'Món tráng miệng lạnh.', GETDATE(), 1, 1, @CatDessert);

    IF EXISTS (SELECT 1 FROM dbo.orders WHERE CHARINDEX(N'[DEMO_ANALYTICS]', address) > 0)
    BEGIN
        PRINT N'Demo analytics orders already exist. Skip inserting duplicate sample data.';
    END
    ELSE
    BEGIN
        PRINT N'Insert completed demo orders for admin analytics...';

        DECLARE @DemoProducts TABLE (
            row_no int IDENTITY(1,1) PRIMARY KEY,
            product_id int NOT NULL,
            product_name nvarchar(200) NOT NULL,
            unit_price float NOT NULL
        );

        INSERT INTO @DemoProducts (product_id, product_name, unit_price)
        SELECT id, name, price
        FROM dbo.products
        WHERE name IN (
            N'Phở bò Kobe',
            N'Cơm rang dưa bò',
            N'Coca Cola',
            N'Gỏi cuốn tôm thịt',
            N'Chả giò hải sản',
            N'Lẩu Thái hải sản',
            N'Bò nướng lá lốt',
            N'Cá hồi sốt chanh dây',
            N'Cơm gà Hội An',
            N'Mì Quảng đặc biệt',
            N'Nước ép dưa hấu',
            N'Trà đào cam sả',
            N'Chè khúc bạch'
        )
        ORDER BY id;

        DECLARE @TableIds TABLE (row_no int IDENTITY(1,1) PRIMARY KEY, table_id bigint NOT NULL);
        INSERT INTO @TableIds (table_id)
        SELECT id FROM dbo.restaurant_table ORDER BY id;

        DECLARE @ProductCount int = (SELECT COUNT(*) FROM @DemoProducts);
        DECLARE @TableCount int = (SELECT COUNT(*) FROM @TableIds);

        IF @ProductCount = 0 OR @TableCount = 0
            THROW 51000, N'Cannot seed analytics data because products or restaurant_table are missing.', 1;

        DECLARE @i int = 1;

        WHILE @i <= 48
        BEGIN
            DECLARE @DaysAgo int = (@i * 3) % 92;
            DECLARE @HourOffset int = 10 + (@i % 11);
            DECLARE @OrderDate datetime2 = DATEADD(hour, @HourOffset, DATEADD(day, -@DaysAgo, CAST(CAST(GETDATE() AS date) AS datetime2)));
            DECLARE @TableId bigint = (SELECT table_id FROM @TableIds WHERE row_no = ((@i - 1) % @TableCount) + 1);
            DECLARE @Username varchar(50) = CASE WHEN EXISTS (SELECT 1 FROM dbo.accounts WHERE username = 'customer') THEN 'customer' ELSE NULL END;

            INSERT INTO dbo.orders (table_id, waiter_name, guest_count, status, created_at, address, create_date, deposit, is_paid, sub_total, tax_amount, total_amount, username)
            VALUES (
                @TableId,
                CASE WHEN @i % 4 = 0 THEN N'Nam' WHEN @i % 4 = 1 THEN N'Vy' WHEN @i % 4 = 2 THEN N'Long' ELSE N'Hạnh' END,
                2 + (@i % 7),
                N'4',
                @OrderDate,
                N'[DEMO_ANALYTICS] Đơn hoàn thành mẫu #' + CAST(@i AS nvarchar(10)),
                @OrderDate,
                0,
                1,
                0,
                0,
                0,
                @Username
            );

            DECLARE @OrderId int = CAST(SCOPE_IDENTITY() AS int);
            DECLARE @LineCount int = 2 + (@i % 3);
            DECLARE @j int = 0;
            DECLARE @SubTotal float = 0;
            DECLARE @TaxTotal float = 0;

            WHILE @j < @LineCount
            BEGIN
                DECLARE @ProductIndex int = ((@i + (@j * 4)) % @ProductCount) + 1;
                DECLARE @ProductId int;
                DECLARE @UnitPrice float;
                DECLARE @Qty int = 1 + ((@i + @j) % 3);
                DECLARE @LineTotal float;
                DECLARE @LineTax float;

                SELECT @ProductId = product_id, @UnitPrice = unit_price
                FROM @DemoProducts
                WHERE row_no = @ProductIndex;

                SET @LineTotal = @UnitPrice * @Qty;
                SET @LineTax = ROUND(@LineTotal * 0.08, 0);

                INSERT INTO dbo.order_details (price, quantity, status, tax_amount, tax_rate, order_id, product_id)
                VALUES (@LineTotal, @Qty, 2, @LineTax, 8.0, @OrderId, @ProductId);

                SET @SubTotal += @LineTotal;
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
    PRINT N'Done: admin analytics sample data is ready.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    DECLARE @ErrorMessage nvarchar(4000) = ERROR_MESSAGE();
    THROW 51001, @ErrorMessage, 1;
END CATCH;
