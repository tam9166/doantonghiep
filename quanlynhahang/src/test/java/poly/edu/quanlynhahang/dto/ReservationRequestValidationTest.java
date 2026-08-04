package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ReservationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsIncompleteOrOutOfRangeReservationPayload() {
        ReservationRequest request = new ReservationRequest();
        request.setCustomerName("");
        request.setCustomerPhone("not-a-phone");
        request.setReservationDate("03/08/2026");
        request.setArrivalTime("25:99");
        request.setExpectedDurationMinutes(5);
        request.setGuestCount(0);
        request.setTableId(0);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsAValidReservationAndBoundedPreorderItem() {
        ReservationRequest request = new ReservationRequest();
        request.setCustomerName("Nguyễn Thị An");
        request.setCustomerPhone("0901234567");
        request.setCustomerEmail("an@example.com");
        request.setReservationDate("2026-08-03");
        request.setArrivalTime("19:30");
        request.setExpectedDurationMinutes(120);
        request.setGuestCount(4);
        request.setTableId(12);

        PreorderItemRequest item = new PreorderItemRequest();
        item.setProductId(5);
        item.setQuantity(2);
        request.setPreorderItems(List.of(item));

        assertTrue(validator.validate(request).isEmpty());
    }
}
