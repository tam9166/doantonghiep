package poly.edu.quanlynhahang.dto;

import java.util.Date;
import poly.edu.quanlynhahang.entity.ServiceZoneAssignment;

public record ServiceZoneAssignmentResponse(Long id, EmployeeSummaryResponse employee,
        String floor, String shift, Date workDate) {
    public static ServiceZoneAssignmentResponse from(ServiceZoneAssignment value) {
        return new ServiceZoneAssignmentResponse(value.getId(), EmployeeSummaryResponse.from(value.getAccount()),
                value.getFloor(), value.getShift(), value.getWorkDate());
    }
}
