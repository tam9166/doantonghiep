-- Batch 1: replace two remote images that failed the browser semantic review.
-- IDs are stable catalog keys; do not affect other products or inactive rows.
UPDATE dbo.Products SET image = '/images/products/com-ga-hoi-an-v2.jpg'
WHERE id = 9 AND status = 1;

UPDATE dbo.Products SET image = '/images/products/tra-dao-cam-sa-v2.jpg'
WHERE id = 12 AND status = 1;
