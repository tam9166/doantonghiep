IF OBJECT_ID(N'dbo.Orders', N'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.Orders')
          AND name = N'CK_Orders_status_legacy'
    )
        ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_status_legacy;

    ALTER TABLE dbo.Orders WITH CHECK
        ADD CONSTRAINT CK_Orders_status_legacy CHECK (status IN (0,1,2,3,4,5,6,7));
END;
