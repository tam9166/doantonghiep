package poly.edu.quanlynhahang.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.RestaurantTableUpsertRequest;
import poly.edu.quanlynhahang.dto.RestaurantTableResponse;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

@RestController
@RequestMapping("/api/admin/tables")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminTableController {

    private final RestaurantTableRepository tableRepository;

    public AdminTableController(RestaurantTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantTableResponse> updateTable(
            @PathVariable Integer id,
            @Valid @RequestBody RestaurantTableUpsertRequest request) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TABLE_NOT_FOUND"));
        request.applyTo(table, false);
        return ResponseEntity.ok(RestaurantTableResponse.from(tableRepository.save(table)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable Integer id) {
        if (!tableRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TABLE_NOT_FOUND");
        }
        tableRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
