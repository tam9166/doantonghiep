package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class MenuPreorderItemResponse {
    private Integer id;
    private String nameVi;
    private String nameEn;
    private String categoryNameVi;
    private String categoryNameEn;
    private String descriptionVi;
    private String descriptionEn;
    private BigDecimal price;
    private String image;
    private Boolean available;
}
