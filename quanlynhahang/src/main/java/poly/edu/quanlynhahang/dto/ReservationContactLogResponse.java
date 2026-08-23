package poly.edu.quanlynhahang.dto;

import java.util.Date;

import poly.edu.quanlynhahang.entity.ContactStatus;

public record ReservationContactLogResponse(
        Long id,
        String staffUsername,
        String contactType,
        ContactStatus result,
        Date contactedAt,
        String note) {
}
