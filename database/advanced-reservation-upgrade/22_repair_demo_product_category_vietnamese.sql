-- Repair known mojibake values in the local demo product/category seed data.
-- The WHERE clause keeps this idempotent and avoids overwriting already-correct names.

UPDATE dbo.products SET name = N'Gỏi cuốn tôm thịt'
WHERE id = 4 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%';

UPDATE dbo.products SET name = N'Chả giò hải sản'
WHERE id = 5 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%';

UPDATE dbo.products SET name = N'Lẩu Thái hải sản'
WHERE id = 6 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%';

UPDATE dbo.products SET name = N'Bò nướng lá lốt'
WHERE id = 7 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%';

UPDATE dbo.products SET name = N'Cá hồi sốt chanh dây'
WHERE id = 8 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%';

UPDATE dbo.products SET name = N'Cơm gà Hội An'
WHERE id = 9 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%';

UPDATE dbo.products SET name = N'Mì Quảng đặc biệt'
WHERE id = 10 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%';

UPDATE dbo.products SET name = N'Nước ép dưa hấu'
WHERE id = 11 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%';

UPDATE dbo.products SET name = N'Trà đào cam sả'
WHERE id = 12 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%Ä%';

UPDATE dbo.products SET name = N'Chè khúc bạch'
WHERE id = 13 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%';

UPDATE dbo.categories SET name = N'Đồ uống'
WHERE id = 2 AND (
       name COLLATE Latin1_General_100_BIN2 LIKE N'%�%'
    OR name COLLATE Latin1_General_100_BIN2 LIKE N'%?%'
    OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Ð%'
);

UPDATE dbo.categories SET name = N'Khai vị'
WHERE id = 3 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%';

UPDATE dbo.categories SET name = N'Lẩu & nướng'
WHERE id = 4 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%';

UPDATE dbo.categories SET name = N'Tráng miệng'
WHERE id = 5 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%';

UPDATE dbo.categories SET name = N'Món chính'
WHERE id = 6 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%Ă%';

UPDATE dbo.categories SET name = N'Đồ uống'
WHERE id = 7 AND name COLLATE Latin1_General_100_BIN2 LIKE N'%Ä%';

SELECT id, name FROM dbo.products WHERE id BETWEEN 4 AND 13 ORDER BY id;
SELECT id, name FROM dbo.categories WHERE id BETWEEN 1 AND 8 ORDER BY id;
