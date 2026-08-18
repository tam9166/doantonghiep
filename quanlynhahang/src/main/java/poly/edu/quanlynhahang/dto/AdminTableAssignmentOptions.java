package poly.edu.quanlynhahang.dto;
import java.util.List;
public record AdminTableAssignmentOptions(Long reservationId, int guestCount, Integer areaId,
        List<ReservationTableResponse> availableTables, List<List<Integer>> recommendedOptions) {}
