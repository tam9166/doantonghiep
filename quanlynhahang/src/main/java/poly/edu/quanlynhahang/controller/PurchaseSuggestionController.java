package poly.edu.quanlynhahang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.ImportInvoiceItemRequest;
import poly.edu.quanlynhahang.dto.ImportInvoiceRequest;
import poly.edu.quanlynhahang.dto.PurchaseSuggestionApprovalRequest;
import poly.edu.quanlynhahang.dto.PurchaseSuggestionBatchRequest;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.ImportInvoice;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.service.InventoryAlertService;
import poly.edu.quanlynhahang.service.InventoryImportService;

import java.util.*;
@RestController
@RequestMapping("/api/admin/purchase-suggestions")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
public class PurchaseSuggestionController {

    private final IngredientRepository ingredientRepository;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final InventoryAlertService inventoryAlertService;
    private final InventoryImportService inventoryImportService;

    public PurchaseSuggestionController(IngredientRepository ingredientRepository,
                                        IngredientBatchRepository ingredientBatchRepository,
                                        InventoryAlertService inventoryAlertService,
                                        InventoryImportService inventoryImportService) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.inventoryAlertService = inventoryAlertService;
        this.inventoryImportService = inventoryImportService;
    }

    /**
     * Lấy danh sách đề xuất mua hàng tự động
     * Logic: Nguyên liệu dưới minStock + tính tốc độ tiêu thụ 7 ngày qua
     */
    @GetMapping
    public ResponseEntity<?> getSuggestions() {
        return ResponseEntity.ok(inventoryAlertService.analyze(3));
    }

    /**
     * Duyệt đề xuất: Tự động tạo lô nhập kho cho nguyên liệu
     */
    @PostMapping("/approve/{ingredientId}")
    public ResponseEntity<?> approveSuggestion(@PathVariable Long ingredientId,
                                                @Valid @RequestBody PurchaseSuggestionApprovalRequest request) {
        return ResponseEntity.ok(approve(ingredientId, request));
    }

    /**
     * Batch intentionally uses one transaction per item. A bad supplier/price/date
     * therefore does not discard valid imports and the UI gets one aggregate result.
     */
    @PostMapping("/approve-batch")
    public ResponseEntity<?> approveBatch(@Valid @RequestBody PurchaseSuggestionBatchRequest request) {
        List<Map<String, Object>> successes = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        for (PurchaseSuggestionBatchRequest.Item item : request.items()) {
            try {
                successes.add(approve(item.ingredientId(), item.approval()));
            } catch (Exception exception) {
                String reason = exception instanceof ResponseStatusException response
                        ? response.getReason() : "Không thể tạo lô nhập kho";
                failures.add(Map.of("ingredientId", item.ingredientId(), "reason", Objects.requireNonNullElse(reason, "Dữ liệu không hợp lệ")));
            }
        }
        return ResponseEntity.status(failures.isEmpty() ? HttpStatus.OK : HttpStatus.MULTI_STATUS)
                .body(Map.of("successCount", successes.size(), "failureCount", failures.size(),
                        "successes", successes, "failures", failures));
    }

    private Map<String, Object> approve(Long ingredientId, PurchaseSuggestionApprovalRequest request) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nguyên liệu"));
        ImportInvoiceItemRequest item = new ImportInvoiceItemRequest();
        item.setIngredientId(ingredientId);
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setExpirationDate(request.expirationDate());
        ImportInvoiceRequest invoiceRequest = new ImportInvoiceRequest();
        invoiceRequest.setSupplier(request.supplier().trim());
        invoiceRequest.setNote(request.note() == null || request.note().isBlank()
                ? "Duyệt đề xuất mua hàng" : request.note().trim());
        invoiceRequest.setSourceRequestId(request.requestId().trim());
        invoiceRequest.setItems(List.of(item));
        ImportInvoice invoice = inventoryImportService.create(invoiceRequest);
        var totalQuantity = ingredientBatchRepository.sumAvailableByIngredientId(ingredientId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ingredientId", ingredientId);
        result.put("invoiceId", invoice.getId());
        result.put("message", "Đã nhập kho " + request.quantity() + " " + ingredient.getUnit() + " " + ingredient.getName());
        result.put("newStock", totalQuantity);
        return result;
    }
}
