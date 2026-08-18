package poly.edu.quanlynhahang.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Entity @Table(name="ai_interaction_logs")
public class AiInteractionLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(length=80) private String sessionId;
 @Column(length=40) private String requestType;
 @Column(length=4000) private String question;
 @Column(columnDefinition="nvarchar(max)") private String response;
 @Column(length=40) private String source;
 private Boolean helpful;
 @Column(length=1000) private String feedbackComment;
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
}
