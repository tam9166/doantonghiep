package poly.edu.quanlynhahang.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Centralized restaurant business hours service.
 * Provides validation and parsing of operating hours.
 */
@Service
public class RestaurantBusinessHoursService {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    @Value("${restaurant.info.opening-time:09:00}")
    private String openingTimeStr;
    
    @Value("${restaurant.info.closing-time:22:00}")
    private String closingTimeStr;
    
    @Value("${restaurant.info.last-order-time:21:30}")
    private String lastOrderTimeStr;
    
    private LocalTime openingTime;
    private LocalTime closingTime;
    private LocalTime lastOrderTime;
    
    /**
     * Initialize time parsing from config values after bean creation.
     */
    @PostConstruct
    public void init() {
        this.openingTime = parseTime(openingTimeStr);
        this.closingTime = parseTime(closingTimeStr);
        this.lastOrderTime = parseTime(lastOrderTimeStr);
    }
    
    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(
                "Invalid time format in configuration: " + timeStr + 
                ". Expected format: HH:mm", e);
        }
    }
    
    /**
     * Check if a given time is within business hours.
     * Handles both normal day scenarios and overnight operations.
     */
    public boolean isOpen(LocalTime time) {
        // Normal case: opening before closing (e.g., 09:00 - 22:00)
        if (!openingTime.isAfter(closingTime)) {
            return !time.isBefore(openingTime) && time.isBefore(closingTime);
        }
        
        // Overnight case: closing is next day (e.g., 23:00 - 06:00)
        return !time.isBefore(openingTime) || time.isBefore(closingTime);
    }
    
    /**
     * Check if an order can be placed at the given time.
     * Orders must be placed before last order time.
     */
    public boolean acceptsOrders(LocalTime time) {
        if (!isOpen(time)) {
            return false;
        }
        if (!openingTime.isAfter(closingTime) || !lastOrderTime.isBefore(openingTime)) {
            return time.isBefore(lastOrderTime);
        }
        // Overnight service with a cutoff after midnight, e.g. 23:00-06:00
        // and last order at 05:30.
        return !time.isBefore(openingTime) || time.isBefore(lastOrderTime);
    }
    
    /**
     * Get formatted business hours string for display.
     */
    public String getFormattedHours() {
        return openingTime.format(TIME_FORMATTER) + " - " + closingTime.format(TIME_FORMATTER);
    }
    
    /**
     * Get opening time.
     */
    public LocalTime getOpeningTime() {
        return openingTime;
    }
    
    /**
     * Get closing time.
     */
    public LocalTime getClosingTime() {
        return closingTime;
    }
    
    /**
     * Get last order time.
     */
    public LocalTime getLastOrderTime() {
        return lastOrderTime;
    }
    
    /**
     * Check if dining extends past closing time (late dining policy).
     */
    public boolean allowsLateDining(LocalTime arrivalTime, int durationMinutes) {
        LocalTime expectedEnd = arrivalTime.plusMinutes(durationMinutes);
        // Allow if customer arrives before last order time but stays past closing
        return !arrivalTime.isAfter(lastOrderTime) && expectedEnd.isAfter(closingTime);
    }
}
