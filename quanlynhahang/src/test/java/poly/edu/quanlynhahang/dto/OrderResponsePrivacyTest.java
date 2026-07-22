package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Product;

class OrderResponsePrivacyTest {
    @Test
    void orderResponseExcludesAccountCredentialsAndPaymentOperator() throws Exception {
        Account account = new Account();
        account.setUsername("customer-a");
        account.setFullname("Customer A");
        account.setEmail("customer@example.test");
        account.setPassword("secret-hash");
        account.setTokenVersion(9L);
        Product product = new Product();
        product.setId(1);
        product.setName("Pho");
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(1);
        Order order = new Order();
        order.setAccount(account);
        order.setPaymentConfirmedBy("cashier-a");
        order.setOrderDetails(List.of(detail));

        String json = new ObjectMapper().writeValueAsString(OrderResponse.from(order));

        assertFalse(json.contains("password"));
        assertFalse(json.contains("tokenVersion"));
        assertFalse(json.contains("customer@example.test"));
        assertFalse(json.contains("cashier-a"));
    }

    @Test
    void monetaryValuesAreExposedAsTwoDecimalBigDecimals() {
        OrderDetail detail = new OrderDetail();
        detail.setPrice(new BigDecimal("0.30"));
        detail.setTaxAmount(new BigDecimal("0.02"));

        Order order = new Order();
        order.setSubTotal(new BigDecimal("0.30"));
        order.setTaxAmount(new BigDecimal("0.02"));
        order.setTotalAmount(new BigDecimal("0.32"));
        order.setDeposit(new BigDecimal("0.15"));
        order.setOrderDetails(List.of(detail));

        OrderResponse response = OrderResponse.from(order);

        assertEquals(new BigDecimal("0.30"), response.subTotal());
        assertEquals(new BigDecimal("0.02"), response.taxAmount());
        assertEquals(new BigDecimal("0.32"), response.totalAmount());
        assertEquals(new BigDecimal("0.15"), response.deposit());
        assertEquals(new BigDecimal("0.30"), response.orderDetails().getFirst().price());
        assertEquals(new BigDecimal("0.02"), response.orderDetails().getFirst().taxAmount());
    }
}
