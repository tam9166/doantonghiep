package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "reservation_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "reservation_code", nullable = false, length = 30)
    private String reservationCode;

    @Column(name = "overall_rating", nullable = false)
    private Integer overallRating;

    @Column(name = "food_rating")
    private Integer foodRating;

    @Column(name = "service_rating")
    private Integer serviceRating;

    @Column(name = "ambience_rating")
    private Integer ambienceRating;

    @Column(name = "cleanliness_rating")
    private Integer cleanlinessRating;

    @Column(columnDefinition = "nvarchar(1000)")
    private String content;

    @Column(name = "image_url", columnDefinition = "nvarchar(500)")
    private String imageUrl;

    private Boolean anonymous = false;

    @Column(name = "admin_reply", columnDefinition = "nvarchar(1000)")
    private String adminReply;

    private Boolean hidden = false;

    @Column(name = "hidden_reason", columnDefinition = "nvarchar(500)")
    private String hiddenReason;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Column(name = "replied_at")
    private Date repliedAt;
}
