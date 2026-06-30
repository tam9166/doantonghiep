package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.dto.ImportInvoiceItemRequest;
import poly.edu.quanlynhahang.dto.ImportInvoiceRequest;
import poly.edu.quanlynhahang.entity.ImportInvoice;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.repository.ImportInvoiceRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;

import poly.edu.quanlynhahang.service.ActivityLogService;

@RestController
@RequestMapping("/api/admin/import-invoices")
@CrossOrigin("*")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
public class ImportInvoiceController {

    @Autowired
    private ImportInvoiceRepository importInvoiceRepository;

    @Autowired
    private IngredientBatchRepository ingredientBatchRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<List<ImportInvoice>> getAllInvoices() {
        return ResponseEntity.ok(importInvoiceRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "importDate")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable Long id) {
        Optional<ImportInvoice> invoiceOpt = importInvoiceRepository.findById(id);
        if (invoiceOpt.isPresent()) {
            List<IngredientBatch> batches = ingredientBatchRepository.findAll().stream()
                    .filter(b -> b.getImportInvoice() != null && b.getImportInvoice().getId().equals(id))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(batches);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createImportInvoice(@RequestBody ImportInvoiceRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Hóa đơn phải có ít nhất 1 nguyên liệu");
        }

        ImportInvoice invoice = new ImportInvoice();
        invoice.setImportDate(new Date());
        invoice.setSupplier(request.getSupplier());
        invoice.setNote(request.getNote());
        
        double totalAmount = 0;
        
        ImportInvoice savedInvoice = importInvoiceRepository.save(invoice);

        for (ImportInvoiceItemRequest itemReq : request.getItems()) {
            Optional<Ingredient> ingOpt = ingredientRepository.findById(itemReq.getIngredientId());
            if (ingOpt.isPresent()) {
                Ingredient ing = ingOpt.get();
                
                IngredientBatch batch = new IngredientBatch();
                batch.setIngredient(ing);
                batch.setImportInvoice(savedInvoice);
                batch.setImportDate(savedInvoice.getImportDate());
                batch.setQuantity(itemReq.getQuantity());
                batch.setUnitPrice(itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : ing.getUnitPrice());
                
                // Hạn sử dụng
                if (itemReq.getExpirationDate() != null) {
                    batch.setExpirationDate(itemReq.getExpirationDate());
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(batch.getImportDate());
                    cal.add(Calendar.DAY_OF_YEAR, ing.getShelfLifeDays() != null ? ing.getShelfLifeDays() : 30);
                    batch.setExpirationDate(cal.getTime());
                }
                
                ingredientBatchRepository.save(batch);
                
                totalAmount += (batch.getQuantity() * batch.getUnitPrice());
                
                // Cập nhật giá nhập mới nhất cho nguyên liệu
                ing.setUnitPrice(batch.getUnitPrice());
                // Cập nhật tồn kho
                List<IngredientBatch> allBatches = ingredientBatchRepository.findByIngredientIdOrderByExpirationDateAsc(ing.getId());
                double totalQty = allBatches.stream()
                        .filter(b -> b.getExpirationDate() == null || b.getExpirationDate().after(new Date()))
                        .mapToDouble(IngredientBatch::getQuantity).sum() + batch.getQuantity();
                
                ing.setQuantity(totalQty);
                ingredientRepository.save(ing);
            }
        }
        
        savedInvoice.setTotalAmount(totalAmount);
        importInvoiceRepository.save(savedInvoice);

        activityLogService.log("CREATE", "ImportInvoice", String.valueOf(savedInvoice.getId()),
                "Nhập kho mới #" + savedInvoice.getId() + " - NCC: " + request.getSupplier() +
                " - Tổng: " + totalAmount + "đ");

        return ResponseEntity.ok(savedInvoice);
    }
}
