package poly.edu.quanlynhahang.controller;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.ApplicationCreateRequest;
import poly.edu.quanlynhahang.dto.ApplicationResponse;
import poly.edu.quanlynhahang.dto.ApplicationUploadRequest;
import poly.edu.quanlynhahang.entity.Application;
import poly.edu.quanlynhahang.repository.ApplicationRepository;
import poly.edu.quanlynhahang.repository.PostRepository;
import poly.edu.quanlynhahang.service.CvFileStorageService;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CvFileStorageService cvFileStorageService;

    @Autowired
    private PostRepository postRepository;

    @PostMapping
    public ResponseEntity<?> submitApplication(@Valid @RequestBody ApplicationCreateRequest request) {
        if (!postRepository.existsById(request.postId())) {
            return ResponseEntity.badRequest().body("Tin tuyển dụng không tồn tại.");
        }
        Application app = new Application();
        app.setFullname(request.fullname().trim());
        app.setPhone(request.phone().trim());
        app.setEmail(normalizeOptional(request.email()));
        app.setMessage(normalizeOptional(request.message()));
        app.setPostId(request.postId());
        app.setCreateDate(new Date());
        return ResponseEntity.ok(ApplicationResponse.from(applicationRepository.save(app)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getAllApplications() {
        return ResponseEntity.ok(applicationRepository.findAllByOrderByCreateDateDesc().stream()
                .map(ApplicationResponse::from)
                .toList());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> submitApplicationWithFile(@Valid ApplicationUploadRequest request) {
        if (!postRepository.existsById(request.getPostId())) {
            return ResponseEntity.badRequest().body("Tin tuyển dụng không tồn tại.");
        }

        Application app = new Application();
        app.setFullname(request.getFullname().trim());
        app.setPhone(request.getPhone().trim());
        app.setEmail(normalizeOptional(request.getEmail()));
        app.setMessage(normalizeOptional(request.getMessage()));
        app.setPostId(request.getPostId());
        app.setCreateDate(new Date());

        MultipartFile file = request.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                app.setCvFile("/api/applications/cv/" + cvFileStorageService.store(file));
            } catch (IllegalArgumentException exception) {
                return ResponseEntity.badRequest().body(exception.getMessage());
            } catch (IOException exception) {
                log.error("Unable to persist validated CV", exception);
                return ResponseEntity.internalServerError().body("Không thể lưu CV lúc này.");
            }
        }

        return ResponseEntity.ok(ApplicationResponse.from(applicationRepository.save(app)));
    }

    @GetMapping("/cv/{filename}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> downloadCv(@PathVariable String filename) throws IOException {
        Resource resource;
        try {
            resource = cvFileStorageService.load(filename);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body("Tên file CV không hợp lệ.");
        }
        if (resource == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
