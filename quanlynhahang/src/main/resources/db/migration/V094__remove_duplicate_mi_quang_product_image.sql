-- The final 71-product duplicate pass found that two logical Mì Quảng menu
-- rows used the same photograph through different paths. Keep the existing
-- special-dish image and assign the unique Da Nang row a distinct verified
-- Commons photograph without assuming its generated identity value.

IF (SELECT COUNT(*) FROM dbo.Products
    WHERE name = N'Mì Quảng Đà Nẵng chuẩn vị' AND status = 1 AND available = 1) = 1
BEGIN
    UPDATE dbo.Products
    SET image = N'/images/products/mi-quang-da-nang-cc-by-sa.jpg'
    WHERE name = N'Mì Quảng Đà Nẵng chuẩn vị' AND status = 1 AND available = 1;
END;
