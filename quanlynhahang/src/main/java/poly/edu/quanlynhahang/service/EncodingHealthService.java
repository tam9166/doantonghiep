package poly.edu.quanlynhahang.service;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.dto.EncodingHealthResponse;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
@Service
public class EncodingHealthService {
    private static final Pattern MOJIBAKE = Pattern.compile("(?:Ã|Â|á»|â€)");
    private final PaymentIntentRepository paymentIntentRepository;
    public EncodingHealthService(PaymentIntentRepository paymentIntentRepository) { this.paymentIntentRepository = paymentIntentRepository; }
    public EncodingHealthResponse inspectPaymentAccountHolders() {
        var holders = paymentIntentRepository.findAll().stream().map(item -> item.getAccountHolder())
                .filter(value -> value != null && !value.isBlank()).toList();
        return new EncodingHealthResponse(holders.size(), holders.stream().filter(value -> MOJIBAKE.matcher(value).find()).count(), "payment_intent.account_holder");
    }
}
