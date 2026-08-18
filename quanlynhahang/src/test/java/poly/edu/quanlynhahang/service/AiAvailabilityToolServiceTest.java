package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test for real availability checking with table and reservation conflicts.
 */
class AiAvailabilityToolServiceTest {
    
    private RestaurantCapacityService capacityService;
    private RestaurantTableRepository tableRepository;
    private TableAreaRepository areaRepository;
    private ReservationRepository reservationRepository;
    private AiAvailabilityToolService service;
    
    @BeforeEach
    void setUp() {
        capacityService = mock(RestaurantCapacityService.class);
        tableRepository = mock(RestaurantTableRepository.class);
        areaRepository = mock(TableAreaRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        
        service = new AiAvailabilityToolService(
            capacityService, tableRepository, areaRepository, reservationRepository);
    }
    
    @Test
    void returnsAvailableWhenCapacityExists() {
        var date = LocalDate.of(2026, 8, 19);
        var time = LocalTime.of(19, 0);
        int guestCount = 4;
        
        // Mock capacity check - available
        when(capacityService.checkCapacity(date, time, 120, guestCount))
            .thenReturn(new RestaurantCapacityService.CapacitySnapshot(true, 500, 300, 200, guestCount, date, time, 120));
        
        // Mock tables - some available
        RestaurantTable table = new RestaurantTable();
        table.setId(1);
        table.setName("Table 1");
        table.setCapacity(4);
        table.setMaxCapacity(6);
        table.setActive(true);
        table.setAreaId(1);
        
        when(tableRepository.findByActiveTrueOrderByAreaIdAscIdAsc())
            .thenReturn(List.of(table));
            
        // No reservations conflict
        when(reservationRepository.findByTableIdAndReservationDateAndReservationStatusIn(anyInt(), eq(date), anySet()))
            .thenReturn(List.of());
            
        // Areas available
        TableArea area = new TableArea();
        area.setId(1);
        area.setNameVi("Sân thường");
        area.setStatus("ACTIVE");
        area.setMinGuestCount(1);
        area.setMaxGuestCount(10);
        
        when(areaRepository.findByStatus("ACTIVE"))
            .thenReturn(List.of(area));
        
        var result = service.getAvailability(date, time, guestCount);
        
        assertTrue(result.available());
        assertEquals(500, result.maximumCapacity());
        assertEquals(200, result.remainingCapacity());
        assertFalse(result.availableTables().isEmpty());
        assertFalse(result.suitableAreas().isEmpty());
    }
    
    @Test
    void returnsNotAvailableWhenNoCapacity() {
        var date = LocalDate.of(2026, 8, 19);
        var time = LocalTime.of(19, 0);
        int guestCount = 100; // More than max capacity
        
        // Mock capacity check - not available
        when(capacityService.checkCapacity(date, time, 120, guestCount))
            .thenReturn(new RestaurantCapacityService.CapacitySnapshot(false, 500, 490, 10, guestCount, date, time, 120));
        
        var result = service.getAvailability(date, time, guestCount);
        
        assertFalse(result.available());
        assertEquals(10, result.remainingCapacity());
    }
    
    @Test
    void detectsTableConflictWithExistingReservation() {
        var date = LocalDate.of(2026, 8, 19);
        var time = LocalTime.of(19, 0);
        int guestCount = 2;
        
        when(capacityService.checkCapacity(date, time, 120, guestCount))
            .thenReturn(new RestaurantCapacityService.CapacitySnapshot(true, 500, 200, 300, guestCount, date, time, 120));
        
        RestaurantTable table = new RestaurantTable();
        table.setId(1);
        table.setName("Table 1");
        table.setCapacity(2);
        table.setMaxCapacity(4);
        table.setActive(true);
        table.setAreaId(1);
        
        when(tableRepository.findByActiveTrueOrderByAreaIdAscIdAsc())
            .thenReturn(List.of(table));
            
        // Existing reservation at same time (conflict)
        Reservation existingReservation = new Reservation();
        existingReservation.setArrivalTime(LocalTime.of(19, 0));
        existingReservation.setExpectedDurationMinutes(120);
        
        when(reservationRepository.findByTableIdAndReservationDateAndReservationStatusIn(
                eq(1), eq(date), anySet()))
            .thenReturn(List.of(existingReservation));
            
        TableArea area = new TableArea();
        area.setId(1);
        area.setNameVi("Phòng VIP");
        area.setStatus("ACTIVE");
        
        when(areaRepository.findByStatus("ACTIVE"))
            .thenReturn(List.of(area));
        
        var result = service.getAvailability(date, time, guestCount);
        
        // Should still show available at capacity level but table might be filtered
        assertTrue(result.availableTables().isEmpty());
    }
    
    @Test
    void filtersTablesByGuestCount() {
        var date = LocalDate.of(2026, 8, 19);
        var time = LocalTime.of(19, 0);
        int guestCount = 8;
        
        when(capacityService.checkCapacity(date, time, 120, guestCount))
            .thenReturn(new RestaurantCapacityService.CapacitySnapshot(true, 500, 200, 300, guestCount, date, time, 120));
        
        RestaurantTable smallTable = new RestaurantTable();
        smallTable.setId(1);
        smallTable.setName("Small Table");
        smallTable.setCapacity(2);
        smallTable.setMaxCapacity(4);
        smallTable.setActive(true);
        smallTable.setAreaId(1);
        
        RestaurantTable largeTable = new RestaurantTable();
        largeTable.setId(2);
        largeTable.setName("Large Table");
        largeTable.setCapacity(8);
        largeTable.setMaxCapacity(10);
        largeTable.setActive(true);
        largeTable.setAreaId(1);
        
        when(tableRepository.findByActiveTrueOrderByAreaIdAscIdAsc())
            .thenReturn(List.of(smallTable, largeTable));
            
        when(reservationRepository.findByTableIdAndReservationDateAndReservationStatusIn(anyInt(), eq(date), anySet()))
            .thenReturn(List.of());
            
        TableArea area = new TableArea();
        area.setId(1);
        area.setNameVi("Sân thường");
        area.setStatus("ACTIVE");
        area.setMinGuestCount(1);
        area.setMaxGuestCount(20);
        
        when(areaRepository.findByStatus("ACTIVE"))
            .thenReturn(List.of(area));
        
        var result = service.getAvailability(date, time, guestCount);
        
        // Only large table should be returned (small table can't fit 8 guests)
        assertEquals(1, result.availableTables().size());
        assertEquals("Large Table", result.availableTables().getFirst().name());
    }
}
