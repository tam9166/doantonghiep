package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Review;

import java.util.Date;

public record ReviewResponse(
        Integer id,
        Integer productId,
        String reviewerName,
        Integer rating,
        String comment,
        Date createDate) {

    public static ReviewResponse from(Review review) {
        Account account = review.getAccount();
        String reviewerName = account == null ? "Khach hang"
                : (account.getFullname() == null || account.getFullname().isBlank()
                ? account.getUsername() : account.getFullname());
        return new ReviewResponse(review.getId(),
                review.getProduct() == null ? null : review.getProduct().getId(),
                reviewerName, review.getRating(), review.getComment(), review.getCreateDate());
    }
}
