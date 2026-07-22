package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.*;
import poly.edu.quanlynhahang.repository.*;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.NotificationService;

import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/admin/purchase-suggestions")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
public class PurchaseSuggestionController {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientBatchRepository ingredientBatchRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Lấy danh sách đề xuất mua hàng tự động
     * Logic: Nguyên liệu dưới minStock + tính tốc độ tiêu thụ 7 ngày qua
     */
    @GetMapping
    public ResponseEntity<?> getSuggestions() {
        List<Ingredient> allIngredients = ingredientRepository.findAll();
        List<Order> orders = orderRepository.findAll();
        List<Recipe> recipes = recipeRepository.findAll();

        // Tính tốc độ tiêu thụ 7 ngày qua
        Date now = new Date();
        long sevenDaysAgo = now.getTime() - (7L * 24 * 60 * 60 * 1000);

        // Build recipe map: productId -> List<Recipe>
        Map<Integer, List<Recipe>> recipeMap = new HashMap<>();
        for (Recipe r : recipes) {
            if (r.getProduct() != null) {
                recipeMap.computeIfAbsent(r.getProduct().getId(), k -> new ArrayList<>()).add(r);
            }
        }

        // Tính nguyên liệu tiêu thụ trong 7 ngày
        Map<Long, Double> consumptionMap = new HashMap<>();
        List<Order> recentOrders = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 4)
                .filter(o -> o.getCreateDate() != null && o.getCreateDate().getTime() >= sevenDaysAgo)
                .collect(Collectors.toList());

        for (Order order : recentOrders) {
            if (order.getOrderDetails() == null) continue;
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getProduct() == null) continue;
                int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
                List<Recipe> productRecipes = recipeMap.getOrDefault(detail.getProduct().getId(), Collections.emptyList());
                for (Recipe r : productRecipes) {
                    if (r.getIngredient() == null) continue;
                    double consumed = (r.getAmountRequired() != null ? r.getAmountRequired() : 0) * qty;
                    consumptionMap.merge(r.getIngredient().getId(), consumed, Double::sum);
                }
            }
        }

        // Tạo danh sách đề xuất
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Ingredient ing : allIngredients) {
            double qty = ing.getQuantity() != null ? ing.getQuantity() : 0;
            double minStock = ing.getMinStock() != null ? ing.getMinStock() : 0;
            double dailyConsumption = consumptionMap.getOrDefault(ing.getId(), 0.0) / 7.0;

            // Tính số ngày còn dùng được
            double daysLeft = dailyConsumption > 0 ? qty / dailyConsumption : 999;

            // Chỉ đề xuất nếu: hết hàng, sắp hết, hoặc dùng trong vòng 5 ngày
            if (qty <= 0 || qty <= minStock || daysLeft <= 5) {
                Map<String, Object> suggestion = new LinkedHashMap<>();
                suggestion.put("ingredientId", ing.getId());
                suggestion.put("name", ing.getName());
                suggestion.put("unit", ing.getUnit());
                suggestion.put("image", ing.getImage());
                suggestion.put("currentStock", qty);
                suggestion.put("minStock", minStock);
                suggestion.put("dailyConsumption", Math.round(dailyConsumption * 100.0) / 100.0);
                suggestion.put("daysLeft", Math.round(daysLeft * 10.0) / 10.0);
                suggestion.put("unitPrice", ing.getUnitPrice());

                // Đề xuất mua: đủ dùng cho 7 ngày + bổ sung lên trên minStock
                double suggestedAmount = Math.max(minStock * 2, dailyConsumption * 7) - qty;
                if (suggestedAmount < 0) suggestedAmount = minStock;
                suggestion.put("suggestedAmount", Math.round(suggestedAmount * 10.0) / 10.0);
                suggestion.put("estimatedCost", java.math.BigDecimal.valueOf(suggestedAmount)
                        .multiply(ing.getUnitPrice() == null ? java.math.BigDecimal.ZERO : ing.getUnitPrice())
                        .setScale(0, java.math.RoundingMode.HALF_UP));

                // Mức độ khẩn cấp
                if (qty <= 0) {
                    suggestion.put("urgency", "critical");
                    suggestion.put("urgencyLabel", "Hết hàng");
                } else if (qty <= minStock) {
                    suggestion.put("urgency", "warning");
                    suggestion.put("urgencyLabel", "Sắp hết");
                } else {
                    suggestion.put("urgency", "info");
                    suggestion.put("urgencyLabel", "Sắp thiếu (" + Math.round(daysLeft) + " ngày)");
                }

                suggestions.add(suggestion);
            }
        }

        // Sắp xếp theo mức độ khẩn cấp
        suggestions.sort((a, b) -> {
            Map<String, Integer> priority = Map.of("critical", 0, "warning", 1, "info", 2);
            return Integer.compare(
                    priority.getOrDefault(a.get("urgency"), 3),
                    priority.getOrDefault(b.get("urgency"), 3));
        });

        // Thống kê tổng quan
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("suggestions", suggestions);
        result.put("totalItems", suggestions.size());
        result.put("totalEstimatedCost", suggestions.stream()
                .mapToDouble(s -> ((Number) s.get("estimatedCost")).doubleValue()).sum());
        result.put("criticalCount", suggestions.stream()
                .filter(s -> "critical".equals(s.get("urgency"))).count());
        result.put("warningCount", suggestions.stream()
                .filter(s -> "warning".equals(s.get("urgency"))).count());

        return ResponseEntity.ok(result);
    }

    /**
     * Duyệt đề xuất: Tự động tạo lô nhập kho cho nguyên liệu
     */
    @PostMapping("/approve/{ingredientId}")
    public ResponseEntity<?> approveSuggestion(
            @PathVariable Long ingredientId,
            @RequestParam Double quantity) {

        Optional<Ingredient> ingOpt = ingredientRepository.findById(ingredientId);
        if (ingOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
        }

        Ingredient ing = ingOpt.get();

        // Tạo lô nhập kho mới
        IngredientBatch batch = new IngredientBatch();
        batch.setIngredient(ing);
        batch.setQuantity(quantity);
        batch.setUnitPrice(ing.getUnitPrice());
        batch.setImportDate(new Date());

        // Tính hạn sử dụng
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, ing.getShelfLifeDays() != null ? ing.getShelfLifeDays() : 30);
        batch.setExpirationDate(cal.getTime());

        ingredientBatchRepository.save(batch);

        // Cập nhật tổng tồn kho
        double totalQuantity = ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ing)
                .stream().mapToDouble(IngredientBatch::getQuantity).sum();
        ing.setQuantity(totalQuantity);
        ingredientRepository.save(ing);

        // Ghi log
        activityLogService.log("CREATE", "PurchaseSuggestion", String.valueOf(ingredientId),
                "Duyệt đề xuất mua hàng: " + ing.getName() + " - SL: " + quantity + " " + ing.getUnit());

        return ResponseEntity.ok(Map.of(
                "message", "Đã duyệt và nhập kho " + quantity + " " + ing.getUnit() + " " + ing.getName(),
                "newStock", totalQuantity
        ));
    }
}
