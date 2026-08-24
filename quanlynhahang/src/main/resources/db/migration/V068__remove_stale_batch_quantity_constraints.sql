IF OBJECT_ID(N'dbo.ingredient_batches', N'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.ingredient_batches') AND name = N'CK_ingredient_batches_quantity_positive')
        ALTER TABLE dbo.ingredient_batches DROP CONSTRAINT CK_ingredient_batches_quantity_positive;

    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.ingredient_batches') AND name = N'CK_ingredient_batches_remaining_lte_quantity')
        ALTER TABLE dbo.ingredient_batches DROP CONSTRAINT CK_ingredient_batches_remaining_lte_quantity;
END;
