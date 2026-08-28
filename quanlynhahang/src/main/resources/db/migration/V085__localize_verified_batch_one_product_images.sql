-- Batch 1: only images visually verified from licensed original sources are localized.
UPDATE dbo.Products SET image = '/images/products/goi-cuon-tom-thit.jpg'
WHERE id = 4 AND status = 1;

UPDATE dbo.Products SET image = '/images/products/mi-quang-dac-biet.jpg'
WHERE id = 10 AND status = 1;

UPDATE dbo.Products SET image = '/images/products/nuoc-ep-dua-hau.jpg'
WHERE id = 11 AND status = 1;

UPDATE dbo.Products SET image = '/images/products/che-khuc-bach.jpg'
WHERE id = 13 AND status = 1;
