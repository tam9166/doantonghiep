package poly.edu.quanlynhahang.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicReservationResponseContractTest {

    @Test
    void publicLookupMasksContactAndOmitsPaymentAndInternalFields() throws Exception {
        PaymentQrResponse payment = new PaymentQrResponse();
        payment.setAccountNumber("0123456789");
        payment.setTransferContent("TT MV2026 SECRET");
        payment.setQrUrl("https://qr.example/internal");

        ReservationResponse source = new ReservationResponse();
        source.setId(99L);
        source.setReservationCode("MV-20260717-0001");
        source.setCustomerName("Nguyen Van A");
        source.setCustomerPhone("0912345678");
        source.setCustomerEmail("customer@example.com");
        source.setPaymentCapabilityToken("capability-secret");
        source.setPayments(List.of(payment));
        source.setManagerNote("Internal note");
        source.setHistory(List.of("Internal history"));

        PublicReservationResponse response = PublicReservationResponse.from(source);
        String json = new ObjectMapper().writeValueAsString(response);

        assertEquals("***5678", response.customerPhone());
        assertEquals("c***@example.com", response.customerEmail());
        assertTrue(json.contains("\"reservationCode\":\"MV-20260717-0001\""));
        assertFalse(json.contains("0912345678"));
        assertFalse(json.contains("customer@example.com"));
        assertFalse(json.contains("paymentCapabilityToken"));
        assertFalse(json.contains("payments"));
        assertFalse(json.contains("accountNumber"));
        assertFalse(json.contains("transferContent"));
        assertFalse(json.contains("qrUrl"));
        assertFalse(json.contains("managerNote"));
        assertFalse(json.contains("history"));
        assertFalse(json.contains("\"id\""));
    }
}
