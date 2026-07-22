package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Review;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReviewRepository;
import poly.edu.quanlynhahang.entity.PointsEventType;
import poly.edu.quanlynhahang.dto.ReviewCreateRequest;
import poly.edu.quanlynhahang.dto.ReviewResponse;
import poly.edu.quanlynhahang.service.PointsLedgerService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    @Autowired
    private PointsLedgerService pointsLedgerService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getReviewsByProduct(@PathVariable Integer productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreateDateDesc(productId);
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        return ResponseEntity.ok(Map.of(
            "reviews", reviews.stream().map(ReviewResponse::from).toList(),
            "averageRating", avg != null ? avg : 0.0
        ));
    }

    @PostMapping("/product/{productId}")
    @Transactional
    public ResponseEntity<?> addReview(@PathVariable Integer productId,
                                       @Valid @RequestBody ReviewCreateRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Account> accountOpt = accountRepository.findById(currentUsername);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (!accountOpt.isPresent() || !productOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Lỗi thông tin!");
        }

        Object ratingValue = request.rating();
        if (!(ratingValue instanceof Number) || ((Number) ratingValue).intValue() < 1
                || ((Number) ratingValue).intValue() > 5) {
            return ResponseEntity.badRequest().body("Điểm đánh giá phải từ 1 đến 5");
        }
        if (!orderDetailRepository.existsCompletedPaidPurchase(currentUsername, productId)) {
            return ResponseEntity.status(403).body("Chỉ khách đã mua và hoàn tất đơn mới được đánh giá món này");
        }

        Optional<Review> existing = reviewRepository.findByAccountUsernameAndProductId(currentUsername, productId);
        if (existing.isPresent()) {
            Review review = existing.get();
            review.setRating(((Number) ratingValue).intValue());
            review.setComment(request.comment() == null ? null : request.comment().trim());
            review.setCreateDate(new Date());
            return ResponseEntity.ok(Map.of(
                    "review", ReviewResponse.from(reviewRepository.save(review)),
                    "message", "Đã cập nhật đánh giá. Điểm thưởng không được cộng lại."
            ));
        }

        Review review = new Review();
        review.setAccount(accountOpt.get());
        review.setProduct(productOpt.get());
        review.setRating(((Number) ratingValue).intValue());
        review.setComment(request.comment() == null ? null : request.comment().trim());
        review.setCreateDate(new Date());

        Review saved = reviewRepository.save(review);
        pointsLedgerService.credit(
                currentUsername,
                PointsEventType.REVIEW,
                "REVIEW:" + saved.getId(),
                2,
                "Thưởng điểm đánh giá sản phẩm #" + productId);

        return ResponseEntity.ok(Map.of(
            "review", ReviewResponse.from(saved),
            "message", "Cảm ơn bạn đã đánh giá! Bạn được cộng +2 Điểm thưởng."
        ));
    }
}
