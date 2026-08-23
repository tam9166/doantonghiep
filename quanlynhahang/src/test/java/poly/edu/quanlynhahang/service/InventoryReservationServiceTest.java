package poly.edu.quanlynhahang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.InventoryReservation;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.InventoryReservationRepository;

class InventoryReservationServiceTest {
    private final InventoryReservationRepository reservationRepository = mock(InventoryReservationRepository.class);
    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final IngredientBatchRepository batchRepository = mock(IngredientBatchRepository.class);
    private final MenuAvailabilityService menuAvailabilityService = mock(MenuAvailabilityService.class);
    private final InventoryReservationService service = new InventoryReservationService(
            reservationRepository, ingredientRepository, batchRepository, menuAvailabilityService, 15);

    private Ingredient ingredient;
    private IngredientBatch firstBatch;

    @BeforeEach
    void setUp() {
        ingredient = new Ingredient();
        ingredient.setId(10L);
        ingredient.setName("Thịt bò");
        ingredient.setQuantity(new BigDecimal("10.0000"));
        firstBatch = new IngredientBatch();
        firstBatch.setId(20L);
        firstBatch.setIngredient(ingredient);
        firstBatch.setQuantity(new BigDecimal("10.0000"));
        when(ingredientRepository.findLockedById(10L)).thenReturn(Optional.of(ingredient));
        when(batchRepository.findAvailableBatchesForUpdate(10L)).thenReturn(List.of(firstBatch));
        when(reservationRepository.sumActiveReservedByIngredientId(
                eq(10L), eq(InventoryReservationStatus.RESERVED), any(Date.class)))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void reserveCreatesHoldWithoutMutatingPhysicalInventory() {
        Order order = order(7);

        service.reserve(order, Map.of(10L, new BigDecimal("4.0000")), future());

        ArgumentCaptor<List<InventoryReservation>> rows = ArgumentCaptor.forClass(List.class);
        verify(reservationRepository).saveAllAndFlush(rows.capture());
        assertThat(rows.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getOrder()).isSameAs(order);
            assertThat(row.getIngredient()).isSameAs(ingredient);
            assertThat(row.getQuantity()).isEqualByComparingTo("4.0000");
            assertThat(row.getStatus()).isEqualTo(InventoryReservationStatus.RESERVED);
        });
        assertThat(firstBatch.getQuantity()).isEqualByComparingTo("10.0000");
        verify(batchRepository, never()).saveAll(any());
    }

    @Test
    void consumeUsesFefoQuantityExactlyOnce() {
        InventoryReservation row = reservation(InventoryReservationStatus.RESERVED, future());
        when(reservationRepository.findLockedByOrderId(7)).thenReturn(List.of(row));

        service.consume(7);
        assertThat(firstBatch.getQuantity()).isEqualByComparingTo("6.0000");
        assertThat(ingredient.getQuantity()).isEqualByComparingTo("6.0000");
        assertThat(row.getStatus()).isEqualTo(InventoryReservationStatus.CONSUMED);

        assertDoesNotThrow(() -> service.consume(7));
        assertThat(firstBatch.getQuantity()).isEqualByComparingTo("6.0000");
        verify(batchRepository).saveAll(List.of(firstBatch));
    }

    @Test
    void releaseReturnsHeldStockWithoutMutatingPhysicalInventory() {
        InventoryReservation row = reservation(InventoryReservationStatus.RESERVED, future());
        when(reservationRepository.findLockedByOrderId(7)).thenReturn(List.of(row));

        service.release(7, InventoryReservationStatus.RELEASED);

        assertThat(row.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
        assertThat(row.getFinalizedAt()).isNotNull();
        assertThat(firstBatch.getQuantity()).isEqualByComparingTo("10.0000");
        verify(batchRepository, never()).saveAll(any());
        verify(menuAvailabilityService).refreshForIngredient(ingredient);
    }

    @Test
    void cancellingPendingDishReducesHoldWithoutRestockingBatch() {
        InventoryReservation row = reservation(InventoryReservationStatus.RESERVED, future());
        when(reservationRepository.findLockedByOrderId(7)).thenReturn(List.of(row));

        service.adjustForCancelledItem(7, Map.of(10L, new BigDecimal("2.0000")), true);

        assertThat(row.getQuantity()).isEqualByComparingTo("2.0000");
        assertThat(row.getStatus()).isEqualTo(InventoryReservationStatus.RESERVED);
        assertThat(firstBatch.getQuantity()).isEqualByComparingTo("10.0000");
        verify(batchRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancellingUnstartedConsumedDishCompensatesPhysicalBatch() {
        InventoryReservation row = reservation(InventoryReservationStatus.CONSUMED, future());
        when(reservationRepository.findLockedByOrderId(7)).thenReturn(List.of(row));
        when(batchRepository.findRestorableBatchesForUpdate(eq(10L), any(Date.class)))
                .thenReturn(List.of(firstBatch));
        when(batchRepository.sumAvailableByIngredientId(10L)).thenReturn(new BigDecimal("12.0000"));

        service.adjustForCancelledItem(7, Map.of(10L, new BigDecimal("2.0000")), true);

        assertThat(firstBatch.getQuantity()).isEqualByComparingTo("12.0000");
        assertThat(ingredient.getQuantity()).isEqualByComparingTo("12.0000");
        assertThat(row.getStatus()).isEqualTo(InventoryReservationStatus.CONSUMED);
        verify(batchRepository).saveAndFlush(firstBatch);
    }

    private InventoryReservation reservation(InventoryReservationStatus status, Date expiresAt) {
        InventoryReservation row = new InventoryReservation();
        row.setOrder(order(7));
        row.setIngredient(ingredient);
        row.setQuantity(new BigDecimal("4.0000"));
        row.setStatus(status);
        row.setExpiresAt(expiresAt);
        return row;
    }

    private Order order(int id) {
        Order order = new Order();
        order.setId(id);
        return order;
    }

    private Date future() {
        return new Date(System.currentTimeMillis() + 60_000);
    }
}
