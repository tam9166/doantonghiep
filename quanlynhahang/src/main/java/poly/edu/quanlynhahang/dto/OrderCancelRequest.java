package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Size;

public record OrderCancelRequest(@Size(max = 500) String reason) {
}
