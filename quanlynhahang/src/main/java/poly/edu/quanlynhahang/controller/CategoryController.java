package poly.edu.quanlynhahang.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.repository.CategoryRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // Lấy danh sách danh mục (Ai cũng xem được)
    @GetMapping
    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    // Thêm danh mục mới (Chỉ Admin)
    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryRepository.save(category));
    }
}