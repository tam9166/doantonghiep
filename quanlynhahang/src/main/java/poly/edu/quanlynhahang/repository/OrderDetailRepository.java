package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.OrderDetail;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
}