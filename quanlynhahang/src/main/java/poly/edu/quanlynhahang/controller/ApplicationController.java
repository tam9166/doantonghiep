package poly.edu.quanlynhahang.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Application;
import poly.edu.quanlynhahang.repository.ApplicationRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    // Khách gửi đơn ứng tuyển (public)
    @PostMapping
    public ResponseEntity<?> submitApplication(@RequestBody Application app) {
        if (app.getFullname() == null || app.getFullname().isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập họ tên!");
        }
        if (app.getPhone() == null || app.getPhone().isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập số điện thoại!");
        }
        app.setCreateDate(new Date());
        return ResponseEntity.ok(applicationRepository.save(app));
    }

    // Admin xem tất cả đơn ứng tuyển
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getAllApplications() {
        return ResponseEntity.ok(applicationRepository.findAllByOrderByCreateDateDesc());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> submitApplicationWithFile(
            @org.springframework.web.bind.annotation.RequestParam("fullname") String fullname,
            @org.springframework.web.bind.annotation.RequestParam("phone") String phone,
            @org.springframework.web.bind.annotation.RequestParam(value="email", required=false) String email,
            @org.springframework.web.bind.annotation.RequestParam(value="message", required=false) String message,
            @org.springframework.web.bind.annotation.RequestParam(value="postId", required=false) Integer postId,
            @org.springframework.web.bind.annotation.RequestParam(value="file", required=false) org.springframework.web.multipart.MultipartFile file) {
        
        if (fullname == null || fullname.isBlank()) return ResponseEntity.badRequest().body("Vui lòng nhập họ tên!");
        if (phone == null || phone.isBlank()) return ResponseEntity.badRequest().body("Vui lòng nhập số điện thoại!");

        Application app = new Application();
        app.setFullname(fullname);
        app.setPhone(phone);
        app.setEmail(email);
        app.setMessage(message);
        app.setPostId(postId);
        app.setCreateDate(new Date());

        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = "uploads/cvs/";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                
                String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                java.nio.file.Path path = java.nio.file.Paths.get(uploadDir + filename);
                java.nio.file.Files.copy(file.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                app.setCvFile("/" + uploadDir + filename);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("Lỗi tải file CV!");
            }
        }

        return ResponseEntity.ok(applicationRepository.save(app));
    }
}
