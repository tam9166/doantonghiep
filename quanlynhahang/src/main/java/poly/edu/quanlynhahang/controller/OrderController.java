package poly.edu.quanlynhahang.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.dto.OrderResponse;
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
        return ResponseEntity.ok(orderRepository.findByAccountUsername(currentUsername).stream()
                .map(OrderResponse::from).toList());
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
        order.setAddress("BÃ n " + tableName + " - KhÃ¡ch: " + name + " - SÄT: " + phone + " - Háº¹n lÃºc: " + time);
        order.setCreateDate(new java.util.Date());
        order.setStatus(5); // 5 = Äáº·t bÃ n háº¹n trÆ°á»›c
        orderRepository.save(order);
        
        return ResponseEntity.ok(java.util.Map.of("message", "Äáº·t bÃ n thÃ nh cÃ´ng!", "orderCode", uniqueOrderCode));
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
    public ResponseEntity<?> addItemsToOrder(@PathVariable Integer id, @Valid @RequestBody OrderRequest orderRequest,
                                              @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(orderCheckoutService.addItems(id, orderRequest, idempotencyKey));
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
                return ResponseEntity.status(409).body("KhÃ´ng thá»ƒ gá»™p bÃ n Ä‘Ã£ thanh toÃ¡n vá»›i bÃ n chÆ°a thanh toÃ¡n!");
            }
        }
        
        Optional<Order> sourceOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(fromTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
            
        Optional<Order> targetOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(toTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
            
        if (sourceOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("BÃ n nguá»“n khÃ´ng cÃ³ hÃ³a Ä‘Æ¡n nÃ o Ä‘ang má»Ÿ!");
        }
        if (targetOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("BÃ n Ä‘Ã­ch khÃ´ng cÃ³ hÃ³a Ä‘Æ¡n nÃ o Ä‘ang má»Ÿ! Vui lÃ²ng order mÃ³n cho bÃ n Ä‘Ã­ch trÆ°á»›c.");
        }
        
        Order sourceOrder = sourceOrderOpt.get();
        Order targetOrder = targetOrderOpt.get();
        
        // Chuyá»ƒn toÃ n bá»™ mÃ³n tá»« hÃ³a Ä‘Æ¡n cÅ© sang hÃ³a Ä‘Æ¡n má»›i
        BigDecimal transferSub = BigDecimal.ZERO;
        BigDecimal transferTax = BigDecimal.ZERO;
        if (sourceOrder.getOrderDetails() != null) {
            for (OrderDetail detail : sourceOrder.getOrderDetails()) {
                detail.setOrder(targetOrder);
                orderDetailRepository.save(detail);
                transferSub = transferSub.add(money(detail.getPrice()));
                transferTax = transferTax.add(money(detail.getTaxAmount()));
            }
        }
        
        BigDecimal targetSubTotal = money(targetOrder.getSubTotal()).add(transferSub);
        BigDecimal targetTaxAmount = money(targetOrder.getTaxAmount()).add(transferTax);
        targetOrder.setSubTotal(targetSubTotal.doubleValue());
        targetOrder.setTaxAmount(targetTaxAmount.doubleValue());
        targetOrder.setTotalAmount(targetSubTotal.add(targetTaxAmount).doubleValue());
        orderRepository.save(targetOrder);
        
        // Há»§y hÃ³a Ä‘Æ¡n cÅ©
        sourceOrder.setStatus(3);
        orderRepository.save(sourceOrder);
        
        // ÄÃ¡nh dáº¥u bÃ n cÅ© lÃ  ÄÃ£ GhÃ©p thay vÃ¬ Trá»‘ng
        tableRepository.findAll().stream()
            .filter(t -> t.getName().equals(fromTable))
            .findFirst()
            .ifPresent(t -> {
                t.setIsOccupied(5);
                t.setReservedTime("[GHÃ‰P Vá»šI: " + toTable + "]");
                tableRepository.save(t);
            });
            
        messagingTemplate.convertAndSend("/topic/orders", "TABLE_MERGED");
        
        return ResponseEntity.ok(java.util.Map.of("message", "Gá»™p bÃ n thÃ nh cÃ´ng!"));
    }

    @PostMapping("/split-table")
    @Transactional
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> splitTable(@RequestBody java.util.Map<String, Object> payload) {
        String fromTable = (String) payload.get("fromTable");
        String toTable = (String) payload.get("toTable");
        List<?> rawIds = (List<?>) payload.get("detailIds");

        if (fromTable == null || toTable == null || rawIds == null || rawIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Dá»¯ liá»‡u khÃ´ng há»£p lá»‡!");
        }
        
        List<Integer> detailIds = rawIds.stream()
            .map(id -> Integer.parseInt(id.toString()))
            .collect(java.util.stream.Collectors.toList());

        Optional<Order> sourceOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(fromTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();

        if (sourceOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("BÃ n nguá»“n khÃ´ng cÃ³ hÃ³a Ä‘Æ¡n nÃ o Ä‘ang má»Ÿ!");
        }

        Order sourceOrder = sourceOrderOpt.get();
        
        // Cá»‘ gáº¯ng tÃ¬m Order cá»§a bÃ n Ä‘Ã­ch
        Optional<Order> targetOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(toTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
        
        Order targetOrder;
        if (targetOrderOpt.isPresent()) {
            targetOrder = targetOrderOpt.get();
        } else {
            // Táº¡o Order má»›i cho bÃ n Ä‘Ã­ch
            String uniqueOrderCode = generateUnique4DigitCode();
            targetOrder = new Order();
            targetOrder.setAccount(sourceOrder.getAccount()); // copy account
            targetOrder.setAddress("MÃƒ ÄÆ N: #" + uniqueOrderCode + " | BÃ n: " + toTable + " | [Táº I QUÃN]");
            targetOrder.setCreateDate(new Date());
            targetOrder.setStatus(sourceOrder.getStatus()); // copy status
            targetOrder = orderRepository.save(targetOrder);
            
            // Cáº­p nháº­t tráº¡ng thÃ¡i bÃ n Ä‘Ã­ch
            final String fUniqueOrderCode = uniqueOrderCode;
            tableRepository.findAll().stream()
                .filter(t -> t.getName().equals(toTable))
                .findFirst()
                .ifPresent(t -> {
                    t.setIsOccupied(2); // CÃ³ khÃ¡ch
                    t.setReservedTime("ÄÆ¡n: #" + fUniqueOrderCode);
                    tableRepository.save(t);
                });
        }
        
        final Order finalTargetOrder = targetOrder;
        BigDecimal moveSub = BigDecimal.ZERO;
        BigDecimal moveTax = BigDecimal.ZERO;
        // Di chuyá»ƒn cÃ¡c order detail
        for (Integer detailId : detailIds) {
            Optional<OrderDetail> detailOpt = orderDetailRepository.findById(detailId);
            if (detailOpt.isPresent()) {
                OrderDetail detail = detailOpt.get();
                if (detail.getOrder().getId().equals(sourceOrder.getId())) {
                    detail.setOrder(finalTargetOrder);
                    orderDetailRepository.save(detail);
                    moveSub = moveSub.add(money(detail.getPrice()));
                    moveTax = moveTax.add(money(detail.getTaxAmount()));
                }
            }
        }
        
        BigDecimal sourceSubTotal = money(sourceOrder.getSubTotal()).subtract(moveSub).max(BigDecimal.ZERO);
        BigDecimal sourceTaxAmount = money(sourceOrder.getTaxAmount()).subtract(moveTax).max(BigDecimal.ZERO);
        sourceOrder.setSubTotal(sourceSubTotal.doubleValue());
        sourceOrder.setTaxAmount(sourceTaxAmount.doubleValue());
        sourceOrder.setTotalAmount(sourceSubTotal.add(sourceTaxAmount).doubleValue());
        orderRepository.save(sourceOrder);
        
        BigDecimal finalTargetSubTotal = money(finalTargetOrder.getSubTotal()).add(moveSub);
        BigDecimal finalTargetTaxAmount = money(finalTargetOrder.getTaxAmount()).add(moveTax);
        finalTargetOrder.setSubTotal(finalTargetSubTotal.doubleValue());
        finalTargetOrder.setTaxAmount(finalTargetTaxAmount.doubleValue());
        finalTargetOrder.setTotalAmount(finalTargetSubTotal.add(finalTargetTaxAmount).doubleValue());
        orderRepository.save(finalTargetOrder);

        // Náº¿u bÃ n nguá»“n khÃ´ng cÃ²n OrderDetail nÃ o, thÃ¬ Há»§y order Ä‘Ã³ vÃ  giáº£i phÃ³ng bÃ n
        long remainingItems = orderDetailRepository.findAll().stream()
            .filter(d -> d.getOrder().getId().equals(sourceOrder.getId()))
            .count();
            
        if (remainingItems == 0) {
            sourceOrder.setStatus(3); // Há»§y
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
        
        return ResponseEntity.ok(java.util.Map.of("message", "TÃ¡ch bÃ n thÃ nh cÃ´ng!"));
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
                    order.setStatus(2); // Cáº£ bÃ n Ä‘Ã£ xong, chá» bÆ°ng
                    orderRepository.save(order);
                } else if (anyReady && order.getStatus() == 1) {
                    order.setStatus(6); // Äang náº¥u (cÃ³ mÃ³n xong trÆ°á»›c)
                    orderRepository.save(order);
                }

                messagingTemplate.convertAndSend("/topic/waiter", "DISH_STATUS_CHANGED");
                messagingTemplate.convertAndSend("/topic/kitchen", "DISH_STATUS_CHANGED");
            }
            return ResponseEntity.ok("Cáº­p nháº­t mÃ³n thÃ nh cÃ´ng!");
        }).orElse(ResponseEntity.badRequest().body("Lá»—i khÃ´ng tÃ¬m tháº¥y mÃ³n!"));
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

    private static BigDecimal money(Double value) {
        return BigDecimal.valueOf(value == null ? 0.0 : value).setScale(2, RoundingMode.HALF_UP);
    }
}
