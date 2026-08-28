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
