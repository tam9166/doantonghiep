package poly.edu.quanlynhahang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.service.RestaurantSettingsService;
import poly.edu.quanlynhahang.service.RestaurantBusinessHoursService;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class RestaurantSettingsController {
    private final RestaurantSettingsService settings;
    private final RestaurantBusinessHoursService businessHours;

    public RestaurantSettingsController(RestaurantSettingsService settings,
                                        RestaurantBusinessHoursService businessHours) {
        this.settings = settings;
        this.businessHours = businessHours;
    }

    @GetMapping("/public")
    public ResponseEntity<?> publicSettings() {
        return ResponseEntity.ok(Map.of(
                "largePartyThreshold", settings.largePartyThreshold(),
                "restaurantMaxCapacity", settings.maxCapacity(),
                "openingTime", businessHours.getOpeningTime().toString(),
                "closingTime", businessHours.getClosingTime().toString(),
                "lastOrderTime", businessHours.getLastOrderTime().toString(),
                "timeZone", "Asia/Ho_Chi_Minh"));
    }

    @GetMapping("/admin")
    public ResponseEntity<?> adminSettings() {
        return publicSettings();
    }

    @PutMapping("/admin")
    public ResponseEntity<?> updateSettings(@RequestBody CapacitySettingsRequest request) {
        settings.updateCapacitySettings(request.largePartyThreshold(), request.restaurantMaxCapacity());
        return publicSettings();
    }

    public record CapacitySettingsRequest(int largePartyThreshold, int restaurantMaxCapacity) {}
}
