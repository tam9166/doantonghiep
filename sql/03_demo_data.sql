SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

USE RestaurantDB;
GO

BEGIN TRANSACTION;

/* 10 import invoices, details, ingredient batches and stock movement history. */
DECLARE @InvoiceNo INT = 1;
WHILE @InvoiceNo <= 10
BEGIN
    DECLARE @InvoiceCode VARCHAR(50) = CONCAT('PNK', FORMAT(@InvoiceNo, '000'));
    DECLARE @IngredientId BIGINT = ((@InvoiceNo - 1) % 20) + 1;
    DECLARE @Quantity DECIMAL(18,3) = 10 + @InvoiceNo;
    DECLARE @UnitPrice DECIMAL(18,2) = 25000 + (@InvoiceNo * 7500);
    DECLARE @Total DECIMAL(18,2) = @Quantity * @UnitPrice;
    DECLARE @Before DECIMAL(18,3) = (SELECT quantity FROM dbo.ingredients WHERE id = @IngredientId);
    DECLARE @After DECIMAL(18,3) = @Before + @Quantity;

    INSERT INTO dbo.import_invoices(invoice_code, supplier, supplier_name, import_date, total_amount, created_by, note)
    VALUES (@InvoiceCode, CONCAT(N'Nhà cung cấp ', @InvoiceNo), CONCAT(N'Nhà cung cấp ', @InvoiceNo),
            DATEADD(day, -@InvoiceNo, SYSDATETIME()), @Total, 'manager', N'Phiếu nhập demo');

    DECLARE @InvoiceId BIGINT = SCOPE_IDENTITY();

    INSERT INTO dbo.ImportInvoiceDetails(invoice_id, ingredient_id, quantity, unit_price, expiry_date, total_price)
    VALUES (@InvoiceId, @IngredientId, @Quantity, @UnitPrice, DATEADD(day, 20 + @InvoiceNo, CAST(SYSDATETIME() AS DATE)), @Total);

    INSERT INTO dbo.ingredient_batches(ingredient_id, import_invoice_id, batch_code, quantity, remaining_quantity,
                                       import_date, expiration_date, expiry_date, unit_price, supplier_name, note)
    VALUES (@IngredientId, @InvoiceId, CONCAT('BATCH-', FORMAT(@InvoiceNo, '000')), @Quantity, @Quantity,
            DATEADD(day, -@InvoiceNo, SYSDATETIME()), DATEADD(day, 20 + @InvoiceNo, CAST(SYSDATETIME() AS DATE)),
            DATEADD(day, 20 + @InvoiceNo, CAST(SYSDATETIME() AS DATE)), @UnitPrice,
            CONCAT(N'Nhà cung cấp ', @InvoiceNo), N'Lô nhập demo');

    DECLARE @BatchId BIGINT = SCOPE_IDENTITY();

    UPDATE dbo.ingredients SET quantity = @After WHERE id = @IngredientId;

    INSERT INTO dbo.InventoryTransactions(ingredient_id, batch_id, transaction_type, quantity, before_quantity, after_quantity, reason, created_by)
    VALUES (@IngredientId, @BatchId, 'IMPORT', @Quantity, @Before, @After, CONCAT(N'Nhập kho từ ', @InvoiceCode), 'manager');

    SET @InvoiceNo += 1;
END;

/* 20 demo orders with two order details each. */
DECLARE @OrderNo INT = 1;
WHILE @OrderNo <= 20
BEGIN
    DECLARE @ProductA INT = ((@OrderNo - 1) % 30) + 1;
    DECLARE @ProductB INT = ((@OrderNo + 7) % 30) + 1;
    DECLARE @QtyA INT = CASE WHEN @OrderNo % 3 = 0 THEN 2 ELSE 1 END;
    DECLARE @QtyB INT = 1;
    DECLARE @PriceA DECIMAL(18,2) = (SELECT price FROM dbo.Products WHERE id = @ProductA);
    DECLARE @PriceB DECIMAL(18,2) = (SELECT price FROM dbo.Products WHERE id = @ProductB);
    DECLARE @SubTotal DECIMAL(18,2) = (@PriceA * @QtyA) + (@PriceB * @QtyB);
    DECLARE @Discount DECIMAL(18,2) = CASE WHEN @OrderNo % 5 = 0 THEN 50000 ELSE 0 END;
    DECLARE @Tax DECIMAL(18,2) = ROUND((@SubTotal - @Discount) * 0.08, 2);
    DECLARE @GrandTotal DECIMAL(18,2) = @SubTotal - @Discount + @Tax;
    DECLARE @StatusName VARCHAR(20) = CASE
        WHEN @OrderNo % 7 = 0 THEN 'CANCELLED'
        WHEN @OrderNo % 5 = 0 THEN 'PAID'
        WHEN @OrderNo % 4 = 0 THEN 'SERVED'
        WHEN @OrderNo % 3 = 0 THEN 'READY'
        WHEN @OrderNo % 2 = 0 THEN 'COOKING'
        ELSE 'PENDING'
    END;
    DECLARE @StatusInt INT = CASE @StatusName
        WHEN 'PENDING' THEN 0 WHEN 'COOKING' THEN 1 WHEN 'READY' THEN 2
        WHEN 'SERVED' THEN 3 WHEN 'PAID' THEN 4 WHEN 'CANCELLED' THEN 5 ELSE 6
    END;

    INSERT INTO dbo.Orders(create_date, username, table_id, order_type, status, status_name, customer_name, customer_phone,
                           reservation_time, address, note, voucher_code, sub_total, discount_amount, tax_amount,
                           total_amount, deposit, is_paid, payment_method, payment_time, created_by)
    VALUES (DATEADD(hour, -@OrderNo * 3, SYSDATETIME()), 'customer', ((@OrderNo - 1) % 20) + 1,
            CASE WHEN @OrderNo % 6 = 0 THEN 'TAKE_AWAY' WHEN @OrderNo % 9 = 0 THEN 'RESERVATION' ELSE 'DINE_IN' END,
            @StatusInt, @StatusName, N'Khách demo', CONCAT('09876543', FORMAT(@OrderNo, '00')),
            CASE WHEN @OrderNo % 9 = 0 THEN DATEADD(day, 1, SYSDATETIME()) ELSE NULL END,
            N'Địa chỉ demo', N'Đơn hàng demo',
            CASE WHEN @OrderNo % 5 = 0 THEN 'FAMILY50' ELSE NULL END,
            @SubTotal, @Discount, @Tax, @GrandTotal,
            CASE WHEN @OrderNo % 9 = 0 THEN 100000 ELSE 0 END,
            CASE WHEN @StatusName = 'PAID' THEN 1 ELSE 0 END,
            CASE WHEN @StatusName = 'PAID' THEN 'CASH' ELSE NULL END,
            CASE WHEN @StatusName = 'PAID' THEN SYSDATETIME() ELSE NULL END,
            CASE WHEN @OrderNo % 2 = 0 THEN 'waiter' ELSE 'manager' END);

    DECLARE @OrderId INT = SCOPE_IDENTITY();

    INSERT INTO dbo.OrderDetails(order_id, product_id, price, unit_price, quantity, line_subtotal, tax_rate, tax_amount, line_total, status, status_name, note)
    VALUES
    (@OrderId, @ProductA, @PriceA, @PriceA, @QtyA, @PriceA * @QtyA, 8, ROUND(@PriceA * @QtyA * 0.08, 2),
     ROUND(@PriceA * @QtyA * 1.08, 2), CASE WHEN @StatusName = 'CANCELLED' THEN 4 ELSE @StatusInt END,
     CASE WHEN @StatusName = 'CANCELLED' THEN 'CANCELLED' ELSE 'SERVED' END, N'Chi tiết món demo'),
    (@OrderId, @ProductB, @PriceB, @PriceB, @QtyB, @PriceB * @QtyB, 8, ROUND(@PriceB * @QtyB * 0.08, 2),
     ROUND(@PriceB * @QtyB * 1.08, 2), CASE WHEN @StatusName = 'CANCELLED' THEN 4 ELSE @StatusInt END,
     CASE WHEN @StatusName = 'CANCELLED' THEN 'CANCELLED' ELSE 'READY' END, N'Chi tiết món demo');

    IF @StatusName IN ('SERVED','PAID')
    BEGIN
        INSERT INTO dbo.Reviews(username, product_id, order_id, rating, comment)
        VALUES ('customer', @ProductA, @OrderId, 4 + (@OrderNo % 2), N'Món ăn ngon, phục vụ tốt.');
    END;

    SET @OrderNo += 1;
END;

/* Seven days of staff schedules and timekeeping. */
DECLARE @DayOffset INT = 0;
WHILE @DayOffset < 7
BEGIN
    DECLARE @WorkDate DATE = DATEADD(day, -@DayOffset, CAST(SYSDATETIME() AS DATE));

    INSERT INTO dbo.work_schedules(username, work_date, shift, shift_name, start_time, end_time, status, note)
    VALUES
    ('manager', @WorkDate, N'Sáng',  N'Sáng',  '06:00', '14:00', 'COMPLETED', N'Ca quản lý'),
    ('waiter',  @WorkDate, N'Chiều', N'Chiều', '14:00', '22:00', 'COMPLETED', N'Ca phục vụ'),
    ('kitchen', @WorkDate, N'Sáng',  N'Sáng',  '06:00', '14:00', 'COMPLETED', N'Ca bếp'),
    ('cashier', @WorkDate, N'Tối',   N'Tối',   '16:00', '23:00', CASE WHEN @DayOffset = 2 THEN 'ABSENT' ELSE 'COMPLETED' END, N'Ca thu ngân');

    INSERT INTO dbo.timekeeping(username, work_date, check_in_time, check_out_time, total_hours, status, note)
    VALUES
    ('manager', @WorkDate, DATEADD(hour, 6, CAST(@WorkDate AS DATETIME2)), DATEADD(hour, 14, CAST(@WorkDate AS DATETIME2)), 8, N'COMPLETED', N'Đúng giờ'),
    ('waiter',  @WorkDate, DATEADD(minute, 5, DATEADD(hour, 14, CAST(@WorkDate AS DATETIME2))), DATEADD(hour, 22, CAST(@WorkDate AS DATETIME2)), 7.92, N'LATE', N'Đi trễ 5 phút'),
    ('kitchen', @WorkDate, DATEADD(hour, 6, CAST(@WorkDate AS DATETIME2)), DATEADD(hour, 14, CAST(@WorkDate AS DATETIME2)), 8, N'COMPLETED', N'Đúng giờ'),
    ('cashier', @WorkDate,
        CASE WHEN @DayOffset = 2 THEN NULL ELSE DATEADD(hour, 16, CAST(@WorkDate AS DATETIME2)) END,
        CASE WHEN @DayOffset = 2 THEN NULL ELSE DATEADD(hour, 23, CAST(@WorkDate AS DATETIME2)) END,
        CASE WHEN @DayOffset = 2 THEN 0 ELSE 7 END,
        CASE WHEN @DayOffset = 2 THEN N'ABSENT' ELSE N'COMPLETED' END,
        CASE WHEN @DayOffset = 2 THEN N'Nghỉ không phép' ELSE N'Hoàn thành ca' END);

    SET @DayOffset += 1;
END;

INSERT INTO dbo.Applications(fullname, phone, email, message, post_id, postId, cv_file, status)
VALUES
(N'Nguyễn Văn Ứng Viên', '0912345678', 'ungvien@example.com', N'Ứng tuyển vị trí phục vụ ca tối.', 2, 2, N'uploads/cv-ung-vien.pdf', 'NEW');

COMMIT TRANSACTION;
GO

PRINT N'03_demo_data.sql completed.';
GO
