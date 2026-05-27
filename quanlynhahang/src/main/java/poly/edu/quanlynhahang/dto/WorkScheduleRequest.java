package poly.edu.quanlynhahang.dto;

import lombok.Data;

@Data
public class WorkScheduleRequest {
    private String username;
    private String workDate; // format: yyyy-MM-dd
    private String shift; // Sáng, Chiều, Tối
}
