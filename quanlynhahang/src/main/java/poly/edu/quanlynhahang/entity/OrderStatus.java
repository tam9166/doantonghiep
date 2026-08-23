package poly.edu.quanlynhahang.entity;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Canonical values persisted in {@code Orders.status}. */
public enum OrderStatus {
    PENDING(0),
    IN_PREPARATION(1),
    READY(2),
    CANCELLED(3),
    COMPLETED(4),
    SCHEDULED(5),
    PARTIALLY_READY(6),
    SERVED(7);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            throw invalid(code);
        }
        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst()
                .orElseThrow(() -> invalid(code));
    }

    private static ResponseStatusException invalid(Integer code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Trạng thái đơn hàng không hợp lệ: " + code);
    }
}
