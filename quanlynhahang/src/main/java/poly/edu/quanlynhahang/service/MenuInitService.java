package poly.edu.quanlynhahang.service;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.CategoryRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;

@Service
public class MenuInitService implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // Nếu bảng Danh mục chưa có dữ liệu thì mới thêm vào
        if (categoryRepository.count() == 0) {
            
            // 1. Tạo các danh mục
            Category cat1 = new Category();
            cat1.setName("Món chính");
            
            Category cat2 = new Category();
            cat2.setName("Đồ uống");

            categoryRepository.saveAll(Arrays.asList(cat1, cat2));

            // 2. Tạo các món ăn thuộc về danh mục tương ứng
            Product p1 = new Product();
            p1.setName("Phở bò Kobe");
            p1.setPrice(65000.0);
            p1.setDescription("Phở bò Kobe thượng hạng nước trong");
            p1.setCategory(cat1);

            Product p2 = new Product();
            p2.setName("Cơm rang dưa bò");
            p2.setPrice(45000.0);
            p2.setDescription("Cơm rang hạt giòn, dưa bò xào đậm vị");
            p2.setCategory(cat1);

            Product p3 = new Product();
            p3.setName("Coca Cola");
            p3.setPrice(15000.0);
            p3.setDescription("Nước ngọt có ga lon 330ml");
            p3.setCategory(cat2);

            productRepository.saveAll(Arrays.asList(p1, p2, p3));

            System.out.println(">> Đã khởi tạo dữ liệu Thực đơn (Món ăn & Danh mục) mẫu thành công!");
        }
    }
}