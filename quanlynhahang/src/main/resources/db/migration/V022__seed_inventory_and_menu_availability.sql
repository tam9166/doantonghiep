-- Seed a minimal, usable inventory for the presentation menu. This migration is idempotent
-- so that it is safe for an existing database and does not overwrite stock already entered.
INSERT INTO dbo.ingredients (image, min_stock, name, quantity, shelf_life_days, unit, unit_price)
SELECT source.image, source.min_stock, source.name, source.quantity, source.shelf_life_days, source.unit, source.unit_price
FROM (VALUES
    (N'Thịt bò Kobe', N'kg', 18.0, 2.0, 420000.00, 5, N'https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=480&q=80'),
    (N'Bánh phở', N'kg', 35.0, 5.0, 25000.00, 7, N'https://images.unsplash.com/photo-1585032226651-759b368d7246?auto=format&fit=crop&w=480&q=80'),
    (N'Gạo thơm', N'kg', 50.0, 8.0, 28000.00, 180, N'https://images.unsplash.com/photo-1586208958839-06c17cacdf08?auto=format&fit=crop&w=480&q=80'),
    (N'Thịt gà', N'kg', 20.0, 3.0, 95000.00, 3, N'https://images.unsplash.com/photo-1604503468506-a8da13d82791?auto=format&fit=crop&w=480&q=80'),
    (N'Tôm tươi', N'kg', 14.0, 2.0, 210000.00, 2, N'https://images.unsplash.com/photo-1565680018434-b513d5e5fd47?auto=format&fit=crop&w=480&q=80'),
    (N'Cá hồi', N'kg', 10.0, 2.0, 320000.00, 2, N'https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=480&q=80'),
    (N'Rau củ tổng hợp', N'kg', 25.0, 4.0, 45000.00, 4, N'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=480&q=80'),
    (N'Gia vị bếp', N'kg', 12.0, 2.0, 85000.00, 365, N'https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&w=480&q=80'),
    (N'Mì Quảng', N'kg', 18.0, 3.0, 30000.00, 30, N'https://images.unsplash.com/photo-1552611052-33e04de081de?auto=format&fit=crop&w=480&q=80'),
    (N'Chanh dây', N'kg', 8.0, 1.0, 65000.00, 5, N'https://images.unsplash.com/photo-1601493700631-2b16ec4b4716?auto=format&fit=crop&w=480&q=80'),
    (N'Dưa hấu', N'kg', 20.0, 3.0, 18000.00, 5, N'https://images.unsplash.com/photo-1563114773-84221bd62daa?auto=format&fit=crop&w=480&q=80'),
    (N'Trà và đào', N'kg', 10.0, 1.0, 120000.00, 45, N'https://images.unsplash.com/photo-1499638673689-79a0b5115d87?auto=format&fit=crop&w=480&q=80'),
    (N'Nguyên liệu tráng miệng', N'kg', 12.0, 2.0, 110000.00, 10, N'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=480&q=80'),
    (N'Nước giải khát', N'chai', 60.0, 12.0, 9000.00, 180, N'https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=480&q=80')
) AS source(name, unit, quantity, min_stock, unit_price, shelf_life_days, image)
WHERE NOT EXISTS (SELECT 1 FROM dbo.ingredients target WHERE target.name = source.name);

INSERT INTO dbo.ingredient_batches (ingredient_id, quantity, import_date, expiration_date, unit_price, version)
SELECT ingredient.id, ingredient.quantity, SYSUTCDATETIME(), DATEADD(DAY, ingredient.shelf_life_days, SYSUTCDATETIME()), ingredient.unit_price, 0
FROM dbo.ingredients ingredient
WHERE ingredient.quantity > 0
  AND NOT EXISTS (SELECT 1 FROM dbo.ingredient_batches batch WHERE batch.ingredient_id = ingredient.id);

INSERT INTO dbo.recipes (product_id, ingredient_id, amount_required)
SELECT product.id, ingredient.id, source.amount_required
FROM (VALUES
    (N'Phở bò Kobe', N'Thịt bò Kobe', 0.18), (N'Phở bò Kobe', N'Bánh phở', 0.25),
    (N'Cơm rang dưa bò', N'Thịt bò Kobe', 0.12), (N'Cơm rang dưa bò', N'Gạo thơm', 0.18),
    (N'Coca Cola', N'Nước giải khát', 1.0),
    (N'Gỏi cuốn tôm thịt', N'Tôm tươi', 0.12), (N'Gỏi cuốn tôm thịt', N'Rau củ tổng hợp', 0.10),
    (N'Chả giò hải sản', N'Tôm tươi', 0.15), (N'Chả giò hải sản', N'Rau củ tổng hợp', 0.10),
    (N'Lẩu Thái hải sản', N'Tôm tươi', 0.30), (N'Lẩu Thái hải sản', N'Rau củ tổng hợp', 0.35),
    (N'Bò nướng lá lốt', N'Thịt bò Kobe', 0.25), (N'Bò nướng lá lốt', N'Rau củ tổng hợp', 0.08),
    (N'Cá hồi sốt chanh dây', N'Cá hồi', 0.25), (N'Cá hồi sốt chanh dây', N'Chanh dây', 0.05),
    (N'Cơm gà Hội An', N'Thịt gà', 0.25), (N'Cơm gà Hội An', N'Gạo thơm', 0.18),
    (N'Mì Quảng đặc biệt', N'Mì Quảng', 0.22), (N'Mì Quảng đặc biệt', N'Thịt gà', 0.15),
    (N'Nước ép dưa hấu', N'Dưa hấu', 0.35),
    (N'Trà đào cam sả', N'Trà và đào', 0.05),
    (N'Chè khúc bạch', N'Nguyên liệu tráng miệng', 0.18)
) AS source(product_name, ingredient_name, amount_required)
JOIN dbo.products product ON product.name = source.product_name
JOIN dbo.ingredients ingredient ON ingredient.name = source.ingredient_name
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.recipes recipe
    WHERE recipe.product_id = product.id AND recipe.ingredient_id = ingredient.id
);

-- A product without a recipe cannot be offered until the kitchen configures its ingredients.
UPDATE product
SET available = CASE WHEN EXISTS (SELECT 1 FROM dbo.recipes recipe WHERE recipe.product_id = product.id) THEN 1 ELSE 0 END
FROM dbo.products product;

-- Give the main presentation dishes distinct, stable images rather than repeating one placeholder.
UPDATE dbo.products
SET image = CASE id
    WHEN 1 THEN N'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=700&q=80'
    WHEN 2 THEN N'https://images.unsplash.com/photo-1603133872878-684f208fb84b?auto=format&fit=crop&w=700&q=80'
    WHEN 3 THEN N'https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=700&q=80'
    WHEN 4 THEN N'https://images.unsplash.com/photo-1543353071-10c8ba85a904?auto=format&fit=crop&w=700&q=80'
    WHEN 5 THEN N'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=700&q=80'
    WHEN 6 THEN N'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=700&q=80&sat=-25'
    WHEN 7 THEN N'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=700&q=80'
    WHEN 8 THEN N'https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=700&q=80'
    WHEN 9 THEN N'https://images.unsplash.com/photo-1603133872878-684f208fb84b?auto=format&fit=crop&w=700&q=80&sat=-30'
    WHEN 10 THEN N'https://images.unsplash.com/photo-1552611052-33e04de081de?auto=format&fit=crop&w=700&q=80'
    WHEN 11 THEN N'https://images.unsplash.com/photo-1621263764928-df1444c5e859?auto=format&fit=crop&w=700&q=80'
    WHEN 12 THEN N'https://images.unsplash.com/photo-1499638673689-79a0b5115d87?auto=format&fit=crop&w=700&q=80'
    WHEN 13 THEN N'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=700&q=80'
    ELSE image
END
WHERE id BETWEEN 1 AND 13;
