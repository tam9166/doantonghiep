package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Product;

class CustomerInvoiceEmailServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void doesNotAttemptDeliveryWhenInvoiceEmailIsDisabled() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        CustomerInvoiceEmailService service = new CustomerInvoiceEmailService(provider, false, "");

        assertEquals(CustomerInvoiceEmailService.DeliveryStatus.NOT_CONFIGURED,
                service.sendPaidInvoiceNotice(new Order(), "customer@example.com"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsAnInvoiceSummaryWhenEmailIsEnabledAndConfigured() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        Order order = new Order();
        order.setId(42);
        order.setTotalAmount(new BigDecimal("125000"));
        Product product = new Product();
        product.setName("Pho bo");
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(2);
        detail.setPrice(new BigDecimal("60000"));
        order.setOrderDetails(List.of(detail));
        CustomerInvoiceEmailService service = new CustomerInvoiceEmailService(
                provider, true, "restaurant@example.com");

        assertEquals(CustomerInvoiceEmailService.DeliveryStatus.SENT,
                service.sendPaidInvoiceNotice(order, "customer@example.com"));
        var messageCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertTrue(messageCaptor.getValue().getText().contains("Hoa don #42"));
        assertTrue(messageCaptor.getValue().getText().contains("Pho bo x2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsFailedWhenSmtpDeliveryThrowsAnException() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
        CustomerInvoiceEmailService service = new CustomerInvoiceEmailService(provider, true, "");

        assertEquals(CustomerInvoiceEmailService.DeliveryStatus.FAILED,
                service.sendPaidInvoiceNotice(new Order(), "customer@example.com"));
    }
}
