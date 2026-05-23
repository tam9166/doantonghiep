package poly.edu.quanlynhahang.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Post;

public interface PostRepository extends JpaRepository<Post, Integer> {
    List<Post> findByTypeAndActiveOrderByCreateDateDesc(String type, Boolean active);
    List<Post> findByActiveOrderByCreateDateDesc(Boolean active);
    List<Post> findAllByOrderByCreateDateDesc();
}
