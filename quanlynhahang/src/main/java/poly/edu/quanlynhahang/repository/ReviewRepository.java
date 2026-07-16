package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.Review;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProductIdOrderByCreateDateDesc(Integer productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Integer productId);

    @Query("""
            SELECT r.product.id AS productId, AVG(r.rating) AS averageRating
            FROM Review r
            WHERE r.product.id IN :productIds
            GROUP BY r.product.id
            """)
    List<ProductRatingSummary> getAverageRatingsByProductIds(
            @Param("productIds") Collection<Integer> productIds);

    Optional<Review> findByAccountUsernameAndProductId(String username, Integer productId);

    interface ProductRatingSummary {
        Integer getProductId();

        Double getAverageRating();
    }
}
