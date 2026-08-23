package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/admin/popular-items")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
public class PopularItemsController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    /**
     * Top sản phẩm bán chạy nhất
     */
    @GetMapping("/products")
    public ResponseEntity<?> getTopProducts(
            @RequestParam(defaultValue = "week") String period) {

        List<Order> filtered = findCompletedOrders(period);

        // Tổng hợp theo sản phẩm
        Map<String, Map<String, Object>> productMap = new LinkedHashMap<>();
        for (Order order : filtered) {
            if (order.getOrderDetails() == null) continue;
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getProduct() == null) continue;
                String productName = detail.getProduct().getName();
                Integer productId = detail.getProduct().getId();

                productMap.computeIfAbsent(productName, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", productName);
                    m.put("productId", productId);
                    m.put("image", detail.getProduct().getImage());
                    m.put("totalQuantity", 0);
                    m.put("totalRevenue", BigDecimal.ZERO);
                    m.put("orderCount", 0);
                    return m;
                });

                Map<String, Object> data = productMap.get(productName);
                data.put("totalQuantity", (int) data.get("totalQuantity") + (detail.getQuantity() != null ? detail.getQuantity() : 0));
                data.put("totalRevenue", ((BigDecimal) data.get("totalRevenue"))
                        .add(detail.getPrice() == null ? BigDecimal.ZERO : detail.getPrice()));
                data.put("orderCount", (int) data.get("orderCount") + 1);
            }
        }

        // Sắp xếp theo số lượng bán giảm dần
        List<Map<String, Object>> result = productMap.values().stream()
                .sorted((a, b) -> Integer.compare((int) b.get("totalQuantity"), (int) a.get("totalQuantity")))
                .limit(20)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Top nguyên liệu tiêu thụ nhiều nhất
     */
    @GetMapping("/ingredients")
    public ResponseEntity<?> getTopIngredients(
            @RequestParam(defaultValue = "week") String period) {

        List<Order> filtered = findCompletedOrders(period);
        List<Integer> productIds = filtered.stream()
                .filter(order -> order.getOrderDetails() != null)
                .flatMap(order -> order.getOrderDetails().stream())
                .filter(detail -> detail.getProduct() != null)
                .map(detail -> detail.getProduct().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Recipe> recipes = productIds.isEmpty()
                ? List.of()
                : recipeRepository.findByProductIdsWithIngredient(productIds);

        // Build recipe map: productId -> List<Recipe>
        Map<Integer, List<Recipe>> recipeMap = new HashMap<>();
        for (Recipe r : recipes) {
            if (r.getProduct() != null) {
                recipeMap.computeIfAbsent(r.getProduct().getId(), k -> new ArrayList<>()).add(r);
            }
        }

        // Tính tổng nguyên liệu tiêu thụ
        Map<String, Map<String, Object>> ingredientMap = new LinkedHashMap<>();
        for (Order order : filtered) {
            if (order.getOrderDetails() == null) continue;
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getProduct() == null) continue;
                Integer productId = detail.getProduct().getId();
                int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;

                List<Recipe> productRecipes = recipeMap.getOrDefault(productId, Collections.emptyList());
                for (Recipe r : productRecipes) {
                    if (r.getIngredient() == null) continue;
                    String ingName = r.getIngredient().getName();
                    BigDecimal consumed = (r.getAmountRequired() == null ? BigDecimal.ZERO : r.getAmountRequired())
                            .multiply(BigDecimal.valueOf(qty));

                    ingredientMap.computeIfAbsent(ingName, k -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", ingName);
                        m.put("unit", r.getIngredient().getUnit());
                        m.put("image", r.getIngredient().getImage());
                        m.put("totalConsumed", BigDecimal.ZERO);
                        m.put("currentStock", r.getIngredient().getQuantity());
                        m.put("minStock", r.getIngredient().getMinStock());
                        return m;
                    });

                    Map<String, Object> data = ingredientMap.get(ingName);
                    data.put("totalConsumed", ((BigDecimal) data.get("totalConsumed")).add(consumed));
                }
            }
        }

        List<Map<String, Object>> result = ingredientMap.values().stream()
                .sorted((a, b) -> ((BigDecimal) b.get("totalConsumed"))
                        .compareTo((BigDecimal) a.get("totalConsumed")))
                .limit(20)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private List<Order> findCompletedOrders(String period) {
        Calendar calendar = Calendar.getInstance();
        switch (period == null ? "" : period.toLowerCase(Locale.ROOT)) {
            case "week" -> calendar.add(Calendar.DAY_OF_YEAR, -7);
            case "month" -> calendar.add(Calendar.DAY_OF_YEAR, -30);
            case "year" -> calendar.add(Calendar.DAY_OF_YEAR, -365);
            default -> calendar.setTimeInMillis(0L);
        }
        return orderRepository.findByStatusSinceWithDetails(4, calendar.getTime());
    }
}
