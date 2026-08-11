-- Thuc don duoc quan ly truc tiep boi SQL Server; khong nap du lieu nghiep vu tu JSON.
SET NOCOUNT ON;

DECLARE @Menu TABLE (
    category_name NVARCHAR(200) NOT NULL,
    product_name NVARCHAR(200) NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    cost_price DECIMAL(18,2) NOT NULL,
    tax_rate FLOAT NOT NULL,
    image_url NVARCHAR(255) NOT NULL,
    description_vi NVARCHAR(MAX) NOT NULL
);

INSERT INTO @Menu (category_name, product_name, price, cost_price, tax_rate, image_url, description_vi)
VALUES
    (N'Khai vị', N'Gỏi củ hủ dừa tôm thịt', 89000.00, 48000.00, 8.0, N'https://images.unsplash.com/photo-1543353071-10c8ba85a904?auto=format&fit=crop&w=600&q=80', N'Củ hủ dừa giòn ngọt trộn tôm thịt, rau răm và nước mắm chua ngọt.'),
    (N'Khai vị', N'Nem cua bể Hải Phòng', 99000.00, 56000.00, 8.0, N'https://images.unsplash.com/photo-1562967914-608f82629710?auto=format&fit=crop&w=600&q=80', N'Nem vuông nhân cua bể, thịt băm và miến, chiên vàng giòn.'),
    (N'Khai vị', N'Chả giò hải sản sốt mayonnaise', 95000.00, 52000.00, 8.0, N'https://images.unsplash.com/photo-1625938145744-e3805153995a?auto=format&fit=crop&w=600&q=80', N'Chả giò nhân tôm mực béo nhẹ, dùng kèm sốt mayonnaise cay.'),
    (N'Khai vị', N'Salad bò áp chảo', 119000.00, 65000.00, 8.0, N'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=600&q=80', N'Bò áp chảo mềm ăn cùng rau xanh, cà chua bi và sốt mè rang.'),
    (N'Khai vị', N'Khoai môn lệ phố', 69000.00, 32000.00, 8.0, N'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=600&q=80', N'Khoai môn nghiền nhân thịt thơm béo, chiên giòn bên ngoài.'),
    (N'Khai vị', N'Súp cua trứng bắc thảo', 79000.00, 41000.00, 8.0, N'https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=600&q=80', N'Súp cua sánh nhẹ với nấm, trứng cút và trứng bắc thảo.'),
    (N'Món chính', N'Bò lúc lắc khoai tây', 169000.00, 92000.00, 8.0, N'https://images.unsplash.com/photo-1546833999-b9f581a1996d?auto=format&fit=crop&w=600&q=80', N'Bò xào lửa lớn cùng ớt chuông, hành tây và khoai tây chiên.'),
    (N'Món chính', N'Gà ta hấp lá chanh', 189000.00, 98000.00, 8.0, N'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?auto=format&fit=crop&w=600&q=80', N'Gà ta hấp giữ vị ngọt tự nhiên, dùng kèm muối tiêu chanh.'),
    (N'Món chính', N'Vịt quay sốt tiêu đen', 229000.00, 128000.00, 8.0, N'https://images.unsplash.com/photo-1518492104633-130d0cc84637?auto=format&fit=crop&w=600&q=80', N'Vịt quay da giòn, phủ sốt tiêu đen đậm đà kiểu nhà hàng.'),
    (N'Món chính', N'Sườn non rim mật ong', 159000.00, 85000.00, 8.0, N'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=600&q=80', N'Sườn non rim mềm, áo mật ong thơm nhẹ và mè rang.'),
    (N'Món chính', N'Cá kho tộ làng quê', 139000.00, 72000.00, 8.0, N'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=600&q=80', N'Cá kho nước màu, tiêu xanh và tóp mỡ, hợp dùng với cơm nóng.'),
    (N'Món chính', N'Heo quay giòn bì', 179000.00, 97000.00, 8.0, N'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=600&q=80', N'Heo quay bì giòn, thịt mềm mọng, dùng cùng dưa chua.'),
    (N'Món chính', N'Cá lóc nướng trui cuốn bánh tráng', 249000.00, 138000.00, 8.0, N'https://images.unsplash.com/photo-1559847844-5315695dadae?auto=format&fit=crop&w=600&q=80', N'Cá lóc nướng thơm, cuốn rau sống, bún và chấm mắm nêm.'),
    (N'Lẩu', N'Lẩu riêu cua bắp bò', 299000.00, 165000.00, 8.0, N'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=600&q=80', N'Nước lẩu riêu cua chua thanh, ăn cùng bắp bò và rau đồng.'),
    (N'Lẩu', N'Lẩu gà lá é', 279000.00, 148000.00, 8.0, N'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=600&q=80', N'Gà ta nấu lá é thơm nồng, nước dùng cay nhẹ kiểu miền Trung.'),
    (N'Lẩu', N'Lẩu thái hải sản đặc biệt', 329000.00, 185000.00, 8.0, N'https://images.unsplash.com/photo-1604909052743-94e838986d24?auto=format&fit=crop&w=600&q=80', N'Tôm, mực, nghêu và cá viên trong nước lẩu Thái chua cay.'),
    (N'Lẩu', N'Lẩu nấm thanh đạm', 239000.00, 118000.00, 8.0, N'https://images.unsplash.com/photo-1600891964599-f61ba0e24092?auto=format&fit=crop&w=600&q=80', N'Nhiều loại nấm tươi, nước dùng rau củ trong ngọt tự nhiên.'),
    (N'Lẩu', N'Lẩu cá tầm măng chua', 359000.00, 205000.00, 8.0, N'https://images.unsplash.com/photo-1603073163308-9654c3fb70b5?auto=format&fit=crop&w=600&q=80', N'Cá tầm tươi nấu măng chua, cà chua và thì là.'),
    (N'Lẩu', N'Lẩu bò nhúng giấm', 319000.00, 176000.00, 8.0, N'https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=600&q=80', N'Bò thái mỏng nhúng nước giấm dừa, cuốn bánh tráng rau sống.'),
    (N'Nướng', N'Ba chỉ bò Mỹ nướng sốt mè', 199000.00, 110000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/9/93/Suea_rong_hai.jpg', N'Ba chỉ bò Mỹ ướp sốt mè thơm, nướng mềm và béo nhẹ.'),
    (N'Nướng', N'Sườn cây nướng BBQ', 219000.00, 122000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/b/bc/M%C3%B3n_n%C6%B0%E1%BB%9Bng_ki%E1%BB%83u_H%C3%A0n_Qu%E1%BB%91c_%28S%C6%B0%E1%BB%9Dn_non_n%C6%B0%E1%BB%9Bng%29%2C_T10_n%C4%83m_2016_%283%29.jpg', N'Sườn cây nướng chậm, phủ sốt BBQ đậm vị khói.'),
    (N'Nướng', N'Gà nướng mắc khén', 189000.00, 96000.00, 8.0, N'https://images.unsplash.com/photo-1532550907401-a500c9a57435?auto=format&fit=crop&w=600&q=80', N'Gà ướp mắc khén Tây Bắc, nướng da vàng thơm.'),
    (N'Nướng', N'Mực nướng sa tế', 229000.00, 132000.00, 8.0, N'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=600&q=80', N'Mực tươi nướng sa tế cay thơm, giữ độ giòn ngọt.'),
    (N'Nướng', N'Xiên que tổng hợp', 159000.00, 78000.00, 8.0, N'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=600&q=80', N'Set xiên nướng gồm bò, gà, rau củ và xúc xích.'),
    (N'Nướng', N'Tôm sú nướng muối ớt', 259000.00, 152000.00, 8.0, N'https://images.unsplash.com/photo-1565680018434-b513d5e5fd47?auto=format&fit=crop&w=600&q=80', N'Tôm sú tươi nướng muối ớt, thịt chắc và ngọt.'),
    (N'Cơm - Mì', N'Cơm chiên hải sản', 99000.00, 52000.00, 8.0, N'https://images.unsplash.com/photo-1603133872878-684f208fb84b?auto=format&fit=crop&w=600&q=80', N'Cơm chiên tơi hạt với tôm, mực, trứng và rau củ.'),
    (N'Cơm - Mì', N'Mì xào bò rau cải', 89000.00, 46000.00, 8.0, N'https://images.unsplash.com/photo-1612929633738-8fe44f7ec841?auto=format&fit=crop&w=600&q=80', N'Mì xào dai mềm cùng bò lát và rau cải xanh.'),
    (N'Cơm - Mì', N'Bún bò Huế đặc biệt', 95000.00, 51000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/e/ee/B%C3%BAn_b%C3%B2_Hu%E1%BA%BF_minh28397.jpg', N'Bún bò nước dùng sả ớt, kèm chả cua, bò nạm và giò heo.'),
    (N'Cơm - Mì', N'Cơm gà Hội An', 89000.00, 45000.00, 8.0, N'https://commons.wikimedia.org/wiki/Special:Redirect/file/Com_ga_Viet_Nam_voi_ga_luoc_com_vang_va_nuoc_cham.jpg?width=1200', N'Cơm vàng nghệ dùng với gà xé, rau răm và nước mạ đậm vị.'),
    (N'Cơm - Mì', N'Mì Quảng tôm thịt', 92000.00, 47000.00, 8.0, N'https://images.unsplash.com/photo-1617196034796-73dfa7b1fd56?auto=format&fit=crop&w=600&q=80', N'Mì Quảng nước xâm xấp, tôm thịt, bánh tráng và đậu phộng.'),
    (N'Cơm - Mì', N'Cơm niêu cá bống kho', 119000.00, 62000.00, 8.0, N'https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=600&q=80', N'Cơm niêu nóng giòn dùng với cá bống kho tiêu.'),
    (N'Hải sản', N'Cua rang me', 329000.00, 198000.00, 8.0, N'https://images.unsplash.com/photo-1559737558-2f5a35f4523b?auto=format&fit=crop&w=600&q=80', N'Cua thịt rang sốt me chua ngọt, thơm bơ tỏi.'),
    (N'Hải sản', N'Nghêu hấp sả', 129000.00, 68000.00, 8.0, N'https://commons.wikimedia.org/wiki/Special:Redirect/file/Steamed_clams.jpg?width=1200', N'Nghêu tươi hấp sả ớt, nước hấp ngọt thanh.'),
    (N'Hải sản', N'Sò điệp nướng mỡ hành', 189000.00, 108000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/b/b4/Grilled_scallops_dish.jpg', N'Sò điệp nướng mỡ hành, đậu phộng rang và hành phi.'),
    (N'Hải sản', N'Cá hồi áp chảo sốt chanh dây', 259000.00, 152000.00, 8.0, N'https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=600&q=80', N'Cá hồi áp chảo da giòn, sốt chanh dây chua dịu.'),
    (N'Hải sản', N'Hàu nướng phô mai', 219000.00, 126000.00, 8.0, N'https://images.unsplash.com/photo-1606851091851-e8c8c0fca5ba?auto=format&fit=crop&w=600&q=80', N'Hàu sữa nướng phô mai béo thơm, dùng nóng tại bàn.'),
    (N'Hải sản', N'Tôm càng xanh sốt bơ tỏi', 299000.00, 178000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/d/dc/Grilled_Garlic_butter_prawns_with_spicy_prawn_heads.jpg', N'Tôm càng xanh cháy bơ tỏi, thịt chắc và ngọt.'),
    (N'Tráng miệng', N'Chè khúc bạch hạnh nhân', 59000.00, 26000.00, 8.0, N'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=600&q=80', N'Khúc bạch mềm béo, nhãn ngọt và hạnh nhân lát.'),
    (N'Tráng miệng', N'Bánh flan caramel', 49000.00, 21000.00, 8.0, N'https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=600&q=80', N'Flan trứng sữa mềm mịn, phủ caramel thơm nhẹ.'),
    (N'Tráng miệng', N'Trái cây nhiệt đới', 69000.00, 32000.00, 8.0, N'https://images.unsplash.com/photo-1490474418585-ba9bad8fd0ea?auto=format&fit=crop&w=600&q=80', N'Đĩa trái cây theo mùa gồm xoài, dưa, thanh long và nho.'),
    (N'Tráng miệng', N'Panna cotta xoài', 69000.00, 31000.00, 8.0, N'https://images.unsplash.com/photo-1488477304112-4944851de03d?auto=format&fit=crop&w=600&q=80', N'Panna cotta sữa tươi mềm mịn, sốt xoài chua ngọt.'),
    (N'Tráng miệng', N'Kem dừa non', 79000.00, 36000.00, 8.0, N'https://images.unsplash.com/photo-1501443762994-82bd5dace89a?auto=format&fit=crop&w=600&q=80', N'Kem dừa mát lạnh dùng trong trái dừa non, thêm đậu phộng.'),
    (N'Đồ uống', N'Trà đào cam sả', 59000.00, 24000.00, 8.0, N'https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=600&q=80', N'Trà đào thơm cam sả, vị ngọt thanh và mát.'),
    (N'Đồ uống', N'Nước ép dưa hấu', 49000.00, 19000.00, 8.0, N'https://images.unsplash.com/photo-1621263764928-df1444c5e859?auto=format&fit=crop&w=600&q=80', N'Dưa hấu ép tươi mỗi ly, không dùng siro.'),
    (N'Đồ uống', N'Sinh tố bơ sữa dừa', 69000.00, 31000.00, 8.0, N'https://images.unsplash.com/photo-1502741224143-90386d7f8c82?auto=format&fit=crop&w=600&q=80', N'Bơ chín xay cùng sữa dừa, béo thơm và mịn.'),
    (N'Đồ uống', N'Cà phê sữa đá', 39000.00, 14000.00, 8.0, N'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=600&q=80', N'Cà phê phin đậm vị, pha sữa đặc và đá viên.'),
    (N'Đồ uống', N'Soda chanh bạc hà', 55000.00, 22000.00, 8.0, N'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=600&q=80', N'Soda mát lạnh với chanh tươi và lá bạc hà.'),
    (N'Đồ uống', N'Trà sen vàng', 65000.00, 27000.00, 8.0, N'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=600&q=80', N'Trà sen thơm nhẹ, kem sữa béo và hạt sen bùi.'),
    (N'Món chay', N'Đậu hũ non sốt nấm đông cô', 89000.00, 42000.00, 8.0, N'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=600&q=80', N'Đậu hũ non mềm, sốt nấm đông cô thanh đạm.'),
    (N'Món chay', N'Cơm chiên hạt sen chay', 85000.00, 39000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/0/01/Lotus_fried_brown_rice.jpg', N'Cơm chiên rau củ, hạt sen và nấm, vị nhẹ dễ ăn.'),
    (N'Món chay', N'Lẩu nấm chay', 239000.00, 112000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/5/5d/Mushroom_hot_pot_in_Yunnan.jpg', N'Lẩu rau củ và nhiều loại nấm tươi, nước dùng ngọt tự nhiên.'),
    (N'Món chay', N'Gỏi cuốn ngũ sắc chay', 79000.00, 34000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/0/02/G%E1%BB%8Fi_Cu%E1%BB%91n_Chay_Vietnamese_Fresh_Vegetarian_Spring_Roll_2019-1599.jpg', N'Gỏi cuốn rau củ, bún, đậu hũ và sốt tương đậu phộng.'),
    (N'Combo/Set tiệc', N'Combo gia đình 4 người', 699000.00, 382000.00, 8.0, N'https://images.unsplash.com/photo-1555244162-803834f70033?auto=format&fit=crop&w=600&q=80', N'Set gồm khai vị, món chính, lẩu nhỏ và đồ uống cho 4 khách.'),
    (N'Combo/Set tiệc', N'Set sinh nhật 6 người', 1199000.00, 658000.00, 8.0, N'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=80', N'Set tiệc sinh nhật đủ món nóng, món nướng và tráng miệng.'),
    (N'Combo/Set tiệc', N'Set hải sản cao cấp', 1499000.00, 862000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/3/34/Seafood_platter_at_the_lighthouse_restaurant_Brisbane.jpg', N'Tôm càng, cua, hàu, cá hồi và lẩu hải sản cho nhóm khách.'),
    (N'Combo/Set tiệc', N'Combo văn phòng 10 phần', 899000.00, 468000.00, 8.0, N'https://images.unsplash.com/photo-1551218808-94e220e084d2?auto=format&fit=crop&w=600&q=80', N'Combo cơm, món mặn, rau và đồ uống phù hợp đặt theo nhóm.'),
    (N'Mộc Vị Đặc Trưng', N'Gỏi cuốn mộc', 69000.00, 38000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/3/3b/G%E1%BB%8Fi_cu%E1%BB%91n_at_a_Vietnamese-style_restaurant_in_Beijing_%2820180103175625%29.jpg', N'Gỏi cuốn thanh mát với tôm, thịt, bún và rau thơm, dùng cùng nước chấm mộc vị.'),
    (N'Mộc Vị Đặc Trưng', N'Nộm rau rừng Đà Nẵng', 79000.00, 43000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/6/68/Vietnamese_mango_salad_with_shrimp.jpg', N'Rau rừng theo mùa trộn chua ngọt cùng xoài xanh, tôm và mè rang.'),
    (N'Mộc Vị Đặc Trưng', N'Bánh tráng cuốn thịt heo', 109000.00, 60000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/1/17/B%C3%A1nh_tr%C3%A1ng_cu%E1%BB%91n_th%E1%BB%8Bt_heo_%2818521%29.jpg', N'Thịt heo hai đầu da, bánh tráng Đại Lộc và rau sống ăn cùng mắm nêm Đà Nẵng.'),
    (N'Mộc Vị Đặc Trưng', N'Đậu hũ non sốt mắm mộc', 69000.00, 36000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/2/29/B%C3%BAn_%C4%91%E1%BA%ADu_m%E1%BA%AFm_t%C3%B4m_%28ph%E1%BA%A7n_b%C3%A1nh_%C4%91%E1%BA%ADu_h%C5%A9_chi%C3%AAn%29_qu%C3%A1n_3_ch%E1%BB%8B_em_t%E1%BA%A1i_Nguy%E1%BB%85n_S%C6%A1n_n%C4%83m_2016_%281%29.jpg', N'Đậu hũ mềm áp chảo, phủ sốt mắm tỏi hành thơm dịu của Mộc Vị.'),
    (N'Mộc Vị Đặc Trưng', N'Chả cá Đà Nẵng nướng lá chuối', 129000.00, 70000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/8/81/Ch%E1%BA%A3_c%C3%A1_L%C3%A3_V%E1%BB%8Dng_H%C3%A0_N%E1%BB%99i_th%C3%A1ng_2_n%C4%83m_2018_%282%29.jpg', N'Chả cá tươi quết thủ công, gói lá chuối và nướng để giữ trọn vị ngọt.'),
    (N'Mộc Vị Đặc Trưng', N'Cá kho tộ mộc mạc', 149000.00, 82000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/b/b4/C%C3%A1_kho_t%E1%BB%99.JPG', N'Cá kho trong tộ đất với nước màu, tiêu xanh và hành, đậm vị cơm nhà.'),
    (N'Mộc Vị Đặc Trưng', N'Cá lóc nướng trui', 249000.00, 138000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/a/aa/C%C3%A1_l%C3%B3c_%C4%91%E1%BB%93ng_n%C6%B0%E1%BB%9Bng_trui_3.jpg', N'Cá lóc đồng nướng trui nguyên con, cuốn bánh tráng rau sống và chấm mắm nêm.'),
    (N'Mộc Vị Đặc Trưng', N'Gà nướng muối ớt bản mộc', 229000.00, 125000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/b/bc/G%C3%A0_n%C6%B0%E1%BB%9Bng_mu%E1%BB%91i_%E1%BB%9Bt.jpg', N'Gà ta ướp muối ớt, nướng da vàng giòn và giữ phần thịt mọng ngọt.'),
    (N'Mộc Vị Đặc Trưng', N'Bò một nắng chấm muối kiến vàng', 239000.00, 132000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/7/7c/Kantsun_Stydi_beef_jerky.jpg', N'Bò phơi một nắng nướng than, xé miếng dùng cùng muối kiến vàng cay thơm.'),
    (N'Mộc Vị Đặc Trưng', N'Sườn nướng mật mía', 189000.00, 104000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/a/a7/M%C3%B3n_n%C6%B0%E1%BB%9Bng_H%C3%A0n_Qu%E1%BB%91c_%28s%C6%B0%E1%BB%9Dn_%C6%B0%E1%BB%9Bp_t%E1%BA%A9m_m%E1%BA%ADt_ong_n%C6%B0%E1%BB%9Bng%29%2C_T10_n%C4%83m_2016_%283%29.jpg', N'Sườn non ướp mật mía, nướng xém cạnh cho vị ngọt dịu và hương khói.'),
    (N'Mộc Vị Đặc Trưng', N'Tôm rang me vườn nhà', 179000.00, 98000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/1/19/980Shrimp_and_prawn_stew_with_Baguio_beans%2C_napa_cabbage%2C_tomatoes_and_tamarind_soup_in_lemon_grass.jpg', N'Tôm tươi rang sốt me sánh bóng, cân bằng vị chua ngọt và thơm hành phi.'),
    (N'Mộc Vị Đặc Trưng', N'Thịt kho tàu lá mơ', 159000.00, 86000.00, 8.0, N'https://commons.wikimedia.org/wiki/Special:Redirect/file/Th%E1%BB%8Bt_kho_T%C3%A0u.jpg?width=1200', N'Thịt ba chỉ kho mềm cùng trứng, ăn kèm lá mơ tạo hậu vị thanh đặc trưng.'),
    (N'Mộc Vị Đặc Trưng', N'Canh chua cá lóc', 139000.00, 75000.00, 8.0, N'https://commons.wikimedia.org/wiki/Special:Redirect/file/Canh_chua_c%C3%A1_l%C3%B3c_%E1%BB%9F_Th%E1%BB%A7y_Tr%C3%BAc_Qu%C3%A1n%2C_%C4%91%C6%B0%E1%BB%9Dng_Nguy%E1%BB%85n_Nh%E1%BB%AF_L%C3%A3m_%283%29.jpg?width=1200', N'Canh chua cá lóc nấu dứa, cà chua, bạc hà và rau thơm miền Nam.'),
    (N'Mộc Vị Đặc Trưng', N'Canh rau tập tàng nấu tôm', 99000.00, 53000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/1/1f/Vietnamese_Shrimp_and_Vegetable_noodle_soup.jpg', N'Rau tập tàng theo mùa nấu tôm tươi, nước canh trong và ngọt tự nhiên.'),
    (N'Mộc Vị Đặc Trưng', N'Rau lang luộc chấm mắm nêm', 59000.00, 30000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/1/15/%C4%90%E1%BB%8Dt_rau_lang.jpg', N'Đọt rau lang luộc vừa chín tới, ăn cùng mắm nêm pha dứa và ớt.'),
    (N'Mộc Vị Đặc Trưng', N'Đậu bắp bí đỏ hấp nước cốt dừa', 79000.00, 42000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/d/d6/BOILED_OR_STEAMED_OKRA_FROM_GARDEN_TO_TABLE.jpg', N'Đậu bắp và bí đỏ hấp mềm, rưới nước cốt dừa béo nhẹ cùng mè rang.'),
    (N'Mộc Vị Đặc Trưng', N'Mì Quảng Đà Nẵng chuẩn vị', 89000.00, 48000.00, 8.0, N'https://commons.wikimedia.org/wiki/Special:Redirect/file/M%C3%AC_Qu%E1%BA%A3ng.jpg?width=1200', N'Mì Quảng sợi vàng, tôm thịt, đậu phộng, rau sống và bánh tráng nướng.'),
    (N'Mộc Vị Đặc Trưng', N'Cơm niêu cá kho + canh rau mộc', 169000.00, 92000.00, 8.0, N'https://upload.wikimedia.org/wikipedia/commons/7/7f/Claypot_rice_1.jpg', N'Cơm niêu cháy giòn dùng cùng cá kho đậm vị và một phần canh rau theo ngày.'),
    (N'Mộc Vị Đặc Trưng', N'Bún mắm nêm Đà Nẵng', 79000.00, 42000.00, 8.0, N'https://commons.wikimedia.org/wiki/Special:Redirect/file/B%C3%BAn_m%E1%BA%AFm_th%E1%BB%8Bt_heo_lu%E1%BB%99c_%E1%BB%9F_%C4%90%C3%A0_N%E1%BA%B5ng.jpg?width=1200', N'Bún mắm nêm với thịt heo, rau sống, mít non, đậu phộng và mắm nêm pha chuẩn vị.'),
    (N'Mộc Vị Đặc Trưng', N'Chè mộc', 49000.00, 25000.00, 8.0, N'https://commons.wikimedia.org/wiki/Special:Redirect/file/Ch%C3%A8_Th%C6%B0ng.jpg?width=1200', N'Chè khoai, đậu xanh và nước cốt dừa nấu ngọt dịu, dùng nóng hoặc lạnh.');

MERGE dbo.Categories WITH (HOLDLOCK) AS target
USING (SELECT DISTINCT category_name FROM @Menu) AS source
ON LOWER(LTRIM(RTRIM(target.name))) = LOWER(LTRIM(RTRIM(source.category_name)))
WHEN MATCHED THEN
    UPDATE SET
        target.name_vi = source.category_name,
        target.name_en = COALESCE(NULLIF(target.name_en, N''), source.category_name)
WHEN NOT MATCHED THEN
    INSERT (name, name_vi, name_en)
    VALUES (source.category_name, source.category_name, source.category_name);

;WITH MenuSource AS (
    SELECT
        menu.*,
        category.id AS category_id
    FROM @Menu AS menu
    CROSS APPLY (
        SELECT TOP (1) c.id
        FROM dbo.Categories AS c
        WHERE LOWER(LTRIM(RTRIM(c.name))) = LOWER(LTRIM(RTRIM(menu.category_name)))
        ORDER BY c.id
    ) AS category
)
MERGE dbo.Products WITH (HOLDLOCK) AS target
USING MenuSource AS source
ON LOWER(LTRIM(RTRIM(target.name))) = LOWER(LTRIM(RTRIM(source.product_name)))
WHEN MATCHED THEN
    UPDATE SET
        target.name_vi = source.product_name,
        target.name_en = COALESCE(NULLIF(target.name_en, N''), source.product_name),
        target.price = source.price,
        target.cost_price = source.cost_price,
        target.tax_rate = source.tax_rate,
        target.image = source.image_url,
        target.description = source.description_vi,
        target.description_vi = source.description_vi,
        target.description_en = COALESCE(NULLIF(target.description_en, N''), source.description_vi),
        target.available = 1,
        target.status = 1,
        target.category_id = source.category_id
WHEN NOT MATCHED THEN
    INSERT (
        name, name_vi, name_en, price, cost_price, tax_rate, image,
        description, description_vi, description_en, create_date,
        available, status, category_id
    )
    VALUES (
        source.product_name, source.product_name, source.product_name,
        source.price, source.cost_price, source.tax_rate, source.image_url,
        source.description_vi, source.description_vi, source.description_vi,
        CAST(GETDATE() AS DATE), 1, 1, source.category_id
    );
