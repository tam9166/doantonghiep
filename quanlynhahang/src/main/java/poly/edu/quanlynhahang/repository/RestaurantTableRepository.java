package poly.edu.quanlynhahang.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.RestaurantTable;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {
    Optional<RestaurantTable> findByName(String name);

    // NOTE: Khóa ghi bảo vệ bước đọc-kiểm tra-gán bàn trước các yêu cầu đồng thời.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id = :id")
    Optional<RestaurantTable> findLockedById(@Param("id") Integer id);

    // NOTE: Danh sách ID được sắp thứ tự để các giao dịch khóa nhiều bàn theo cùng một trình tự.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id in :ids order by t.id")
    List<RestaurantTable> findLockedByIdIn(@Param("ids") Collection<Integer> ids);

    // NOTE: Tự động gán bàn khóa các bàn đang hoạt động trong đúng khu vực trước khi chọn ứng viên.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.areaId = :areaId and t.active = true order by t.displayOrder, t.name")
    List<RestaurantTable> findLockedActiveByAreaId(@Param("areaId") Integer areaId);

    List<RestaurantTable> findByActiveTrueOrderByAreaIdAscIdAsc();

    @Query("select t from RestaurantTable t left join fetch t.area order by t.floor, t.areaId, t.displayOrder, t.name")
    List<RestaurantTable> findAllWithArea();

    @Query("select t from RestaurantTable t "
            + "where t.active = true or t.active is null "
            + "order by t.areaId asc, t.id asc")
    List<RestaurantTable> findOperationalTables();

    @Query("select t from RestaurantTable t "
            + "where t.areaId = :areaId and (t.active = true or t.active is null) "
            + "order by t.displayOrder asc, t.id asc")
    List<RestaurantTable> findOperationalTablesByAreaId(@Param("areaId") Integer areaId);
}
