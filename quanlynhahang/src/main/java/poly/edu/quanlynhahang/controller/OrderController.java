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
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private RestaurantTableRepository tableRepository;

    @GetMapping("/history")
    public ResponseEntity<?> getMyOrders() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderRepository.findByAccountUsername(currentUsername));
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
        if (orderRequest.getAddress() != null && orderRequest.getAddress().contains("Lúc:")) {
            order.setStatus(5); // Chờ hẹn giờ
        } else {
            order.setStatus(1); // Chuyển thẳng bếp
        }
        
        Order savedOrder = orderRepository.save(order);

        for (OrderDetailRequest item : orderRequest.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                OrderDetail detail = new OrderDetail();
                detail.setOrder(savedOrder);
                detail.setProduct(product);
                detail.setQuantity(item.getQuantity());
                detail.setPrice(product.getPrice() * item.getQuantity());
                orderDetailRepository.save(detail);
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
        return ResponseEntity.ok("Đặt hàng thành công! Mã: " + uniqueOrderCode);
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