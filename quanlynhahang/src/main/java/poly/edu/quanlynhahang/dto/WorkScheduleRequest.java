package poly.edu.quanlynhahang.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class WorkScheduleRequest {
    @NotBlank @Size(max = 100)
    private String username;
    @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String workDate; // format: yyyy-MM-dd
    @NotBlank @Pattern(regexp = "^(Sáng|Chiều|Tối)$")
    private String shift; // Sáng, Chiều, Tối
}
