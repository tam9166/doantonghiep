package poly.edu.quanlynhahang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.dto.TableAreaResponse;
import poly.edu.quanlynhahang.dto.TableAreaUpsertRequest;
import poly.edu.quanlynhahang.repository.TableAreaRepository;

import jakarta.validation.Valid;

import java.util.Date;
@RestController
@RequestMapping("/api/areas")
public class TableAreaController {
    private final TableAreaRepository areaRepository;

    public TableAreaController(TableAreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @GetMapping
    public ResponseEntity<?> getActiveAreas() {
        return ResponseEntity.ok(areaRepository.findByStatusOrderByNameViAsc("ACTIVE").stream()
                .map(TableAreaResponse::from).toList());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getAreasForAdmin() {
        return ResponseEntity.ok(areaRepository.findAll().stream().map(TableAreaResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getArea(@PathVariable Integer id) {
        return areaRepository.findById(id)
                .<ResponseEntity<?>>map(area -> ResponseEntity.ok(TableAreaResponse.from(area)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> saveArea(@Valid @RequestBody TableAreaUpsertRequest request) {
        TableArea area = new TableArea();
        applyRequest(area, request);
        area.setUpdatedAt(new Date());
        area.setCreatedAt(new Date());
        return ResponseEntity.ok(TableAreaResponse.from(areaRepository.save(area)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> updateArea(@PathVariable Integer id,
                                        @Valid @RequestBody TableAreaUpsertRequest request) {
        return areaRepository.findById(id).map(area -> {
            applyRequest(area, request);
            area.setUpdatedAt(new Date());
            return ResponseEntity.ok(TableAreaResponse.from(areaRepository.save(area)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> deactivateArea(@PathVariable Integer id) {
        return areaRepository.findById(id).map(area -> {
            area.setStatus("INACTIVE");
            area.setUpdatedAt(new Date());
            return ResponseEntity.ok(TableAreaResponse.from(areaRepository.save(area)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void applyRequest(TableArea area, TableAreaUpsertRequest request) {
        area.setNameVi(request.nameVi().trim());
        area.setNameEn(request.nameEn());
        area.setDescriptionVi(request.descriptionVi());
        area.setDescriptionEn(request.descriptionEn());
        area.setImageUrl(request.imageUrl());
        area.setBasePrice(request.basePrice() == null ? java.math.BigDecimal.ZERO : request.basePrice());
        area.setCapacity(request.capacity() == null ? 0 : request.capacity());
        area.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
    }
}
