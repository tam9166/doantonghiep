package poly.edu.quanlynhahang.service;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.CategoryRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;

@Service
@Profile("demo")
@Order(10)
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
            p1.setImage("https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?auto=format&fit=crop&w=600&q=80");
            p1.setCostPrice(35000.0);
            p1.setTaxRate(8.0);
            p1.setAvailable(true);
            p1.setStatus(true);
            p1.setCategory(cat1);

            Product p2 = new Product();
            p2.setName("Cơm rang dưa bò");
            p2.setPrice(45000.0);
            p2.setDescription("Cơm rang hạt giòn, dưa bò xào đậm vị");
            p2.setImage("https://images.unsplash.com/photo-1603133872878-684f208fb84b?auto=format&fit=crop&w=600&q=80");
            p2.setCostPrice(23000.0);
            p2.setTaxRate(8.0);
            p2.setAvailable(true);
            p2.setStatus(true);
            p2.setCategory(cat1);

            Product p3 = new Product();
            p3.setName("Coca Cola");
            p3.setPrice(15000.0);
            p3.setDescription("Nước ngọt có ga lon 330ml");
            p3.setImage("https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=600&q=80");
            p3.setCostPrice(8000.0);
            p3.setTaxRate(8.0);
            p3.setAvailable(true);
            p3.setStatus(true);
            p3.setCategory(cat2);

            productRepository.saveAll(Arrays.asList(p1, p2, p3));

            System.out.println(">> Đã khởi tạo dữ liệu Thực đơn (Món ăn & Danh mục) mẫu thành công!");
        }
    }
}
