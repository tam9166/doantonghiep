package poly.edu.quanlynhahang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentWebhookResponse {
    private boolean success;
    private String code;
    private String message;
}
