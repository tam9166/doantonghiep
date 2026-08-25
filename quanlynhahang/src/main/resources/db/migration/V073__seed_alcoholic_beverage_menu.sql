SET NOCOUNT ON;

IF COL_LENGTH(N'dbo.Products', N'volume_ml') IS NULL
    EXEC(N'ALTER TABLE dbo.Products ADD volume_ml INT NULL');

IF COL_LENGTH(N'dbo.Products', N'alcohol_percentage') IS NULL
    EXEC(N'ALTER TABLE dbo.Products ADD alcohol_percentage DECIMAL(5,2) NULL');

EXEC(N'ALTER TABLE dbo.Products ALTER COLUMN image NVARCHAR(1000) NULL');

GO

DECLARE @Beverages TABLE (
    category_name NVARCHAR(200) NOT NULL,
    product_name NVARCHAR(200) NOT NULL,
    volume_ml INT NOT NULL,
    alcohol_percentage DECIMAL(5,2) NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    cost_price DECIMAL(18,2) NOT NULL,
    image_url NVARCHAR(1000) NOT NULL,
    description_vi NVARCHAR(1000) NOT NULL
);

INSERT INTO @Beverages VALUES
(N'Bia Việt Nam',N'Saigon Special',330,4.90,39000,21000,N'https://saigonbeer.com.au/photos/products/saigon_special_69a3fa76_ae1e_486b_9a8a_950b94423bdf.png',N'Lon 330ml, 4.9% ABV; lager Việt Nam vị malt cân bằng, dùng lạnh.'),
(N'Bia Việt Nam',N'Saigon Lager',330,4.30,32000,17000,N'https://sabibeco.com/uploads/product/2020_03/bia-lon-saigon-lager.jpg',N'Lon 330ml, 4.3% ABV; lager nhẹ, hậu vị gọn.'),
(N'Bia Việt Nam',N'Bia 333',330,5.30,35000,18000,N'https://www.ikemitsu.co.jp/_bosys/wp-content/uploads/2024/02/333_can.png',N'Lon 330ml, 5.3% ABV; hương malt rõ và vị đắng vừa.'),
(N'Bia Việt Nam',N'Bia Hà Nội',330,4.60,34000,18000,N'https://biahanoi.muabianhanh.com/wp-content/uploads/2023/11/Web_HNP2lon2-2048x2048.jpg',N'Lon 330ml, 4.6% ABV; phong cách lager truyền thống miền Bắc.'),
(N'Bia Việt Nam',N'Larue',330,4.20,32000,17000,N'https://heineken-vietnam.com.vn/images/2022/larue-smooth-20221130-b.jpg',N'Lon 330ml, 4.2% ABV; vị êm, tươi mát.'),
(N'Bia quốc tế',N'Heineken',330,5.00,55000,31000,N'https://upload.wikimedia.org/wikipedia/commons/8/8d/CreativeTools.se_-_PackshotCreator_-_Heineken_beer_bottle_v01_%284290167332%29.jpg',N'Chai 330ml, 5% ABV; lager cân bằng với hậu vị thanh.'),
(N'Bia quốc tế',N'Tiger',330,5.00,49000,27000,N'https://upload.wikimedia.org/wikipedia/commons/9/99/Tiger_Beer_Bottles.png',N'Chai 330ml, 5% ABV; lager đậm vừa, dùng lạnh.'),
(N'Bia quốc tế',N'Budweiser',330,5.00,59000,34000,N'https://upload.wikimedia.org/wikipedia/commons/f/f1/Budweiser_beer.jpg',N'Chai 330ml, 5% ABV; lager kiểu Mỹ, vị sạch và nhẹ.'),
(N'Bia quốc tế',N'Corona Extra',355,4.50,69000,41000,N'https://www.bargainbooze.co.uk/wp-content/uploads/2022/11/corona620.png',N'Chai 355ml, 4.5% ABV; lager Mexico nhẹ và sảng khoái.'),
(N'Bia quốc tế',N'Hoegaarden',330,4.90,79000,47000,N'https://cdn.shopify.com/s/files/1/2017/0991/products/Hoegaarden_Anno_1445_1400x.jpeg?v=1512614237',N'Chai 330ml, 4.9% ABV; bia lúa mì Bỉ thơm cam và gia vị.'),
(N'Vang đỏ',N'Jacob''s Creek Cabernet Sauvignon',750,13.50,590000,360000,N'https://solidwineonline.com/cdn/shop/files/JacobsCreekCabernetSauvignon.png?v=1706082353',N'Chai 750ml, 13.5% ABV; vang đỏ Úc với trái cây chín và tannin vừa.'),
(N'Vang đỏ',N'Casillero del Diablo Cabernet Sauvignon',750,13.50,650000,390000,N'https://i5.walmartimages.com/seo/Casillero-del-Diablo-Cabernet-Sauvignon-Chile-750-ml-Bottle-14-ABV_c48e2eef-d05c-4303-9efa-683cf4a46e7c.d5863bf24160e39109531e262b043c76.png',N'Chai 750ml, 13.5% ABV; vang Chile đậm vừa, hợp món nướng.'),
(N'Vang đỏ',N'Yellow Tail Shiraz',750,13.50,620000,370000,N'https://www.cebooze.com/app/uploads/2020/09/yellow-tail-shiraz-800x800.jpg',N'Chai 750ml, 13.5% ABV; Shiraz Úc mềm, hương mận và gia vị.'),
(N'Vang đỏ',N'Penfolds Koonunga Hill Shiraz Cabernet',750,14.00,890000,540000,N'https://cdn.metcash.media/image/upload/w_1500,h_1500,c_pad,b_auto/alm-online/images/855798.jpg',N'Chai 750ml, 14% ABV; phối trộn Shiraz Cabernet cân bằng.'),
(N'Vang đỏ',N'Château Los Boldos Cabernet Sauvignon',750,14.00,820000,490000,N'https://images.tcdn.com.br/img/img_prod/1199398/vinho_chanteau_los_boldos_gran_reserva_cabernet_sauvignon_750ml_2937_2_952b139c9e03c5de27dd43c88de28c65.jpg',N'Chai 750ml, 14% ABV; Cabernet Sauvignon Chile tròn vị.'),
(N'Vang trắng',N'Jacob''s Creek Chardonnay',750,12.50,590000,350000,N'https://mickeykellysbar.com/wp-content/uploads/2020/06/jacobs-creek-chardonnay.jpg',N'Chai 750ml, 12.5% ABV; Chardonnay Úc tươi, hương trái cây.'),
(N'Vang trắng',N'Casillero del Diablo Sauvignon Blanc',750,13.00,650000,390000,N'https://i5.walmartimages.com/seo/Casillero-del-Diablo-Sauvignon-Blanc-Chile-750-ml-Glass-Bottle-13-ABV_12d7e3c4-1c95-438b-b9ad-9aad6bcc8a9e.3351ca5d0cc6d490d3e8a8cc0f80fb57.png',N'Chai 750ml, 13% ABV; vang trắng Chile thanh mát.'),
(N'Vang trắng',N'Yellow Tail Chardonnay',750,13.00,620000,370000,N'https://i5.walmartimages.com/seo/Yellow-Tail-Chardonnay-Australia-White-Wine-750-ml-Bottle-13-ABV_1513083c-804b-40ca-948c-599e8554322b.7bfa9d8650c8ad1510703e782b5974f9.jpeg',N'Chai 750ml, 13% ABV; Chardonnay mềm, hương đào và vani nhẹ.'),
(N'Vang trắng',N'Oyster Bay Sauvignon Blanc',750,13.00,890000,550000,N'https://www.oysterbaywines.com/uploads/SB-24-wGlass-Blue-gradient-1980x1988px.jpg',N'Chai 750ml, 13% ABV; Sauvignon Blanc New Zealand tươi giòn.'),
(N'Vang trắng',N'Villa Maria Sauvignon Blanc',750,13.00,920000,570000,N'https://www.saq.com/media/catalog/product/1/1/11974951-1_1659641150.png',N'Chai 750ml, 13% ABV; vang trắng New Zealand thơm thảo mộc.'),
(N'Whisky',N'Johnnie Walker Black Label',700,40.00,1450000,980000,N'https://images.ctfassets.net/waruwpig3jxu/rYk8WxpJ1ellZvibRckWT/0e8df23f16b39c6d986faf6f68af8fee/black-750ml_producthero_lifestyle-01_desktop.webp',N'Chai 700ml, 40% ABV; blended Scotch có hương khói và trái cây.'),
(N'Whisky',N'Chivas Regal 12',700,40.00,1350000,900000,N'https://cdn.metcash.media/image/upload/w_1500,h_1500,c_pad,b_auto/alm-online/images/583139.jpg',N'Chai 700ml, 40% ABV; Scotch 12 năm êm, hương mật ong.'),
(N'Whisky',N'Ballantine''s Finest',700,40.00,890000,590000,N'https://ww1.valuecellars.com.au/files/2016/05/5010106113127-1.png',N'Chai 700ml, 40% ABV; blended Scotch cân bằng và dễ uống.'),
(N'Whisky',N'Jack Daniel''s Old No.7',700,40.00,1150000,760000,N'https://cdn.selection-prestige.de/media/catalog/product/cache/image/1536x/a4e40ebdc3e371adff845072e1c73f37/9/9/99733_jack-daniels-old-no-7-tennessee-whiskey-10l-40-vol.jpg',N'Chai 700ml, 40% ABV; Tennessee whiskey hương caramel và gỗ sồi.'),
(N'Whisky',N'Jameson',700,40.00,1050000,690000,N'https://www.finewinedelivery.co.nz/content/products/original/16561.jpg?width=1136',N'Chai 700ml, 40% ABV; Irish whiskey êm và thanh.'),
(N'Vodka',N'Absolut',700,40.00,890000,570000,N'https://www.absolut.com/wp-content/uploads/absolut-vodka-original-2021-against-white-background.jpg?imwidth=350',N'Chai 700ml, 40% ABV; vodka Thụy Điển sạch và cân bằng.'),
(N'Vodka',N'Smirnoff Red',700,37.50,690000,440000,N'https://cdn.metcash.media/image/upload/w_1500,h_1500,c_pad,b_auto/alm-online/images/591815.jpg',N'Chai 700ml, 37.5% ABV; vodka trung tính, phù hợp pha chế.'),
(N'Vodka',N'Finlandia',700,40.00,790000,510000,N'https://ie.coca-colahellenic.com/content/dam/cch/ie/images/our-24-7-portfolio_new/FINLANDIA%20ORIGINAL%20Bottle.jpg',N'Chai 700ml, 40% ABV; vodka Phần Lan trong, vị gọn.'),
(N'Vodka',N'Grey Goose',700,40.00,1650000,1120000,N'https://www.greygoose.com/binaries/content/gallery/greygoose/products/grey-goose-vodka/ggo-bottle-intl.png',N'Chai 700ml, 40% ABV; vodka Pháp mềm và tinh tế.'),
(N'Vodka',N'Belvedere',700,40.00,1550000,1050000,N'https://liquorshop.hk/wp-content/uploads/2020/08/Belvedere-Vodka.jpg',N'Chai 700ml, 40% ABV; vodka Ba Lan từ lúa mạch đen.'),
(N'Cognac / Brandy',N'Hennessy VS',700,40.00,1850000,1280000,N'https://www.hennessy.com/sites/hennessy/files/2020-01/VS_0.png',N'Chai 700ml, 40% ABV; cognac trẻ, hương trái cây và gỗ sồi.'),
(N'Cognac / Brandy',N'Rémy Martin VSOP',700,40.00,2450000,1690000,N'https://static-prod.remymartin.com/app/uploads/2023/11/vsop-collection-1600-front-02.png',N'Chai 700ml, 40% ABV; cognac VSOP tròn vị và nhiều tầng hương.'),
(N'Cognac / Brandy',N'Martell VSOP',700,40.00,2250000,1540000,N'https://devinecellars.com.au/wp-content/uploads/Martell-VSOP-Cognac.jpg',N'Chai 700ml, 40% ABV; cognac VSOP mềm với hương trái cây.'),
(N'Cognac / Brandy',N'Courvoisier VSOP',700,40.00,2350000,1600000,N'https://worldsbestwines.eu/wp-content/uploads/Courvoisier-VSOP-70cl-Bottle.jpg',N'Chai 700ml, 40% ABV; cognac VSOP cân bằng, hậu vị dài.'),
(N'Cognac / Brandy',N'Camus VSOP',700,40.00,2550000,1760000,N'https://cdn.shopify.com/s/files/1/0050/2395/7103/products/cognac-intensely-aromatic-camus-vsop-confezione_33540_zoom_700x.jpg?v=1670500723',N'Chai 700ml, 40% ABV; cognac VSOP thơm đậm và thanh lịch.'),
(N'Sake',N'Dassai 45',720,16.00,1150000,760000,N'https://cdn.shopify.com/s/files/1/0212/1922/products/dassai_45_1020x.progressive.jpg?v=1615324785',N'Chai 720ml, 16% ABV; Junmai Daiginjo thanh, hương trái cây.'),
(N'Sake',N'Gekkeikan Traditional',750,15.60,690000,440000,N'https://us.gekkeikan.com/wp-content/uploads/2020/03/TRADITIONAL-1.5-FRONT-1152x1536.png',N'Chai 750ml, 15.6% ABV; sake truyền thống cân bằng.'),
(N'Sake',N'Hakutsuru Junmai',720,15.50,790000,510000,N'https://aem.lcbo.com/content/dam/lcbo/products/0/1/2/8/012849.jpg.thumb.1280.1280.jpg',N'Chai 720ml, 15.5% ABV; Junmai đậm vừa, dùng ấm hoặc lạnh.'),
(N'Sake',N'Ozeki Junmai',750,14.50,720000,470000,N'https://drinxmarket.com/wp-content/uploads/2023/05/104776.png',N'Chai 750ml, 14.5% ABV; Junmai êm và dễ kết hợp món ăn.'),
(N'Sake',N'Kubota Senju',720,15.00,1250000,830000,N'https://images.squarespace-cdn.com/content/v1/5c334aca372b96b6bfd22e33/1600792366965-DERDSNOLLSP1ZZMNV7GV/Kubota+Senju+Tokubetsu+Honjozo+720ml+2000x2000+%281%29.jpg',N'Chai 720ml, 15% ABV; Honjozo khô thanh, hậu vị sạch.');

MERGE dbo.Categories WITH (HOLDLOCK) AS target
USING (SELECT DISTINCT category_name FROM @Beverages) AS source
ON LOWER(LTRIM(RTRIM(target.name))) = LOWER(LTRIM(RTRIM(source.category_name)))
WHEN MATCHED THEN UPDATE SET target.name_vi = source.category_name
WHEN NOT MATCHED THEN INSERT (name, name_vi, name_en)
VALUES (source.category_name, source.category_name, source.category_name);

;WITH BeverageSource AS (
    SELECT beverage.*, category.id AS category_id
    FROM @Beverages beverage
    CROSS APPLY (SELECT TOP (1) id FROM dbo.Categories
                 WHERE LOWER(LTRIM(RTRIM(name))) = LOWER(LTRIM(RTRIM(beverage.category_name))) ORDER BY id) category
)
MERGE dbo.Products WITH (HOLDLOCK) AS target
USING BeverageSource AS source
ON LOWER(LTRIM(RTRIM(target.name))) = LOWER(LTRIM(RTRIM(source.product_name)))
WHEN MATCHED THEN UPDATE SET target.price=source.price, target.cost_price=source.cost_price,
    target.tax_rate=10, target.image=source.image_url, target.description=source.description_vi,
    target.description_vi=source.description_vi, target.available=1, target.status=1,
    target.category_id=source.category_id, target.volume_ml=source.volume_ml,
    target.alcohol_percentage=source.alcohol_percentage
WHEN NOT MATCHED THEN INSERT
    (name,name_vi,name_en,price,cost_price,tax_rate,image,description,description_vi,description_en,
     create_date,available,status,category_id,volume_ml,alcohol_percentage)
VALUES (source.product_name,source.product_name,source.product_name,source.price,source.cost_price,10,
    source.image_url,source.description_vi,source.description_vi,source.description_vi,CAST(GETDATE() AS DATE),
    1,1,source.category_id,source.volume_ml,source.alcohol_percentage);

INSERT INTO dbo.ingredients (name,unit,quantity,min_stock,unit_price,shelf_life_days,image)
SELECT N'Tồn kho - '+product_name,N'chai/lon',120,12,cost_price,730,image_url
FROM @Beverages source
WHERE NOT EXISTS (SELECT 1 FROM dbo.ingredients target WHERE target.name=N'Tồn kho - '+source.product_name);

INSERT INTO dbo.ingredient_batches (ingredient_id,quantity,import_date,expiration_date,unit_price,version)
SELECT ingredient.id,120,SYSUTCDATETIME(),DATEADD(DAY,730,SYSUTCDATETIME()),ingredient.unit_price,0
FROM dbo.ingredients ingredient
JOIN @Beverages source ON ingredient.name=N'Tồn kho - '+source.product_name
WHERE NOT EXISTS (SELECT 1 FROM dbo.ingredient_batches batch WHERE batch.ingredient_id=ingredient.id);

INSERT INTO dbo.recipes (product_id,ingredient_id,amount_required)
SELECT product.id,ingredient.id,1
FROM @Beverages source
JOIN dbo.Products product ON product.name=source.product_name
JOIN dbo.ingredients ingredient ON ingredient.name=N'Tồn kho - '+source.product_name
WHERE NOT EXISTS (SELECT 1 FROM dbo.recipes recipe
                  WHERE recipe.product_id=product.id AND recipe.ingredient_id=ingredient.id);
