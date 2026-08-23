package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Collections;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import poly.edu.quanlynhahang.dto.AssistantQueryRequest;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.repository.OrderRepository;

class RoleAwareAssistantServiceTest {
    private final StaffOperationsAssistantService operations = mock(StaffOperationsAssistantService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final RoleAwareAssistantService service = new RoleAwareAssistantService(operations, orderRepository);

    @Test
    void derivesKitchenRoleFromSecurityContextAndDoesNotAcceptRoleFromRequest() {
        when(operations.answer(anyString())).thenReturn("Tồn kho chính xác");
        var authentication = new UsernamePasswordAuthenticationToken("kitchen", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_KITCHEN")));

        var response = service.query(authentication, new AssistantQueryRequest("Nguyên liệu nào sắp hết?", "c-1", "vi"));

        assertEquals("KITCHEN", response.role());
        assertEquals("INVENTORY_SUMMARY", response.intent());
        assertEquals("Tồn kho chính xác", response.reply());
        assertEquals("DATABASE", response.source());
    }

    @Test
    void identifiesExpiringBatchIntentForAccentlessVietnamese() {
        when(operations.answer(anyString())).thenReturn("Lô sắp hết hạn");
        var authentication = new UsernamePasswordAuthenticationToken("manager", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));

        var response = service.query(authentication, new AssistantQueryRequest("Lo nao sap het han", null, "vi"));

        assertEquals("MANAGER", response.role());
        assertEquals("EXPIRING_INGREDIENT_BATCHES", response.intent());
    }

    @Test
    void cashierReceivesUnpaidInvoiceSummaryFromDatabaseAndCancelledOrdersAreExcluded() {
        Order unpaid = new Order();
        unpaid.setRemainingAmount(new java.math.BigDecimal("125000"));
        unpaid.setStatus(1);
        Order cancelled = new Order();
        cancelled.setRemainingAmount(new java.math.BigDecimal("90000"));
        cancelled.setStatus(3);
        when(orderRepository.findOutstandingOrders(3, java.math.BigDecimal.ZERO)).thenReturn(List.of(unpaid));
        var authentication = new UsernamePasswordAuthenticationToken("cashier", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));

        var response = service.query(authentication,
                new AssistantQueryRequest("Có bao nhiêu hóa đơn chưa thanh toán?", null, "vi"));

        assertEquals("UNPAID_INVOICE_SUMMARY", response.intent());
        assertEquals(1, response.data().get("invoiceCount"));
        assertEquals(new java.math.BigDecimal("125000"), response.data().get("remainingAmount"));
    }

    @Test
    void cashierCollectionSummaryOnlyCountsPaidNonCancelledOrdersCreatedToday() {
        Order paidToday = new Order();
        paidToday.setStatus(1);
        paidToday.setIsPaid(true);
        paidToday.setPaidAmount(new java.math.BigDecimal("350000"));
        paidToday.setCreateDate(new Date());

        Order paidTodayWithoutPaidAmount = new Order();
        paidTodayWithoutPaidAmount.setStatus(1);
        paidTodayWithoutPaidAmount.setIsPaid(true);
        paidTodayWithoutPaidAmount.setTotalAmount(new java.math.BigDecimal("120000"));
        paidTodayWithoutPaidAmount.setCreateDate(new Date());

        Order unpaidToday = new Order();
        unpaidToday.setStatus(1);
        unpaidToday.setIsPaid(false);
        unpaidToday.setPaidAmount(new java.math.BigDecimal("90000"));
        unpaidToday.setCreateDate(new Date());

        Order cancelledToday = new Order();
        cancelledToday.setStatus(3);
        cancelledToday.setIsPaid(true);
        cancelledToday.setPaidAmount(new java.math.BigDecimal("80000"));
        cancelledToday.setCreateDate(new Date());

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        Order paidYesterday = new Order();
        paidYesterday.setStatus(1);
        paidYesterday.setIsPaid(true);
        paidYesterday.setPaidAmount(new java.math.BigDecimal("70000"));
        paidYesterday.setCreateDate(yesterday.getTime());

        when(orderRepository.findPaidOrdersSince(org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.any(java.util.Date.class))).thenReturn(List.of(
                paidToday, paidTodayWithoutPaidAmount));
        var authentication = new UsernamePasswordAuthenticationToken("cashier", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));

        var response = service.query(authentication,
                new AssistantQueryRequest("Doanh thu hom nay la bao nhieu?", null, "vi"));

        assertEquals("TODAY_COLLECTION_SUMMARY", response.intent());
        assertEquals(2, response.data().get("paidInvoiceCount"));
        assertEquals(new java.math.BigDecimal("470000"), response.data().get("collectedAmount"));
    }
}
