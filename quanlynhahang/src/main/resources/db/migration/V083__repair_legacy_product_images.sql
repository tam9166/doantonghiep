-- Replace confirmed legacy/demo product images without reseeding the product table.
-- Product IDs are stable keys from the baseline menu; updates are limited to active rows.
UPDATE dbo.Products SET image = N'https://upload.wikimedia.org/wikipedia/commons/a/aa/Can_of_Coca_Cola_%2826899145485%29.jpg'
WHERE id = 3 AND status = 1;

UPDATE dbo.Products SET image = N'https://images.unsplash.com/photo-1625938145744-e3805153995a?auto=format&fit=crop&w=700&q=80'
WHERE id = 5 AND status = 1;

UPDATE dbo.Products SET image = N'https://images.unsplash.com/photo-1604909052743-94e838986d24?auto=format&fit=crop&w=700&q=80'
WHERE id = 6 AND status = 1;

UPDATE dbo.Products SET image = N'https://cdn.tgdd.vn/Files/2017/03/23/964066/cach-lam-bo-nuong-la-lot-va-nuoc-cham-thom-ngon-dung-vi-202110021300247088.jpg'
WHERE id = 7 AND status = 1;

-- The old demo rows all pointed to one generic food photo. Reuse only the
-- already approved menu assets that match each dish family.
UPDATE dbo.Products SET image = CASE id
    WHEN 14 THEN N'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=700&q=80'
    WHEN 15 THEN N'https://commons.wikimedia.org/wiki/Special:Redirect/file/Com_ga_Viet_Nam_voi_ga_luoc_com_vang_va_nuoc_cham.jpg?width=1200'
    WHEN 16 THEN N'https://images.unsplash.com/photo-1604909052743-94e838986d24?auto=format&fit=crop&w=700&q=80'
    WHEN 17 THEN N'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=700&q=80'
    WHEN 18 THEN N'https://images.unsplash.com/photo-1543353071-10c8ba85a904?auto=format&fit=crop&w=700&q=80'
    WHEN 19 THEN N'https://images.unsplash.com/photo-1625938145744-e3805153995a?auto=format&fit=crop&w=700&q=80'
    WHEN 20 THEN N'https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=700&q=80'
    WHEN 21 THEN N'https://images.unsplash.com/photo-1621263764928-df1444c5e859?auto=format&fit=crop&w=700&q=80'
    ELSE image
END
WHERE id BETWEEN 14 AND 21 AND status = 1;

UPDATE dbo.Products SET image = N'https://images.unsplash.com/photo-1551218808-94e220e084d2?auto=format&fit=crop&w=700&q=80'
WHERE id = 72 AND status = 1;
