package poly.edu.quanlynhahang.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.AiFaqExample;
import java.util.List;
public interface AiFaqExampleRepository extends JpaRepository<AiFaqExample,Long> { List<AiFaqExample> findByEnabledTrue(); }
