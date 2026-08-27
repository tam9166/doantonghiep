package poly.edu.quanlynhahang.controller;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.ImportInvoiceRequest;
import poly.edu.quanlynhahang.entity.ImportInvoice;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.repository.ImportInvoiceRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.service.InventoryImportService;

@RestController
@RequestMapping("/api/admin/import-invoices")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
public class ImportInvoiceController {
    private final ImportInvoiceRepository invoiceRepository;
    private final IngredientBatchRepository batchRepository;
    private final InventoryImportService inventoryImportService;

    public ImportInvoiceController(ImportInvoiceRepository invoiceRepository,
                                   IngredientBatchRepository batchRepository,
                                   InventoryImportService inventoryImportService) {
        this.invoiceRepository = invoiceRepository;
        this.batchRepository = batchRepository;
        this.inventoryImportService = inventoryImportService;
    }

    @GetMapping
    public ResponseEntity<List<ImportInvoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceRepository.findAll(Sort.by(Sort.Direction.DESC, "importDate")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable Long id) {
        if (!invoiceRepository.existsById(id)) return ResponseEntity.notFound().build();
        List<IngredientBatch> batches = batchRepository.findAll().stream()
                .filter(b -> b.getImportInvoice() != null && id.equals(b.getImportInvoice().getId())).toList();
        return ResponseEntity.ok(batches);
    }

    @PostMapping
    public ResponseEntity<ImportInvoice> createImportInvoice(@Valid @RequestBody ImportInvoiceRequest request) {
        return ResponseEntity.ok(inventoryImportService.create(request));
    }
}
