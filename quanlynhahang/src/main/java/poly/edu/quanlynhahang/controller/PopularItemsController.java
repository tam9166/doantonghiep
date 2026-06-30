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

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin("*")
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

        List<Order> orders = orderRepository.findAll();
        Date now = new Date();

        // Lọc theo thời gian
        List<Order> filtered = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 4) // Chỉ đơn hoàn thành
                .filter(o -> {
                    if (o.getCreateDate() == null) return false;
                    long diff = now.getTime() - o.getCreateDate().getTime();
                    long days = diff / (1000 * 60 * 60 * 24);
                    if ("week".equals(period)) return days <= 7;
                    if ("month".equals(period)) return days <= 30;
                    if ("year".equals(period)) return days <= 365;
                    return true;
                })
                .collect(Collectors.toList());

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
                    m.put("totalRevenue", 0.0);
                    m.put("orderCount", 0);
                    return m;
                });

                Map<String, Object> data = productMap.get(productName);
                data.put("totalQuantity", (int) data.get("totalQuantity") + (detail.getQuantity() != null ? detail.getQuantity() : 0));
                data.put("totalRevenue", (double) data.get("totalRevenue") + (detail.getPrice() != null ? detail.getPrice() : 0.0));
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

        List<Order> orders = orderRepository.findAll();
        List<Recipe> recipes = recipeRepository.findAll();
        Date now = new Date();

        // Lọc đơn hàng theo thời gian
        List<Order> filtered = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 4)
                .filter(o -> {
                    if (o.getCreateDate() == null) return false;
                    long diff = now.getTime() - o.getCreateDate().getTime();
                    long days = diff / (1000 * 60 * 60 * 24);
                    if ("week".equals(period)) return days <= 7;
                    if ("month".equals(period)) return days <= 30;
                    if ("year".equals(period)) return days <= 365;
                    return true;
                })
                .collect(Collectors.toList());

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
                    double consumed = (r.getAmountRequired() != null ? r.getAmountRequired() : 0) * qty;

                    ingredientMap.computeIfAbsent(ingName, k -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", ingName);
                        m.put("unit", r.getIngredient().getUnit());
                        m.put("image", r.getIngredient().getImage());
                        m.put("totalConsumed", 0.0);
                        m.put("currentStock", r.getIngredient().getQuantity());
                        m.put("minStock", r.getIngredient().getMinStock());
                        return m;
                    });

                    Map<String, Object> data = ingredientMap.get(ingName);
                    data.put("totalConsumed", (double) data.get("totalConsumed") + consumed);
                }
            }
        }

        List<Map<String, Object>> result = ingredientMap.values().stream()
                .sorted((a, b) -> Double.compare((double) b.get("totalConsumed"), (double) a.get("totalConsumed")))
                .limit(20)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
