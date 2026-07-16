package poly.edu.quanlynhahang.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.repository.CategoryRepository;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> addCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    // Sửa danh mục
    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateCategory(@org.springframework.web.bind.annotation.PathVariable Integer id, @RequestBody Category categoryDetails) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(categoryDetails.getName());
            return ResponseEntity.ok(categoryRepository.save(category));
        }).orElse(ResponseEntity.badRequest().body(null));
    }

    // Xóa danh mục
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deleteCategory(@org.springframework.web.bind.annotation.PathVariable Integer id) {
        if (!categoryRepository.existsById(id)) return ResponseEntity.badRequest().body("Không tìm thấy danh mục!");
        categoryRepository.deleteById(id);
        return ResponseEntity.ok("Xóa thành công!");
    }
}
