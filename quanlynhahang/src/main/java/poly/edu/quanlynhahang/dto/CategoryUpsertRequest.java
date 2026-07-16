package poly.edu.quanlynhahang.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryUpsertRequest(
        @NotBlank @Size(max = 100) String name) {
}
