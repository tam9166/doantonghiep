package poly.edu.quanlynhahang.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.service.OrderCheckoutService;
import poly.edu.quanlynhahang.service.TableSessionService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/table-sessions")
public class TableSessionController {
    private static final Logger log = LoggerFactory.getLogger(TableSessionController.class);
    private final TableSessionService tableSessions;
    private final OrderCheckoutService checkout;
    private final SimpMessagingTemplate messaging;

    public TableSessionController(TableSessionService tableSessions, OrderCheckoutService checkout,
                                  SimpMessagingTemplate messaging) {
        this.tableSessions = tableSessions;
        this.checkout = checkout;
        this.messaging = messaging;
    }

    @PostMapping("/admin/{tableId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAITER')")
    public ResponseEntity<?> issue(@PathVariable Integer tableId) {
        return ResponseEntity.ok(tableSessions.issue(tableId));
    }

    @GetMapping("/resolve")
    public ResponseEntity<?> resolve(@RequestHeader("X-Table-Session-Token") String token) {
        TableSessionService.ResolvedSession resolved = tableSessions.resolve(token);
        return ResponseEntity.ok(new PublicTableSessionResponse(
                resolved.tableId(), resolved.tableName(), resolved.expiresAt(),
                resolved.currentOrder() == null ? null : new PublicCurrentOrder(
                        resolved.currentOrder().getId(), resolved.currentOrder().getOrderCode(),
                        resolved.currentOrder().getStatus())));
    }

    @PutMapping("/orders/{orderId}/add-items")
    public ResponseEntity<?> addItems(@PathVariable Integer orderId,
                                      @RequestHeader("X-Table-Session-Token") String token,
                                      @RequestHeader("X-Idempotency-Key") String idempotencyKey,
                                      @Valid @RequestBody OrderRequest request) {
        tableSessions.requireForOrder(token, orderId);
        var result = checkout.addItems(orderId, request, idempotencyKey);
        try {
            messaging.convertAndSend("/topic/kitchen", "NEW_ORDER");
            messaging.convertAndSend("/topic/waiter", "DISH_STATUS_CHANGED");
        } catch (RuntimeException exception) {
            // The database operation is authoritative; a temporary broker failure
            // must not make the client retry a successfully added line item.
            log.warn("Order {} was updated but realtime notification failed", orderId, exception);
        }
        return ResponseEntity.ok(result);
    }

    public record PublicTableSessionResponse(Integer tableId, String tableName,
                                             java.time.LocalDateTime expiresAt, PublicCurrentOrder currentOrder) {}
    public record PublicCurrentOrder(Integer id, String orderCode, Integer status) {}
}
