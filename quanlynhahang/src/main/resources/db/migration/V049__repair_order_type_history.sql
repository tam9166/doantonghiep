-- V041 assigned TAKEAWAY to every historical row. Existing table-linked orders
-- are unambiguously dine-in and must be repaired for reports and order history.
IF OBJECT_ID(N'dbo.Orders', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.Orders', N'order_type') IS NOT NULL
BEGIN
    EXEC(N'
        UPDATE dbo.Orders
           SET order_type = N''DINE_IN''
         WHERE table_id IS NOT NULL
           AND order_type = N''TAKEAWAY'';
    ');

    -- New writes must state their business type instead of silently becoming takeaway.
    DECLARE @order_type_default sysname;
    SELECT @order_type_default = default_definition.name
      FROM sys.default_constraints default_definition
      JOIN sys.columns column_definition
        ON column_definition.object_id = default_definition.parent_object_id
       AND column_definition.column_id = default_definition.parent_column_id
     WHERE default_definition.parent_object_id = OBJECT_ID(N'dbo.Orders')
       AND column_definition.name = N'order_type';

    IF @order_type_default IS NOT NULL
    BEGIN
        DECLARE @drop_order_type_default nvarchar(1000);
        SET @drop_order_type_default = N'ALTER TABLE dbo.Orders DROP CONSTRAINT '
                + QUOTENAME(@order_type_default);
        EXEC sys.sp_executesql @drop_order_type_default;
    END;
END;
