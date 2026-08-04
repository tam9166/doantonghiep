package poly.edu.quanlynhahang.dto;

import java.util.Date;

import poly.edu.quanlynhahang.entity.Application;

public record ApplicationResponse(
        Integer id,
        String fullname,
        String phone,
        String email,
        String message,
        Integer postId,
        String cvFile,
        Date createDate) {

    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(), application.getFullname(), application.getPhone(), application.getEmail(),
                application.getMessage(), application.getPostId(), application.getCvFile(), application.getCreateDate());
    }
}
