package poly.edu.quanlynhahang.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableSession;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.TableSessionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class TableSessionService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final TableSessionRepository sessions;
    private final RestaurantTableRepository tables;
    private final OrderRepository orders;
    private final long lifetimeHours;

    public TableSessionService(TableSessionRepository sessions,
                               RestaurantTableRepository tables,
                               OrderRepository orders,
                               @Value("${restaurant.table-session.lifetime-hours:12}") long lifetimeHours) {
        this.sessions = sessions;
        this.tables = tables;
        this.orders = orders;
        this.lifetimeHours = lifetimeHours;
    }

    @Transactional
    public IssuedSession issue(Integer tableId) {
        RestaurantTable table = tables.findLockedById(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn"));
        revokeActiveForTableInternal(tableId, LocalDateTime.now());
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        LocalDateTime now = LocalDateTime.now();
        TableSession session = new TableSession();
        session.setTableId(tableId);
        session.setTokenHash(hash(token));
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusHours(lifetimeHours));
        session.setActive(true);
        sessions.save(session);
        return new IssuedSession(token, table.getId(), table.getName(), session.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public ResolvedSession resolve(String token) {
        TableSession session = requireValid(token);
        RestaurantTable table = tables.findById(session.getTableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "Bàn của mã QR không còn tồn tại"));
        Order current = orders.findOpenDineInOrdersByTableIdWithDetails(table.getId()).stream()
                .findFirst().orElse(null);
        return new ResolvedSession(table.getId(), table.getName(), session.getExpiresAt(), current);
    }

    @Transactional(readOnly = true)
    public TableSession requireForTable(String token, Integer tableId) {
        TableSession session = requireValid(token);
        if (!session.getTableId().equals(tableId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mã QR không thuộc bàn này");
        }
        return session;
    }

    @Transactional
    public Order requireForOrder(String token, Integer orderId) {
        TableSession session = requireValid(token);
        Order order = orders.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (!session.getTableId().equals(order.getTableId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mã QR không có quyền trên đơn hàng này");
        }
        if (Boolean.TRUE.equals(order.getIsPaid()) || Integer.valueOf(3).equals(order.getStatus())
                || Integer.valueOf(4).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã đóng");
        }
        return order;
    }

    @Transactional
    public void revokeActiveForTable(Integer tableId) {
        revokeActiveForTableInternal(tableId, LocalDateTime.now());
    }

    private void revokeActiveForTableInternal(Integer tableId, LocalDateTime revokedAt) {
        List<TableSession> active = sessions.findActiveByTableIdForUpdate(tableId);
        active.forEach(session -> {
            session.setActive(false);
            session.setRevokedAt(revokedAt);
        });
        sessions.saveAll(active);
    }

    private TableSession requireValid(String token) {
        if (token == null || token.isBlank() || token.length() > 200) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mã QR không hợp lệ");
        }
        TableSession session = sessions.findByTokenHash(hash(token.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mã QR không hợp lệ"));
        if (!Boolean.TRUE.equals(session.getActive()) || session.getRevokedAt() != null
                || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Mã QR đã hết hạn hoặc đã được thu hồi");
        }
        return session;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedSession(String token, Integer tableId, String tableName, LocalDateTime expiresAt) {}
    public record ResolvedSession(Integer tableId, String tableName, LocalDateTime expiresAt, Order currentOrder) {}
}
