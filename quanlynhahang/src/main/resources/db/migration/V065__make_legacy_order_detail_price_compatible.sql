IF OBJECT_ID(N'dbo.order_details', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.order_details', N'unit_price') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.default_constraints dc
       JOIN sys.columns c
         ON c.object_id = dc.parent_object_id
        AND c.column_id = dc.parent_column_id
       WHERE dc.parent_object_id = OBJECT_ID(N'dbo.order_details')
         AND c.name = N'unit_price'
   )
BEGIN
    ALTER TABLE dbo.order_details
        ADD CONSTRAINT DF_order_details_legacy_unit_price DEFAULT 0 FOR unit_price;
END;
