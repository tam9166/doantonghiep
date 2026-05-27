package poly.edu.quanlynhahang.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ImportInvoiceItemRequest {
    private Long ingredientId;
    private Double quantity;
    private Double unitPrice;
    private Date expirationDate;
}
