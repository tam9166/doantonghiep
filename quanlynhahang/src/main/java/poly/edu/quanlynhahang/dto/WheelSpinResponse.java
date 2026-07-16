package poly.edu.quanlynhahang.dto;

public record WheelSpinResponse(
        String type,
        int value,
        String label,
        String voucherCode,
        Integer currentPoints,
        String membershipTier) {
}
