package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.service.CustomerInvoiceEmailService;
import poly.edu.quanlynhahang.dto.GuestBookingRequest;

class OrderWorkflowGuardTest {

    @Test
    void rejectsDishStatusOutsideTheDefinedWorkflow() {
        OrderController controller = new OrderController();
        OrderDetailRepository detailRepository = mock(OrderDetailRepository.class);
        ReflectionTestUtils.setField(controller, "orderDetailRepository", detailRepository);

        assertEquals(HttpStatus.BAD_REQUEST,
                controller.updateOrderDetailStatus(11, 99).getStatusCode());
    }

    @Test
    void waiterServingAnOrderUsesTheServedStatusInsteadOfCancellation() {
        WaiterController controller = new WaiterController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Order order = new Order();
        order.setId(15);
        when(orderRepository.findById(15)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);

        assertEquals(HttpStatus.OK, controller.confirmServed(15).getStatusCode());
        assertEquals(7, order.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void rejectsCompletingAnOrderWithoutAnyDishes() {
        AdminOrderController controller = new AdminOrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Order order = new Order();
        order.setId(21);
        order.setOrderDetails(List.of());
        when(orderRepository.findById(21)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.updateOrderStatus(21, 2));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void rejectsServingADishThatTheKitchenHasNotCompleted() {
        OrderController controller = new OrderController();
        OrderDetailRepository detailRepository = mock(OrderDetailRepository.class);
        OrderDetail detail = new OrderDetail();
        detail.setStatus(0);
        when(detailRepository.findById(44)).thenReturn(Optional.of(detail));
        ReflectionTestUtils.setField(controller, "orderDetailRepository", detailRepository);

        var response = controller.updateOrderDetailStatus(44, 2);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(detailRepository, never()).save(any(OrderDetail.class));
    }

    @Test
    void rejectsMakingATableEmptyWhileItHasAnUnpaidOrder() {
        RestaurantTableController controller = new RestaurantTableController();
        RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        RestaurantTable table = new RestaurantTable();
        table.setId(8);
        when(tableRepository.findById(8)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenUnpaidOrderForTable(8, table.getName())).thenReturn(true);
        ReflectionTestUtils.setField(controller, "tableRepository", tableRepository);
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);

        var response = controller.updateStatus(8, 0, null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(tableRepository, never()).save(any(RestaurantTable.class));
    }

    @Test
    void recordsAnInvoiceRequestOnlyForThePaidOrderOwner() {
        OrderController controller = new OrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Account account = new Account();
        account.setUsername("customer-owner");
        account.setEmail("owner@example.com");
        Order order = new Order();
        order.setAccount(account);
        order.setIsPaid(true);
        when(orderRepository.findLockedById(31)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        CustomerInvoiceEmailService invoiceEmailService = mock(CustomerInvoiceEmailService.class);
        when(invoiceEmailService.sendPaidInvoiceNotice(order, "owner@example.com"))
                .thenReturn(CustomerInvoiceEmailService.DeliveryStatus.NOT_CONFIGURED);
        ReflectionTestUtils.setField(controller, "customerInvoiceEmailService", invoiceEmailService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer-owner", null, List.of()));

        try {
            var response = controller.requestInvoice(31, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(order.getInvoiceRequested());
            assertEquals("owner@example.com", order.getInvoiceEmail());
            verify(orderRepository).save(order);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void rejectsInvoiceRequestForAnotherCustomerOrUnpaidOrder() {
        OrderController controller = new OrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Account account = new Account();
        account.setUsername("customer-owner");
        account.setEmail("owner@example.com");
        Order order = new Order();
        order.setAccount(account);
        order.setIsPaid(true);
        when(orderRepository.findLockedById(32)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("another-customer", null, List.of()));

        try {
            assertEquals(HttpStatus.FORBIDDEN, controller.requestInvoice(32, null).getStatusCode());
            verify(orderRepository, never()).save(any(Order.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void guestBookingKeepsVietnameseTextInTheStoredOrderAndResponse() {
        OrderController controller = new OrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        when(orderRepository.findAll()).thenReturn(List.of());
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);

        var response = controller.guestBooking(
                new GuestBookingRequest("Nguyễn Thị An", "0901234567", "Bàn B01", "19:00"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Đặt bàn thành công!", body.get("message"));

        var orderCaptor = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertTrue(orderCaptor.getValue().getAddress().contains("Khách: Nguyễn Thị An"));
        assertTrue(orderCaptor.getValue().getAddress().contains("SĐT: 0901234567"));
    }
}
