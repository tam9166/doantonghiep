IF NOT EXISTS (SELECT 1 FROM dbo.restaurant_settings WHERE setting_key = 'min_profit_margin_percent')
    INSERT INTO dbo.restaurant_settings(setting_key, setting_value, description, version)
    VALUES ('min_profit_margin_percent', '30.00', N'Biên lợi nhuận mục tiêu tối thiểu tính trên doanh thu', 0);
GO

IF OBJECT_ID('tempdb..#menu_cost_audit') IS NOT NULL DROP TABLE #menu_cost_audit;

SELECT product.id,
       product.name,
       product.price old_price,
       product.cost_price old_cost_price,
       CAST(SUM(recipe.amount_required * ingredient.unit_price) AS DECIMAL(18,2)) calculated_cost
INTO #menu_cost_audit
FROM dbo.products product
JOIN dbo.recipes recipe ON recipe.product_id = product.id
JOIN dbo.ingredients ingredient ON ingredient.id = recipe.ingredient_id
WHERE recipe.amount_required > 0 AND ingredient.unit_price >= 0
GROUP BY product.id, product.name, product.price, product.cost_price;

UPDATE product
SET cost_price = audit.calculated_cost
FROM dbo.products product
JOIN #menu_cost_audit audit ON audit.id = product.id;

INSERT INTO dbo.activity_logs(username, action, entity_type, entity_id, description,
                              old_value, new_value, [timestamp])
SELECT 'SYSTEM', 'AUTO_PAUSE_NEGATIVE_MARGIN', 'Product', CONVERT(varchar(50), audit.id),
       N'Tạm dừng món đang bán không cao hơn giá vốn sau khi đồng bộ giá vốn từ công thức',
       CONCAT(N'dishId=', audit.id, N' | dishName=', audit.name,
              N' | price=', audit.old_price, N' | legacyCostPrice=', audit.old_cost_price),
       CONCAT(N'dishId=', audit.id, N' | dishName=', audit.name,
              N' | price=', audit.old_price, N' | costPrice=', audit.calculated_cost,
              N' | marginStatus=NEGATIVE_MARGIN'),
       SYSUTCDATETIME()
FROM #menu_cost_audit audit
JOIN dbo.products product ON product.id = audit.id
WHERE (product.status = 1 OR product.available = 1)
  AND product.price <= audit.calculated_cost;

UPDATE product
SET status = 0, available = 0
FROM dbo.products product
JOIN #menu_cost_audit audit ON audit.id = product.id
WHERE product.price <= audit.calculated_cost;

DROP TABLE #menu_cost_audit;
GO
