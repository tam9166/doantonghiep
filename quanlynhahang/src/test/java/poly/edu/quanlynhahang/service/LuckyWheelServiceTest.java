package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.config.LuckyWheelProperties;
import poly.edu.quanlynhahang.dto.WheelSpinResponse;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.entity.WheelSpinHistory;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;
import poly.edu.quanlynhahang.repository.WheelSpinHistoryRepository;

class LuckyWheelServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final VoucherRepository voucherRepository = mock(VoucherRepository.class);
    private final WheelSpinHistoryRepository historyRepository = mock(WheelSpinHistoryRepository.class);
    private final PointsLedgerService pointsLedgerService = mock(PointsLedgerService.class);

    private LuckyWheelProperties properties;
    private Account account;

    @BeforeEach
    void setUp() {
        properties = new LuckyWheelProperties();
        account = new Account();
        account.setUsername("customer");
        account.setPoints(100);
        account.setMembershipTier("Đồng");
        when(accountRepository.findLockedByUsername("customer")).thenReturn(Optional.of(account));
    }

    @Test
    void rejectsSecondSpinOfTheDayAfterAcquiringAccountLock() {
        WheelSpinHistory existing = new WheelSpinHistory();
        when(historyRepository.findByAccountUsernameAndSpinDate(eq("customer"), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        LuckyWheelService service = serviceSelecting(0);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.spin("customer"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        InOrder order = inOrder(accountRepository, historyRepository);
        order.verify(accountRepository).findLockedByUsername("customer");
        order.verify(historyRepository).findByAccountUsernameAndSpinDate(eq("customer"), any(LocalDate.class));
        verify(orderRepository, never()).existsEligibleLuckyWheelOrder(
                any(), anyInt(), anyBoolean(), anyDouble(), any(Date.class), any(Date.class));
    }

    @Test
    void rejectsAccountWithoutEligiblePaidCompletedOrder() {
        when(historyRepository.findByAccountUsernameAndSpinDate(eq("customer"), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(orderRepository.existsEligibleLuckyWheelOrder(
                eq("customer"), eq(4), eq(true), eq(3_000_000.0), any(Date.class), any(Date.class)))
                .thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> serviceSelecting(0).spin("customer"));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(historyRepository, never()).save(any());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    void clampsConfiguredDiscountAndCreatesServerOwnedVoucher() {
        properties.setMaximumDiscountPercent(5);
        properties.setRewards(List.of(new LuckyWheelProperties.Reward("discount", 90, "GIẢM GIÁ")));
        when(historyRepository.findByAccountUsernameAndSpinDate(eq("customer"), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(orderRepository.existsEligibleLuckyWheelOrder(
                eq("customer"), eq(4), eq(true), eq(3_000_000.0), any(Date.class), any(Date.class)))
                .thenReturn(true);
        when(historyRepository.save(any())).thenAnswer(invocation -> {
            WheelSpinHistory history = invocation.getArgument(0);
            history.setId(10L);
            return history;
        });
        when(voucherRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WheelSpinResponse response = serviceSelecting(0).spin("customer");

        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(voucherCaptor.capture());
        Voucher voucher = voucherCaptor.getValue();
        assertEquals(5, voucher.getDiscountPercent());
        assertEquals("customer", voucher.getAccount().getUsername());
        assertEquals(5, response.value());
        assertEquals(voucher.getCode(), response.voucherCode());
        verify(pointsLedgerService, never()).credit(any(), any(), any(), anyInt(), any());
    }

    private LuckyWheelService serviceSelecting(int index) {
        return new LuckyWheelService(
                accountRepository,
                orderRepository,
                voucherRepository,
                historyRepository,
                pointsLedgerService,
                properties,
                ignored -> index);
    }
}
