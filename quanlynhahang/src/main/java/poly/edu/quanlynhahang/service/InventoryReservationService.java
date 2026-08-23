package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.InventoryReservation;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.exception.InsufficientInventoryException;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.InventoryReservationRepository;

@Service
public class InventoryReservationService {
    private final InventoryReservationRepository reservationRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientBatchRepository batchRepository;
    private final MenuAvailabilityService menuAvailabilityService;
    private final long holdMinutes;

    public InventoryReservationService(
            InventoryReservationRepository reservationRepository,
            IngredientRepository ingredientRepository,
            IngredientBatchRepository batchRepository,
            MenuAvailabilityService menuAvailabilityService,
            @Value("${restaurant.inventory.hold-minutes:15}") long holdMinutes) {
        this.reservationRepository = reservationRepository;
        this.ingredientRepository = ingredientRepository;
        this.batchRepository = batchRepository;
        this.menuAvailabilityService = menuAvailabilityService;
        this.holdMinutes = Math.max(1, holdMinutes);
    }

    public Date defaultExpiry() {
        return Date.from(Instant.now().plusSeconds(holdMinutes * 60));
    }

    @Transactional
    public void reserve(Order order, Map<Long, BigDecimal> requirements, Date expiresAt) {
        if (order == null || order.getId() == null || requirements == null || requirements.isEmpty()) return;
        if (!reservationRepository.findLockedByOrderId(order.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã có lượt giữ kho");
        }
        Date now = new Date();
        Date effectiveExpiry = expiresAt != null && expiresAt.after(now) ? expiresAt : defaultExpiry();
        Map<String, String> shortages = new LinkedHashMap<>();
        Map<Ingredient, BigDecimal> validated = new LinkedHashMap<>();

        requirements.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Ingredient ingredient = ingredientRepository.findLockedById(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT, "Nguyên liệu không còn tồn tại"));
            List<IngredientBatch> batches = batchRepository.findAvailableBatchesForUpdate(entry.getKey());
            BigDecimal physical = sumBatches(batches);
            BigDecimal held = activeReserved(entry.getKey(), now);
            BigDecimal available = physical.subtract(held).max(BigDecimal.ZERO);
            if (available.compareTo(entry.getValue()) < 0) {
                shortages.put(ingredient.getName(),
                        "required=" + entry.getValue() + ", available=" + available);
            }
            validated.put(ingredient, entry.getValue());
        });
        if (!shortages.isEmpty()) throw new InsufficientInventoryException(shortages);

        List<InventoryReservation> rows = validated.entrySet().stream().map(entry -> {
            InventoryReservation row = new InventoryReservation();
            row.setOrder(order);
            row.setIngredient(entry.getKey());
            row.setQuantity(entry.getValue());
            row.setStatus(InventoryReservationStatus.RESERVED);
            row.setExpiresAt(effectiveExpiry);
            row.setCreatedAt(now);
            return row;
        }).toList();
        reservationRepository.saveAllAndFlush(rows);
        validated.keySet().forEach(menuAvailabilityService::refreshForIngredient);
    }

    @Transactional
    public void renew(Integer orderId, Date expiresAt) {
        List<InventoryReservation> rows = reservationRepository.findLockedByOrderId(orderId);
        if (rows.isEmpty()) return;
        if (rows.stream().anyMatch(row -> InventoryReservationStatus.CONSUMED.equals(row.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kho của đơn đã được tiêu thụ");
        }
        Date now = new Date();
        Date effectiveExpiry = expiresAt != null && expiresAt.after(now) ? expiresAt : defaultExpiry();
        Map<String, String> shortages = new LinkedHashMap<>();
        for (InventoryReservation row : rows) {
            Ingredient ingredient = ingredientRepository.findLockedById(row.getIngredient().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Nguyên liệu không còn tồn tại"));
            BigDecimal physical = sumBatches(batchRepository.findAvailableBatchesForUpdate(ingredient.getId()));
            BigDecimal heldByOthers = activeReserved(ingredient.getId(), now);
            if (InventoryReservationStatus.RESERVED.equals(row.getStatus())
                    && row.getExpiresAt() != null && row.getExpiresAt().after(now)) {
                heldByOthers = heldByOthers.subtract(row.getQuantity()).max(BigDecimal.ZERO);
            }
            BigDecimal available = physical.subtract(heldByOthers).max(BigDecimal.ZERO);
            if (available.compareTo(row.getQuantity()) < 0) {
                shortages.put(ingredient.getName(),
                        "required=" + row.getQuantity() + ", available=" + available);
            }
        }
        if (!shortages.isEmpty()) throw new InsufficientInventoryException(shortages);
        rows.forEach(row -> {
            row.setStatus(InventoryReservationStatus.RESERVED);
            row.setExpiresAt(effectiveExpiry);
            row.setFinalizedAt(null);
        });
        reservationRepository.saveAllAndFlush(rows);
        rows.forEach(row -> menuAvailabilityService.refreshForIngredient(row.getIngredient()));
    }

    @Transactional
    public void consume(Integer orderId) {
        List<InventoryReservation> rows = reservationRepository.findLockedByOrderId(orderId);
        if (rows.isEmpty() || rows.stream().allMatch(row ->
                InventoryReservationStatus.CONSUMED.equals(row.getStatus()))) return;
        Date now = new Date();
        if (rows.stream().anyMatch(row -> !InventoryReservationStatus.RESERVED.equals(row.getStatus())
                || row.getExpiresAt() == null || !row.getExpiresAt().after(now))) {
            expireRows(rows, now);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STOCK_RESERVATION_EXPIRED");
        }
        for (InventoryReservation row : rows) {
            Ingredient ingredient = ingredientRepository.findLockedById(row.getIngredient().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Nguyên liệu không còn tồn tại"));
            List<IngredientBatch> batches = batchRepository.findAvailableBatchesForUpdate(ingredient.getId());
            BigDecimal remaining = row.getQuantity();
            for (IngredientBatch batch : batches) {
                if (remaining.signum() <= 0) break;
                BigDecimal current = batch.getQuantity() == null ? BigDecimal.ZERO : batch.getQuantity();
                BigDecimal consumed = current.min(remaining);
                batch.setQuantity(current.subtract(consumed));
                remaining = remaining.subtract(consumed);
            }
            if (remaining.signum() > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Kho thực tế không đủ để hoàn tất đơn");
            }
            batchRepository.saveAll(batches);
            ingredient.setQuantity(sumBatches(batches));
            ingredientRepository.save(ingredient);
            row.setStatus(InventoryReservationStatus.CONSUMED);
            row.setFinalizedAt(now);
        }
        reservationRepository.saveAllAndFlush(rows);
        rows.forEach(row -> menuAvailabilityService.refreshForIngredient(row.getIngredient()));
    }

    @Transactional
    public void release(Integer orderId, InventoryReservationStatus finalStatus) {
        if (!InventoryReservationStatus.RELEASED.equals(finalStatus)
                && !InventoryReservationStatus.EXPIRED.equals(finalStatus)) {
            throw new IllegalArgumentException("Trạng thái giải phóng kho không hợp lệ");
        }
        List<InventoryReservation> rows = reservationRepository.findLockedByOrderId(orderId);
        Date now = new Date();
        rows.stream().filter(row -> InventoryReservationStatus.RESERVED.equals(row.getStatus())).forEach(row -> {
            row.setStatus(finalStatus);
            row.setFinalizedAt(now);
        });
        reservationRepository.saveAllAndFlush(rows);
        rows.forEach(row -> menuAvailabilityService.refreshForIngredient(row.getIngredient()));
    }

    @Transactional
    public void adjustForCancelledItem(Integer orderId,
                                       Map<Long, BigDecimal> requirements,
                                       boolean restorePhysicalStock) {
        if (requirements == null || requirements.isEmpty()) return;
        List<InventoryReservation> rows = reservationRepository.findLockedByOrderId(orderId);
        Map<Long, InventoryReservation> byIngredient = rows.stream().collect(java.util.stream.Collectors.toMap(
                row -> row.getIngredient().getId(), row -> row));
        Date now = new Date();
        requirements.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            InventoryReservation row = byIngredient.get(entry.getKey());
            if (row != null && InventoryReservationStatus.RESERVED.equals(row.getStatus())) {
                BigDecimal remaining = row.getQuantity().subtract(entry.getValue());
                if (remaining.signum() <= 0) {
                    row.setQuantity(entry.getValue().min(row.getQuantity()));
                    row.setStatus(InventoryReservationStatus.RELEASED);
                    row.setFinalizedAt(now);
                } else {
                    row.setQuantity(remaining);
                }
            } else if (restorePhysicalStock
                    && (row == null || InventoryReservationStatus.CONSUMED.equals(row.getStatus()))) {
                restoreIngredient(entry.getKey(), entry.getValue(), now);
            }
        });
        reservationRepository.saveAllAndFlush(rows);
        requirements.keySet().forEach(ingredientId -> ingredientRepository.findById(ingredientId)
                .ifPresent(menuAvailabilityService::refreshForIngredient));
    }

    private void restoreIngredient(Long ingredientId, BigDecimal quantity, Date now) {
        Ingredient ingredient = ingredientRepository.findLockedById(ingredientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Nguyên liệu không còn tồn tại"));
        List<IngredientBatch> batches = batchRepository.findRestorableBatchesForUpdate(ingredientId, now);
        IngredientBatch target;
        if (batches.isEmpty()) {
            target = new IngredientBatch();
            target.setIngredient(ingredient);
            target.setQuantity(BigDecimal.ZERO);
            target.setImportDate(now);
            int shelfLifeDays = ingredient.getShelfLifeDays() == null ? 1 : Math.max(1, ingredient.getShelfLifeDays());
            target.setExpirationDate(Date.from(now.toInstant().plusSeconds(shelfLifeDays * 86_400L)));
            target.setUnitPrice(ingredient.getUnitPrice() == null ? BigDecimal.ZERO : ingredient.getUnitPrice());
        } else {
            target = batches.getFirst();
        }
        target.setQuantity((target.getQuantity() == null ? BigDecimal.ZERO : target.getQuantity()).add(quantity));
        batchRepository.saveAndFlush(target);
        BigDecimal aggregate = batchRepository.sumAvailableByIngredientId(ingredientId);
        ingredient.setQuantity(aggregate == null ? BigDecimal.ZERO : aggregate);
        ingredientRepository.save(ingredient);
    }

    private void expireRows(List<InventoryReservation> rows, Date now) {
        rows.stream().filter(row -> InventoryReservationStatus.RESERVED.equals(row.getStatus())).forEach(row -> {
            row.setStatus(InventoryReservationStatus.EXPIRED);
            row.setFinalizedAt(now);
        });
        reservationRepository.saveAllAndFlush(rows);
        rows.forEach(row -> menuAvailabilityService.refreshForIngredient(row.getIngredient()));
    }

    private BigDecimal activeReserved(Long ingredientId, Date now) {
        BigDecimal value = reservationRepository.sumActiveReservedByIngredientId(
                ingredientId, InventoryReservationStatus.RESERVED, now);
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal sumBatches(List<IngredientBatch> batches) {
        return batches.stream().map(IngredientBatch::getQuantity)
                .filter(value -> value != null && value.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
