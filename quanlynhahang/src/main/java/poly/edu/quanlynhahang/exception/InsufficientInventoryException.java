package poly.edu.quanlynhahang.exception;

import java.util.Map;

public class InsufficientInventoryException extends RuntimeException {
    private final Map<String, String> shortages;

    public InsufficientInventoryException(Map<String, String> shortages) {
        super("INSUFFICIENT_INVENTORY");
        this.shortages = Map.copyOf(shortages);
    }

    public Map<String, String> getShortages() {
        return shortages;
    }
}
