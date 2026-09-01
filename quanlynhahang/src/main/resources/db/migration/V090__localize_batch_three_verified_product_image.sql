-- Batch 3: replace the rights-unverified, tightly cropped Saigon Special
-- catalog image with a verified Wikimedia Commons photograph.
UPDATE products
SET image = '/images/products/saigon-special-cc-by-sa.jpg'
WHERE id = 382
  AND status = 1;
