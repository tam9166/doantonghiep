package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.quanlynhahang.entity.RestaurantSetting;
import poly.edu.quanlynhahang.repository.RestaurantSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;

@Service
public class RestaurantSettingsService {
    private static final Logger log = LoggerFactory.getLogger(RestaurantSettingsService.class);
    public static final String LARGE_PARTY_THRESHOLD = "large_party_threshold";
    public static final String MAX_CAPACITY = "restaurant_max_capacity";
    public static final String MIN_PROFIT_MARGIN_PERCENT = "min_profit_margin_percent";

    private final RestaurantSettingRepository repository;

    public RestaurantSettingsService(RestaurantSettingRepository repository) {
        this.repository = repository;
    }

    public int largePartyThreshold() {
        return positiveInt(LARGE_PARTY_THRESHOLD, 10);
    }

    public int maxCapacity() {
        return positiveInt(MAX_CAPACITY, 200);
    }

    public BigDecimal minimumProfitMarginPercent() {
        return decimalBetween(MIN_PROFIT_MARGIN_PERCENT, new BigDecimal("30.00"),
                BigDecimal.ZERO, new BigDecimal("95.00"));
    }

    @Transactional
    public void updateCapacitySettings(int largePartyThreshold, int maxCapacity) {
        if (largePartyThreshold < 2 || largePartyThreshold > 100) {
            throw new IllegalArgumentException("Ngưỡng đoàn đông phải từ 2 đến 100 khách");
        }
        int physicalMax = 500; // Hard ceiling enforced by physicalMaxCapacity()
        if (maxCapacity < largePartyThreshold || maxCapacity > physicalMax) {
            throw new IllegalArgumentException(
                "Sức chứa phải lớn hơn hoặc bằng ngưỡng đoàn đông và không vượt quá " + physicalMax);
        }
        updateValue(LARGE_PARTY_THRESHOLD, largePartyThreshold);
        updateValue(MAX_CAPACITY, maxCapacity);
    }

    private void updateValue(String key, int value) {
        RestaurantSetting setting = repository.findById(key).orElseGet(() -> {
            RestaurantSetting created = new RestaurantSetting();
            created.setKey(key);
            return created;
        });
        setting.setValue(String.valueOf(value));
        repository.save(setting);
    }

    public RestaurantSetting lockCapacitySetting() {
        return repository.findLockedByKey(MAX_CAPACITY).orElseThrow(() ->
                new IllegalStateException("Missing required restaurant setting: " + MAX_CAPACITY));
    }

    private int positiveInt(String key, int fallback) {
        return repository.findById(key).map(RestaurantSetting::getValue).map(value -> {
            try {
                int parsed = Integer.parseInt(value.trim());
                return parsed > 0 ? parsed : fallback;
            } catch (NumberFormatException exception) {
                log.warn("Restaurant setting {} has invalid integer value '{}'; using fallback {}",
                        key, value, fallback);
                return fallback;
            }
        }).orElse(fallback);
    }

    private BigDecimal decimalBetween(String key, BigDecimal fallback,
                                      BigDecimal minimum, BigDecimal maximum) {
        return repository.findById(key).map(RestaurantSetting::getValue).map(value -> {
            try {
                BigDecimal parsed = new BigDecimal(value.trim());
                return parsed.compareTo(minimum) >= 0 && parsed.compareTo(maximum) <= 0
                        ? parsed : fallback;
            } catch (NumberFormatException exception) {
                log.warn("Restaurant setting {} has invalid decimal value '{}'; using fallback {}",
                        key, value, fallback);
                return fallback;
            }
        }).orElse(fallback);
    }
}
