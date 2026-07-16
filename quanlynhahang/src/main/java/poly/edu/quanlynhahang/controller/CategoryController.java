package poly.edu.quanlynhahang.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.CategoryUpsertRequest;
import poly.edu.quanlynhahang.dto.CategoryResponse;

import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.repository.CategoryRepository;
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // Lấy danh sách danh mục (Ai cũng xem được)
    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    // Thêm danh mục mới (Chỉ Admin)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> addCategory(@Valid @RequestBody CategoryUpsertRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());
        return ResponseEntity.ok(CategoryResponse.from(categoryRepository.save(category)));
    }

    // Sửa danh mục
    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateCategory(@org.springframework.web.bind.annotation.PathVariable Integer id,
                                            @Valid @RequestBody CategoryUpsertRequest request) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(request.name().trim());
            return ResponseEntity.ok(CategoryResponse.from(categoryRepository.save(category)));
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
