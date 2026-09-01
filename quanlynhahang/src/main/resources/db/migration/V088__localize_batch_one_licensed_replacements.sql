-- Batch 1 final clearance: replace only products whose Commons provenance and
-- license were verified during the final review.  IDs 8 and 22 remain under
-- review and are intentionally not changed here.
UPDATE dbo.Products SET image = '/images/products/goi-cuon-tom-thit-cc0.jpg'
WHERE id = 4 AND status = 1;

UPDATE dbo.Products SET image = '/images/products/bo-nuong-la-lot-cc-by.jpg'
WHERE id = 7 AND status = 1;

UPDATE dbo.Products SET image = '/images/products/mi-quang-dac-biet-cc-by.jpg'
WHERE id = 10 AND status = 1;

UPDATE dbo.Products SET image = '/images/products/nuoc-ep-dua-hau-cc0.jpg'
WHERE id = 11 AND status = 1;

UPDATE dbo.Products SET image = '/images/products/che-khuc-bach-cc-by-sa.jpg'
WHERE id = 13 AND status = 1;
