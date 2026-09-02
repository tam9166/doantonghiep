package poly.edu.quanlynhahang.dto;

import lombok.Data;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Data
public class ImportInvoiceRequest {
    @NotBlank
    private String supplier;
    private String note;
    @NotEmpty
    @Valid
    private List<ImportInvoiceItemRequest> items;
    private String sourceRequestId;
}
