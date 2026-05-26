package poly.edu.quanlynhahang.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private poly.edu.quanlynhahang.repository.ReviewRepository reviewRepository;

    // API lấy tất cả món ăn
    @GetMapping
    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            Double avg = reviewRepository.getAverageRatingByProductId(p.getId());
            p.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        }
        return products;
    }

    // API lấy món ăn theo ID của danh mục (Ví dụ: truyền số 2 vào sẽ lấy ra Đồ uống)
    @GetMapping("/category/{categoryId}")
    public List<Product> getProductsByCategory(@PathVariable Integer categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);
        for (Product p : products) {
            Double avg = reviewRepository.getAverageRatingByProductId(p.getId());
            p.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        }
        return products;
    }
}