package poly.edu.quanlynhahang.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.PostUpsertRequest;
import poly.edu.quanlynhahang.dto.PostResponse;

import poly.edu.quanlynhahang.entity.Post;
import poly.edu.quanlynhahang.repository.PostRepository;
@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public class AdminPostController {

    @Autowired
    private PostRepository postRepository;

    // Lấy tất cả bài (kể cả bài ẩn) - dành cho Admin
    @GetMapping
    public ResponseEntity<?> getAllPosts() {
        return ResponseEntity.ok(postRepository.findAllByOrderByCreateDateDesc().stream()
                .map(PostResponse::from)
                .toList());
    }

    // Tạo bài mới
    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody PostUpsertRequest request) {
        Post post = new Post();
        applyRequest(post, request);
        post.setCreateDate(new Date());
        return ResponseEntity.ok(PostResponse.from(postRepository.save(post)));
    }

    // Cập nhật bài
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Integer id, @Valid @RequestBody PostUpsertRequest request) {
        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy bài đăng!");
        }
        applyRequest(post, request);
        return ResponseEntity.ok(PostResponse.from(postRepository.save(post)));
    }

    private void applyRequest(Post post, PostUpsertRequest request) {
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setImage(request.image() == null ? null : request.image().trim());
        post.setType(request.type() == null ? "NEWS" : request.type());
        post.setActive(request.active() == null || request.active());
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
