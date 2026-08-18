package poly.edu.quanlynhahang.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.AiInteractionLog;
import java.util.List;
import java.util.Optional;
public interface AiInteractionLogRepository extends JpaRepository<AiInteractionLog,Long>{List<AiInteractionLog> findTop200ByOrderByCreatedAtDesc(); Optional<AiInteractionLog> findByIdAndSessionId(Long id,String sessionId);}
