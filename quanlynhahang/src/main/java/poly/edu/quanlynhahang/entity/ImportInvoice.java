package poly.edu.quanlynhahang.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "import_invoices")
public class ImportInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date importDate;

    @Column(columnDefinition = "nvarchar(255)")
    private String supplier;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(columnDefinition = "nvarchar(max)")
    private String note;
}
