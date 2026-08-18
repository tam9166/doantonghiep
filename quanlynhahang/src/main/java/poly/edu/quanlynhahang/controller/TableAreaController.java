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
        if (request.minGuestCount() != null && request.maxGuestCount() != null && request.minGuestCount() > request.maxGuestCount()) throw new IllegalArgumentException("Sức chứa tối thiểu không được lớn hơn tối đa");
        area.setNameVi(request.nameVi().trim());
        area.setNameEn(request.nameEn());
        area.setDescriptionVi(request.descriptionVi());
        area.setDescriptionEn(request.descriptionEn());
        area.setImageUrl(request.imageUrl());
        area.setGallery(cleanList(request.gallery(), 20));
        area.setBasePrice(request.basePrice() == null ? java.math.BigDecimal.ZERO : request.basePrice());
        area.setCapacity(request.capacity() == null ? 0 : request.capacity());
        area.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
        area.setAreaType(request.areaType() == null ? poly.edu.quanlynhahang.entity.AreaType.DINING : request.areaType());
        area.setMinGuestCount(request.minGuestCount() == null ? 1 : request.minGuestCount());
        area.setMaxGuestCount(request.maxGuestCount() == null ? 1000 : request.maxGuestCount());
        area.setMinBookingHours(request.minBookingHours() == null ? 2 : request.minBookingHours());
        area.setHourlyRate(request.hourlyRate() == null ? java.math.BigDecimal.ZERO : request.hourlyRate());
        area.setPackagePrice(request.packagePrice() == null ? java.math.BigDecimal.ZERO : request.packagePrice());
        area.setMaxTables(request.maxTables());
        area.setDefaultGuestsPerTable(request.defaultGuestsPerTable() == null ? 10 : request.defaultGuestsPerTable());
        area.setSuitableEventTypes(cleanList(request.suitableEventTypes(), 20));
    }
    private java.util.List<String> cleanList(java.util.List<String> values,int max){if(values==null)return new java.util.ArrayList<>();return values.stream().filter(java.util.Objects::nonNull).map(String::trim).filter(v->!v.isBlank()).distinct().limit(max).toList();}
}
