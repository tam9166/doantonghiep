package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;

class EncodingHealthServiceTest {
    @Test
    void countsOnlySuspiciousPaymentAccountHolderValues() {
        PaymentIntent clean = new PaymentIntent(); clean.setAccountHolder("Hoàng Nguyễn Minh Tâm");
        PaymentIntent broken = new PaymentIntent(); broken.setAccountHolder("HoÃ ng");
        PaymentIntentRepository repository = mock(PaymentIntentRepository.class);
        when(repository.findAll()).thenReturn(List.of(clean, broken));
        var result = new EncodingHealthService(repository).inspectPaymentAccountHolders();
        assertEquals(2, result.scanned());
        assertEquals(1, result.suspicious());
    }
}
