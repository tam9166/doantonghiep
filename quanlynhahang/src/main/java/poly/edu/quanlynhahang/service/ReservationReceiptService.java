package poly.edu.quanlynhahang.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationTableAssignment;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReservationReceiptService {
    private static final Logger log = LoggerFactory.getLogger(ReservationReceiptService.class);
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ReservationRepository reservations;
    private final PaymentIntentRepository payments;
    private final boolean enabled;
    private final String from;
    private final String restaurantName;
    private final String restaurantAddress;
    private final String restaurantHotline;
    private final String restaurantEmail;

    public ReservationReceiptService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                     ReservationRepository reservations,
                                     PaymentIntentRepository payments,
                                     @Value("${app.receipt-email.enabled:false}") boolean enabled,
                                     @Value("${app.receipt-email.from:}") String from,
                                     @Value("${restaurant.info.name:Mộc Vị Restaurant}") String restaurantName,
                                     @Value("${restaurant.info.address:}") String restaurantAddress,
                                     @Value("${restaurant.info.hotline:}") String restaurantHotline,
                                     @Value("${restaurant.info.email:}") String restaurantEmail) {
        this.mailSenderProvider = mailSenderProvider;
        this.reservations = reservations;
        this.payments = payments;
        this.enabled = enabled;
        this.from = from;
        this.restaurantName = restaurantName;
        this.restaurantAddress = restaurantAddress;
        this.restaurantHotline = restaurantHotline;
        this.restaurantEmail = restaurantEmail;
    }

    /** Registers delivery only after the payment transaction has committed successfully. */
    public void scheduleAfterPaymentCommit(Reservation reservation, PaymentIntent payment) {
        Runnable action;
        try {
            Delivery delivery = prepare(reservation, payment);
            action = () -> deliver(delivery);
        } catch (Exception exception) {
            String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            String limited = error.substring(0, Math.min(500, error.length()));
            action = () -> reservations.updateReceiptDelivery(reservation.getId(), "FAILED", null, limited);
            log.warn("Receipt generation failed for reservation {}", reservation.getReservationCode(), exception);
        }
        Runnable finalAction = action;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { finalAction.run(); }
            });
        } else {
            finalAction.run();
        }
    }

    @Transactional(readOnly = true)
    public void resend(Long reservationId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy đặt bàn"));
        PaymentIntent paid = payments.findByReservationIdAndStatusOrderByCreatedAtDesc(reservationId, PaymentStatus.PAID)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "Đặt bàn chưa có giao dịch thành công"));
        scheduleAfterPaymentCommit(reservation, paid);
    }

    private Delivery prepare(Reservation reservation, PaymentIntent payment) {
        String recipient = reservation.getCustomerEmail();
        byte[] pdf = generatePdf(reservation, payment);
        String html = buildHtml(reservation, payment);
        return new Delivery(reservation.getId(), reservation.getReservationCode(), recipient, pdf, html);
    }

    private void deliver(Delivery delivery) {
        if (!enabled || delivery.recipient() == null || delivery.recipient().isBlank()) {
            reservations.updateReceiptDelivery(delivery.reservationId(), "NOT_CONFIGURED", null,
                    delivery.recipient() == null || delivery.recipient().isBlank() ? "Khách hàng chưa cung cấp email" : null);
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            reservations.updateReceiptDelivery(delivery.reservationId(), "NOT_CONFIGURED", null, "SMTP chưa được cấu hình");
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            if (from != null && !from.isBlank()) helper.setFrom(from.trim());
            helper.setTo(delivery.recipient());
            helper.setSubject("Xác nhận đặt bàn thành công – " + restaurantName + " – " + delivery.bookingCode());
            helper.setText(delivery.html(), true);
            helper.addAttachment("Bien-nhan-dat-coc-" + delivery.bookingCode() + ".pdf",
                    new ByteArrayResource(delivery.pdf()), "application/pdf");
            sender.send(message);
            reservations.updateReceiptDelivery(delivery.reservationId(), "SENT", new Date(), null);
        } catch (Exception exception) {
            String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            reservations.updateReceiptDelivery(delivery.reservationId(), "FAILED", null,
                    error.substring(0, Math.min(500, error.length())));
            log.warn("Receipt email delivery failed for reservation {}", delivery.bookingCode(), exception);
        }
    }

    byte[] generatePdf(Reservation reservation, PaymentIntent payment) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();
            BaseFont baseFont;
            try (var fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
                if (fontStream == null) throw new IllegalStateException("Không tìm thấy font PDF Noto Sans");
                baseFont = BaseFont.createFont("NotoSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                        true, fontStream.readAllBytes(), null);
            }
            Font title = new Font(baseFont, 16, Font.BOLD);
            Font heading = new Font(baseFont, 11, Font.BOLD);
            Font body = new Font(baseFont, 10);
            Paragraph brand = new Paragraph(restaurantName, heading);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);
            Paragraph receiptTitle = new Paragraph("BIÊN NHẬN ĐẶT CỌC / XÁC NHẬN ĐẶT BÀN", title);
            receiptTitle.setAlignment(Element.ALIGN_CENTER);
            receiptTitle.setSpacingAfter(18);
            document.add(receiptTitle);

            PdfPTable table = new PdfPTable(new float[]{1.2f, 2.4f});
            table.setWidthPercentage(100);
            addRow(table, "Mã booking", reservation.getReservationCode(), heading, body);
            addRow(table, "Mã giao dịch", value(payment.getBankTransactionCode(), payment.getPaymentCode()), heading, body);
            addRow(table, "Khách hàng", reservation.getCustomerName(), heading, body);
            addRow(table, "Điện thoại", reservation.getCustomerPhone(), heading, body);
            addRow(table, "Email", value(reservation.getCustomerEmail(), "Không cung cấp"), heading, body);
            addRow(table, "Ngày giờ đặt", reservation.getReservationDate() + " " + reservation.getArrivalTime(), heading, body);
            addRow(table, "Số khách", String.valueOf(reservation.getGuestCount()), heading, body);
            addRow(table, "Khu vực", reservation.getArea() == null ? "Chờ bố trí" : reservation.getArea().getNameVi(), heading, body);
            addRow(table, "Bàn", tableNames(reservation), heading, body);
            addRow(table, "Loại sự kiện", reservation.getEventType() == null ? "Không" : reservation.getEventType().name(), heading, body);
            addRow(table, "Tổng tiền dự kiến", money(reservation.getTotalAmount()), heading, body);
            addRow(table, "Tiền đã thanh toán", money(reservation.getPaidAmount()), heading, body);
            addRow(table, "Số tiền còn lại", money(reservation.getRemainingAmount()), heading, body);
            addRow(table, "Thời gian thanh toán", formatDate(payment.getPaidAt()), heading, body);
            addRow(table, "Phương thức", payment.getPaymentOption().name(), heading, body);
            addRow(table, "Trạng thái", payment.getStatus().name(), heading, body);
            addRow(table, "Ghi chú", value(reservation.getSpecialRequest(), "Không có"), heading, body);
            document.add(table);
            Paragraph footer = new Paragraph("\n" + restaurantAddress + "\nHotline: " + restaurantHotline
                    + " · Email: " + restaurantEmail, body);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể sinh PDF biên nhận", exception);
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font heading, Font body) {
        table.addCell(new Paragraph(label, heading));
        table.addCell(new Paragraph(value == null ? "" : value, body));
    }

    private String buildHtml(Reservation reservation, PaymentIntent payment) {
        return "<div style='font-family:Arial,sans-serif;color:#2b2118;line-height:1.6'>"
                + "<h2 style='color:#7c1025'>Xin chào " + escape(reservation.getCustomerName()) + ",</h2>"
                + "<p>Nhà hàng xác nhận đã nhận khoản thanh toán cho booking <b>" + escape(reservation.getReservationCode()) + "</b>.</p>"
                + "<p><b>Ngày giờ:</b> " + reservation.getReservationDate() + " " + reservation.getArrivalTime() + "<br>"
                + "<b>Số khách:</b> " + reservation.getGuestCount() + "<br>"
                + "<b>Khu vực:</b> " + escape(reservation.getArea() == null ? "Chờ bố trí" : reservation.getArea().getNameVi()) + "<br>"
                + "<b>Bố trí bàn:</b> " + escape(tableNames(reservation)) + "<br>"
                + "<b>Tiền đã thanh toán:</b> " + money(reservation.getPaidAmount()) + "</p>"
                + "<p>Quý khách vui lòng cung cấp mã booking khi đến nhà hàng. Hotline hỗ trợ: <b>" + escape(restaurantHotline) + "</b>.</p>"
                + "<p>Biên nhận PDF được đính kèm email này.</p></div>";
    }

    private String tableNames(Reservation reservation) {
        List<String> names = reservation.getTableAssignments() == null ? List.of() : reservation.getTableAssignments().stream()
                .map(ReservationTableAssignment::getTable).filter(java.util.Objects::nonNull)
                .map(table -> table.getName()).filter(java.util.Objects::nonNull).distinct().toList();
        if (names.isEmpty() && reservation.getTable() != null) names = List.of(reservation.getTable().getName());
        return names.isEmpty() ? "Chờ nhà hàng bố trí" : String.join(", ", names);
    }

    private String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(VIETNAMESE).format(amount == null ? BigDecimal.ZERO : amount);
    }

    private String formatDate(Date date) {
        return date == null ? "" : DATE_TIME.format(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private String value(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private record Delivery(Long reservationId, String bookingCode, String recipient, byte[] pdf, String html) {}
}
