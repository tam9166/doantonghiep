package poly.edu.quanlynhahang.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name="ai_faq_examples")
public class AiFaqExample {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=1000) private String question;
    @Column(nullable=false, columnDefinition="nvarchar(max)") private String idealAnswer;
    @Column(nullable=false) private Boolean enabled = true;
}
