package poly.edu.quanlynhahang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.entity.IngredientBatch;
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
    @Autowired private IngredientBatchRepository ingredientBatchRepository;
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

        // 🌟 TỰ ĐỘNG TRỪ KHO NGUYÊN LIỆU (FEFO - Trừ theo lô hết hạn trước)
        for (OrderDetailRequest item : orderRequest.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                List<Recipe> recipes = recipeRepository.findByProduct(product);
                for (Recipe recipe : recipes) {
                    var ingredient = recipe.getIngredient();
                    if (ingredient != null) {
                        double deduct = recipe.getAmountRequired() * item.getQuantity();
                        
                        // Lấy các lô hàng còn tồn kho, ưu tiên hết hạn trước
                        List<IngredientBatch> batches = ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ingredient);
                        
                        for (IngredientBatch batch : batches) {
                            if (deduct <= 0) break; // Đã trừ đủ
                            
                            if (batch.getQuantity() >= deduct) {
                                batch.setQuantity(batch.getQuantity() - deduct);
                                deduct = 0;
                            } else {
                                deduct -= batch.getQuantity();
                                batch.setQuantity(0.0);
                            }
                            ingredientBatchRepository.save(batch);
                        }
                        
                        // Cập nhật lại tổng tồn kho của Ingredient
                        double totalQuantity = ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ingredient)
                                .stream().mapToDouble(IngredientBatch::getQuantity).sum();
                        ingredient.setQuantity(totalQuantity);
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


    @PostMapping("/merge-tables")
    public ResponseEntity<?> mergeTables(@RequestBody java.util.Map<String, String> payload) {
        String fromTable = payload.get("fromTable");
        String toTable = payload.get("toTable");
        
        Optional<Order> sourceOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(fromTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
            
        Optional<Order> targetOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(toTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
            
        if (sourceOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn nguồn không có hóa đơn nào đang mở!");
        }
        if (targetOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn đích không có hóa đơn nào đang mở! Vui lòng order món cho bàn đích trước.");
        }
        
        Order sourceOrder = sourceOrderOpt.get();
        Order targetOrder = targetOrderOpt.get();
        
        // Chuyển toàn bộ món từ hóa đơn cũ sang hóa đơn mới
        if (sourceOrder.getOrderDetails() != null) {
            for (OrderDetail detail : sourceOrder.getOrderDetails()) {
                detail.setOrder(targetOrder);
                orderDetailRepository.save(detail);
            }
        }
        
        // Hủy hóa đơn cũ
        sourceOrder.setStatus(3);
        orderRepository.save(sourceOrder);
        
        // Giải phóng bàn cũ
        tableRepository.findAll().stream()
            .filter(t -> t.getName().equals(fromTable))
            .findFirst()
            .ifPresent(t -> {
                t.setIsOccupied(0);
                t.setReservedTime(null);
                tableRepository.save(t);
            });
            
        messagingTemplate.convertAndSend("/topic/orders", "TABLE_MERGED");
        
        return ResponseEntity.ok(java.util.Map.of("message", "Gộp bàn thành công!"));
    }

    @PostMapping("/split-table")
    public ResponseEntity<?> splitTable(@RequestBody java.util.Map<String, Object> payload) {
        String fromTable = (String) payload.get("fromTable");
        String toTable = (String) payload.get("toTable");
        List<?> rawIds = (List<?>) payload.get("detailIds");

        if (fromTable == null || toTable == null || rawIds == null || rawIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ!");
        }
        
        List<Integer> detailIds = rawIds.stream()
            .map(id -> Integer.parseInt(id.toString()))
            .collect(java.util.stream.Collectors.toList());

        Optional<Order> sourceOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(fromTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();

        if (sourceOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn nguồn không có hóa đơn nào đang mở!");
        }

        Order sourceOrder = sourceOrderOpt.get();
        
        // Cố gắng tìm Order của bàn đích
        Optional<Order> targetOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(toTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
        
        Order targetOrder;
        if (targetOrderOpt.isPresent()) {
            targetOrder = targetOrderOpt.get();
        } else {
            // Tạo Order mới cho bàn đích
            String uniqueOrderCode = generateUnique4DigitCode();
            targetOrder = new Order();
            targetOrder.setAccount(sourceOrder.getAccount()); // copy account
            targetOrder.setAddress("MÃ ĐƠN: #" + uniqueOrderCode + " | Bàn: " + toTable + " | [TẠI QUÁN]");
            targetOrder.setCreateDate(new Date());
            targetOrder.setStatus(sourceOrder.getStatus()); // copy status
            targetOrder = orderRepository.save(targetOrder);
            
            // Cập nhật trạng thái bàn đích
            final String fUniqueOrderCode = uniqueOrderCode;
            tableRepository.findAll().stream()
                .filter(t -> t.getName().equals(toTable))
                .findFirst()
                .ifPresent(t -> {
                    t.setIsOccupied(2); // Có khách
                    t.setReservedTime("Đơn: #" + fUniqueOrderCode);
                    tableRepository.save(t);
                });
        }
        
        final Order finalTargetOrder = targetOrder;
        // Di chuyển các order detail
        for (Integer detailId : detailIds) {
            orderDetailRepository.findById(detailId).ifPresent(detail -> {
                if (detail.getOrder().getId().equals(sourceOrder.getId())) {
                    detail.setOrder(finalTargetOrder);
                    orderDetailRepository.save(detail);
                }
            });
        }

        // Nếu bàn nguồn không còn OrderDetail nào, thì Hủy order đó và giải phóng bàn
        long remainingItems = orderDetailRepository.findAll().stream()
            .filter(d -> d.getOrder().getId().equals(sourceOrder.getId()))
            .count();
            
        if (remainingItems == 0) {
            sourceOrder.setStatus(3); // Hủy
            orderRepository.save(sourceOrder);
            tableRepository.findAll().stream()
                .filter(t -> t.getName().equals(fromTable))
                .findFirst()
                .ifPresent(t -> {
                    t.setIsOccupied(0);
                    t.setReservedTime(null);
                    tableRepository.save(t);
                });
        }

        messagingTemplate.convertAndSend("/topic/orders", "TABLE_SPLIT");
        
        return ResponseEntity.ok(java.util.Map.of("message", "Tách bàn thành công!"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/details/{detailId}/status")
    public ResponseEntity<?> updateOrderDetailStatus(@PathVariable Integer detailId, @org.springframework.web.bind.annotation.RequestParam Integer status) {
        return orderDetailRepository.findById(detailId).map(detail -> {
            detail.setStatus(status);
            orderDetailRepository.save(detail);

            Order order = detail.getOrder();
            if (order != null) {
                boolean allDone = true;
                boolean anyReady = false;
                if (order.getOrderDetails() != null) {
                    for (OrderDetail d : order.getOrderDetails()) {
                        if (d.getStatus() == null || d.getStatus() == 0) {
                            allDone = false;
                        }
                        if (d.getStatus() != null && d.getStatus() == 1) {
                            anyReady = true;
                        }
                    }
                }
                
                if (allDone && (order.getStatus() == 1 || order.getStatus() == 6)) {
                    order.setStatus(2); // Cả bàn đã xong, chờ bưng
                    orderRepository.save(order);
                } else if (anyReady && order.getStatus() == 1) {
                    order.setStatus(6); // Đang nấu (có món xong trước)
                    orderRepository.save(order);
                }

                messagingTemplate.convertAndSend("/topic/waiter", "DISH_STATUS_CHANGED");
                messagingTemplate.convertAndSend("/topic/kitchen", "DISH_STATUS_CHANGED");
            }
            return ResponseEntity.ok("Cập nhật món thành công!");
        }).orElse(ResponseEntity.badRequest().body("Lỗi không tìm thấy món!"));
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