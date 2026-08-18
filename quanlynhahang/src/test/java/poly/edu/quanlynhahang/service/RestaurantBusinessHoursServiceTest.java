package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for RestaurantBusinessHoursService including cross-midnight scenarios.
 */
class RestaurantBusinessHoursServiceTest {
    
    private RestaurantBusinessHoursService service;
    
    @BeforeEach
    void setUp() {
        service = new RestaurantBusinessHoursService();
        // Use default values from annotation defaults (09:00 - 22:00, last order 21:30)
        service.openingTime = LocalTime.of(9, 0);
        service.closingTime = LocalTime.of(22, 0);
        service.lastOrderTime = LocalTime.of(21, 30);
    }
    
    @Test
    void isOpenDuringNormalOperatingHours() {
        assertTrue(service.isOpen(LocalTime.of(10, 0)));
        assertTrue(service.isOpen(LocalTime.of(12, 0)));
        assertTrue(service.isOpen(LocalTime.of(18, 0)));
        assertTrue(service.isOpen(LocalTime.of(21, 0)));
    }
    
    @Test
    void isClosedBeforeOpeningAndAfterClosing() {
        assertFalse(service.isOpen(LocalTime.of(8, 0)));
        assertFalse(service.isOpen(LocalTime.of(7, 0)));
        assertFalse(service.isOpen(LocalTime.of(22, 0)));
        assertFalse(service.isOpen(LocalTime.of(23, 0)));
        assertFalse(service.isOpen(LocalTime.of(0, 0)));
    }
    
    @Test
    void acceptsOrdersWithinLastOrderTime() {
        assertTrue(service.acceptsOrders(LocalTime.of(10, 0)));
        assertTrue(service.acceptsOrders(LocalTime.of(18, 0)));
        assertTrue(service.acceptsOrders(LocalTime.of(21, 0)));
        assertTrue(service.acceptsOrders(LocalTime.of(21, 29)));
    }
    
    @Test
    void rejectsOrdersAfterLastOrderTime() {
        assertFalse(service.acceptsOrders(LocalTime.of(21, 30)));
        assertFalse(service.acceptsOrders(LocalTime.of(21, 31)));
        assertFalse(service.acceptsOrders(LocalTime.of(22, 0)));
    }
    
    @Test
    void allowsLateDiningPolicy() {
        // Customer arrives at 21:30 (last order time), stays until past closing
        assertTrue(service.allowsLateDining(LocalTime.of(21, 30), 120));
        
        // Customer arrives before last order but after closing
        assertFalse(service.allowsLateDining(LocalTime.of(22, 0), 60));
    }
    
    @Test
    void handlesCrossMidnightScenario() {
        // Simulate overnight restaurant (e.g., 23:00 - 06:00 next day)
        service.openingTime = LocalTime.of(23, 0);
        service.closingTime = LocalTime.of(6, 0);
        
        // Should be open late night
        assertTrue(service.isOpen(LocalTime.of(23, 0)));
        assertTrue(service.isOpen(LocalTime.of(0, 0)));
        assertTrue(service.isOpen(LocalTime.of(3, 0)));
        assertTrue(service.isOpen(LocalTime.of(5, 30)));
        
        // Should be closed during day
        assertFalse(service.isOpen(LocalTime.of(12, 0)));
        assertFalse(service.isOpen(LocalTime.of(18, 0)));
    }
    
    @Test
    void providesFormattedHours() {
        assertEquals("09:00 - 22:00", service.getFormattedHours());
    }
    
    @Test
    void returnsCorrectTimeAccessors() {
        assertEquals(LocalTime.of(9, 0), service.getOpeningTime());
        assertEquals(LocalTime.of(22, 0), service.getClosingTime());
        assertEquals(LocalTime.of(21, 30), service.getLastOrderTime());
    }
}
