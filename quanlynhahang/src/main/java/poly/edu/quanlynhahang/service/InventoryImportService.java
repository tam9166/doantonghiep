package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.ImportInvoiceItemRequest;
import poly.edu.quanlynhahang.dto.ImportInvoiceRequest;
import poly.edu.quanlynhahang.entity.ImportInvoice;
import poly.edu.quanlynhahang.entity.ImportInvoiceDetail;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.IngredientBatchStatus;
import poly.edu.quanlynhahang.repository.ImportInvoiceDetailRepository;
import poly.edu.quanlynhahang.repository.ImportInvoiceRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;

@Service
public class InventoryImportService {
    private final ImportInvoiceRepository invoiceRepository;
    private final ImportInvoiceDetailRepository detailRepository;
    private final IngredientBatchRepository batchRepository;
    private final IngredientRepository ingredientRepository;
    private final ActivityLogService activityLogService;
    private final MenuAvailabilityService menuAvailabilityService;

    public InventoryImportService(ImportInvoiceRepository invoiceRepository,
                                  ImportInvoiceDetailRepository detailRepository,
                                  IngredientBatchRepository batchRepository,
                                  IngredientRepository ingredientRepository,
                                  ActivityLogService activityLogService,
                                  MenuAvailabilityService menuAvailabilityService) {
        this.invoiceRepository = invoiceRepository;
        this.detailRepository = detailRepository;
        this.batchRepository = batchRepository;
        this.ingredientRepository = ingredientRepository;
        this.activityLogService = activityLogService;
        this.menuAvailabilityService = menuAvailabilityService;
    }

    @Transactional
    public ImportInvoice create(ImportInvoiceRequest request) {
        Date now = new Date();
        ImportInvoice invoice = new ImportInvoice();
        invoice.setInvoiceCode(nextInvoiceCode(now));
        invoice.setImportDate(now);
        invoice.setSupplier(request.getSupplier().trim());
        invoice.setNote(request.getNote());
        invoice.setTotalAmount(BigDecimal.ZERO.setScale(2));
        ImportInvoice saved = invoiceRepository.saveAndFlush(invoice);
        BigDecimal total = BigDecimal.ZERO;

        for (ImportInvoiceItemRequest item : request.getItems()) {
            Ingredient ingredient = ingredientRepository.findById(item.getIngredientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Không tìm thấy nguyên liệu id " + item.getIngredientId()));
            BigDecimal quantity = item.getQuantity();
            if (quantity == null || quantity.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng nhập phải lớn hơn 0");
            }
            BigDecimal unitPrice = item.getUnitPrice() == null
                    ? Objects.requireNonNullElse(ingredient.getUnitPrice(), BigDecimal.ZERO)
                    : item.getUnitPrice();
            if (unitPrice.signum() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn giá nhập không được âm");
            }
            Date importDate = item.getImportDate() == null ? now : item.getImportDate();
            if (importDate.after(now)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày nhập không được ở tương lai");
            }
            Date expiration = item.getExpirationDate();
            if (expiration == null) expiration = new Date(importDate.getTime() + 30L * 86_400_000L);
            if (!expiration.after(now)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể nhập lô đã hết hạn");
            }

            ImportInvoiceDetail detail = new ImportInvoiceDetail();
            detail.setInvoice(saved);
            detail.setIngredient(ingredient);
            detail.setQuantity(quantity);
            detail.setUnitPrice(unitPrice);
            detail.setExpiryDate(expiration);
            detail.setTotalPrice(quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
            detailRepository.save(detail);

            IngredientBatch batch = new IngredientBatch();
            batch.setIngredient(ingredient);
            batch.setImportInvoice(saved);
            batch.setImportDate(importDate);
            batch.setExpirationDate(expiration);
            batch.setQuantity(quantity);
            batch.setUnitPrice(unitPrice);
            batch.setStatus(IngredientBatchStatus.AVAILABLE);
            batchRepository.save(batch);

            ingredient.setUnitPrice(unitPrice);
            // Batches are the source of truth; the new row is already in this sum.
            BigDecimal usable = batchRepository.sumAvailableByIngredientId(ingredient.getId());
            ingredient.setQuantity(usable == null ? BigDecimal.ZERO : usable);
            ingredientRepository.save(ingredient);
            menuAvailabilityService.refreshForIngredient(ingredient);
            total = total.add(detail.getTotalPrice());
        }
        saved.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        saved = invoiceRepository.save(saved);
        activityLogService.log("CREATE", "ImportInvoice", String.valueOf(saved.getId()),
                "Nhập kho " + saved.getInvoiceCode() + " - NCC: " + saved.getSupplier()
                        + " - Tổng: " + saved.getTotalAmount() + "đ");
        return saved;
    }

    @Transactional
    public IngredientBatch createSingleBatch(Long ingredientId, BigDecimal quantity,
                                              BigDecimal unitPrice, Date expirationDate,
                                              String supplier) {
        ImportInvoiceItemRequest item = new ImportInvoiceItemRequest();
        item.setIngredientId(ingredientId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setExpirationDate(expirationDate);
        ImportInvoiceRequest request = new ImportInvoiceRequest();
        request.setSupplier(supplier == null || supplier.isBlank() ? "Hệ thống nhập kho" : supplier);
        request.setItems(List.of(item));
        ImportInvoice invoice = create(request);
        return batchRepository.findAll().stream()
                .filter(batch -> batch.getImportInvoice() != null
                        && invoice.getId().equals(batch.getImportInvoice().getId()))
                .findFirst().orElseThrow();
    }

    private String nextInvoiceCode(Date date) {
        String day = new SimpleDateFormat("yyyyMMdd").format(date);
        String code;
        do {
            code = "IMP-" + day + "-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 6).toUpperCase();
        } while (invoiceRepository.existsByInvoiceCode(code));
        return code;
    }
}
