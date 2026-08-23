package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;

@Service
public class KitchenService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private OrderStateMachineService orderStateMachineService;

    // Hàm xử lý khi Bếp bấm "Bắt đầu nấu" hoặc "Xác nhận nấu"
    @Transactional
    public void cookOrder(Integer orderId) {
        // 1. Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        // 2. Quét từng món trong đơn để trừ nguyên liệu
        for (OrderDetail detail : order.getOrderDetails()) {
            
            // Tìm công thức nấu của món này
            //List<Recipe> recipes = recipeRepository.findByProductId(detail.getProduct().getId());
            // THAY BẰNG DÒNG NÀY (Bỏ chữ Id đi):
List<Recipe> recipes = recipeRepository.findByProduct(detail.getProduct());

            for (Recipe r : recipes) {
                Ingredient ing = r.getIngredient();
                
                // Số lượng nguyên liệu cần = (Nguyên liệu cho 1 món) x (Số lượng khách đặt)
                BigDecimal totalNeeded = r.getAmountRequired().multiply(BigDecimal.valueOf(detail.getQuantity()));
                
                // Kiểm tra xem kho còn đủ không
                if (ing.getQuantity().compareTo(totalNeeded) < 0) {
                    throw new RuntimeException("Hết nguyên liệu cho món: " + detail.getProduct().getName());
                }
                
                // Trừ đi số nguyên liệu đã dùng và lưu lại vào Database
                ing.setQuantity(ing.getQuantity().subtract(totalNeeded));
                ingredientRepository.save(ing);
            }
        }

        // 3. Cập nhật trạng thái đơn hàng thành 1 (Đang nấu)
        // Lưu ý: Tùy vào luồng của bạn, nếu nút này là "Nấu xong", bạn có thể set status = 2
        orderStateMachineService.transition(order, poly.edu.quanlynhahang.entity.OrderStatus.IN_PREPARATION);
        orderRepository.save(order);
    }
}
