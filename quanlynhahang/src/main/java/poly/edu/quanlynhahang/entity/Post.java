package poly.edu.quanlynhahang.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Data
@Entity
@Table(name = "Posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "nvarchar(300)", nullable = false)
    private String title;

    @Column(columnDefinition = "nvarchar(MAX)")
    private String content;

    @Column(length = 500)
    private String image;

    // NEWS hoặc RECRUITMENT
    @Column(length = 20, nullable = false)
    private String type = "NEWS";

    private Integer likes = 0;

    private Boolean active = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date")
    private Date createDate = new Date();
}
