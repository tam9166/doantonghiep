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
}
