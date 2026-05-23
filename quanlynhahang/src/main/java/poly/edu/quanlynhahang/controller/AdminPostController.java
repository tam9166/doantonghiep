package poly.edu.quanlynhahang.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Post;
import poly.edu.quanlynhahang.repository.PostRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public class AdminPostController {

    @Autowired
    private PostRepository postRepository;

    // Lấy tất cả bài (kể cả bài ẩn) - dành cho Admin
    @GetMapping
    public ResponseEntity<?> getAllPosts() {
        return ResponseEntity.ok(postRepository.findAllByOrderByCreateDateDesc());
    }

    // Tạo bài mới
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Post post) {
        post.setCreateDate(new Date());
        return ResponseEntity.ok(postRepository.save(post));
    }

    // Cập nhật bài
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Integer id, @RequestBody Post postDetails) {
        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy bài đăng!");
        }
        post.setTitle(postDetails.getTitle());
        post.setContent(postDetails.getContent());
        post.setImage(postDetails.getImage());
        post.setType(postDetails.getType());
        post.setActive(postDetails.getActive());
        return ResponseEntity.ok(postRepository.save(post));
    }

    // Xóa bài
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Integer id) {
        if (!postRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Không tìm thấy bài đăng!");
        }
        postRepository.deleteById(id);
        return ResponseEntity.ok("Đã xóa bài đăng!");
    }
}
