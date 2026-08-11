-- Mỗi món phải có công thức và tồn kho trước khi được mở bán cho khách.
INSERT INTO dbo.ingredients (name, unit, quantity, min_stock, unit_price, shelf_life_days, image)
SELECT source.name, N'kg', 20.0, 2.0, source.unit_price, 7, source.image
FROM (VALUES
    (N'Rau và bánh tráng cuốn mộc', 45000.00, N'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=480&q=80'),
    (N'Rau rừng Đà Nẵng', 55000.00, N'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=480&q=80'),
    (N'Thịt heo cuốn', 125000.00, N'https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=480&q=80'),
    (N'Đậu hũ non', 38000.00, N'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=480&q=80'),
    (N'Chả cá Đà Nẵng', 145000.00, N'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=480&q=80'),
    (N'Cá kho tộ', 120000.00, N'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=480&q=80'),
    (N'Cá lóc tươi', 110000.00, N'https://images.unsplash.com/photo-1559847844-5315695dadae?auto=format&fit=crop&w=480&q=80'),
    (N'Bò một nắng', 260000.00, N'https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=480&q=80'),
    (N'Sườn heo', 135000.00, N'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=480&q=80'),
    (N'Thịt ba chỉ', 120000.00, N'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=480&q=80'),
    (N'Rau tập tàng', 42000.00, N'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=480&q=80'),
    (N'Rau lang', 32000.00, N'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=480&q=80'),
    (N'Đậu bắp và bí đỏ', 35000.00, N'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=480&q=80'),
    (N'Bún tươi', 28000.00, N'https://images.unsplash.com/photo-1585032226651-759b368d7246?auto=format&fit=crop&w=480&q=80'),
    (N'Nguyên liệu chè mộc', 75000.00, N'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=480&q=80')
) AS source(name, unit_price, image)
WHERE NOT EXISTS (SELECT 1 FROM dbo.ingredients target WHERE target.name = source.name);

INSERT INTO dbo.ingredient_batches (ingredient_id, quantity, import_date, expiration_date, unit_price, version)
SELECT ingredient.id, ingredient.quantity, SYSUTCDATETIME(), DATEADD(DAY, ingredient.shelf_life_days, SYSUTCDATETIME()), ingredient.unit_price, 0
FROM dbo.ingredients ingredient
WHERE ingredient.quantity > 0
  AND NOT EXISTS (SELECT 1 FROM dbo.ingredient_batches batch WHERE batch.ingredient_id = ingredient.id);

INSERT INTO dbo.recipes (product_id, ingredient_id, amount_required)
SELECT product.id, ingredient.id, source.amount_required
FROM (VALUES
    (N'Gỏi cuốn mộc', N'Rau và bánh tráng cuốn mộc', 0.18),
    (N'Nộm rau rừng Đà Nẵng', N'Rau rừng Đà Nẵng', 0.20),
    (N'Bánh tráng cuốn thịt heo', N'Thịt heo cuốn', 0.22),
    (N'Đậu hũ non sốt mắm mộc', N'Đậu hũ non', 0.18),
    (N'Chả cá Đà Nẵng nướng lá chuối', N'Chả cá Đà Nẵng', 0.20),
    (N'Cá kho tộ mộc mạc', N'Cá kho tộ', 0.25),
    (N'Cá lóc nướng trui', N'Cá lóc tươi', 0.30),
    (N'Gà nướng muối ớt bản mộc', N'Thịt gà', 0.28),
    (N'Bò một nắng chấm muối kiến vàng', N'Bò một nắng', 0.18),
    (N'Sườn nướng mật mía', N'Sườn heo', 0.28),
    (N'Tôm rang me vườn nhà', N'Tôm tươi', 0.22),
    (N'Thịt kho tàu lá mơ', N'Thịt ba chỉ', 0.25),
    (N'Canh chua cá lóc', N'Cá lóc tươi', 0.18),
    (N'Canh rau tập tàng nấu tôm', N'Rau tập tàng', 0.16),
    (N'Rau lang luộc chấm mắm nêm', N'Rau lang', 0.18),
    (N'Đậu bắp bí đỏ hấp nước cốt dừa', N'Đậu bắp và bí đỏ', 0.20),
    (N'Mì Quảng Đà Nẵng chuẩn vị', N'Mì Quảng', 0.22),
    (N'Cơm niêu cá kho + canh rau mộc', N'Gạo thơm', 0.20),
    (N'Bún mắm nêm Đà Nẵng', N'Bún tươi', 0.22),
    (N'Chè mộc', N'Nguyên liệu chè mộc', 0.18)
) AS source(product_name, ingredient_name, amount_required)
JOIN dbo.products product ON product.name = source.product_name
JOIN dbo.ingredients ingredient ON ingredient.name = source.ingredient_name
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.recipes recipe
    WHERE recipe.product_id = product.id AND recipe.ingredient_id = ingredient.id
);

-- Món thiếu công thức hoặc thiếu bất kỳ nguyên liệu nào sẽ hiện là "Tạm hết".
UPDATE product
SET available = CASE
    WHEN product.status = 1
     AND EXISTS (SELECT 1 FROM dbo.recipes recipe WHERE recipe.product_id = product.id)
     AND NOT EXISTS (
         SELECT 1
         FROM dbo.recipes recipe
         LEFT JOIN dbo.ingredients ingredient ON ingredient.id = recipe.ingredient_id
         WHERE recipe.product_id = product.id
           AND (recipe.amount_required IS NULL OR recipe.amount_required <= 0
                OR ingredient.quantity IS NULL OR ingredient.quantity < recipe.amount_required)
     ) THEN 1
    ELSE 0
END
FROM dbo.products product;
