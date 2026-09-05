package poly.edu.quanlynhahang.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Product;

@SpringBootTest
@Transactional
class OperationalDishQueueIntegrationTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void pendingCookDetailIsVisibleToBothWaiterAndKitchenRegardlessOfParentStatus() {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        Order order = new Order();
        order.setOrderCode("QUEUE-" + UUID.randomUUID().toString().substring(0, 12));
        order.setOrderType(OrderType.TAKEAWAY);
        order.setPaymentOption(OrderPaymentOption.PAY_AT_RESTAURANT);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setStatus(OrderStatus.COMPLETED.code());
        order.setCreateDate(new Date());
        order.setSubTotal(new BigDecimal("100000"));
        order.setTotalAmount(new BigDecimal("108000"));
        order.setRemainingAmount(new BigDecimal("108000"));
        order = orderRepository.saveAndFlush(order);

        OrderDetail detail = new OrderDetail();
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setQuantity(1);
        detail.setPrice(new BigDecimal("100000"));
        detail.setTaxAmount(new BigDecimal("8000"));
        detail.setStatus(0);
        detail.setQueuedAt(new Date());
        orderDetailRepository.saveAndFlush(detail);

        Date startOfDay = Date.from(LocalDate.now(BUSINESS_ZONE).atStartOfDay(BUSINESS_ZONE).toInstant());
        Integer orderId = order.getId();
        assertTrue(orderRepository.findWaiterOperationalOrdersWithDetails(
                        OrderStatus.CANCELLED.code(), PaymentStatus.REFUNDED, List.of(0, 1)).stream()
                .anyMatch(candidate -> orderId.equals(candidate.getId())));
        assertTrue(orderRepository.findKitchenBoardOrdersWithDetails(
                        OrderStatus.CANCELLED.code(), PaymentStatus.REFUNDED,
                        List.of(0), List.of(1, 2), startOfDay).stream()
                .anyMatch(candidate -> orderId.equals(candidate.getId())));
    }
}
