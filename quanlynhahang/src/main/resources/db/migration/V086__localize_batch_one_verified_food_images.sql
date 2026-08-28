-- Batch 1 correction: replace two semantically incorrect remote images only
-- after visual verification and license review. Keep the update ID-scoped.
UPDATE dbo.Products
SET image = '/images/products/cha-gio-hai-san-v2.jpg'
WHERE id = 5 AND status = 1;

UPDATE dbo.Products
SET image = '/images/products/lau-thai-hai-san-v2.jpg'
WHERE id = 6 AND status = 1;
