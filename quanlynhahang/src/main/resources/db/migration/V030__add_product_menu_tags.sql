/* Product tags used by menu recommendations and the public menu. */
IF COL_LENGTH('dbo.Products', 'diet_type') IS NULL
BEGIN
    ALTER TABLE dbo.Products ADD diet_type NVARCHAR(20) NOT NULL
        CONSTRAINT DF_Products_diet_type DEFAULT N'MAN';
END;

IF COL_LENGTH('dbo.Products', 'cooking_method') IS NULL
BEGIN
    ALTER TABLE dbo.Products ADD cooking_method NVARCHAR(20) NOT NULL
        CONSTRAINT DF_Products_cooking_method DEFAULT N'KHAC';
END;

IF COL_LENGTH('dbo.Products', 'spicy_level') IS NULL
BEGIN
    ALTER TABLE dbo.Products ADD spicy_level INT NOT NULL
        CONSTRAINT DF_Products_spicy_level DEFAULT 0;
END;

IF COL_LENGTH('dbo.Products', 'is_signature_dish') IS NULL
BEGIN
    ALTER TABLE dbo.Products ADD is_signature_dish BIT NOT NULL
        CONSTRAINT DF_Products_is_signature_dish DEFAULT 0;
END;

GO

UPDATE dbo.Products
SET diet_type = COALESCE(NULLIF(diet_type, N''), N'MAN'),
    cooking_method = COALESCE(NULLIF(cooking_method, N''), N'KHAC'),
    spicy_level = CASE WHEN spicy_level BETWEEN 0 AND 3 THEN spicy_level ELSE 0 END,
    is_signature_dish = COALESCE(is_signature_dish, 0);

GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Products_diet_type')
    ALTER TABLE dbo.Products ADD CONSTRAINT CK_Products_diet_type CHECK (diet_type IN (N'CHAY', N'MAN'));

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Products_cooking_method')
    ALTER TABLE dbo.Products ADD CONSTRAINT CK_Products_cooking_method CHECK (cooking_method IN (N'NUONG', N'HAP', N'CHIEN', N'XAO', N'LUOC', N'KHAC'));

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Products_spicy_level')
    ALTER TABLE dbo.Products ADD CONSTRAINT CK_Products_spicy_level CHECK (spicy_level BETWEEN 0 AND 3);
