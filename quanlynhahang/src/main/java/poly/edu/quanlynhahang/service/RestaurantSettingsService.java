package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.quanlynhahang.entity.RestaurantSetting;
import poly.edu.quanlynhahang.repository.RestaurantSettingRepository;

@Service
public class RestaurantSettingsService {
    public static final String LARGE_PARTY_THRESHOLD = "large_party_threshold";
    public static final String MAX_CAPACITY = "restaurant_max_capacity";

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

    @Transactional
    public void updateCapacitySettings(int largePartyThreshold, int maxCapacity) {
        if (largePartyThreshold < 2 || largePartyThreshold > 100) {
            throw new IllegalArgumentException("Ngưỡng đoàn đông phải từ 2 đến 100 khách");
        }
        if (maxCapacity < largePartyThreshold || maxCapacity > 10000) {
            throw new IllegalArgumentException("Sức chứa phải lớn hơn hoặc bằng ngưỡng đoàn đông và không quá 10.000");
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
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }).orElse(fallback);
    }
}
