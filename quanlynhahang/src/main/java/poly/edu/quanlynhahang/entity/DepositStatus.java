package poly.edu.quanlynhahang.entity;

public enum DepositStatus {
    NOT_REQUIRED,
    PENDING,
    PAID,
    /** A paid deposit retained by the restaurant because the guest did not arrive. */
    FORFEITED,
    REFUNDED
}
