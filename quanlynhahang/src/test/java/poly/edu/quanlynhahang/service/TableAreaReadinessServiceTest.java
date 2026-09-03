package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.AreaType;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

class TableAreaReadinessServiceTest {

    private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
    private final TableAreaReadinessService service = new TableAreaReadinessService(tableRepository);

    @Test
    void activeDiningAreaNeedsAtLeastTwoOperationalTables() {
        TableArea area = area(7, 12, AreaType.DINING, "ACTIVE");
        when(tableRepository.findOperationalTablesByAreaId(7)).thenReturn(List.of(table(4)));

        TableAreaReadinessService.Readiness readiness = service.evaluate(area);

        assertFalse(readiness.bookingReady());
        assertEquals(1, readiness.usableTableCount());
        assertTrue(readiness.reason().contains("ít nhất 2 bàn"));
    }

    @Test
    void activeDiningAreaRejectsTablesExceedingAreaCapacity() {
        TableArea area = area(8, 10, AreaType.DINING, "ACTIVE");
        when(tableRepository.findOperationalTablesByAreaId(8)).thenReturn(List.of(table(6), table(6)));

        TableAreaReadinessService.Readiness readiness = service.evaluate(area);

        assertFalse(readiness.bookingReady());
        assertEquals(12, readiness.totalTableCapacity());
        assertTrue(readiness.reason().contains("vượt sức chứa"));
    }

    @Test
    void activeDiningAreaIsReadyWithTwoOperationalTablesWithinCapacity() {
        TableArea area = area(9, 10, AreaType.DINING, "ACTIVE");
        when(tableRepository.findOperationalTablesByAreaId(9)).thenReturn(List.of(table(4), table(6)));

        TableAreaReadinessService.Readiness readiness = service.evaluate(area);

        assertTrue(readiness.bookingReady());
        assertEquals(2, readiness.usableTableCount());
        assertEquals(10, readiness.totalTableCapacity());
    }

    @Test
    void inactiveAreaIsNeverBookingReady() {
        TableArea area = area(10, 10, AreaType.PRIVATE_ROOM, "INACTIVE");

        TableAreaReadinessService.Readiness readiness = service.evaluate(area);

        assertFalse(readiness.bookingReady());
        assertTrue(readiness.reason().contains("tạm ngưng"));
    }

    @Test
    void eventHallKeepsEventBookingAvailabilityIndependentFromDiningTables() {
        TableArea area = area(11, 200, AreaType.EVENT_HALL, "ACTIVE");

        TableAreaReadinessService.Readiness readiness = service.evaluate(area);

        assertTrue(readiness.bookingReady());
        assertEquals(0, readiness.usableTableCount());
    }

    private TableArea area(int id, int capacity, AreaType areaType, String status) {
        TableArea area = new TableArea();
        area.setId(id);
        area.setCapacity(capacity);
        area.setAreaType(areaType);
        area.setStatus(status);
        return area;
    }

    private RestaurantTable table(int capacity) {
        RestaurantTable table = new RestaurantTable();
        table.setMaxCapacity(capacity);
        table.setCapacity(capacity);
        table.setActive(true);
        return table;
    }
}
