package poly.edu.quanlynhahang.repository;

import java.util.List;
import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    List<Application> findByPostIdOrderByCreateDateDesc(Integer postId);
    List<Application> findAllByOrderByCreateDateDesc();
    List<Application> findByCvFileIsNotNullAndCreateDateBefore(Date cutoff);
}
