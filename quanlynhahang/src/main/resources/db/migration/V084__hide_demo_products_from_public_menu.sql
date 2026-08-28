-- Demo catalog rows are seed/test data and must not be visible to customers.
UPDATE dbo.Products
SET status = 0,
    available = 0
WHERE id BETWEEN 14 AND 21
  AND name LIKE N'Demo %';
