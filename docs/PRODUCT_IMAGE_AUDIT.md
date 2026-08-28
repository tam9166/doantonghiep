# Product image audit (2026-08-28)

## 1. Source of truth

The customer menu reads `Product.image` from the backend `/api/products` response. The durable source is therefore `dbo.Products.image`, populated by Flyway migrations (`V002`, `V022`, `V027`, `V073`) and exposed by the product DTOs. `DemoDataSeeder` only repairs null/broken values and does not own the product catalog.

## 2. Inventory snapshot

The pre-repair active `dbo.Products` snapshot contained **132 products**. There were **124 distinct image URLs**, so **8 products** shared an exact URL with another product. Photo-ID normalization found these cross-product groups:

After V083/V084 on the local RestaurantDB, the public active catalog contains **124 products with 124 distinct exact URLs** and **0 active `Demo ...` rows**. This exact-URL check does not replace human review of visually similar photo families.

Batch 1 now localizes nine assets through V085/V086/V087/V088. V088 is the final clearance migration for five additional license-verified assets (IDs 4, 7, 10, 11 and 13). ID 8 remains semantically correct but rights-unverified, and ID 22 remains a semantic FAIL; neither is replaced with an unlicensed candidate.

| Group | Products | Assessment |
|---|---|---|
| `photo-1504674900247` | Demo Pho Bo (14), Demo Com Ga (15), Demo Lau Thai (16), Demo Bo Nuong (17), Demo Goi Cuon (18), Demo Cha Gio (19), Demo Tra Dao (20), Demo Nuoc Ep (21), Set sinh nhật 6 người (72) | Confirmed generic/demo reuse; repaired by V083. |
| `photo-1547592180` | Chả giò hải sản (5), Lẩu Thái hải sản (6), Lẩu gà lá é (36) | Confirmed wrong cross-dish reuse; IDs 5 and 6 repaired by V083; ID 36 remains for manual visual approval. |
| `photo-1543353071` | Gỏi cuốn tôm thịt (4), Gỏi củ hủ dừa tôm thịt (22) | Similar dish family; requires manual visual approval before replacing either image. |
| `photo-1555939594` | Bò nướng lá lốt (7), Xiên que tổng hợp (45) | Confirmed generic grilling reuse; ID 7 was initially repaired by V083 and finalized with the verified V088 Commons asset; ID 45 remains for manual visual approval. |
| `photo-1467003909585` | Cá hồi sốt chanh dây (8), Cá hồi áp chảo sốt chanh dây (55) | Same ingredient with different presentation; manual approval required. |
| `photo-1488477181946` | Chè khúc bạch (13), Chè khúc bạch hạnh nhân (58) | Same dessert family; manual approval required. |

Nine Batch 1 assets are now stored locally under `static/images/products` and served by semantic `/images/products/...` paths. The other catalog URLs remain remote HTTPS values. Remote bytes were downloaded temporarily for this audit; no remote asset is required by the hash calculation in CI.

## 3. Repairs applied

| ID | Product | Old issue | New image/source |
|---:|---|---|---|
| 3 | Coca Cola | Wrong/non-brand beverage image reported in the old catalog | Wikimedia Commons Coca-Cola can (`Can_of_Coca_Cola_(26899145485).jpg`) |
| 5 | Chả giò hải sản | Reused hot-pot image | Wikimedia Commons `Fried spring roll (2427631743).jpg`, localized by V086 |
| 6 | Lẩu Thái hải sản | Reused non-specific hot-pot image | Wikimedia Commons `Image of hot pot and food.jpg`, localized by V086 |
| 9 | Cơm gà Hội An | Lower-fold browser review showed fried chicken rather than Hội An-style rice | Wikimedia Commons `Cơm gà Tam Kỳ, Quảng Nam.JPG`, localized by V087 |
| 12 | Trà đào cam sả | Lower-fold browser review showed generic dark tea/cocktail | Wikimedia Commons `Peach iced tea with orange slices.jpg`, localized by V087 |
| 4 | Gỏi cuốn tôm thịt | V085 image had no preserved source record | Wikimedia Commons `Gỏi cuốn.jpg` (CC0), localized by V088 |
| 7 | Bò nướng lá lốt | V083 retained a remote TGDD image with unclear rights | Wikimedia Commons `Bò nướng lá lốt (39354093491).jpg` (CC BY 2.0), localized by V088 |
| 10 | Mì Quảng đặc biệt | V085 image had no preserved source record | Wikimedia Commons `Mì Quảng.jpg` (CC BY 2.0), localized by V088 |
| 11 | Nước ép dưa hấu | V085 image had no preserved source record | Wikimedia Commons `Water melon juice.jpg` (CC0), localized by V088 |
| 13 | Chè khúc bạch | V085 image had no preserved source record | Wikimedia Commons `Chè khúc bạch ở quán Thảo Vy 2020 09 06.jpg` (CC BY-SA 4.0), localized by V088 |
| 14–21 | Demo Pho Bo, Demo Com Ga, Demo Lau Thai, Demo Bo Nuong, Demo Goi Cuon, Demo Cha Gio, Demo Tra Dao, Demo Nuoc Ep | One generic food placeholder shared by 8 products | Matching approved menu assets |
| 72 | Set sinh nhật 6 người | Generic food placeholder | Existing office-combo/set presentation asset |

The changes are made by stable product IDs in `V083__repair_legacy_product_images.sql` and the follow-up V088 clearance migration; no product rows are deleted or reseeded. The eight `Demo ...` rows (IDs 14–21) were confirmed seed/test data, so `V084__hide_demo_products_from_public_menu.sql` sets both `status=0` and `available=0`; they are no longer public. V088 intentionally leaves ID 8 and ID 22 for review rather than choosing an unverified image.

## 4. Broken/placeholder status

The database snapshot had no blank product image values among active products. Eight Demo/Test-style rows were found active before the repair; after V084, the public active count is reduced by those eight and no active product name begins with `Demo `. URL liveness was not treated as proof of semantic correctness; remote URLs should be monitored separately because CI must not depend on third-party availability. The old generic/demo URL was a confirmed placeholder/reuse pattern and is removed from repaired IDs.

## 6. Batch 1 verification manifest

**BATCH_1_AUDIT_COMPLETE: YES. BATCH_1_ALL_IMAGES_FIXED: NO.** Total products: 12; semantic PASS: 11; semantic FAIL: 1; license VERIFIED: 10; license UNVERIFIED: 2; unresolved/NEED_USER_REVIEW: 2; exact duplicate URLs/local bytes: 0; dHash near-duplicate pairs at distance ≤16: 0. V083_APPLIED: YES. V084_STATUS: KEEP (the eight `Demo ...` rows are confirmed seed data and hidden). V085_APPLIED: YES. V086_APPLIED: YES. V087_APPLIED: YES. V088_APPLIED: YES. Browser QA and license classification are complete for all 12 cards. ID 8 remains NEED_USER_REVIEW for rights evidence; ID 22 remains a documented semantic FAIL/REPLACE_REQUIRED because none of the reviewed candidates combines a HIGH semantic match with a VERIFIED reusable license.

| ID | Product | Candidate sources reviewed | Visual finding | Confidence | Action |
|---:|---|---|---|---|---|
| 3 | Coca Cola | Wikimedia Commons Coca-Cola can sources | Correct red Coca-Cola can; current licensed source retained | HIGH | KEEP |
| 4 | Gỏi cuốn tôm thịt | VietFoodShop; Kiến Thức Vui; Wikimedia Commons `Gỏi cuốn.jpg` | Transparent rice-paper rolls with visible shrimp, vermicelli and greens; CC0 source selected | HIGH | REPLACE (V088 local) |
| 5 | Chả giò hải sản | Kingfoodmart; Henry Seafood; Wikimedia Commons `Fried spring roll (2427631743).jpg` | Multiple golden fried Vietnamese spring rolls with herbs; Commons file is from a Vietnamese restaurant and is visually suitable | HIGH | REPLACE (V086 local) |
| 6 | Lẩu Thái hải sản | Coto Sea; Tripadvisor; Wikimedia Commons `Image of hot pot and food.jpg` | Orange/red Thai broth in a hot-pot pan with shrimp/clams and surrounding seafood ingredients | HIGH | REPLACE (V086 local) |
| 7 | Bò nướng lá lốt | Bách Hóa Xanh; Fohla Food; Sao Biển Tourist; Wikimedia Commons `Bò nướng lá lốt (39354093491).jpg` | Multiple dark-green leaf-wrapped beef rolls with peanuts and scallion oil; CC BY 2.0 source selected | HIGH | REPLACE (V088 local) |
| 8 | Cá hồi sốt chanh dây | GoFood; Tripi; recipe source | Salmon fillet and yellow passion-fruit sauce are visible, but source rights are unclear | MEDIUM | NEED_USER_REVIEW |
| 9 | Cơm gà Hội An | Wikimedia Commons `Cơm gà Tam Kỳ, Quảng Nam.JPG`; three candidate pages reviewed | Yellow rice, chicken, herbs and dipping/soup sides are visible; CC BY-SA 3.0 source | HIGH | REPLACE (V087 local) |
| 10 | Mì Quảng đặc biệt | Wikimedia Commons Mì Quảng category; `Mì Quảng.jpg` | Broad rice noodles, shrimp/pork toppings, herbs, peanuts and bánh đa; CC BY 2.0 source selected | HIGH | REPLACE (V088 local) |
| 11 | Nước ép dưa hấu | ReViet Juice; EUS Fruit; Wikimedia Commons `Water melon juice.jpg` | Red watermelon juice in glasses; CC0 source selected | HIGH | REPLACE (V088 local) |
| 12 | Trà đào cam sả | Wikimedia Commons `Peach iced tea with orange slices.jpg`; three candidate pages reviewed | Amber iced tea, peach/orange garnish and lemongrass-like stalk garnish are visible; CC BY-SA 4.0 source | HIGH | REPLACE (V087 local) |
| 13 | Chè khúc bạch | Wikimedia Commons `Chè khúc bạch ở quán Thảo Vy 2020 09 06.jpg`; Foody; Philips recipe PDF | White khúc bạch cubes, colorful jelly and fruit in sweet soup; CC BY-SA 4.0 source selected | HIGH | REPLACE (V088 local) |
| 22 | Gỏi củ hủ dừa tôm thịt | Mytour; VTC News; Ẩm Thực Hai Lúa; Openverse salad candidates | Current platter is a generic salad and does not visibly prove sliced coconut shoot, shrimp and pork; no high-confidence licensed replacement selected | LOW | NEED_USER_REVIEW |

### Browser QA evidence (2026-08-28)

Chrome loaded `/menu` successfully and the DOM contained all 12 Batch 1 names. The V088 local assets for IDs 4, 7, 10, 11 and 13 returned `200 image/jpeg` on packaged-jar smoke; anonymous `HEAD /images/**` is also explicitly permitted and covered by regression test, while an unrelated protected admin endpoint returned `401`. The screenshot [`batch1-menu-top.png`](qa/product-images/batch1-menu-top.png) covers the first six cards, and [`batch1-menu-bottom.png`](qa/product-images/batch1-menu-bottom.png) covers the six lower Batch 1 cards.

| Product | Image loaded | Name/visual match | Crop | Visual duplicate | Result |
|---|---|---|---|---|---|
| Coca Cola | YES | YES (red branded can) | YES | NO | PASS |
| Gỏi cuốn tôm thịt | YES | YES (fresh rice-paper rolls/shrimp) | YES | NO | PASS |
| Chả giò hải sản | YES | YES (multiple golden fried rolls) | YES | NO | PASS (license/attribution recorded) |
| Lẩu Thái hải sản | YES | YES (orange Thai hot-pot broth with shrimp/seafood) | YES | NO | PASS (license/attribution recorded) |
| Bò nướng lá lốt | YES | YES (multiple leaf-wrapped rolls) | YES | NO | PASS (CC BY 2.0 recorded) |
| Cá hồi sốt chanh dây | YES | YES (salmon with yellow/orange sauce) | YES | NO | PASS (license review pending) |
| Cơm gà Hội An | YES | YES (yellow rice, chicken, herbs/sides) | YES | NO | PASS (license verified) |
| Mì Quảng đặc biệt | YES | YES (broad noodles, toppings, herbs, peanuts) | YES | NO | PASS (CC BY 2.0 recorded) |
| Nước ép dưa hấu | YES | YES (red watermelon drink) | YES | NO | PASS (CC0 recorded) |
| Trà đào cam sả | YES | YES (amber peach iced tea with orange garnish) | YES | NO | PASS (license verified) |
| Chè khúc bạch | YES | YES (white khúc bạch cubes, fruit and almond) | YES | NO | PASS (CC BY-SA 4.0 recorded) |
| Gỏi củ hủ dừa tôm thịt | YES | NO (generic platter; coconut shoot/shrimp/pork not identifiable) | YES | NO | FAIL (needs a licensed replacement) |

The final Chrome pass contains all six lower-fold cards in [`batch1-menu-bottom.png`](qa/product-images/batch1-menu-bottom.png). The lower six are now checked visually; only ID 22 fails the hard semantic rule. The first six were rechecked after the V087/V088 localization passes and remain stable.

### ID 22 candidate search and decision

Three independent visual references were reviewed for `Gỏi củ hủ dừa tôm thịt`. They are recorded as `VISUAL_REFERENCE_ONLY`: none has a reusable license statement that can be verified for project distribution, so none was downloaded into the repository or used as a replacement.

| Candidate | Source page / image page | License / author | Visual description | Semantic confidence | License confidence | Decision |
|---|---|---|---|---|---|---|
| 1 | [VTC News recipe](https://vtcnews.vn/cach-lam-goi-cu-hu-dua-tom-thit-don-gian-ar992604.html) / [image](https://cdn-i.vtcnews.vn/resize/th/upload/2025/12/12/cu-hu-dua-tom-thit-14170044.jpg) | License not stated; image credit shown as H.T/B.L in article | Mixed salad in a bowl with abundant boiled shrimp, sliced pork, pale coconut shoot strips, herbs and carrot; clearly Vietnamese mixed salad. | HIGH | LOW | REJECT for distribution; keep as visual reference only. |
| 2 | [Kingfoodmart recipe](https://kingfoodmart.com/bai-viet/cong-thuc-nau-an/goi-cu-hu-dua-tom-thit) / [finished-dish image](https://storage.googleapis.com/onelife-public/blog.onelife.vn/2021/10/cach-lam-goi-cu-hu-dua-tom-thit-mon-khai-vi-739528505799.jpg) | License not stated; source credits Cooky/OneLife content | Finished bowl contains pale shredded coconut shoot, shrimp, pork, vegetables and herbs, but the image is a small overhead photo. | HIGH | LOW | REJECT for distribution; keep as visual reference only. |
| 3 | [Vietnam National Tourism information page](https://nongthon.vietnamtourism.gov.vn/goi-cu-hu-dua-tom-thit-mon-ngon-cua-xu-dua/) / [image](https://nongthon.vietnamtourism.gov.vn/wp-content/uploads/2026/01/goi-cu-hu-dua-tom-thit-1677232790-300x225.jpg) | License/photographer not stated on the page | Plated Vietnamese mixed salad with visible shredded pale ingredient, shrimp, herbs and red garnish; presentation matches the required gỏi structure. | MEDIUM-HIGH | LOW | REJECT for distribution; keep as visual reference only. |

Decision: no candidate meets both `SEMANTIC = HIGH` and `LICENSE = VERIFIED`. ID 22 remains `NEED_USER_REVIEW` / `REPLACE_REQUIRED`; the current generic platter is not promoted to PASS and no migration was created for an unlicensed replacement.

SHA-256 is recorded for every local image. Remote images are explicitly `REMOTE_NOT_FINALIZED`; no remote image with unclear rights was added to the repository merely to produce a checksum. dHash comparison was completed from the already reviewed browser/cache bytes and is retained only as duplicate-screening evidence.

### Final manifest (12/12)

| ID | Product | Final image/source | Author | License | SHA-256 | dHash | Semantic | License | Browser | Duplicate | Final status |
|---:|---|---|---|---|---|---|---|---|---|---|---|
| 3 | Coca Cola | Wikimedia Commons `Can of Coca Cola (26899145485).jpg` | Willis Lam | CC BY-SA 2.0 | `REMOTE_NOT_FINALIZED` | `0011101100101001001010010010100100101001001110010011100100110011` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 4 | Gỏi cuốn tôm thịt | `/images/products/goi-cuon-tom-thit-cc0.jpg` (V088) | Tran Hai Duong | CC0 1.0 | `9F7DC5E0B2CCB7E872713B204F0FD0AB6B126644916AF30A8A3260FF804A4CD3` | `0010100100101100100101011101101110100100010100001101001010101001` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 5 | Chả giò hải sản | `/images/products/cha-gio-hai-san-v2.jpg` (V086) | pelican | CC BY-SA 2.0 | `B6ECC1358BDC40162FC07336C5D56B2E77FC76C617060332548ABE4896183127` | `1000110101110111011110011101100011001000101110001001100110000001` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 6 | Lẩu Thái hải sản | `/images/products/lau-thai-hai-san-v2.jpg` (V086) | Leoniabarclay88870 | CC BY-SA 4.0 | `58B33F5AE661954D77000EAB44667AC28BC274A6619E6FB026BCFC7515AF044D` | `0010110010101100011000011110010101101110011001000101010001110110` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 7 | Bò nướng lá lốt | `/images/products/bo-nuong-la-lot-cc-by.jpg` (V088) | Vinnie Cartabiano | CC BY 2.0 | `59C9E6155F49C9E5940823A35F4413E9CFA58DB80F3D4E0FEE6495DE13E17C47` | `0100101001100011101001010101110001001011110101011010101011010111` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 8 | Cá hồi sốt chanh dây | Unsplash photo-1467003909585 | source/license record missing | UNVERIFIED | `REMOTE_NOT_FINALIZED` | `1110011101101011110011011101110011100110100110101100110001110010` | PASS | UNVERIFIED | PASS | UNIQUE | NEED_USER_REVIEW |
| 9 | Cơm gà Hội An | `/images/products/com-ga-hoi-an-v2.jpg` (V087) | Đông Sơn | CC BY-SA 3.0 | `BD860C8B1AD1D7E46EC0D202AEEBA20C20F2EF78C34A55182A71158C136FF082` | `0100011100100010001100100010111011000111110001111100100111000011` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 10 | Mì Quảng đặc biệt | `/images/products/mi-quang-dac-biet-cc-by.jpg` (V088) | SauceSupreme | CC BY 2.0 | `559174C9B8A89430CB3152C14CB5CA0A350649C831301C71414CD5568E241AB8` | `1001101101010101111010100101001001011101010101000110110101011101` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 11 | Nước ép dưa hấu | `/images/products/nuoc-ep-dua-hau-cc0.jpg` (V088) | Divya Chinnasamy | CC0 1.0 | `0DDEFF5CC79A5465A54067550AF5FCF1DA0D0AF26ED18E3B1AB43A4996B65B37` | `1011010010101101110110001000110000101001001010010010001100100011` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 12 | Trà đào cam sả | `/images/products/tra-dao-cam-sa-v2.jpg` (V087) | Baoothersks | CC BY-SA 4.0 | `C2B00A082154D76C0B613A45D32AE70A1389CAA10CEC5A5812AB7B69CD8088F1` | `0101000111101100011001000100011001100011001100100001011110011011` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 13 | Chè khúc bạch | `/images/products/che-khuc-bach-cc-by-sa.jpg` (V088) | Phương Huy | CC BY-SA 4.0 | `C0C64C9DCC18406B12DD851EA9B9FAEBB4CA74D783E475F32AF489D419DE67CD` | `1100110000100110011110100110011101010111011011011100110010001010` | PASS | VERIFIED | PASS | UNIQUE | FINAL |
| 22 | Gỏi củ hủ dừa tôm thịt | Unsplash photo-1543353071-10c8ba85a904 | source/license record missing | UNVERIFIED | `REMOTE_NOT_FINALIZED` | `0001100100011000100110001001110000011110100111101001111010011110` | FAIL | UNVERIFIED | PASS | UNIQUE | REPLACE_REQUIRED |

The dHash implementation used a 9×8 grayscale reduction and compared each 64-bit hash pairwise; distance ≤16 was the documented near-duplicate flag. No pair crossed that threshold. `DHASH_STATUS = COMPLETE`; `PHASH_STATUS = NOT_AVAILABLE` because Pillow/imagehash is not installed and no dependency was added only for this audit. The three catalog visual-review families remain: (4,22) fresh-roll/salad family, (7,45) grill/leaf-wrapped family, and (8,55) salmon/passion-fruit family. These are semantically related catalog families, not exact byte duplicates; IDs 45 and 55 are outside Batch 1 and were not changed.

## 5. QA and limitations

- `ProductImageAuditContractTest` prevents regression to the generic URL and verifies ID-scoped migration updates through V088.
- `npm run lint`, `npm run test`, `npm run build`, focused Maven product/migration/security tests and full Maven tests pass after V088 is applied (464 backend tests, 0 failures, 0 errors).
- Browser screenshots can verify layout and image loading, but semantic food correctness still needs human visual approval for the unresolved groups above.

## 7. Batch 2 verification (2026-08-28)

### Scope and selection

The next public, active products after the Batch 1 exclusion set
`[3,4,5,6,7,8,9,10,11,12,13,22]` were selected by ascending database ID:

`75–89` — Gỏi cuốn mộc; Nộm rau rừng Đà Nẵng; Bánh tráng cuốn thịt heo; Đậu hũ non sốt mắm mộc; Chả cá Đà Nẵng nướng lá chuối; Cá kho tộ mộc mạc; Cá lóc nướng trui; Gà nướng muối ớt bản mộc; Bò một nắng chấm muối kiến vàng; Sườn nướng mật mía; Tôm rang me vườn nhà; Thịt kho tàu lá mơ; Canh chua cá lóc; Canh rau tập tàng nấu tôm; Rau lang luộc chấm mắm nêm.

The query excluded inactive rows and all `Demo ...`/test products. The backend Product API remains the source of truth; no frontend name-to-image map was introduced. Batch 1 conclusions, including unresolved IDs 8 and 22, are unchanged.

### Visual/provenance manifest

All 15 current URLs were downloaded to a temporary review cache and loaded successfully. SHA-256 and dHash values below describe those review bytes; remote catalog URLs remain `REMOTE_NOT_FINALIZED` because they are not committed assets. dHash uses the repository's documented 9×8 grayscale comparison and no Batch 1 local image was an exact or ≤16-distance duplicate.

| ID | Product | Current visual finding | Semantic | Source / author / license | SHA-256 (review cache) | dHash | Action |
|---:|---|---|---|---|---|---|---|
| 75 | Gỏi cuốn mộc | Four fresh rice-paper rolls with greens and dipping sauce | HIGH | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Gỏi_cuốn_at_a_Vietnamese-style_restaurant_in_Beijing_(20180103175625).jpg); N509FZ; CC BY-SA 4.0 | `860DBD2122F514CC9BA68DEF1F7D2C5FC798E092061DAF0BBB9EB827317B634C` | `0101000100010000101001000111100001010010100101100111000100110011` | KEEP |
| 76 | Nộm rau rừng Đà Nẵng | Green-mango/shrimp salad; forest greens are not identifiable | MEDIUM | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Vietnamese_mango_salad_with_shrimp.jpg); HungryHuy; CC BY 2.0 | `E84DC2989E727F5A5676F42BFE0DB96D246B5D08453675784C7AAEC46513EB5C` | `0011001000110110010101111100111010011111110011011110011000110101` | NEED_USER_REVIEW |
| 77 | Bánh tráng cuốn thịt heo | Pork slices, rice paper, noodles, herbs and vegetables | HIGH | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Bánh_tráng_cuốn_thịt_heo_(18521).jpg); Lê Huỳnh Bộ; CC BY-SA 4.0 | `24B70A93A8FA7F621992AE3C105DC7E5E3FF657224DC69531CC626B5009C948D` | `0110101010101001000010010101010100110101011101000011001001101001` | KEEP |
| 78 | Đậu hũ non sốt mắm mộc | Bún đậu platter; fried tofu and shrimp paste, not tofu in fish sauce | LOW | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Bún_đậu_mắm_tôm_(phần_bánh_đậu_hũ_chiên)_quán_3_chị_em_tại_Nguyễn_Sơn_năm_2016_(1).jpg); Phương Huy; Public domain | `7CE98CCB1E5F0A0BEA4F28BB7CF7375B8F1D668E1F43A2EEC07F7DA6297A248F` | `0000111110000110010011110011110000111001100010011000100100111011` | REPLACE_REQUIRED / NEED_USER_REVIEW |
| 79 | Chả cá Đà Nẵng nướng lá chuối | Existing chả cá Lã Vọng pan was the wrong presentation; fish wrapped in banana leaves is now localized | HIGH | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Grilled_fish_banana_leaves.jpg); John Walker; CC BY 2.0 | `5E4FB4AE7820186EB547E3AB8BE8E6F032BEE4EEDE61F259260B92E5B11E049C` (localized) | `0011000110101010010101011011000111010001000110100100111011110010` | REPLACE (V089 local) |
| 80 | Cá kho tộ mộc mạc | Braised fish visibly served in a clay pot | HIGH | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Cá_kho_tộ.JPG); Binh Giang; Public domain | `D456BD6E12BC7C41C3EBBB899D87D49ED87B79A49E0C29DCF3CE84168A299FAD` | `0000100100000100001011000001110100101001011011110011101100001011` | KEEP |
| 81 | Cá lóc nướng trui | Whole fish roasting over an open fire | HIGH | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Cá_lóc_đồng_nướng_trui_3.jpg); Thang Nguyen; CC BY-SA 2.0 | `E7D6AA1066CF977810DFA29DA83C366D885B33A60444BB762D185073AAC9D3CB` | `0100011110001101010101011011001110101001010100010111001001101010` | KEEP |
| 82 | Gà nướng muối ớt bản mộc | Whole roasted chicken with red chili coating | HIGH | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Gà_nướng_muối_ớt.jpg); Baoothersks; CC BY-SA 4.0 | `72A5FB83BAA43BE2AA554FD2B2389F2BA7D6EA8C32244B67DE6F4753C7BF175F` | `1011010100110100011010011110100111100101011000101010110010010110` | KEEP |
| 83 | Bò một nắng chấm muối kiến vàng | Beef jerky/strips shown; dipping salt is not visible | MEDIUM | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Kantsun_Stydi_beef_jerky.jpg); JIP; CC BY-SA 4.0 | `79A2A702F34209A385BAA2B73E28FBBC034DC4C53921D9FE6173E1660166390B` | `0110101100011011001000110101000101101011110010110010001110010101` | NEED_USER_REVIEW |
| 84 | Sườn nướng mật mía | Glazed grilled ribs; source identifies Korean-style honey ribs | MEDIUM | [Wikimedia category](https://commons.wikimedia.org/wiki/Category:Party_food_in_Vietnam); Phương Huy; Public domain | `9366A9DA2350CBEFF3E8CEB88EA66532F30CE8188CD66964432AA9C5BD2F6692` | `1001101110100111001100010010101001101011101100011001001000001101` | NEED_USER_REVIEW |
| 85 | Tôm rang me vườn nhà | Shrimp/tamarind stew with vegetables; wetter than a dry rang me | MEDIUM | [Wikimedia file](https://commons.wikimedia.org/wiki/File:980Shrimp_and_prawn_stew_with_Baguio_beans,_napa_cabbage,_tomatoes_and_tamarind_soup_in_lemon_grass.jpg); Judgefloro; CC0 1.0 | `E2BE0A7E9E5D929BE2DB9560C82D4C9B6A77DE1B1EBAA25D330302287A692089` | `0001111000001111011001110110001101010011001011100011110000111010` | NEED_USER_REVIEW |
| 86 | Thịt kho tàu lá mơ | Thịt kho tàu with egg; lá mơ is not visible | MEDIUM | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Thịt_kho_Tàu.jpg); Viethavvh; CC BY-SA 4.0 | `6013B78DB9ED2D94D945D77C0B6025E8C9CDC8C2C9D4FD3A264568229A40F3AA` | `0001011101000011111000001110100111101001011010001111101000111010` | NEED_USER_REVIEW |
| 87 | Canh chua cá lóc | Sour soup with snakehead fish, pineapple, tomato and herbs | HIGH | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Canh_chua_cá_lóc_ở_Thủy_Trúc_Quán,_đường_Nguyễn_Nhữ_Lãm_(3).jpg); Phương Huy; Public domain | `A98F89D9AAE2CE3D473CEA170165DE2C8751A424DCDB29C690FBFDA7B6DB4C12` | `0000010000101011111111001010100010101010011010100101000101001100` | KEEP |
| 88 | Canh rau tập tàng nấu tôm | Shrimp-and-vegetable noodle soup; noodles make the dish mismatch | LOW | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Vietnamese_Shrimp_and_Vegetable_noodle_soup.jpg); Peachyeung316; CC BY-SA 4.0 | `F4C1163659113693D7574E42819C2207924532EA1A70B0170F5253CFAF0D3444` | `0011010010001011011101100101100100010111010001011000010011101000` | REPLACE_REQUIRED / NEED_USER_REVIEW |
| 89 | Rau lang luộc chấm mắm nêm | Raw sweet-potato shoots; not boiled greens or dipping sauce | LOW | [Wikimedia file](https://commons.wikimedia.org/wiki/File:Đọt_rau_lang.jpg); Bùi Thụy Đào Nguyên; CC BY-SA 3.0 | `0C81E7EB3B795CE76FB5CE407DD02D6CC10C8B3618DEB3BB8CEAF40754990901` | `1011101000111011011011100010111000101101011110110110111100101111` | REPLACE_REQUIRED / NEED_USER_REVIEW |

### Replacement candidate review

Only ID 79 met both the semantic and rights gates. The selected Commons image shows fish wrapped in banana leaves over a grill and is licensed CC BY 2.0; it is localized under a semantic filename in both public frontend assets and the bundled Spring Boot static tree. V089 updates only product ID 79 after V088.

The other mismatches remain review-only because no candidate met both gates. The following are visual references, not downloaded or used assets:

| Product | Candidate pages reviewed (minimum three) | Finding / decision |
|---|---|---|
| 76 Nộm rau rừng Đà Nẵng | [Dĩa rau sống](https://commons.wikimedia.org/wiki/File:Dĩa_rau_sống_ở_đường_NS_ng29th12n2022_(2).jpg) (CC0); [mango salad](https://commons.wikimedia.org/wiki/File:Vietnamese_mango_salad_with_shrimp.jpg) (CC BY 2.0); [prawn salad](https://commons.wikimedia.org/wiki/File:Prawn_salad.jpg) (CC BY-SA 4.0) | None proves Đà Nẵng forest greens; retain current image for user review. |
| 78 Đậu hũ non sốt mắm mộc | [Bún đậu 2019](https://commons.wikimedia.org/wiki/File:Bún_đậu_mắm_tôm_(2019).jpg) (CC BY-SA 4.0); [đậu hũ sốt cà](https://commons.wikimedia.org/wiki/File:Bữa_cơm_gia_đình_ng6th4n2021_(tô_đậu_hũ_sốt_cà)_(1).jpg) (license on page); [Chuối ốc đậu](https://commons.wikimedia.org/wiki/File:Chuối_ốc_đậu.jpg) (CC BY-SA 2.0) | All show different tofu dishes; none is tofu in fish sauce. |
| 79 Chả cá Đà Nẵng nướng lá chuối | [Grilled fish banana leaves](https://commons.wikimedia.org/wiki/File:Grilled_fish_banana_leaves.jpg) (CC BY 2.0); [Cá hồi nướng SG](https://commons.wikimedia.org/wiki/File:Cá_hồi_nướng_SG,ng13th2n2022_(2).jpg) (CC0); current [Chả cá Lã Vọng](https://commons.wikimedia.org/wiki/File:Chả_cá_Lã_Vọng_Hà_Nội_tháng_2_năm_2018_(2).jpg) (CC BY-SA 4.0) | First candidate is semantically HIGH and rights VERIFIED; localized in V089. |
| 88 Canh rau tập tàng nấu tôm | [VnExpress recipe](https://vnexpress.net/canh-rau-tap-tang-nau-tom-2769654.html) (copyright not stated); [Canh cua khoai sọ rau rút](https://commons.wikimedia.org/wiki/File:Canh_cua_khoai_sọ_rau_rút.jpg) (different soup); current [Vietnamese shrimp noodle soup](https://commons.wikimedia.org/wiki/File:Vietnamese_Shrimp_and_Vegetable_noodle_soup.jpg) (CC BY-SA 4.0) | No candidate clearly shows mixed garden greens with shrimp without a conflicting dish; review required. |
| 89 Rau lang luộc chấm mắm nêm | [Đọt rau lang](https://commons.wikimedia.org/wiki/File:Đọt_rau_lang.jpg) (CC BY-SA 3.0, raw); [Sweet potato leaves](https://commons.wikimedia.org/wiki/File:Sweet_potato_leaves.jpg) (CC0, raw); [Patriotic soup](https://commons.wikimedia.org/wiki/File:Song_dynasty's_'Patriotic_soup'_-Protect_the_Country_Dish_(護國菜)_.jpg) (cooked leaf soup) | No candidate shows the required Vietnamese boiled greens plus mắm nêm pairing; review required. |

### Batch 2 release state

`BATCH_2_AUDIT_COMPLETE = YES` · `BATCH_2_ALL_IMAGES_FIXED = NO` · `BATCH_2_REPLACEMENTS = 1` (ID 79) · `NEED_USER_REVIEW = [76,78,83,84,85,86,88,89]` · `REPLACE_REQUIRED = [78,88,89]`.

The localized ID 79 image is served from the same bundled Spring Boot SPA strategy as Batch 1. No remote image was silently copied into the repository, no Batch 1 asset was reused, and no crop/mirror/recolor transformation was applied. Browser/API regression is limited to the changed product and the Batch 1 smoke contract; full visual approval of the review-only items remains a product-owner decision.

Packaged-application QA confirmed that `/api/products` returns ID 79 with
`/images/products/cha-ca-da-nang-nuong-la-chuoi.jpg`; anonymous `GET` and
`HEAD` requests to that asset return `200`, while anonymous
`GET /api/admin/products` remains `401`. The public menu evidence is captured
in [Batch 2 menu page 3](qa/product-images/batch2-menu-1.png) and
[Batch 2 menu page 4](qa/product-images/batch2-menu-2.png). The clean-database
contract migrated V001 through V089 successfully; focused backend tests, all
464 backend tests, Maven package, frontend lint, 121 frontend tests, and the
frontend production build passed.

## 8. Batch 3 verification (2026-08-28)

### Scope and selection

The current SQL Server source of truth was queried for rows where `status = 1`
and `available = 1`, ordered by product ID. Batch 1 IDs
`[3,4,5,6,7,8,9,10,11,12,13,22]`, Batch 2 IDs `[75..89]`, and all inactive,
demo or test rows were excluded. The resulting 15-product Batch 3 is:

`[90,91,92,93,94,382,383,384,385,386,387,388,389,390,391]`.

Batch 1 and Batch 2 assets, migrations and backlog decisions were not changed.
In particular, V089 still maps ID 79 to
`/images/products/cha-ca-da-nang-nuong-la-chuoi.jpg`.

### Current-image audit and final manifest

Every current URL was downloaded to a temporary review cache and visually
opened. SHA-256 and dHash below describe the reviewed bytes, except ID 382's
final values which describe the localized V090 asset. The semantic checklist
uses the exact product-name claims: named ingredients must be visible when
reasonable, combo names must show their defining components, and branded beer
must visibly identify the stated brand.

| ID | Product / category | Old image | Visual and semantic checklist | Provenance / license | SHA-256 | dHash | Duplicate | Decision / browser / final status |
|---:|---|---|---|---|---|---|---|---|
| 90 | Đậu bắp bí đỏ hấp nước cốt dừa / Mộc Vị Đặc Trưng | [boiled okra](https://commons.wikimedia.org/wiki/File:BOILED_OR_STEAMED_OKRA_FROM_GARDEN_TO_TABLE.jpg) | Pot of okra only; pumpkin and coconut sauce are absent. LOW/FAIL. | Elmer Centeno Guevarra; CC BY-SA 4.0 | `A90217C9BF3BF8BE40AA03BB1780B25E2ACAE44659C2550E68739DDA2CF51E9D` | `1111100010110011000001101100110001010000010010110000100111100000` | UNIQUE | KEEP old bytes only because no eligible replacement exists; browser FAIL; `REPLACE_REQUIRED` / `NEED_USER_REVIEW` |
| 91 | Mì Quảng Đà Nẵng chuẩn vị / Mộc Vị Đặc Trưng | [Mì Quảng](https://commons.wikimedia.org/wiki/File:Mì_Quảng.jpg) | Broad noodles, pork/shrimp, herbs and bánh đa identify Mì Quảng. HIGH/PASS. | SauceSupreme; CC BY 2.0 | `DB6CCD5E1999F7F563040FA5C46FCF29AEDCDEDFC4A5E33F13F2E5A85849C544` | `0110010010100010000101111000100110110011100100111101001001110011` | UNIQUE | KEEP; browser PASS; `FINAL` |
| 92 | Cơm niêu cá kho + canh rau mộc / Mộc Vị Đặc Trưng | [Claypot rice](https://commons.wikimedia.org/wiki/File:Claypot_rice_1.jpg) | Claypot rice, fish and greens are visible, but a distinct vegetable soup is not. MEDIUM. | Neodymium+Nd; CC BY-SA 4.0 | `28C038FFA4A4457394D43BE70779CD28C6D41C03C3CBBAFFE7CD5E2D5B0188AE` | `0001111101111000000110000011100000111100001110001001101011101100` | UNIQUE | KEEP; browser NEED_USER_REVIEW; `NEED_USER_REVIEW` |
| 93 | Bún mắm nêm Đà Nẵng / Mộc Vị Đặc Trưng | [Đà Nẵng bún mắm](https://commons.wikimedia.org/wiki/File:Bún_mắm_thịt_heo_luộc_ở_Đà_Nẵng.jpg) | Rice noodles, boiled pork, vegetables and the documented Đà Nẵng fermented-shrimp-paste dish are visible. HIGH/PASS. | Beelerb; CC BY-SA 4.0 | `71ECF50BE70BE3567745AE11D3F9C6EE2067C0F09E49657DBCE62609AF7881AA` | `0011001111011000110110001100010111010011111010001100000000100001` | UNIQUE | KEEP; browser PASS; `FINAL` |
| 94 | Chè mộc / Mộc Vị Đặc Trưng | [Chè Thưng](https://commons.wikimedia.org/wiki/File:Chè_Thưng.jpg) | Vietnamese coconut dessert with taro/cassava/beans is clear, but “mộc” is not a verifiable dish variant. MEDIUM. | Zxcvasdfqwer888; CC BY-SA 3.0 | `A92137ACBA2381DD355A440443305F52B9978E199A1508255238B366A915BB26` | `0000011100010011001100110101100001011001000101110100111101111111` | UNIQUE | KEEP; browser NEED_USER_REVIEW; `NEED_USER_REVIEW` |
| 382 | Saigon Special / Bia Việt Nam | `saigonbeer.com.au/.../saigon_special...png` | Old 154×336 packshot identified the brand but produced a very tight crop. HIGH. | TLGI commercial product image; reuse license not stated | final `A11ABB2EF18BBE1663FA768B77130D335A15B5BD7435C93D640DDF22B538D770` | final `1001100010000000110100111101011111011111111101111101111100011111` | UNIQUE | REPLACE with `/images/products/saigon-special-cc-by-sa.jpg`; browser PASS after V090/V091; `FINAL` |
| 383 | Saigon Lager / Bia Việt Nam | `sabibeco.com/.../bia-lon-saigon-lager.jpg` | Correct white Saigon Lager can and brand. HIGH/PASS. | Commercial product source; reuse license not stated | `65D10350A2A19359C47CF2534C6087BFB6003007E25F92A7E4928DF2429E6E7F` | `0110100001101000011000000111000001110000011110000110100001110000` | UNIQUE; packshot-shape near group | KEEP; browser NEED_USER_REVIEW; `LICENSE_UNVERIFIED` / `NEED_USER_REVIEW` |
| 384 | Bia 333 / Bia Việt Nam | `ikemitsu.co.jp/.../333_can.png` | Correct 333-branded can. HIGH/PASS. | Commercial distributor source; reuse license not stated | `6711CE24D05CD6288227A1A1BB3AA0F8673D8E86197F94750A9AE6DA44A25BBD` | `0110000001100000011010000110100001100000010000000100000001100000` | UNIQUE; packshot-shape near group | KEEP; browser NEED_USER_REVIEW; `LICENSE_UNVERIFIED` / `NEED_USER_REVIEW` |
| 385 | Bia Hà Nội / Bia Việt Nam | `biahanoi.muabianhanh.com/.../Web_HNP2lon2...jpg` | Correct Hanoi-branded cans. HIGH/PASS. | Retailer source; reuse license not stated | `DE32A002C901275A22427D90BD385120FE7D2109403F289F06457A4113000AF0` | `0110100000111000011010000110100001101000011010000110100001101000` | UNIQUE; packshot-shape near group | KEEP; browser NEED_USER_REVIEW; `LICENSE_UNVERIFIED` / `NEED_USER_REVIEW` |
| 386 | Larue / Bia Việt Nam | `heineken-vietnam.com.vn/.../larue-smooth...jpg` | Correct Larue Smooth can and brand. HIGH/PASS. | Heineken Vietnam product source; reuse license not stated | `45EEB56A5117AB80DBCCCA36C16A3CF1F6396A8E515F79A723A7A1FCC2D77A15` | `0011000000110000001100000011000000110000001100000011000000110000` | UNIQUE; packshot-shape near group | KEEP; browser NEED_USER_REVIEW; `LICENSE_UNVERIFIED` / `NEED_USER_REVIEW` |
| 387 | Heineken / Bia quốc tế | [Heineken bottle](https://commons.wikimedia.org/wiki/File:CreativeTools.se_-_PackshotCreator_-_Heineken_beer_bottle_v01_(4290167332).jpg) | Correct Heineken bottle and label. HIGH/PASS. | Creative Tools; CC BY 2.0 | `84F179B1E0D4AD8807E8DDE3E26F13368128392A3E09CB6935046CE95372EAE1` | `1111000001110000001100000111000001110000011100000111000011110000` | UNIQUE; packshot-shape near group | KEEP; browser PASS; `FINAL` |
| 388 | Tiger / Bia quốc tế | [Tiger bottles](https://commons.wikimedia.org/wiki/File:Tiger_Beer_Bottles.png) | Multiple correctly branded Tiger bottles. HIGH/PASS. | Asia Pacific Breweries Limited; CC BY-SA 3.0 | `2B3AE44B438DAA62CC30DC0802297BE430D957CF61F0D5FC5F2BF694B3E9C5DD` | `0110001101110011001100111010010110100101101001010011001101100011` | UNIQUE | KEEP; browser PASS; `FINAL` |
| 389 | Budweiser / Bia quốc tế | [Budweiser beer](https://commons.wikimedia.org/wiki/File:Budweiser_beer.jpg) | Correct Budweiser bottle and branded glass. HIGH/PASS. | Matthew Hurst; CC BY-SA 2.0 | `3928202019987907F0FAD4BD30F4DEE05D7510D9E15350023B38813F19561500` | `1100000100100011000000111010001000110010001111001010111000011110` | UNIQUE | KEEP; browser PASS; `FINAL` |
| 390 | Corona Extra / Bia quốc tế | `bargainbooze.co.uk/.../corona620.png` | Correct Corona Extra bottle and label. HIGH/PASS. | Retailer source; reuse license not stated | `4A835724D293666A9FDDE90EBA0A07CC444765DC1C504EA53DECBE2EA97893DB` | `0000100000010100000001000001110000010100000101000000110000010100` | UNIQUE | KEEP; browser NEED_USER_REVIEW; `LICENSE_UNVERIFIED` / `NEED_USER_REVIEW` |
| 391 | Hoegaarden / Bia quốc tế | Shopify CDN `Hoegaarden_Anno_1445...jpeg` | Correct Hoegaarden bottles and six-pack. HIGH/PASS. | Store CDN; original author/reuse license not stated | `31554A802B7CFBA06FF7388802E96E12F358A72B90050660CD0D0D6EED57DC1A` | `1010100010101010101010001111100000010111010101010110010111100110` | UNIQUE | KEEP; browser NEED_USER_REVIEW; `LICENSE_UNVERIFIED` / `NEED_USER_REVIEW` |

### Duplicate analysis

There are no same URLs, local paths or SHA-256 values within Batch 3 or across
the recorded Batch 1/2 assets. No Batch 3 image is at dHash distance ≤16 from a
Batch 1/2 image. Seven Batch-3 pairs crossed the dHash screening threshold:
`383–384 (12)`, `383–385 (10)`, `383–386 (16)`, `383–387 (11)`,
`384–385 (10)`, `384–387 (15)`, and `386–387 (9)`. Visual review confirmed
that these are different branded cans/bottles on similarly plain packshot
backgrounds, not duplicate scenes. No image was cropped, mirrored, resized or
recolored to evade duplicate detection.

### Replacement candidate review

Only products whose image was wrong or whose provenance was unacceptable were
searched. No search-result thumbnail was used as an asset.

| Product | Candidate | Author / license | Visual and semantic confidence | Decision |
|---|---|---|---|---|
| ID 90 Đậu bắp bí đỏ hấp nước cốt dừa | [Coconut Pumpkin Curry](https://www.myskikitchen.com/recipes/coconut-pumpkin-curry) | Dee Kirk; reuse license not stated | Pumpkin, coconut milk and okra are present, but it is a curry with prawns. MEDIUM. | VISUAL_REFERENCE_ONLY |
| ID 90 | [Pumpkin & okra ragout](https://www.kikkoman.co.uk/food-service/recipes/detail/pumpkin-okra-ragout-with-rice) | Kikkoman; reuse license not stated | Pumpkin, okra and coconut milk are present, but it is a ragout with rice/chickpeas. MEDIUM. | VISUAL_REFERENCE_ONLY |
| ID 90 | [Coconut-milk steamed fish](https://www.karifying.com/blog/granny-ds-village-steamed-fish) | Karifying in the Kitchen; reuse license not stated | Pumpkin, okra and coconut milk are visible, but fish changes the named dish. LOW-MEDIUM. | VISUAL_REFERENCE_ONLY; `SEARCH_EXHAUSTED` |
| ID 382 Saigon Special | [Saigon Special Beer](https://commons.wikimedia.org/wiki/File:Saigon_Special_Beer.jpg) | Wiki.cullin; CC BY-SA 4.0 | Exact branded bottle, higher resolution and less extreme aspect ratio. HIGH; license VERIFIED. | SELECTED; localized by V090 |
| ID 382 | [Bia Saigon Special beer](https://commons.wikimedia.org/wiki/File:Bia_Saigon_Special_beer.jpg) | Riza Nugraha; CC BY 2.0 | Exact branded bottle, but lower resolution and narrower composition. HIGH; license VERIFIED. | REJECT in favor of candidate 1 |
| ID 382 | [Saigon Special Can](https://www.saigonbeer.com.au/productdetail/saigon_special_can-20.gss) | TLGI Pty Ltd; license not stated | Exact branded can; the existing low-resolution catalog source derives from this product family. HIGH. | REJECT; license UNVERIFIED |

### V090/V091 asset and browser QA

V090 is ID-scoped (`WHERE id = 382 AND status = 1`) for the upgraded source-of-
truth database. V073 originally inserted alcohol products through a name-based
MERGE and let SQL Server allocate identity values, so a clean database can give
Saigon Special a different numeric ID. V091 therefore requires exactly one
`Saigon Special` semantic-key match, resolves its ID, and performs the UPDATE by
that resolved ID. Neither migration can alter a Batch 1/2 row. The original
Commons bytes are stored at
`Frontend/nha-hang-frontend/public/images/products/saigon-special-cc-by-sa.jpg`
and
`quanlynhahang/src/main/resources/static/images/products/saigon-special-cc-by-sa.jpg`.
Both files have SHA-256
`A11ABB2EF18BBE1663FA768B77130D335A15B5BD7435C93D640DDF22B538D770`.

Chrome page-4 QA before V090 is recorded in
[Batch 3 menu upper view](qa/product-images/batch3-menu-1.png) and
[Batch 3 menu lower view](qa/product-images/batch3-menu-2.png). After applying
V090 and restarting the application, the same two screenshots are regenerated
so the final evidence reflects the localized ID 382 asset.

`BATCH_3_AUDIT_COMPLETE = YES` · `BATCH_3_ALL_IMAGES_FIXED = NO` ·
`BATCH_3_REPLACEMENTS = 1` (ID 382) ·
`NEED_USER_REVIEW = [90,92,94,383,384,385,386,390,391]` ·
`REPLACE_REQUIRED = [90]`.

Final verification passed: the focused product/migration tests ran 2 tests with
no failures; a blank SQL Server database migrated V001 through V091; the full
backend suite ran 464 tests with no failures or errors; Maven package passed;
frontend lint passed; all 32 frontend test files / 121 tests passed; and the
Vite production build passed. Packaged-application smoke returned `200
image/jpeg` for anonymous GET and HEAD of the V090 asset and `401` for the
anonymous admin products endpoint.

## 9. Batch 4 verification (2026-08-28)

### Scope and selection

The source-of-truth query selected the next 15 `status = 1 AND available = 1`
products after excluding every Batch 1--3 ID and all demo/test rows. The exact
scope is `[392,393,394,395,396,397,398,399,400,401,402,403,404,405,406]`.
No Batch 1--3 migration, image, decision or backlog item was changed.

### Current-image manifest

Each URL was opened from the original source and then loaded in the packaged
customer menu. Browser natural dimensions were non-zero for all 15 cards. The
commercial catalog sources below do not publish redistribution terms, therefore
they remain `LICENSE_UNVERIFIED` even when the product label is visually exact.
The dHash values use the audit's 9x8 grayscale comparison; ID 402's value is
computed from the browser-rendered WebP because the Windows image decoder cannot
read WebP directly. No image was cropped, mirrored, resized or recolored.

| ID | Product / category | Current image URL | Visual/semantic checklist | Source / license | SHA-256 (review bytes) | dHash | Duplicate | Initial status |
|---:|---|---|---|---|---|---|---|---|
| 392 | Jacob's Creek Cabernet Sauvignon / Vang đỏ | [solidwineonline](https://solidwineonline.com/cdn/shop/files/JacobsCreekCabernetSauvignon.png?v=1706082353) | Bottle label clearly says Jacob's Creek Cabernet Sauvignon; HIGH | Commercial product CDN; license not stated | `3C0B68BE4C07490D67337452624ACC907EBC10F0C50B516633EB34C75648AC8D` | `0001000000010000001100000011000000110000001100000011000000110000` | UNIQUE | LICENSE_UNVERIFIED |
| 393 | Casillero del Diablo Cabernet Sauvignon / Vang đỏ | [Walmart image](https://i5.walmartimages.com/seo/Casillero-del-Diablo-Cabernet-Sauvignon-Chile-750-ml-Bottle-14-ABV_c48e2eef-d05c-4303-9efa-683cf4a46e7c.d5863bf24160e39109531e262b043c76.png) | Casillero del Diablo label and Cabernet Sauvignon are visible; HIGH | Retailer CDN; license not stated | `C2CFA4EBE9B3C74CCCB6C7E3A33463EFD552E2F9B5E6455E638678664C225A46` | `1110011011100010111001101110011111100111111001101110001111100011` | UNIQUE | LICENSE_UNVERIFIED |
| 394 | Yellow Tail Shiraz / Vang đỏ | [cebooze](https://www.cebooze.com/app/uploads/2020/09/yellow-tail-shiraz-800x800.jpg) | Yellow Tail branding and Shiraz varietal are visible; HIGH | Retailer source; license not stated | `02C71580EB284005862FA6170C147F124E15D274DAEF2F4E7B32A71D2B8AB2B8` | `0001000000010000001100000011000000110000001100000011000000110000` | UNIQUE | LICENSE_UNVERIFIED |
| 395 | Penfolds Koonunga Hill Shiraz Cabernet / Vang đỏ | [Metcash image](https://cdn.metcash.media/image/upload/w_1500,h_1500,c_pad,b_auto/alm-online/images/855798.jpg) | Label reads Cabernet Sauvignon only; the named Shiraz Cabernet blend is not established; LOW/FAIL | Retailer source; license not stated | `1D2525BB2A3F015567928BC339721267163962DBB459D84CC661B646D051D352` | `0001000000010000001100000011000000110000001110000011000000110000` | UNIQUE | REPLACE_REQUIRED / NEED_USER_REVIEW |
| 396 | Château Los Boldos Cabernet Sauvignon / Vang đỏ | [Tcdn product image](https://images.tcdn.com.br/img/img_prod/1199398/vinho_chanteau_los_boldos_gran_reserva_cabernet_sauvignon_750ml_2937_2_952b139c9e03c5de27dd43c88de28c65.jpg) | Château Los Boldos label and Cabernet Sauvignon are visible; HIGH | Retailer source; license not stated | `006A106AF071F982274CB92E1DEDB9C6856D629FE41407A97082D92D1B4A014D` | `1111000011110000111100001111000011110000110101001111000011010000` | UNIQUE | LICENSE_UNVERIFIED |
| 397 | Jacob's Creek Chardonnay / Vang trắng | [Mickey Kelly's Bar image](https://mickeykellysbar.com/wp-content/uploads/2020/06/jacobs-creek-chardonnay.jpg) | Jacob's Creek Classic Chardonnay label is clear; HIGH | Commercial bar source; license not stated; browser loads successfully | `8E4AC04A009A52165A40F8F85EFFDC7F4F7085D9D539F31F707CF782BE2A46F0` | `0001000000010000001100000011000000110000001100000011000000110000` | UNIQUE | LICENSE_UNVERIFIED |
| 398 | Casillero del Diablo Sauvignon Blanc / Vang trắng | [Walmart image](https://i5.walmartimages.com/seo/Casillero-del-Diablo-Sauvignon-Blanc-Chile-750-ml-Glass-Bottle-13-ABV_12d7e3c4-1c95-438b-b9ad-9aad6bcc8a9e.3351ca5d0cc6d490d3e8a8cc0f80fb57.png) | Casillero del Diablo and Sauvignon Blanc are visible; HIGH | Retailer CDN; license not stated | `B675899FC2663F87D40D6548AF8B5D73418E975877EF5CA691FCB4DFF395B895` | `1110011011100010111000101110001111100010111000101110001111100011` | UNIQUE | LICENSE_UNVERIFIED |
| 399 | Yellow Tail Chardonnay / Vang trắng | [Walmart image](https://i5.walmartimages.com/seo/Yellow-Tail-Chardonnay-Australia-750-ml-Bottle-13-ABV_1513083c-804b-40ca-948c-599e8554322b.7bfa9d8650c8ad1510703e782b5974f9.jpeg) | Yellow Tail and Chardonnay label are visible; HIGH | Retailer CDN; license not stated | `AB1049A70767437F5F851B4D25DD81E1244FCE742A8F65A382A66284D0720FD7` | `0001000000010000000100000011100000111000001110000011000000110000` | UNIQUE | LICENSE_UNVERIFIED |
| 400 | Oyster Bay Sauvignon Blanc / Vang trắng | [Oyster Bay source](https://www.oysterbaywines.com/uploads/SB-24-wGlass-Blue-gradient-1980x1988px.jpg) | Oyster Bay bottle, Sauvignon Blanc and New Zealand label are clear; HIGH | Winery product source; license not stated | `D4A0F6F02D277627583F4409DD56279F768A5E6A8F118EF81A0055E57CB561FF` | `0110010001010100010101000110000001100000011100000110000001100000` | UNIQUE | LICENSE_UNVERIFIED |
| 401 | Villa Maria Sauvignon Blanc / Vang trắng | [SAQ image](https://www.saq.com/media/catalog/product/1/1/11974951-1_1659641150.png) | Villa Maria, Sauvignon Blanc and Marlborough labels are clear; HIGH | Retailer source; license not stated | `6985A7CABA781DB91EF05C06680E64CA3B6E080F22524C0D86B25CD4992FFCC8` | `0000110000001100000011000000101000001110000101100000111000010110` | UNIQUE | LICENSE_UNVERIFIED |
| 402 | Johnnie Walker Black Label / Whisky | [Contentful product hero](https://images.ctfassets.net/waruwpig3jxu/rYk8WxpJ1ellZvibRckWT/0e8df23f16b39c6d986faf6f68af8fee/black-750ml_producthero_lifestyle-01_desktop.webp) | Johnnie Walker Black Label bottle and label are unmistakable; HIGH | Brand CDN; license not stated | `5577BF3A1A4F9920307EF6919AFAA27BE6248760D02562C3F283DEB994E330DD` | `0001011111001011110011000011100100110011001110010111000101110011` (browser render) | UNIQUE | LICENSE_UNVERIFIED |
| 403 | Chivas Regal 12 / Whisky | [Metcash image](https://cdn.metcash.media/image/upload/w_1500,h_1500,c_pad,b_auto/alm-online/images/583139.jpg) | Chivas Regal 12 and blended Scotch whisky labels are clear; HIGH | Retailer source; license not stated | `F8A490150E19D7BC9655F9518A20348C93031FE4D8455F59F9680C951696AEC2` | `0011000000110000001100000111000001101000011100000110100001101000` | UNIQUE | LICENSE_UNVERIFIED |
| 404 | Ballantine's Finest / Whisky | [Value Cellars image](https://ww1.valuecellars.com.au/files/2016/05/5010106113127-1.png) | Ballantine's Finest label and Scotch whisky claim are clear; HIGH | Retailer source; license not stated | `46278DA21193AAC3EBF96CA06F3EABF79CE3357DAF18281B3B8F00D15E23100A` | `0011000000110000011100000111000001110000011100000110100001110000` | UNIQUE | LICENSE_UNVERIFIED |
| 405 | Jack Daniel's Old No.7 / Whisky | [Selection Prestige image](https://cdn.selection-prestige.de/media/catalog/product/cache/image/1536x/a4e40ebdc3e371adff845072e1c73f37/9/9/99733_jack-daniels-old-no-7-tennessee-whiskey-10l-40-vol.jpg) | Jack Daniel's Old No.7 Tennessee Whiskey label is clear; HIGH | Retailer source; license not stated | `0AA17912D4FF08F142F105B9454C4DDAD958809354EC8E8E40E38F8123C82FD0` | `0001000000110000001100000011000000110000001100000011000000110000` | UNIQUE | LICENSE_UNVERIFIED |
| 406 | Jameson / Whisky | [Fine Wine Delivery image](https://www.finewinedelivery.co.nz/content/products/original/16561.jpg?width=1136) | Jameson Irish Whiskey label is clear; HIGH | Retailer source; license not stated | `15C2347E3CF35903DD03F68BCC9D23DB0D42B7C3D5D83A9579C042F23BED838B` | `0011000000110000011100000111100001110000011100000111100001111000` | UNIQUE | LICENSE_UNVERIFIED |

### ID 395 replacement review

The current image is a Koonunga Hill Cabernet Sauvignon bottle, while the
catalog name explicitly claims a Shiraz Cabernet blend, so it is not promoted to
HIGH semantic confidence. Three original-source candidates were checked:

| Candidate | Source / author | License | Visual result | Decision |
|---|---|---|---|---|
| Penfolds Koonunga Hill Shiraz 2024 | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Penfolds_Koonunga_Hill_Shiraz_2024_wine.jpg), Kgbo | CC BY-SA 4.0 | Correct brand and Shiraz, but Cabernet component is not visible; MEDIUM | VISUAL_REFERENCE_ONLY |
| Penfolds Koonunga Hill Shiraz Cabernet 2023 | [Flickr](https://www.flickr.com/photos/hhs/54903691831/in/album-72157600982551800), hhschueller | CC BY-NC-SA 2.0 | Exact product wording, but non-commercial license; HIGH visual | REJECT for this bundle |
| Penfolds Koonunga Hill Shiraz Cabernet 2019 | [Flickr](https://www.flickr.com/photos/spelio/51996841701/), spelio | CC BY-NC-SA 2.0 | Exact product wording, but non-commercial license; HIGH visual | REJECT for this bundle |

`SEARCH_EXHAUSTED_FOR_ELIGIBLE_REPLACEMENT = YES`; no new asset or migration is
created in Batch 4. ID 395 remains `REPLACE_REQUIRED / NEED_USER_REVIEW` (semantic
FAIL) for an owner decision on an appropriately licensed exact blend photograph.

### Duplicate analysis

There are no same URLs, local paths or SHA-256 values within Batch 4 or against
the recorded Batch 1--3 assets. Pairwise dHash screening produced 42 Batch-4
near pairs and 33 cross-batch near pairs at distance ≤16. Every flagged pair was
visually reviewed: the within-batch pairs are different wine/whisky brands,
labels, bottle silhouettes or scenes; the cross-batch pairs are distinct
products from the beer/food packshot groups. These are false positives caused by
plain-background bottle geometry, not the same scene or crop. Consequently:

`EXACT_DUPLICATE = 0` · `DHASH_NEAR_DUPLICATE = 75` ·
`DHASH_REAL_DUPLICATE = 0` · `DHASH_FALSE_POSITIVE = 75`.

### Browser QA and regression

The packaged customer menu was rendered at desktop resolution and navigated to
pages 4 and 5. Screenshots cover the full Batch-4 card set:
[Batch 4 menu page 4](qa/product-images/batch4-menu-1.png) and
[Batch 4 menu page 5](qa/product-images/batch4-menu-2.png). The browser reported
non-zero natural dimensions for IDs 392--406 and the labels matched the
manifest. ID 79 still resolves to
`/images/products/cha-ca-da-nang-nuong-la-chuoi.jpg`; ID 382 still resolves to
`/images/products/saigon-special-cc-by-sa.jpg`.

No Batch-4 migration or local asset was required, so V091 remains the latest
Flyway version and no new Flyway history entry is expected.

`BATCH_4_AUDIT_COMPLETE = YES` · `BATCH_4_ALL_IMAGES_FIXED = NO` ·
`SEMANTIC_PASS = 14` · `SEMANTIC_FAIL = 1` ·
`SEMANTIC_MEDIUM_REVIEW = 0` · `LICENSE_VERIFIED = 0` ·
`LICENSE_UNVERIFIED = 15` · `NEED_USER_REVIEW = [392,393,394,395,396,397,398,399,400,401,402,403,404,405,406]` ·
`REPLACE_REQUIRED = [395]`.

## Batch 5 — verification and targeted remediation

Batch 5 was selected from the live `RestaurantDB` source of truth with
`status = 1 AND available = 1`, after excluding every Batch 1–4 ID and demo
rows. The exact scope is `[407,408,409,410,411,412,413,414,415,416,417,418,419,420,421]`.
The customer menu was checked on pages 5 and 6; source failures were recorded
from the rendered result rather than inferred from filenames.

| ID | Product / category | Old image | Browser / visual result | Semantic | Source / license | SHA-256 (review bytes) | dHash | Final image | Final status |
|---:|---|---|---|---|---|---|---|---|---|
| 407 | Absolut / Vodka | `https://www.absolut.com/wp-content/uploads/absolut-vodka-original-2021-against-white-background.jpg?imwidth=350` | Source failed; UI rendered generic food fallback | FAIL → HIGH after replacement | [Commons candidate](https://commons.wikimedia.org/wiki/File:Absolut_vodka.jpg), Henrik Abelsson, CC BY 2.5 | `B525E14633794C6F2699E3BA32890FA349461FB20C83FDEE0B2AFA2D1EEF71D9` | `1110111011100110110001101111000011010001011010010110000111101001` | `/images/products/absolut-vodka-cc-by.jpg` | FINAL |
| 408 | Smirnoff Red / Vodka | `https://cdn.metcash.media/image/upload/w_1500,h_1500,c_pad,b_auto/alm-online/images/591815.jpg` | Bottle and SMIRNOFF/VODKA label loaded | HIGH | Retailer CDN; license not stated | `24CF3B359310997B927E64535058B773FED07474106DEB08CB8BD527F167CE3E` | `0011000000110000011100000111000001110000011100000111000001110000` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 409 | Finlandia / Vodka | `https://ie.coca-colahellenic.com/content/dam/cch/ie/images/our-24-7-portfolio_new/FINLANDIA%20ORIGINAL%20Bottle.jpg` | Source failed; UI rendered generic food fallback | FAIL → HIGH after replacement | [Commons candidate](https://commons.wikimedia.org/wiki/File:Finlandia_Classic_vodka.jpg), Undicca, CC BY-SA 4.0 | `61DAB858134155862A141CE9DEF9DE415A1A7BEC26708A5E8EC351F099C77053` | `1100100111001001100010111001001010010100001101000111010101110001` | `/images/products/finlandia-vodka-cc-by-sa.jpg` | FINAL |
| 410 | Grey Goose / Vodka | `https://www.greygoose.com/binaries/content/gallery/greygoose/products/grey-goose-vodka/ggo-bottle-intl.png` | Grey Goose bottle and label loaded | HIGH | Official brand CDN; license not stated | `CAE6A8AE7BCFE4D5ADE4FFE372A4BBADC5EFCCA1D089B0ECA307E140E0D85054` | `0000111000001010000011110000111100010011000101110000101100001111` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 411 | Belvedere / Vodka | `https://liquorshop.hk/wp-content/uploads/2020/08/Belvedere-Vodka.jpg` | Belvedere label and bottle loaded | HIGH | Retailer source; license not stated | `32C665CF6A022D01D2FD0F7A861FC49A6EF59C04D51A6E11B734D66C3047E128` | `0001000000010000001100000011000000110000001100000010100000111000` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 412 | Hennessy VS / Cognac / Brandy | `https://www.hennessy.com/sites/hennessy/files/2020-01/VS_0.png` | Source failed; UI rendered generic food fallback | FAIL → HIGH after replacement | [Commons candidate](https://commons.wikimedia.org/wiki/File:2023_Hennessy_V.S._Cognac.jpg), Jacek Halicki, CC BY-SA 4.0 | `CE5EE7082A30EB20351A94A0AD227547A626F4A5AB784A8B93DEBE9B30628A2D` | `0100000001100010011000110110000011100000111001101110011011100100` | `/images/products/hennessy-vs-cognac-cc-by-sa.jpg` | FINAL |
| 413 | Rémy Martin VSOP / Cognac / Brandy | `https://static-prod.remymartin.com/app/uploads/2023/11/vsop-collection-1600-front-02.png` | Rémy Martin and VSOP labels visible | HIGH | Official brand CDN; license not stated | `4CCEFEBEFBF09C3AB16F56CFAB5D9F574C3F6A0885FCE7EA4A98A04C61027299` | `0000100000001000000010000001110000001100000111000001110000011100` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 414 | Martell VSOP / Cognac / Brandy | `https://devinecellars.com.au/wp-content/uploads/Martell-VSOP-Cognac.jpg` | Martell bottle and VSOP box visible | HIGH | Retailer source; license not stated | `98105BFA46B6DCB47553D0CDF9D109CCB2E48FC0839AA12404F9CC80B5AC2974` | `0101110001010100110100001101010011010100110101001101010011101100` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 415 | Courvoisier VSOP / Cognac / Brandy | `https://worldsbestwines.eu/wp-content/uploads/Courvoisier-VSOP-70cl-Bottle.jpg` | Courvoisier VSOP label visible | HIGH | Retailer source; license not stated | `C5B986C13155D69DE057D8FF55B66A5F2C78BBBD03EE3F2532986C6D8025F7F9` | `0011000000110000001100000111000011110000111010000111000001110000` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 416 | Camus VSOP / Cognac / Brandy | `https://cdn.shopify.com/s/files/1/0050/2395/7103/products/cognac-intensely-aromatic-camus-vsop-confezione_33540_zoom_700x.jpg?v=1670500723` | Camus VSOP bottle and box visible | HIGH | Retailer CDN; license not stated | `9AD2BD805A917238F759DC83B771C863FF32D06F4EB96341A07E7A19100F9830` | `1110000011100000101100001111000011010100111110001110010011100000` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 417 | Dassai 45 / Sake | `https://cdn.shopify.com/s/files/1/0212/1922/products/dassai_45_1020x.progressive.jpg?v=1615324785` | Dassai 45 label visible | HIGH | Retailer CDN; license not stated | `4A8A4A5D7BC4078D8E22A9FF1C5D4A7DBB26C944CF3B47E5ACDCB43A3C133A4B` | `0011000000110000001100000011000000110000001100000011000000110000` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 418 | Gekkeikan Traditional / Sake | `https://us.gekkeikan.com/wp-content/uploads/2020/03/TRADITIONAL-1.5-FRONT-1152x1536.png` | Browser showed a 200×200 “This image” placeholder | FAIL / MEDIUM | Official brand URL; license not stated; [Commons reference](https://commons.wikimedia.org/wiki/File:Gekkeikanbottle.JPG) is CC BY-SA/GFDL but a different packaging/context | `E525391811505BC1233AA3D59D40B74101784E7B83426E408828928C1FDEAFB4` | `0001000000010000001100000011000001100000011010000110000001111000` | unchanged remote URL | REPLACE_REQUIRED / NEED_USER_REVIEW |
| 419 | Hakutsuru Junmai / Sake | `https://aem.lcbo.com/content/dam/lcbo/products/0/1/2/8/012849.jpg.thumb.1280.1280.jpg` | Hakutsuru label and Junmai sake bottle visible | HIGH | Retailer source; license not stated | `AD5262007C2535964AC35E6330E287660094524C04E7BB61A45E1298B14346EF` | `0001000000110000001100000011000000110000001100000011000001110000` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 420 | Ozeki Junmai / Sake | `https://drinxmarket.com/wp-content/uploads/2023/05/104776.png` | Ozeki Sake / Premium Junmai label visible | HIGH | Retailer source; license not stated | `3BD502FFC1A6D2EB4D7EC5E51FE176BB46DC7F4D2244F772A16AF127CB02384F` | `0001010000010110010101010011001101110001001100110111000111100001` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |
| 421 | Kubota Senju / Sake | `https://images.squarespace-cdn.com/content/v1/5c334aca372b96b6bfd22e33/1600792366965-DERDSNOLLSP1ZZMNV7GV/Kubota+Senju+Tokubetsu+Honjozo+720ml+2000x2000+%281%29.jpg` | Kubota Senju label visible | HIGH | Retailer/source site; license not stated | `88B93D5753CFBFC96AE917D923693EF5BAC154401ED1F893CF322328CCDCA077` | `0001000000010000001100000011000000110000001100000011000000110000` | unchanged remote URL | LICENSE_UNVERIFIED / NEED_USER_REVIEW |

The three accepted Commons replacements were visually inspected before
localization. Gekkeikan's Commons image is a visual reference only: its bottle
and setting do not prove the exact “Traditional” retail variant, so it was not
bundled automatically. The four original failures are retained in the
manifest as evidence; after V092, IDs 407, 409 and 412 use the verified local
assets above.

### Batch 5 replacement candidates

| Product | Candidate review | Decision |
|---|---|---|
| Absolut | [Absolut vodka](https://commons.wikimedia.org/wiki/File:Absolut_vodka.jpg), Henrik Abelsson, CC BY 2.5; exact Absolut bottle, HIGH | ACCEPT; localized |
| Finlandia | [Finlandia Classic vodka](https://commons.wikimedia.org/wiki/File:Finlandia_Classic_vodka.jpg), Undicca, CC BY-SA 4.0; exact Finlandia Classic bottle, HIGH | ACCEPT; localized |
| Hennessy VS | [2023 Hennessy V.S. Cognac](https://commons.wikimedia.org/wiki/File:2023_Hennessy_V.S._Cognac.jpg), Jacek Halicki, CC BY-SA 4.0; exact V.S. label, HIGH | ACCEPT; localized |
| Gekkeikan Traditional | [Gekkeikanbottle.JPG](https://commons.wikimedia.org/wiki/File:Gekkeikanbottle.JPG), Fotonovela, CC BY-SA/GFDL; Gekkeikan sake bottle but not proven exact Traditional packaging, MEDIUM | VISUAL_REFERENCE_ONLY; keep review |

`NEW_MIGRATION = V092__localize_batch_five_verified_product_images.sql`.
It uses exact Unicode product-name matches with an active/public row-count guard;
it does not assume identity values, reseed, delete or insert products. Frontend
and backend copies of all three accepted assets have matching SHA-256 values.

### Batch 5 duplicate analysis

There are no same URLs, local paths or SHA-256 values among the final Batch 5
assets or against recorded Batch 1–4 assets. Pairwise dHash screening produced
22 Batch-5 internal near pairs and 94 cross-batch near pairs at distance ≤16.
All flagged pairs were visually reviewed; they are different branded bottles,
labels or packshot compositions. No same scene/crop/resize reuse was found.

`BATCH_5_AUDIT_COMPLETE = YES` · `BATCH_5_ALL_IMAGES_FIXED = NO` ·
`INITIAL_SEMANTIC_PASS = 11` · `INITIAL_SEMANTIC_FAIL = 4` ·
`FINAL_SEMANTIC_HIGH = 14` · `FINAL_SEMANTIC_MEDIUM = 1` ·
`LICENSE_VERIFIED = 3` · `LICENSE_UNVERIFIED = 12` ·
`NEED_USER_REVIEW = [408,410,411,413,414,415,416,417,418,419,420,421]` ·
`REPLACE_REQUIRED = [418]` · `EXACT_DUPLICATE = 0` ·
`DHASH_NEAR_DUPLICATE = 116` · `DHASH_REAL_DUPLICATE = 0`.

### Batch 5 browser QA

[Batch 5 menu page 5](qa/product-images/batch5-menu-1.png) and
[Batch 5 menu page 6](qa/product-images/batch5-menu-2.png) cover IDs 407–421.
Before V092, the browser recorded generic fallback renders for 407, 409 and
412 and a placeholder render for 418; all other cards loaded their branded
remote image. After applying V092 and restarting the packaged application, the
browser loaded the local assets for 407, 409 and 412 with non-zero natural
dimensions and the expected product labels. ID 418 still renders the original
200×200 “This image” placeholder and remains `REPLACE_REQUIRED / NEED_USER_REVIEW`.
