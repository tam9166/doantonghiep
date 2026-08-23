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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id = :id")
    Optional<RestaurantTable> findLockedById(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id in :ids order by t.id")
    List<RestaurantTable> findLockedByIdIn(@Param("ids") Collection<Integer> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.areaId = :areaId and t.active = true order by t.displayOrder, t.name")
    List<RestaurantTable> findLockedActiveByAreaId(@Param("areaId") Integer areaId);

    List<RestaurantTable> findByActiveTrueOrderByAreaIdAscIdAsc();

    @Query("select t from RestaurantTable t "
            + "where t.active = true or t.active is null "
            + "order by t.areaId asc, t.id asc")
    List<RestaurantTable> findOperationalTables();
}
