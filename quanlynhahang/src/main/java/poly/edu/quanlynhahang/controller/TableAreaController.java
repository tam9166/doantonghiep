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
import poly.edu.quanlynhahang.repository.AreaPricingRepository;
import poly.edu.quanlynhahang.entity.AreaPricing;
import poly.edu.quanlynhahang.entity.AreaType;
import poly.edu.quanlynhahang.service.TableAreaReadinessService;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;

import java.util.Date;
@RestController
@RequestMapping("/api/areas")
public class TableAreaController {
    private final TableAreaRepository areaRepository;
    private final AreaPricingRepository pricingRepository;
    private final TableAreaReadinessService readinessService;

    public TableAreaController(TableAreaRepository areaRepository,
                               AreaPricingRepository pricingRepository,
                               TableAreaReadinessService readinessService) {
        this.areaRepository = areaRepository;
        this.pricingRepository = pricingRepository;
        this.readinessService = readinessService;
    }

    @GetMapping
    public ResponseEntity<?> getActiveAreas() {
        return ResponseEntity.ok(areaRepository.findByStatusOrderByNameViAsc("ACTIVE").stream()
                .map(area -> TableAreaResponse.from(area, readinessService.evaluate(area)))
                .filter(TableAreaResponse::bookingReady)
                .toList());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getAreasForAdmin() {
        return ResponseEntity.ok(areaRepository.findAll().stream()
                .map(area -> TableAreaResponse.from(area, readinessService.evaluate(area)))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getArea(@PathVariable Integer id) {
        return areaRepository.findById(id).<ResponseEntity<?>>map(area -> {
            TableAreaReadinessService.Readiness readiness = readinessService.evaluate(area);
            if (!readiness.bookingReady()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(TableAreaResponse.from(area, readiness));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    public ResponseEntity<?> saveArea(@Valid @RequestBody TableAreaUpsertRequest request) {
        TableArea area = new TableArea();
        applyRequest(area, request);
        area.setUpdatedAt(new Date());
        area.setCreatedAt(new Date());
        areaRepository.save(area);
        savePricing(area, request);
        return ResponseEntity.ok(TableAreaResponse.from(area, readinessService.evaluate(area)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    public ResponseEntity<?> updateArea(@PathVariable Integer id,
                                        @Valid @RequestBody TableAreaUpsertRequest request) {
        return areaRepository.findById(id).map(area -> {
            applyRequest(area, request);
            area.setUpdatedAt(new Date());
            areaRepository.save(area);
            savePricing(area, request);
            return ResponseEntity.ok(TableAreaResponse.from(area, readinessService.evaluate(area)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> deactivateArea(@PathVariable Integer id) {
        return areaRepository.findById(id).map(area -> {
            area.setStatus("INACTIVE");
            area.setUpdatedAt(new Date());
            TableArea saved = areaRepository.save(area);
            return ResponseEntity.ok(TableAreaResponse.from(saved, readinessService.evaluate(saved)));
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
        // Retained only as a legacy column; reservation pricing lives in AreaPricing.
        area.setBasePrice(java.math.BigDecimal.ZERO);
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
    private void savePricing(TableArea area, TableAreaUpsertRequest request) {
        AreaPricing pricing = pricingRepository.findByAreaId(area.getId()).orElseGet(AreaPricing::new);
        pricing.setArea(area);
        boolean vip = AreaType.PRIVATE_ROOM.equals(area.getAreaType());
        pricing.setRoomFee(vip && request.roomFee() != null ? request.roomFee() : java.math.BigDecimal.ZERO);
        pricing.setMinimumSpend(vip && request.minimumSpend() != null ? request.minimumSpend() : java.math.BigDecimal.ZERO);
        pricing.setActive(vip);
        pricingRepository.save(pricing);
        area.setPricing(pricing);
    }
    private java.util.List<String> cleanList(java.util.List<String> values,int max){if(values==null)return new java.util.ArrayList<>();return values.stream().filter(java.util.Objects::nonNull).map(String::trim).filter(v->!v.isBlank()).distinct().limit(max).toList();}
}
