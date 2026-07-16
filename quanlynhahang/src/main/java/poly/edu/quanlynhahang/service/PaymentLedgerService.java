package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentDirection;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PaymentTransaction;
import poly.edu.quanlynhahang.entity.PaymentTransactionStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

@Service
public class PaymentLedgerService {

    private static final Set<ReservationStatus> PAYMENT_DRIVEN_STATES = EnumSet.of(
            ReservationStatus.PENDING,
            ReservationStatus.DEPOSIT_REQUIRED,
            ReservationStatus.DEPOSIT_PENDING,
            ReservationStatus.DEPOSIT_PAID,
            ReservationStatus.FULLY_PAID);

    private final PaymentIntentRepository intentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationStateMachine stateMachine;
    private final ReservationRealtimeService realtimeService;
    private final ActivityLogService activityLogService;
    private final OrderPaymentService orderPaymentService;

    public PaymentLedgerService(PaymentIntentRepository intentRepository,
                                PaymentTransactionRepository transactionRepository,
                                ReservationRepository reservationRepository,
                                ReservationStateMachine stateMachine,
                                ReservationRealtimeService realtimeService,
                                ActivityLogService activityLogService,
                                OrderPaymentService orderPaymentService) {
        this.intentRepository = intentRepository;
        this.transactionRepository = transactionRepository;
        this.reservationRepository = reservationRepository;
        this.stateMachine = stateMachine;
        this.realtimeService = realtimeService;
        this.activityLogService = activityLogService;
        this.orderPaymentService = orderPaymentService;
    }

    @Transactional
    public PaymentLedgerResult recordCredit(String provider,
                                            String providerTransactionId,
                                            String paymentCode,
                                            String transferContent,
                                            BigDecimal amount,
                                            String receiverAccount,
                                            String payloadHash) {
        PaymentTransaction duplicate = transactionRepository
                .findByProviderTransactionId(providerTransactionId)
                .orElse(null);
        if (duplicate != null) {
            return new PaymentLedgerResult("PAYMENT_ALREADY_PROCESSED", paymentCode);
        }

        PaymentIntent intent = intentRepository.findLockedByPaymentCode(paymentCode)
                .orElse(null);
        if (intent == null) {
            PaymentTransaction unmatched = transaction(
                    provider, providerTransactionId, amount, transferContent, payloadHash);
            unmatched.setStatus(PaymentTransactionStatus.MANUAL_REVIEW);
            transactionRepository.save(unmatched);
            return new PaymentLedgerResult("PAYMENT_MANUAL_REVIEW", paymentCode);
        }

        PaymentTransaction transaction = transaction(
                provider, providerTransactionId, amount, transferContent, payloadHash);
        transaction.setPaymentIntentId(intent.getId());
        transaction.setAggregateType(intent.getAggregateType());
        transaction.setAggregateId(intent.getAggregateId());

        if (!matchesSnapshot(intent, transferContent, receiverAccount)
                || PaymentStatus.REPLACED.equals(intent.getStatus())
                || PaymentStatus.EXPIRED.equals(intent.getStatus())
                || PaymentStatus.CANCELLED.equals(intent.getStatus())) {
            transaction.setStatus(PaymentTransactionStatus.MANUAL_REVIEW);
            transactionRepository.save(transaction);
            activityLogService.log(
                    "PAYMENT_MANUAL_REVIEW",
                    "PaymentIntent",
                    String.valueOf(intent.getId()),
                    "Giao dịch cần đối soát thủ công");
            return new PaymentLedgerResult("PAYMENT_MANUAL_REVIEW", paymentCode);
        }

        transaction.setStatus(PaymentTransactionStatus.SUCCESS);
        transactionRepository.saveAndFlush(transaction);

        BigDecimal intentPaid = netAmount(transactionRepository
                .findByPaymentIntentIdAndStatus(intent.getId(), PaymentTransactionStatus.SUCCESS));
        intent.setPaidAmount(intentPaid.max(BigDecimal.ZERO));
        intent.setRemainingAmount(intent.getAmount().subtract(intentPaid).max(BigDecimal.ZERO));
        intent.setStatus(paymentStatus(intentPaid, intent.getAmount()));
        intentRepository.save(intent);

        BigDecimal aggregatePaid = netAmount(transactionRepository
                .findByAggregateTypeAndAggregateIdAndStatus(
                        intent.getAggregateType(), intent.getAggregateId(), PaymentTransactionStatus.SUCCESS));
        if ("ORDER".equals(intent.getAggregateType())) {
            PaymentStatus orderPaymentStatus = paymentStatus(aggregatePaid, intent.getAmount());
            orderPaymentService.applyLedgerPayment(intent.getAggregateId().intValue(), aggregatePaid, orderPaymentStatus);
            activityLogService.log(
                    "PAYMENT_CREDIT",
                    "Order",
                    String.valueOf(intent.getAggregateId()),
                    "Đã ghi nhận giao dịch " + providerTransactionId);
            return new PaymentLedgerResult("PAYMENT_" + orderPaymentStatus.name(), paymentCode);
        }

        Reservation reservation = intent.getReservation();
        if (reservation == null) {
            throw new IllegalStateException("PaymentIntent RESERVATION không gắn với đặt bàn");
        }
        reservation.setPaidAmount(aggregatePaid.max(BigDecimal.ZERO));
        reservation.setRemainingAmount(reservation.getTotalAmount().subtract(aggregatePaid).max(BigDecimal.ZERO));
        reservation.setPaymentStatus(paymentStatus(aggregatePaid, reservation.getTotalAmount()));
        updateReservationMilestone(reservation, aggregatePaid);
        reservationRepository.save(reservation);

        activityLogService.log(
                "PAYMENT_CREDIT",
                "PaymentIntent",
                String.valueOf(intent.getId()),
                "Đã ghi nhận giao dịch " + providerTransactionId);
        realtimeService.publish(
                "PAYMENT_UPDATED",
                reservation.getReservationCode(),
                reservation.getReservationStatus(),
                reservation.getReservationStatus(),
                reservation.getPaymentStatus().name(),
                null);
        return new PaymentLedgerResult("PAYMENT_" + reservation.getPaymentStatus().name(), paymentCode);
    }

    private PaymentTransaction transaction(String provider,
                                           String providerTransactionId,
                                           BigDecimal amount,
                                           String transferContent,
                                           String payloadHash) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setProvider(provider);
        transaction.setProviderTransactionId(providerTransactionId);
        transaction.setAmount(amount);
        transaction.setDirection(PaymentDirection.CREDIT);
        transaction.setRawReference(limit(transferContent, 200));
        transaction.setPayloadHash(payloadHash);
        return transaction;
    }

    private boolean matchesSnapshot(PaymentIntent intent, String transferContent, String receiverAccount) {
        return intent.getAccountNumber() != null
                && intent.getAccountNumber().equals(receiverAccount)
                && intent.getTransferContent() != null
                && intent.getTransferContent().equalsIgnoreCase(
                        transferContent == null ? "" : transferContent.trim());
    }

    private BigDecimal netAmount(List<PaymentTransaction> transactions) {
        return transactions.stream().reduce(
                BigDecimal.ZERO,
                (total, transaction) -> PaymentDirection.REFUND.equals(transaction.getDirection())
                        ? total.subtract(transaction.getAmount())
                        : total.add(transaction.getAmount()),
                BigDecimal::add);
    }

    private PaymentStatus paymentStatus(BigDecimal paidAmount, BigDecimal expectedAmount) {
        int comparison = paidAmount.compareTo(expectedAmount);
        if (paidAmount.signum() <= 0) return PaymentStatus.UNPAID;
        if (comparison < 0) return PaymentStatus.PARTIALLY_PAID;
        if (comparison == 0) return PaymentStatus.PAID;
        return PaymentStatus.OVERPAID;
    }

    private void updateReservationMilestone(Reservation reservation, BigDecimal aggregatePaid) {
        if (reservation.getDepositAmount() != null
                && reservation.getDepositAmount().signum() > 0
                && aggregatePaid.compareTo(reservation.getDepositAmount()) >= 0) {
            reservation.setDepositStatus(DepositStatus.PAID);
        }
        if (!PAYMENT_DRIVEN_STATES.contains(reservation.getReservationStatus())) {
            return;
        }

        ReservationStatus next = null;
        if (aggregatePaid.compareTo(reservation.getTotalAmount()) >= 0) {
            next = ReservationStatus.FULLY_PAID;
        } else if (reservation.getDepositAmount() != null
                && aggregatePaid.compareTo(reservation.getDepositAmount()) >= 0) {
            next = ReservationStatus.DEPOSIT_PAID;
        }
        if (next != null && next != reservation.getReservationStatus()) {
            stateMachine.assertCanTransition(reservation.getReservationStatus(), next);
            reservation.setReservationStatus(next);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
