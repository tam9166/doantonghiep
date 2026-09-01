-- Batch 5: localize only products with verified, semantic-high Commons assets.
-- Match by exact Unicode name and require one active/public row; do not assume
-- SQL Server identity values are stable between upgrade and clean databases.

IF (SELECT COUNT(*) FROM dbo.Products
    WHERE name = N'Absolut' AND status = 1 AND available = 1) = 1
BEGIN
    UPDATE dbo.Products
    SET image = N'/images/products/absolut-vodka-cc-by.jpg'
    WHERE name = N'Absolut' AND status = 1 AND available = 1;
END;

IF (SELECT COUNT(*) FROM dbo.Products
    WHERE name = N'Finlandia' AND status = 1 AND available = 1) = 1
BEGIN
    UPDATE dbo.Products
    SET image = N'/images/products/finlandia-vodka-cc-by-sa.jpg'
    WHERE name = N'Finlandia' AND status = 1 AND available = 1;
END;

IF (SELECT COUNT(*) FROM dbo.Products
    WHERE name = N'Hennessy VS' AND status = 1 AND available = 1) = 1
BEGIN
    UPDATE dbo.Products
    SET image = N'/images/products/hennessy-vs-cognac-cc-by-sa.jpg'
    WHERE name = N'Hennessy VS' AND status = 1 AND available = 1;
END;
