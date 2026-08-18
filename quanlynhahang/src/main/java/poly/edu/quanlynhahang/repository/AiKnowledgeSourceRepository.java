package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.AiKnowledgeSource;
import java.util.List;

public interface AiKnowledgeSourceRepository extends JpaRepository<AiKnowledgeSource, Long> {
    List<AiKnowledgeSource> findByEnabledTrueOrderByUpdatedAtDesc();
}
