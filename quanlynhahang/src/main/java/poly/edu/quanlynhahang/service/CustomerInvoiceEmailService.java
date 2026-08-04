package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;

@Service
public class CustomerInvoiceEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerInvoiceEmailService.class);

    public enum DeliveryStatus {
        SENT,
        NOT_CONFIGURED,
        FAILED
    }

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;

    public CustomerInvoiceEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                       @Value("${app.invoice-email.enabled:false}") boolean enabled,
                                       @Value("${app.invoice-email.from:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from;
    }

    public DeliveryStatus sendPaidInvoiceNotice(Order order, String recipient) {
        if (!enabled) {
            return DeliveryStatus.NOT_CONFIGURED;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return DeliveryStatus.NOT_CONFIGURED;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                message.setFrom(from.trim());
            }
            message.setTo(recipient);
            message.setSubject("Hoa don thanh toan - Moc Vi Restaurant");
            message.setText(buildMessage(order));
            mailSender.send(message);
            return DeliveryStatus.SENT;
        } catch (RuntimeException exception) {
            LOGGER.warn("Invoice email delivery failed for order {}", order.getId(), exception);
            return DeliveryStatus.FAILED;
        }
    }

    private String buildMessage(Order order) {
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        StringBuilder message = new StringBuilder("Cam on Quy khach da thanh toan tai Moc Vi Restaurant.\n\n")
                .append("Hoa don #").append(order.getId()).append("\n")
                .append("Chi tiet mon an:\n");

        List<OrderDetail> details = order.getOrderDetails();
        if (details == null || details.isEmpty()) {
            message.append("- Khong co chi tiet mon an.\n");
        } else {
            for (OrderDetail detail : details) {
                String productName = detail.getProduct() == null || detail.getProduct().getName() == null
                        ? "Mon an" : detail.getProduct().getName();
                int quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();
                BigDecimal price = detail.getPrice() == null ? BigDecimal.ZERO : detail.getPrice();
                message.append("- ").append(productName).append(" x").append(quantity)
                        .append(": ").append(currency.format(price)).append("\n");
            }
        }

        return message.append("\nTong thanh toan: ").append(currency.format(total)).append("\n\n")
                .append("Vui long luu email nay de doi chieu.")
                .toString();
    }
}
