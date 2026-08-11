UPDATE dbo.table_areas
SET name_vi = N'Tầng 2 - Sảnh tiệc',
    description_vi = N'Không gian rộng cho gia đình và nhóm đông.',
    image_url = N'https://upload.wikimedia.org/wikipedia/commons/8/8c/Banquet_Room_interior%2C_Brother_Hotel_20130212.jpg',
    updated_at = SYSUTCDATETIME()
WHERE name_en = N'Level 2 - Banquet Hall';

UPDATE dbo.table_areas
SET name_vi = N'Tầng 3-5 - Phòng VIP',
    description_vi = N'Phòng riêng yên tĩnh, phù hợp tiếp khách và sinh nhật.',
    image_url = N'https://upload.wikimedia.org/wikipedia/commons/2/2a/Jump_Restaurant_Private_Dining_Room_%289160005659%29.jpg',
    updated_at = SYSUTCDATETIME()
WHERE name_en = N'Level 3-5 - Private Rooms';

UPDATE dbo.table_areas
SET name_vi = N'Tầng 6 - Sân thượng',
    description_vi = N'Không gian ngoài trời với view phố, sông và sân vườn.',
    image_url = N'https://upload.wikimedia.org/wikipedia/commons/2/26/Thiranagama_Sri_Lanka_Riff_Hotel_5-star_resort_Rooftop_sunset_dining.jpg',
    updated_at = SYSUTCDATETIME()
WHERE name_en = N'Level 6 - Rooftop';

UPDATE rt
SET image_url = ta.image_url
FROM dbo.restaurant_table rt
JOIN dbo.table_areas ta ON ta.id = rt.area_id
WHERE rt.image_url IS NULL OR LTRIM(RTRIM(rt.image_url)) = N'' OR rt.image_url LIKE N'/images/%';
