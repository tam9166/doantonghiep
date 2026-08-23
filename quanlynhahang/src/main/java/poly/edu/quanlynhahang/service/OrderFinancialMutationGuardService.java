package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.repository.OrderVoucherUsageRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;
import poly.edu.quanlynhahang.repository.InventoryReservationRepository;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;

/** Prevents table composition operations from corrupting payment allocation. */
@Service
public class OrderFinancialMutationGuardService {
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final OrderVoucherUsageRepository voucherUsageRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public OrderFinancialMutationGuardService(PaymentIntentRepository paymentIntentRepository,
                                              PaymentTransactionRepository paymentTransactionRepository,
                                              RefundTransactionRepository refundTransactionRepository,
                                              OrderVoucherUsageRepository voucherUsageRepository,
                                              InventoryReservationRepository inventoryReservationRepository) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.refundTransactionRepository = refundTransactionRepository;
        this.voucherUsageRepository = voucherUsageRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
    }

    public void requireSafeForTableComposition(Order... orders) {
        for (Order order : orders) {
            if (order == null || order.getId() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(order.getIsPaid())
                    || positive(order.getPaidAmount())
                    || financialStatus(order.getPaymentStatus())
                    || paymentIntentRepository.existsByOrderId(order.getId())
                    || paymentTransactionRepository.existsByAggregateTypeAndAggregateId(
                            "ORDER", order.getId().longValue())
                    || refundTransactionRepository.existsByOrderId(order.getId())
                    || voucherUsageRepository.existsByOrderId(order.getId())
                    || inventoryReservationRepository.existsByOrderIdAndStatus(
                            order.getId(), InventoryReservationStatus.RESERVED)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể gộp/tách bàn khi đơn đã có thanh toán, hoàn tiền, voucher hoặc giữ tồn kho");
            }
        }
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private boolean financialStatus(PaymentStatus status) {
        return status != null && status != PaymentStatus.UNPAID && status != PaymentStatus.CANCELLED;
    }
}
