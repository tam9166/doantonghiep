IF COL_LENGTH('dbo.Products', 'name_vi') IS NULL
    ALTER TABLE dbo.Products ADD name_vi NVARCHAR(200) NULL;
IF COL_LENGTH('dbo.Products', 'name_en') IS NULL
    ALTER TABLE dbo.Products ADD name_en NVARCHAR(200) NULL;
IF COL_LENGTH('dbo.Products', 'description_vi') IS NULL
    ALTER TABLE dbo.Products ADD description_vi NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.Products', 'description_en') IS NULL
    ALTER TABLE dbo.Products ADD description_en NVARCHAR(MAX) NULL;

IF COL_LENGTH('dbo.Categories', 'name_vi') IS NULL
    ALTER TABLE dbo.Categories ADD name_vi NVARCHAR(200) NULL;
IF COL_LENGTH('dbo.Categories', 'name_en') IS NULL
    ALTER TABLE dbo.Categories ADD name_en NVARCHAR(200) NULL;

GO

UPDATE dbo.Products
SET name_vi = COALESCE(NULLIF(name_vi, N''), name),
    description_vi = COALESCE(NULLIF(description_vi, N''), description),
    name_en = COALESCE(NULLIF(name_en, N''), name),
    description_en = COALESCE(NULLIF(description_en, N''), description);

UPDATE dbo.Categories
SET name_vi = COALESCE(NULLIF(name_vi, N''), name),
    name_en = COALESCE(NULLIF(name_en, N''), name);

UPDATE dbo.Categories
SET name_en = CASE name_vi
    WHEN N'Món chính' THEN N'Main dishes'
    WHEN N'Đồ uống' THEN N'Beverages'
    WHEN N'Khai vị' THEN N'Appetizers'
    WHEN N'Lẩu & nướng' THEN N'Hotpot and grill'
    WHEN N'Tráng miệng' THEN N'Desserts'
    WHEN N'Lẩu' THEN N'Hotpot'
    WHEN N'Nướng' THEN N'Grill'
    WHEN N'Cơm - Mì' THEN N'Rice and noodles'
    WHEN N'Hải sản' THEN N'Seafood'
    WHEN N'Món chay' THEN N'Vegetarian dishes'
    WHEN N'Combo/Set tiệc' THEN N'Party sets'
    ELSE name_en
END;

UPDATE dbo.Products
SET name_en = CASE id
    WHEN 1 THEN N'Kobe beef pho'
    WHEN 2 THEN N'Fried rice with pickled mustard greens and beef'
    WHEN 3 THEN N'Coca-Cola'
    WHEN 4 THEN N'Shrimp and pork fresh spring rolls'
    WHEN 5 THEN N'Seafood spring rolls'
    WHEN 6 THEN N'Thai seafood hotpot'
    WHEN 7 THEN N'Grilled beef wrapped in betel leaves'
    WHEN 8 THEN N'Pan-seared salmon with passion fruit sauce'
    WHEN 9 THEN N'Hoi An chicken rice'
    WHEN 10 THEN N'Special Quang noodles'
    WHEN 11 THEN N'Fresh watermelon juice'
    WHEN 12 THEN N'Peach, orange and lemongrass tea'
    WHEN 13 THEN N'Almond panna cotta dessert'
    WHEN 22 THEN N'Coconut heart salad with shrimp and pork'
    WHEN 23 THEN N'Hai Phong crab spring rolls'
    WHEN 24 THEN N'Seafood spring rolls with mayonnaise'
    WHEN 25 THEN N'Seared beef salad'
    WHEN 26 THEN N'Le Pho taro croquettes'
    WHEN 27 THEN N'Crab soup with century egg'
    WHEN 28 THEN N'Shaking beef with potatoes'
    WHEN 29 THEN N'Steamed free-range chicken with lime leaves'
    WHEN 30 THEN N'Roast duck with black pepper sauce'
    WHEN 31 THEN N'Honey-braised pork ribs'
    WHEN 32 THEN N'Clay-pot braised fish'
    WHEN 33 THEN N'Crispy roast pork'
    WHEN 34 THEN N'Grilled snakehead fish with rice paper rolls'
    WHEN 35 THEN N'Crab and beef hotpot'
    WHEN 36 THEN N'Chicken hotpot with lemon basil'
    WHEN 37 THEN N'Special Thai seafood hotpot'
    WHEN 38 THEN N'Vegetable mushroom hotpot'
    WHEN 39 THEN N'Sturgeon hotpot with pickled bamboo shoots'
    WHEN 40 THEN N'Beef fondue with vinegar'
    WHEN 41 THEN N'Grilled US beef belly with sesame sauce'
    WHEN 42 THEN N'BBQ pork ribs'
    WHEN 43 THEN N'Grilled chicken with mac khen pepper'
    WHEN 44 THEN N'Grilled squid with satay'
    WHEN 45 THEN N'Mixed grilled skewers'
    WHEN 46 THEN N'Grilled giant freshwater prawns with chili salt'
    WHEN 47 THEN N'Seafood fried rice'
    WHEN 48 THEN N'Stir-fried noodles with beef and greens'
    WHEN 49 THEN N'Special Hue beef noodle soup'
    WHEN 50 THEN N'Shrimp and pork Quang noodles'
    WHEN 51 THEN N'Clay-pot rice with braised goby fish'
    WHEN 52 THEN N'Tamarind stir-fried crab'
    WHEN 53 THEN N'Steamed clams with lemongrass'
    WHEN 54 THEN N'Grilled scallops with scallion oil'
    WHEN 55 THEN N'Pan-seared salmon with passion fruit sauce'
    WHEN 56 THEN N'Grilled oysters with cheese'
    WHEN 57 THEN N'Garlic butter giant freshwater prawns'
    WHEN 58 THEN N'Almond panna cotta dessert'
    WHEN 59 THEN N'Caramel flan'
    WHEN 60 THEN N'Tropical fruit platter'
    WHEN 61 THEN N'Mango panna cotta'
    WHEN 62 THEN N'Young coconut ice cream'
    WHEN 63 THEN N'Avocado coconut smoothie'
    WHEN 64 THEN N'Vietnamese iced milk coffee'
    WHEN 65 THEN N'Mint lemon soda'
    WHEN 66 THEN N'Golden lotus tea'
    WHEN 67 THEN N'Silken tofu with shiitake mushroom sauce'
    WHEN 68 THEN N'Vegetarian lotus seed fried rice'
    WHEN 69 THEN N'Vegetarian mushroom hotpot'
    WHEN 70 THEN N'Vegetarian rainbow spring rolls'
    WHEN 71 THEN N'Family set for four guests'
    WHEN 72 THEN N'Birthday set for six guests'
    WHEN 73 THEN N'Premium seafood set'
    WHEN 74 THEN N'Office combo for ten portions'
    ELSE name_en
END;
