package poly.edu.quanlynhahang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import poly.edu.quanlynhahang.dto.TableLayoutResponse;
import poly.edu.quanlynhahang.dto.TableLayoutUpsertRequest;
import poly.edu.quanlynhahang.entity.TableLayout;
import poly.edu.quanlynhahang.repository.TableLayoutRepository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/admin/table-layouts")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public class TableLayoutController {
    private final TableLayoutRepository layoutRepository;

    public TableLayoutController(TableLayoutRepository layoutRepository) {
        this.layoutRepository = layoutRepository;
    }

    @GetMapping
    public ResponseEntity<?> getLayouts() {
        return ResponseEntity.ok(layoutRepository.findByActiveTrueOrderByFloorNameAscTableIdAsc().stream()
                .map(TableLayoutResponse::from)
                .toList());
    }

    @PutMapping("/bulk")
    public ResponseEntity<?> saveLayouts(
            @Valid @Size(max = 200) @RequestBody List<@Valid TableLayoutUpsertRequest> layouts) {
        List<TableLayout> saved = layouts.stream()
                .map(this::upsertLayout)
                .toList();
        return ResponseEntity.ok(saved.stream().map(TableLayoutResponse::from).toList());
    }

    private TableLayout upsertLayout(TableLayoutUpsertRequest incoming) {
        TableLayout layout = layoutRepository
                .findFirstByTableIdAndActiveTrueOrderByUpdatedAtDesc(incoming.tableId())
                .orElseGet(TableLayout::new);
        layout.setTableId(incoming.tableId());
        layout.setAreaId(incoming.areaId());
        layout.setFloorName(incoming.floorName());
        layout.setXPosition(defaultDecimal(incoming.xPosition(), BigDecimal.ZERO));
        layout.setYPosition(defaultDecimal(incoming.yPosition(), BigDecimal.ZERO));
        layout.setWidth(defaultDecimal(incoming.width(), BigDecimal.valueOf(170)));
        layout.setHeight(defaultDecimal(incoming.height(), BigDecimal.valueOf(130)));
        layout.setShape(incoming.shape() == null || incoming.shape().isBlank() ? "RECTANGLE" : incoming.shape().trim());
        layout.setRotation(defaultDecimal(incoming.rotation(), BigDecimal.ZERO));
        layout.setActive(true);
        layout.setUpdatedAt(new Date());
        return layoutRepository.save(layout);
    }

    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }
}
