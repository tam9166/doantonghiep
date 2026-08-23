package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.CancellationRequestStatus;

public record CancellationRequestReceipt(
        String requestCode,
        CancellationRequestStatus status,
        String message) {
}
