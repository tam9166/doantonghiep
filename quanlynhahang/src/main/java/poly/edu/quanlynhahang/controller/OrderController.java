package poly.edu.quanlynhahang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.entity.Recipe;

import org.springframework.messaging.simp.SimpMessagingTemplate;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private RestaurantTableRepository tableRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private poly.edu.quanlynhahang.repository.VoucherRepository voucherRepository;

    @GetMapping("/history")
    public ResponseEntity<?> getMyOrders() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderRepository.findByAccountUsername(currentUsername));
    }

    @PostMapping("/guest-booking")
    public ResponseEntity<?> guestBooking(@RequestBody java.util.Map<String, String> payload) {
        String name = payload.get("customerName");
        String phone = payload.get("phone");
        String tableName = payload.get("tableName");
        String time = payload.get("scheduledTime");
        
        String uniqueOrderCode = generateUnique4DigitCode();
        Order order = new Order();
        order.setAccount(null); // Guest
        order.setAddress("Bàn " + tableName + " - Khách: " + name + " - SĐT: " + phone + " - Hẹn lúc: " + time);
        order.setCreateDate(new java.util.Date());
        order.setStatus(5); // 5 = Đặt bàn hẹn trước
        orderRepository.save(order);

        // Đánh dấu bàn đã được đặt cọc
        List<RestaurantTable> allTables = tableRepository.findAll();
        for (RestaurantTable t : allTables) {
            if (t.getName().equalsIgnoreCase(tableName)) {
                t.setIsOccupied(1); // 1 = Đã đặt/có khách
                t.setReservedTime("Cọc Bàn #" + uniqueOrderCode + " lúc " + time);
                tableRepository.save(t);
                break;
            }
        }
        
        return ResponseEntity.ok(java.util.Map.of("message", "Đặt bàn thành công!", "orderCode", uniqueOrderCode));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody OrderRequest orderRequest) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Account> accountOpt = accountRepository.findById(currentUsername);
        
        if (!accountOpt.isPresent()) return ResponseEntity.badRequest().body("Lỗi tài khoản!");

        String uniqueOrderCode = generateUnique4DigitCode();
        Order order = new Order();
        order.setAccount(accountOpt.get());
        
        String finalAddress = "MÃ ĐƠN: #" + uniqueOrderCode + " | " + orderRequest.getAddress();
        order.setAddress(finalAddress);
        order.setCreateDate(new Date());
        
        // 🌟 Đơn đặt bàn trước (có "Lúc:") → status=5 (Chờ hẹn giờ, chưa gửi bếp)
        // Đơn tại quán / giao hàng → status=1 (Chuyển thẳng bếp)
        if (orderRequest.getAddress() != null && orderRequest.getAddress().contains("Lúc:") && !orderRequest.getAddress().contains("[TẠI QUÁN]")) {
            order.setStatus(5); // Chờ hẹn giờ
        } else {
            order.setStatus(1); // Chuyển thẳng bếp
        }
        
        Order savedOrder = orderRepository.save(order);

        Account account = accountOpt.get();
        double discount = 0;

        // 🌟 Đơn TẠI QUÁN: Không áp dụng giảm giá lúc gọi món (thanh toán sau)
        boolean isDineIn = orderRequest.getAddress() != null && orderRequest.getAddress().contains("[TẠI QUÁN]");
        
        if (!isDineIn) {
            if ("Kim Cương".equals(account.getMembershipTier())) discount = 0.15;
            else if ("Vàng".equals(account.getMembershipTier())) discount = 0.10;
            else if ("Bạc".equals(account.getMembershipTier())) discount = 0.05;

            // Xử lý Voucher nếu có (chỉ cho đơn giao hàng / đặt bàn)
            if (orderRequest.getVoucherCode() != null && !orderRequest.getVoucherCode().isEmpty()) {
                Optional<Voucher> vOpt = voucherRepository.findByCode(orderRequest.getVoucherCode());
                if (vOpt.isPresent() && !vOpt.get().getIsUsed()) {
                    Voucher voucher = vOpt.get();
                    if (voucher.getAccount() == null || voucher.getAccount().getUsername().equals(account.getUsername())) {
                        discount += (double) voucher.getDiscountPercent() / 100.0;
                        voucher.setIsUsed(true);
                        voucherRepository.save(voucher);
                    }
                }
            }
        }
        
        // Đảm bảo giảm giá không vượt quá 100%
        if (discount > 1.0) discount = 1.0;

        final double finalDiscount = discount;

        for (OrderDetailRequest item : orderRequest.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                OrderDetail detail = new OrderDetail();
                detail.setOrder(savedOrder);
                detail.setProduct(product);
                detail.setQuantity(item.getQuantity());
                detail.setPrice(product.getPrice() * item.getQuantity() * (1 - finalDiscount));
                orderDetailRepository.save(detail);
            });
        }

        // 🌟 TỰ ĐỘNG TRỪ KHO NGUYÊN LIỆU
        for (OrderDetailRequest item : orderRequest.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                List<Recipe> recipes = recipeRepository.findByProduct(product);
                for (Recipe recipe : recipes) {
                    var ingredient = recipe.getIngredient();
                    if (ingredient != null && ingredient.getQuantity() != null) {
                        double deduct = recipe.getAmountRequired() * item.getQuantity();
                        ingredient.setQuantity(Math.max(0, ingredient.getQuantity() - deduct));
                        ingredientRepository.save(ingredient);
                    }
                }
            });
        }

        if (orderRequest.getAddress() != null) {
            List<RestaurantTable> allTables = tableRepository.findAll();
            for (RestaurantTable t : allTables) {
                if (orderRequest.getAddress().contains(t.getName())) {
                    int status = orderRequest.getAddress().contains("[TẠI QUÁN]") ? 2 : 1;
                    t.setIsOccupied(status);
                    t.setReservedTime("Đơn: #" + uniqueOrderCode);
                    tableRepository.save(t);
                    break;
                }
            }
        }
        if (order.getStatus() == 1) {
            messagingTemplate.convertAndSend("/topic/kitchen", "NEW_ORDER");
        }
        return ResponseEntity.ok(java.util.Map.of(
            "message", "Đặt hàng thành công! Mã: " + uniqueOrderCode,
            "orderId", savedOrder.getId()
        ));
    }

    private String generateUnique4DigitCode() {
        Random random = new Random();
        String code;
        boolean isDuplicate;
        do {
            int number = random.nextInt(9000) + 1000;
            code = String.valueOf(number);
            final String checkCode = "#" + code;
            isDuplicate = orderRepository.findAll().stream()
                .anyMatch(o -> o.getAddress() != null && o.getAddress().contains(checkCode));
        } while (isDuplicate);
        return code;
    }
}