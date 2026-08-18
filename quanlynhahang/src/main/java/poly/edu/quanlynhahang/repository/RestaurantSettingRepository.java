package poly.edu.quanlynhahang.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import poly.edu.quanlynhahang.entity.RestaurantSetting;

import java.util.Optional;

public interface RestaurantSettingRepository extends JpaRepository<RestaurantSetting, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RestaurantSetting s where s.key = :key")
    Optional<RestaurantSetting> findLockedByKey(@Param("key") String key);
}
