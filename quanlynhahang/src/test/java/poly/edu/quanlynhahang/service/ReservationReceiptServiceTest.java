package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ReservationReceiptServiceTest {
    @Test
    void receiptStatusUpdateStartsANewTransactionAfterPaymentCommit() throws Exception {
        var method = ReservationRepository.class.getMethod(
                "updateReceiptDelivery", Long.class, String.class, Date.class, String.class);
        var transactional = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        org.junit.jupiter.api.Assertions.assertNotNull(transactional);
        org.junit.jupiter.api.Assertions.assertEquals(
                org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
                transactional.propagation());
    }

    @Test
    void generatesPdfWithEmbeddedVietnameseFont() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        ReservationReceiptService service = new ReservationReceiptService(
                provider, mock(ReservationRepository.class), mock(PaymentIntentRepository.class),
                false, "", "Nhà hàng Mộc Vị", "Đà Nẵng", "0900000000", "contact@example.com");
        Reservation reservation = reservation();
        PaymentIntent payment = payment(reservation);

        byte[] pdf = service.generatePdf(reservation, payment);

        assertTrue(pdf.length > 10_000, "PDF should include the embedded Unicode font");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    private Reservation reservation() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationCode("MV-20260816-0001");
        reservation.setCustomerName("Nguyễn Văn An");
        reservation.setCustomerPhone("0900000000");
        reservation.setCustomerEmail("an@example.com");
        reservation.setReservationDate(LocalDate.of(2026, 8, 20));
        reservation.setArrivalTime(LocalTime.of(18, 30));
        reservation.setGuestCount(4);
        reservation.setTotalAmount(new BigDecimal("1000000"));
        reservation.setPaidAmount(new BigDecimal("500000"));
        reservation.setRemainingAmount(new BigDecimal("500000"));
        TableArea area = new TableArea();
        area.setNameVi("Sân vườn");
        reservation.setArea(area);
        return reservation;
    }

    private PaymentIntent payment(Reservation reservation) {
        PaymentIntent payment = new PaymentIntent();
        payment.setReservation(reservation);
        payment.setPaymentCode("PAY-001");
        payment.setBankTransactionCode("BANK-001");
        payment.setPaymentOption(PaymentOption.DEPOSIT_50);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(new Date());
        return payment;
    }
}
