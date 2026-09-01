-- Batch 2: replace the semantically incorrect Chả cá image with a verified,
-- locally served Commons photograph.  Keep the update ID-scoped and
-- idempotent so an already corrected catalog is not rewritten.
UPDATE products
SET image = '/images/products/cha-ca-da-nang-nuong-la-chuoi.jpg'
WHERE id = 79
  AND status = 1;
