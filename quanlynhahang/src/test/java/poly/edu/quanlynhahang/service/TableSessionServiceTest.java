package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableSession;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.TableSessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TableSessionServiceTest {
    private final TableSessionRepository sessions = mock(TableSessionRepository.class);
    private final RestaurantTableRepository tables = mock(RestaurantTableRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final TableSessionService service = new TableSessionService(sessions, tables, orders, 12);

    @Test
    void issuingCapabilityRevokesThePreviousSessionAndNeverPersistsTheRawToken() {
        RestaurantTable table = new RestaurantTable();
        table.setId(7);
        table.setName("B07");
        TableSession previous = validSession(7);
        when(tables.findLockedById(7)).thenReturn(Optional.of(table));
        when(sessions.findActiveByTableIdForUpdate(7)).thenReturn(List.of(previous));

        TableSessionService.IssuedSession issued = service.issue(7);

        assertEquals(7, issued.tableId());
        assertTrue(issued.token().length() >= 40);
        assertFalse(previous.getActive());
        verify(sessions).saveAll(List.of(previous));
        verify(sessions).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getTokenHash().matches("[0-9a-f]{64}")
                        && !saved.getTokenHash().equals(issued.token())));
    }

    @Test
    void revokedQrCannotAccessTheNextTableSession() {
        TableSession revoked = validSession(4);
        revoked.setActive(false);
        revoked.setRevokedAt(LocalDateTime.now());
        String token = "old-capability-token";
        String hash = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "hash", token);
        revoked.setTokenHash(hash);
        when(sessions.findByTokenHash(hash)).thenReturn(Optional.of(revoked));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.resolve(token));

        assertEquals(HttpStatus.GONE, error.getStatusCode());
    }

    @Test
    void capabilityCanAccessOnlyTheCurrentOpenOrderOfItsOwnTable() {
        String token = "current-capability-token";
        String hash = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "hash", token);
        TableSession session = validSession(8);
        session.setTokenHash(hash);
        Order ownOrder = new Order();
        ownOrder.setId(81);
        ownOrder.setTableId(8);
        ownOrder.setStatus(1);
        ownOrder.setIsPaid(false);
        Order otherOrder = new Order();
        otherOrder.setId(91);
        otherOrder.setTableId(9);
        otherOrder.setStatus(1);
        otherOrder.setIsPaid(false);
        when(sessions.findByTokenHash(hash)).thenReturn(Optional.of(session));
        when(orders.findLockedById(81)).thenReturn(Optional.of(ownOrder));
        when(orders.findLockedById(91)).thenReturn(Optional.of(otherOrder));

        assertEquals(ownOrder, service.requireForOrder(token, 81));
        ResponseStatusException forbidden = assertThrows(ResponseStatusException.class,
                () -> service.requireForOrder(token, 91));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    private TableSession validSession(Integer tableId) {
        TableSession session = new TableSession();
        session.setTableId(tableId);
        session.setTokenHash("a".repeat(64));
        session.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        session.setActive(true);
        return session;
    }
}
