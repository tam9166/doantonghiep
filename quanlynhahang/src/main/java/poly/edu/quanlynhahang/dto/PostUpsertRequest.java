package poly.edu.quanlynhahang.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PostUpsertRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 50_000) String content,
        @Size(max = 500) String image,
        @Pattern(regexp = "NEWS|RECRUITMENT") String type,
        Boolean active) {
}
