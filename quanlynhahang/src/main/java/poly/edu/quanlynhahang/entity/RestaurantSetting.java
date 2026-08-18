package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Data
@Entity
@Table(name = "restaurant_settings")
public class RestaurantSetting {
    @Id
    @Column(name = "setting_key", length = 80)
    private String key;

    @Column(name = "setting_value", columnDefinition = "nvarchar(1000)", nullable = false)
    private String value;

    @Column(name = "description", columnDefinition = "nvarchar(500)")
    private String description;

    @Version
    @Column(nullable = false)
    private Long version = 0L;
}
