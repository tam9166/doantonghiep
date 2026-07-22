package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
