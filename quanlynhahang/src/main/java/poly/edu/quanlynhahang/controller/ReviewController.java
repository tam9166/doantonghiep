package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Review;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReviewRepository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getReviewsByProduct(@PathVariable Integer productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreateDateDesc(productId);
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        return ResponseEntity.ok(Map.of(
            "reviews", reviews,
            "averageRating", avg != null ? avg : 0.0
        ));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<?> addReview(@PathVariable Integer productId, @RequestBody Map<String, Object> payload) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Account> accountOpt = accountRepository.findById(currentUsername);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (!accountOpt.isPresent() || !productOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Lỗi thông tin!");
        }

        Review review = new Review();
        review.setAccount(accountOpt.get());
        review.setProduct(productOpt.get());
        review.setRating((Integer) payload.get("rating"));
        review.setComment((String) payload.get("comment"));
        review.setCreateDate(new Date());

        reviewRepository.save(review);
        
        // Cộng 2 điểm thưởng cho việc đánh giá
        Account acc = accountOpt.get();
        if (acc.getPoints() == null) acc.setPoints(0);
        acc.setPoints(acc.getPoints() + 2);
        
        // Xét thăng hạng
        if (acc.getPoints() >= 2000) acc.setMembershipTier("Kim Cương");
        else if (acc.getPoints() >= 1000) acc.setMembershipTier("Vàng");
        else if (acc.getPoints() >= 500) acc.setMembershipTier("Bạc");
        else acc.setMembershipTier("Đồng");
        
        accountRepository.save(acc);

        return ResponseEntity.ok(Map.of(
            "review", review,
            "message", "Cảm ơn bạn đã đánh giá! Bạn được cộng +2 Điểm thưởng."
        ));
    }
}
