package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import poly.edu.quanlynhahang.entity.ContactStatus;

public record ReservationContactUpdateRequest(
        @NotNull ContactStatus status,
        @Size(max = 1000) String note) {
}
