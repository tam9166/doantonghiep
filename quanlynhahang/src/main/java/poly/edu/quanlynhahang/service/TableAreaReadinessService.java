package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.entity.AreaType;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

import java.util.List;

@Service
public class TableAreaReadinessService {
    private static final String ACTIVE = "ACTIVE";
    private static final int MIN_OPERATIONAL_TABLES = 2;

    private final RestaurantTableRepository tableRepository;

    public TableAreaReadinessService(RestaurantTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    public Readiness evaluate(TableArea area) {
        if (area == null) {
            return new Readiness(false, "Không tìm thấy khu vực", 0, 0);
        }
        if (!ACTIVE.equals(area.getStatus())) {
            return new Readiness(false, "Khu vực đang tạm ngưng", 0, 0);
        }
        if (AreaType.EVENT_HALL.equals(area.getAreaType())) {
            return new Readiness(true, "Sảnh sự kiện sẵn sàng nhận đặt chỗ", 0, 0);
        }

        List<RestaurantTable> tables = tableRepository.findOperationalTablesByAreaId(area.getId());
        int usableTableCount = tables.size();
        int totalTableCapacity = tables.stream().mapToInt(this::capacityOf).sum();
        int areaCapacity = area.getCapacity() == null ? 0 : area.getCapacity();

        if (usableTableCount < MIN_OPERATIONAL_TABLES) {
            return new Readiness(false, "Cần ít nhất 2 bàn đang hoạt động", usableTableCount, totalTableCapacity);
        }
        if (areaCapacity > 0 && totalTableCapacity > areaCapacity) {
            return new Readiness(false,
                    "Tổng sức chứa bàn " + totalTableCapacity + " vượt sức chứa khu vực " + areaCapacity,
                    usableTableCount, totalTableCapacity);
        }
        return new Readiness(true, "Sẵn sàng nhận đặt bàn", usableTableCount, totalTableCapacity);
    }

    public void requireBookingReady(TableArea area) {
        Readiness readiness = evaluate(area);
        if (!readiness.bookingReady()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, readiness.reason());
        }
    }

    private int capacityOf(RestaurantTable table) {
        if (table.getMaxCapacity() != null) return table.getMaxCapacity();
        if (table.getCapacity() != null) return table.getCapacity();
        return 0;
    }

    public record Readiness(
            boolean bookingReady,
            String reason,
            int usableTableCount,
            int totalTableCapacity) {
    }
}
