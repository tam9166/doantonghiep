/*
  Analytics demo data for FPoly Restaurant.
  Creates 28 paid/completed orders across the last seven days.
  Safe to execute repeatedly: the marker address prevents duplicates.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM dbo.orders WHERE address = N'[DEMO_ANALYTICS_2026]')
    BEGIN
        UPDATE dbo.products
        SET cost_price = CASE id
            WHEN 1 THEN 32000 WHEN 2 THEN 22000 WHEN 3 THEN 6000
            WHEN 4 THEN 26000 WHEN 5 THEN 37000 WHEN 6 THEN 145000
            WHEN 7 THEN 69000 WHEN 8 THEN 112000 WHEN 9 THEN 45000
            WHEN 10 THEN 42000
        END
        WHERE id BETWEEN 1 AND 10 AND (cost_price IS NULL OR cost_price = 0);

        DECLARE @dayOffset INT = 0;
        DECLARE @slot INT;
        DECLARE @productA INT;
        DECLARE @productB INT;
        DECLARE @productC INT;
        DECLARE @priceA DECIMAL(18, 2);
        DECLARE @priceB DECIMAL(18, 2);
        DECLARE @priceC DECIMAL(18, 2);
        DECLARE @qtyA INT;
        DECLARE @qtyB INT;
        DECLARE @qtyC INT;
        DECLARE @subTotal DECIMAL(18, 2);
        DECLARE @taxAmount DECIMAL(18, 2);
        DECLARE @totalAmount DECIMAL(18, 2);
        DECLARE @orderId INT;
        DECLARE @orderDate DATETIME2;

        WHILE @dayOffset < 7
        BEGIN
            SET @slot = 1;

            WHILE @slot <= 4
            BEGIN
                SET @productA = ((@dayOffset * 4 + @slot - 1) % 10) + 1;
                SET @productB = ((@dayOffset * 4 + @slot + 2) % 10) + 1;
                SET @productC = ((@dayOffset * 4 + @slot + 5) % 10) + 1;
                SET @qtyA = 2 + ((@dayOffset + @slot) % 3);
                SET @qtyB = 1 + ((@dayOffset + @slot) % 2);
                SET @qtyC = 1 + ((@dayOffset + @slot + 1) % 2);

                SELECT @priceA = price FROM dbo.products WHERE id = @productA;
                SELECT @priceB = price FROM dbo.products WHERE id = @productB;
                SELECT @priceC = price FROM dbo.products WHERE id = @productC;

                SET @subTotal = @priceA * @qtyA + @priceB * @qtyB + @priceC * @qtyC;
                SET @taxAmount = ROUND(@subTotal * 0.08, 0);
                SET @totalAmount = @subTotal + @taxAmount;
                SET @orderDate = DATEADD(hour, 10 + (@slot * 2), DATEADD(day, -@dayOffset, CAST(CAST(SYSDATETIME() AS date) AS datetime2)));

                INSERT INTO dbo.orders (
                    create_date, status, address, sub_total, tax_amount, total_amount,
                    is_paid, payment_option, payment_status, paid_amount, remaining_amount, version
                ) VALUES (
                    @orderDate, N'4', N'[DEMO_ANALYTICS_2026]', @subTotal, @taxAmount, @totalAmount,
                    1, 'PAY_AT_RESTAURANT', 'PAID', @totalAmount, 0, 0
                );

                SET @orderId = SCOPE_IDENTITY();

                INSERT INTO dbo.order_details (price, unit_price, quantity, status, tax_amount, tax_rate, line_subtotal, line_total, order_id, product_id)
                VALUES
                    (@priceA, @priceA, @qtyA, 2, ROUND(@priceA * @qtyA * 0.08, 0), 8.0, @priceA * @qtyA, @priceA * @qtyA * 1.08, @orderId, @productA),
                    (@priceB, @priceB, @qtyB, 2, ROUND(@priceB * @qtyB * 0.08, 0), 8.0, @priceB * @qtyB, @priceB * @qtyB * 1.08, @orderId, @productB),
                    (@priceC, @priceC, @qtyC, 2, ROUND(@priceC * @qtyC * 0.08, 0), 8.0, @priceC * @qtyC, @priceC * @qtyC * 1.08, @orderId, @productC);

                SET @slot += 1;
            END;

            SET @dayOffset += 1;
        END;
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
