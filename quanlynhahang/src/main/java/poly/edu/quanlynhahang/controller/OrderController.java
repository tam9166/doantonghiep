package poly.edu.quanlynhahang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.RestaurantTable;
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
import poly.edu.quanlynhahang.service.OrderCheckoutService;

import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    @Autowired private OrderCheckoutService orderCheckoutService;

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
        
        return ResponseEntity.ok(java.util.Map.of("message", "Đặt bàn thành công!", "orderCode", uniqueOrderCode));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody OrderRequest orderRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderCheckoutService.checkout(orderRequest, username));
    }

    @PutMapping("/{id}/add-items")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> addItemsToOrder(@PathVariable Integer id, @RequestBody OrderRequest orderRequest) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (!orderOpt.isPresent()) return ResponseEntity.badRequest().body("Không tìm thấy đơn hàng!");
        
        Order savedOrder = orderOpt.get();

        double[] totals = new double[2];
        totals[0] = savedOrder.getSubTotal() != null ? savedOrder.getSubTotal() : 0.0;
        totals[1] = savedOrder.getTaxAmount() != null ? savedOrder.getTaxAmount() : 0.0;

        for (OrderDetailRequest item : orderRequest.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                OrderDetail detail = new OrderDetail();
                detail.setOrder(savedOrder);
                detail.setProduct(product);
                detail.setQuantity(item.getQuantity());
                
                double subTotalLine = product.getPrice() * item.getQuantity();
                double itemTaxRate = product.getTaxRate() != null ? product.getTaxRate() : 8.0;
                double taxAmountLine = subTotalLine * itemTaxRate / 100.0;

                detail.setPrice(subTotalLine);
                detail.setTaxRate(itemTaxRate);
                detail.setTaxAmount(taxAmountLine);
                detail.setStatus(1);
                
                totals[0] += subTotalLine;
                totals[1] += taxAmountLine;

                orderDetailRepository.save(detail);
            });
        }
        
        savedOrder.setSubTotal(totals[0]);
        savedOrder.setTaxAmount(totals[1]);
        savedOrder.setTotalAmount(totals[0] + totals[1]);

        // TỰ ĐỘNG TRỪ KHO NGUYÊN LIỆU (FEFO)
        for (OrderDetailRequest item : orderRequest.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                List<Recipe> recipes = recipeRepository.findByProduct(product);
                for (Recipe recipe : recipes) {
                    var ingredient = recipe.getIngredient();
                    if (ingredient != null) {
                        double deduct = recipe.getAmountRequired() * item.getQuantity();
                        List<IngredientBatch> batches = ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ingredient);
                        for (IngredientBatch batch : batches) {
                            if (deduct <= 0) break;
                            if (batch.getQuantity() >= deduct) {
                                batch.setQuantity(batch.getQuantity() - deduct);
                                deduct = 0;
                            } else {
                                deduct -= batch.getQuantity();
                                batch.setQuantity(0.0);
                            }
                            ingredientBatchRepository.save(batch);
                        }
                        double totalQuantity = ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ingredient)
                                .stream().mapToDouble(IngredientBatch::getQuantity).sum();
                        ingredient.setQuantity(totalQuantity);
                        ingredientRepository.save(ingredient);
                        
                        List<Recipe> relatedRecipes = recipeRepository.findByIngredient(ingredient);
                        for (Recipe r : relatedRecipes) {
                            if (r.getProduct() != null && r.getProduct().getAvailable()) {
                                if (totalQuantity < r.getAmountRequired()) {
                                    r.getProduct().setAvailable(false);
                                    productRepository.save(r.getProduct());
                                }
                            }
                        }
                    }
                }
            });
        }
        
        savedOrder.setStatus(1);
        orderRepository.save(savedOrder);
        
        org.springframework.messaging.simp.SimpMessagingTemplate mt = messagingTemplate;
        if(mt != null) mt.convertAndSend("/topic/kitchen", "NEW_ORDER");
        
        return ResponseEntity.ok(java.util.Map.of("message", "Gọi thêm món thành công!"));
    }

    @PostMapping("/merge-tables")
    @Transactional
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> mergeTables(@RequestBody java.util.Map<String, String> payload) {
        String fromTable = payload.get("fromTable");
        String toTable = payload.get("toTable");

        Optional<Order> anySourceOrder = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(fromTable) && o.getStatus() != 3)
            .findFirst();

        Optional<Order> anyTargetOrder = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(toTable) && o.getStatus() != 3)
            .findFirst();

        if (anySourceOrder.isPresent() && anyTargetOrder.isPresent()) {
            boolean sourcePaid = Boolean.TRUE.equals(anySourceOrder.get().getIsPaid());
            boolean targetPaid = Boolean.TRUE.equals(anyTargetOrder.get().getIsPaid());
            if (sourcePaid != targetPaid) {
                return ResponseEntity.status(409).body("Không thể gộp bàn đã thanh toán với bàn chưa thanh toán!");
            }
        }
        
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
        double transferSub = 0.0;
        double transferTax = 0.0;
        if (sourceOrder.getOrderDetails() != null) {
            for (OrderDetail detail : sourceOrder.getOrderDetails()) {
                detail.setOrder(targetOrder);
                orderDetailRepository.save(detail);
                transferSub += detail.getPrice() != null ? detail.getPrice() : 0.0;
                transferTax += detail.getTaxAmount() != null ? detail.getTaxAmount() : 0.0;
            }
        }
        
        targetOrder.setSubTotal((targetOrder.getSubTotal() != null ? targetOrder.getSubTotal() : 0.0) + transferSub);
        targetOrder.setTaxAmount((targetOrder.getTaxAmount() != null ? targetOrder.getTaxAmount() : 0.0) + transferTax);
        targetOrder.setTotalAmount(targetOrder.getSubTotal() + targetOrder.getTaxAmount());
        orderRepository.save(targetOrder);
        
        // Hủy hóa đơn cũ
        sourceOrder.setStatus(3);
        orderRepository.save(sourceOrder);
        
        // Đánh dấu bàn cũ là Đã Ghép thay vì Trống
        tableRepository.findAll().stream()
            .filter(t -> t.getName().equals(fromTable))
            .findFirst()
            .ifPresent(t -> {
                t.setIsOccupied(5);
                t.setReservedTime("[GHÉP VỚI: " + toTable + "]");
                tableRepository.save(t);
            });
            
        messagingTemplate.convertAndSend("/topic/orders", "TABLE_MERGED");
        
        return ResponseEntity.ok(java.util.Map.of("message", "Gộp bàn thành công!"));
    }

    @PostMapping("/split-table")
    @Transactional
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
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
        double moveSub = 0.0;
        double moveTax = 0.0;
        // Di chuyển các order detail
        for (Integer detailId : detailIds) {
            Optional<OrderDetail> detailOpt = orderDetailRepository.findById(detailId);
            if (detailOpt.isPresent()) {
                OrderDetail detail = detailOpt.get();
                if (detail.getOrder().getId().equals(sourceOrder.getId())) {
                    detail.setOrder(finalTargetOrder);
                    orderDetailRepository.save(detail);
                    moveSub += detail.getPrice() != null ? detail.getPrice() : 0.0;
                    moveTax += detail.getTaxAmount() != null ? detail.getTaxAmount() : 0.0;
                }
            }
        }
        
        sourceOrder.setSubTotal(Math.max(0, (sourceOrder.getSubTotal() != null ? sourceOrder.getSubTotal() : 0.0) - moveSub));
        sourceOrder.setTaxAmount(Math.max(0, (sourceOrder.getTaxAmount() != null ? sourceOrder.getTaxAmount() : 0.0) - moveTax));
        sourceOrder.setTotalAmount(sourceOrder.getSubTotal() + sourceOrder.getTaxAmount());
        orderRepository.save(sourceOrder);
        
        finalTargetOrder.setSubTotal((finalTargetOrder.getSubTotal() != null ? finalTargetOrder.getSubTotal() : 0.0) + moveSub);
        finalTargetOrder.setTaxAmount((finalTargetOrder.getTaxAmount() != null ? finalTargetOrder.getTaxAmount() : 0.0) + moveTax);
        finalTargetOrder.setTotalAmount(finalTargetOrder.getSubTotal() + finalTargetOrder.getTaxAmount());
        orderRepository.save(finalTargetOrder);

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
    @PreAuthorize("hasAnyRole('KITCHEN', 'MANAGER', 'ADMIN')")
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
