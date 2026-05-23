package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.repository.PostRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    // Lấy tất cả bài đang active (cho trang chủ)
    @GetMapping
    public ResponseEntity<?> getAllActivePosts() {
        return ResponseEntity.ok(postRepository.findByActiveOrderByCreateDateDesc(true));
    }

    // Lấy bài Tin Tức
    @GetMapping("/news")
    public ResponseEntity<?> getNewsPosts() {
        return ResponseEntity.ok(postRepository.findByTypeAndActiveOrderByCreateDateDesc("NEWS", true));
    }

    // Lấy bài Tuyển Dụng
    @GetMapping("/recruitment")
    public ResponseEntity<?> getRecruitmentPosts() {
        return ResponseEntity.ok(postRepository.findByTypeAndActiveOrderByCreateDateDesc("RECRUITMENT", true));
    }

    // Like bài đăng
    @PutMapping("/{id}/like")
    public ResponseEntity<?> likePost(@PathVariable Integer id) {
        return postRepository.findById(id).map(post -> {
            post.setLikes((post.getLikes() == null ? 0 : post.getLikes()) + 1);
            return ResponseEntity.ok(postRepository.save(post));
        }).orElse(ResponseEntity.badRequest().build());
    }
}
