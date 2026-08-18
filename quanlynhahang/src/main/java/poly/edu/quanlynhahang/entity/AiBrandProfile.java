package poly.edu.quanlynhahang.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name = "ai_brand_profile")
public class AiBrandProfile {
    @Id private Integer id = 1;
    @Column(length=150) private String brandName = "Mộc Vị Restaurant";
    @Column(length=300) private String addressing = "Xưng Nhà hàng và gọi khách là Quý khách";
    @Column(length=500) private String toneOfVoice = "Thân thiện, lịch sự, ngắn gọn";
    @Column(columnDefinition="nvarchar(max)") private String preferredWords;
    @Column(columnDefinition="nvarchar(max)") private String forbiddenWords;
    @Column(columnDefinition="nvarchar(max)") private String adviceStyle;
    @Column(columnDefinition="nvarchar(max)") private String unknownAnswerRule = "Nói rõ chưa có thông tin và mời khách liên hệ nhân viên";
    @Column(columnDefinition="nvarchar(max)") private String noFabricationRule = "Không bịa giá, món ăn, bàn trống hoặc chính sách";
    @Column(columnDefinition="nvarchar(max)") private String handoffRule = "Chuyển nhân viên khi yêu cầu cần xác nhận dữ liệu động hoặc khi khách yêu cầu";
}
