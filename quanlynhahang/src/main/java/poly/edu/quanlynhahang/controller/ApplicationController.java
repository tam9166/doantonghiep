package poly.edu.quanlynhahang.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import poly.edu.quanlynhahang.entity.Application;
import poly.edu.quanlynhahang.repository.ApplicationRepository;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private static final long MAX_CV_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CV_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Map<String, Set<String>> ALLOWED_CV_MIME_TYPES = Map.of(
            "pdf", Set.of("application/pdf"),
            "doc", Set.of("application/msword", "application/octet-stream"),
            "docx", Set.of(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/zip",
                    "application/octet-stream"
            )
    );

    @Autowired
    private ApplicationRepository applicationRepository;

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Value("${app.upload.private-root:private-uploads}")
    private String privateUploadRoot;

    @PostMapping
    public ResponseEntity<?> submitApplication(@Valid @RequestBody ApplicationCreateRequest request) {
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
    public ResponseEntity<?> submitApplicationWithFile(
            @org.springframework.web.bind.annotation.RequestParam("fullname") String fullname,
            @org.springframework.web.bind.annotation.RequestParam("phone") String phone,
            @org.springframework.web.bind.annotation.RequestParam(value = "email", required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(value = "message", required = false) String message,
            @org.springframework.web.bind.annotation.RequestParam(value = "postId", required = false) Integer postId,
            @org.springframework.web.bind.annotation.RequestParam(value = "file", required = false) MultipartFile file) {

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
                String extension = validateCvFile(file);
                Path cvDir = Path.of(privateUploadRoot).toAbsolutePath().normalize().resolve("cvs").normalize();
                java.nio.file.Files.createDirectories(cvDir);

                String filename = UUID.randomUUID() + "." + extension;
                Path path = cvDir.resolve(filename).normalize();
                if (!path.startsWith(cvDir)) {
                    return ResponseEntity.badRequest().body("Tên file CV không hợp lệ!");
                }

                java.nio.file.Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                app.setCvFile("/api/applications/cv/" + filename);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(e.getMessage() != null ? e.getMessage() : "File CV không hợp lệ!");
            }
        }

        return ResponseEntity.ok(ApplicationResponse.from(applicationRepository.save(app)));
    }

    @GetMapping("/cv/{filename}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> downloadCv(@PathVariable String filename) throws IOException {
        if (!filename.matches("^[a-fA-F0-9\\-]{36}\\.(pdf|doc|docx)$")) {
            return ResponseEntity.badRequest().body("Tên file CV không hợp lệ!");
        }

        Path cvDir = Path.of(privateUploadRoot).toAbsolutePath().normalize().resolve("cvs").normalize();
        Path filePath = cvDir.resolve(filename).normalize();
        if (!filePath.startsWith(cvDir) || !java.nio.file.Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        String contentType = java.nio.file.Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String validateCvFile(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_CV_SIZE_BYTES) {
            throw new IllegalArgumentException("File CV tối đa 5MB!");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_CV_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("CV chỉ chấp nhận file PDF, DOC hoặc DOCX!");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CV_MIME_TYPES.get(extension).contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Kiểu file CV không hợp lệ!");
        }

        byte[] header = new byte[8];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        }

        boolean isPdf = read >= 4 && header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46;
        boolean isOleDoc = read >= 8
                && (header[0] & 0xff) == 0xd0 && (header[1] & 0xff) == 0xcf
                && (header[2] & 0xff) == 0x11 && (header[3] & 0xff) == 0xe0
                && (header[4] & 0xff) == 0xa1 && (header[5] & 0xff) == 0xb1
                && (header[6] & 0xff) == 0x1a && (header[7] & 0xff) == 0xe1;
        boolean isZipDocx = read >= 4 && header[0] == 0x50 && header[1] == 0x4b;

        if (("pdf".equals(extension) && !isPdf)
                || ("doc".equals(extension) && !isOleDoc)
                || ("docx".equals(extension) && !isZipDocx)) {
            throw new IllegalArgumentException("Nội dung file CV không khớp định dạng!");
        }

        return extension;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
