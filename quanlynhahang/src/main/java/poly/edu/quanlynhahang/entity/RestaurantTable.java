package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "restaurant_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    private String name;
    
    private String floor;

    @Column(name = "is_occupied")
    private Integer isOccupied = 0;

    @Column(name = "has_view")
    private Boolean hasView = false;
    
    @Column(name = "reserved_time")
    private String reservedTime;

    @Column(name = "capacity", columnDefinition = "int default 4")
    private Integer capacity = 4;

    @Column(name = "view_type", columnDefinition = "nvarchar(50)")
    private String viewType;

    @Column(name = "min_capacity", columnDefinition = "int default 1")
    private Integer minCapacity = 1;

    @Column(name = "max_capacity", columnDefinition = "int default 4")
    private Integer maxCapacity = 4;

    @Column(name = "seat_count", columnDefinition = "int default 4")
    private Integer seatCount = 4;

    @Column(name = "reservation_price", precision = 18, scale = 0)
    private BigDecimal reservationPrice = BigDecimal.ZERO;

    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "position_description", columnDefinition = "nvarchar(255)")
    private String positionDescription;

    @Column(name = "is_window_seat")
    private Boolean windowSeat = false;

    @Column(name = "is_private_room")
    private Boolean privateRoom = false;

    @Column(name = "is_child_friendly")
    private Boolean childFriendly = true;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "image_url", columnDefinition = "nvarchar(500)")
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "notes", columnDefinition = "nvarchar(500)")
    private String notes;

    public RestaurantTable(Integer id, String name, String floor, Integer isOccupied, Boolean hasView,
                           String reservedTime, Integer capacity, String viewType) {
        this.id = id;
        this.name = name;
        this.floor = floor;
        this.isOccupied = isOccupied;
        this.hasView = hasView;
        this.reservedTime = reservedTime;
        this.capacity = capacity;
        this.viewType = viewType;
        this.minCapacity = 1;
        this.maxCapacity = capacity != null ? capacity : 4;
        this.seatCount = capacity != null ? capacity : 4;
        this.reservationPrice = BigDecimal.ZERO;
        this.windowSeat = Boolean.TRUE.equals(hasView);
        this.privateRoom = floor != null && floor.toLowerCase().contains("vip");
        this.childFriendly = true;
        this.active = true;
    }
}
