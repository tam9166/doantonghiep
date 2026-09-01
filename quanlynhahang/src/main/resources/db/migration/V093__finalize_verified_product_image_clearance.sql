-- Final clearance: localize only branded products whose exact variant and
-- reusable source license were verified. Match the unique logical product so
-- clean and upgraded databases are safe even when identity values differ.

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Bia 333' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/bia-333-cc-by-sa.jpg'
    WHERE name = N'Bia 333' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Larue' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/larue-beer-public-domain.jpg'
    WHERE name = N'Larue' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Corona Extra' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/corona-extra-cc-by-sa.png'
    WHERE name = N'Corona Extra' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Hoegaarden' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/hoegaarden-original-cc0.jpg'
    WHERE name = N'Hoegaarden' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Casillero del Diablo Cabernet Sauvignon' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/casillero-del-diablo-cabernet-sauvignon-cc-by-sa.jpg'
    WHERE name = N'Casillero del Diablo Cabernet Sauvignon' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Johnnie Walker Black Label' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/johnnie-walker-black-label-cc-by-sa.jpg'
    WHERE name = N'Johnnie Walker Black Label' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Chivas Regal 12' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/chivas-regal-12-cc-by-sa.jpg'
    WHERE name = N'Chivas Regal 12' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Ballantine''s Finest' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/ballantines-finest-cc-by-sa.jpg'
    WHERE name = N'Ballantine''s Finest' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Jack Daniel''s Old No.7' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/jack-daniels-old-no-7-cc0.jpg'
    WHERE name = N'Jack Daniel''s Old No.7' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Jameson' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/jameson-original-cc-by-sa.jpg'
    WHERE name = N'Jameson' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Smirnoff Red' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/smirnoff-red-cc-by.jpg'
    WHERE name = N'Smirnoff Red' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Grey Goose' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/grey-goose-cc-by.jpg'
    WHERE name = N'Grey Goose' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Belvedere' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/belvedere-vodka-cc-by-sa.jpg'
    WHERE name = N'Belvedere' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Rémy Martin VSOP' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/remy-martin-vsop-cc-by.jpg'
    WHERE name = N'Rémy Martin VSOP' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Martell VSOP' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/martell-vsop-cc0.jpg'
    WHERE name = N'Martell VSOP' AND status = 1 AND available = 1;

IF (SELECT COUNT(*) FROM dbo.Products WHERE name = N'Courvoisier VSOP' AND status = 1 AND available = 1) = 1
    UPDATE dbo.Products SET image = N'/images/products/courvoisier-vsop-cc-by-sa.jpg'
    WHERE name = N'Courvoisier VSOP' AND status = 1 AND available = 1;
