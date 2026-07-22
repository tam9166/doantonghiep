package poly.edu.quanlynhahang.dto;

import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Review;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewResponsePrivacyTest {

    @Test
    void reviewResponseExcludesAccountCredentialsAndProductInternals() throws Exception {
        Account account = new Account();
        account.setUsername("customer-a");
        account.setFullname("Customer A");
        account.setPassword("bcrypt-hash");
        account.setTokenVersion(3L);
        Product product = new Product();
        product.setId(7);
        product.setCostPrice(10_000.0);
        Review review = new Review();
        review.setAccount(account);
        review.setProduct(product);
        review.setRating(5);

        String json = new ObjectMapper().writeValueAsString(ReviewResponse.from(review));

        assertTrue(json.contains("Customer A"));
        assertFalse(json.contains("bcrypt-hash"));
        assertFalse(json.contains("tokenVersion"));
        assertFalse(json.contains("costPrice"));
    }
}
