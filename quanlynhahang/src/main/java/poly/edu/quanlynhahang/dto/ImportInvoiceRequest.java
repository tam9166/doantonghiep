package poly.edu.quanlynhahang.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImportInvoiceRequest {
    private String supplier;
    private String note;
    private List<ImportInvoiceItemRequest> items;
}
