package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

class TableLifecycleServiceTest {
    private final RestaurantTableRepository tables = mock(RestaurantTableRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final TableReleaseGuardService guard = mock(TableReleaseGuardService.class);
    private final TableSessionService sessions = mock(TableSessionService.class);
    private final TableLifecycleService service = new TableLifecycleService(tables, orders, guard, sessions);

    @Test
    void releaseLocksChecksMutatesAndRevokesCapability() {
        RestaurantTable table = table(7, 2);
        when(tables.findLockedByIdIn(List.of(7))).thenReturn(List.of(table));

        service.release(7);

        assertEquals(0, table.getIsOccupied());
        verify(guard).prepareForRelease(7);
        verify(tables).save(table);
        verify(sessions).revokeActiveForTable(7);
    }

    @Test
    void failedInvariantLeavesTableAndCapabilityUntouched() {
        RestaurantTable table = table(8, 2);
        when(tables.findLockedByIdIn(List.of(8))).thenReturn(List.of(table));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "pending refund"))
                .when(guard).prepareForRelease(8);

        assertThrows(ResponseStatusException.class, () -> service.release(8));

        assertEquals(2, table.getIsOccupied());
        verify(tables, never()).save(table);
        verify(sessions, never()).revokeActiveForTable(8);
    }

    @Test
    void duplicateReleaseIsIdempotentAfterRecheckingInvariants() {
        RestaurantTable table = table(9, 0);
        when(tables.findLockedByIdIn(List.of(9))).thenReturn(List.of(table));

        service.release(9);

        verify(guard).prepareForRelease(9);
        verify(tables, never()).save(table);
        verifyNoInteractions(sessions);
    }

    @Test
    void paidTableMovesToCleaningWithinBackendLifecycle() {
        RestaurantTable table = table(10, 2);
        when(tables.findLockedById(10)).thenReturn(Optional.of(table));
        when(tables.save(table)).thenReturn(table);

        service.markCleaningAfterPayment(10);

        assertEquals(3, table.getIsOccupied());
        assertEquals("Đã thanh toán - chờ dọn", table.getReservedTime());
        verify(guard).prepareForRelease(10, null);
        verify(sessions).revokeActiveForTable(10);
    }

    @Test
    void paymentReleaseExcludesTheInvoiceBeingPaidFromOtherInvoiceGuard() {
        RestaurantTable table = table(11, 2);
        when(tables.findLockedById(11)).thenReturn(Optional.of(table));
        when(tables.save(table)).thenReturn(table);

        service.markCleaningAfterPayment(11, 229);

        assertEquals(3, table.getIsOccupied());
        verify(guard).prepareForRelease(11, 229);
    }

    private RestaurantTable table(int id, int status) {
        RestaurantTable table = new RestaurantTable();
        table.setId(id);
        table.setIsOccupied(status);
        return table;
    }
}
