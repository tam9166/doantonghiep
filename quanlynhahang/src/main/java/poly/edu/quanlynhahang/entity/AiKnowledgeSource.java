package poly.edu.quanlynhahang.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_knowledge_sources")
public class AiKnowledgeSource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 30)
    private String type = "TEXT";
    @Column(length = 255)
    private String originalFilename;
    @Column(length = 100)
    private String mimeType;
    @Column(nullable = false, length = 20)
    private String processingStatus = "READY";
    @Column(length = 500)
    private String processingError;
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;
    @Column(nullable = false)
    private Boolean enabled = true;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
}
