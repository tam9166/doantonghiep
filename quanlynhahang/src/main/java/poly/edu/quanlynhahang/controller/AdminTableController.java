package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/tables")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public class AdminTableController {

    @Autowired
    private RestaurantTableRepository tableRepository;

    // 1. API Sửa thông tin bàn
    // 1. API Sửa thông tin bàn
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTable(@PathVariable Integer id, @RequestBody RestaurantTable tableDetails) {
        // Tìm bàn trong Database
        java.util.Optional<RestaurantTable> tableOpt = tableRepository.findById(id);
        
        // Nếu tìm thấy bàn
        if (tableOpt.isPresent()) {
            RestaurantTable table = tableOpt.get();
            table.setName(tableDetails.getName());
            table.setFloor(tableDetails.getFloor());
            table.setHasView(tableDetails.getHasView());
            // Không cập nhật isOccupied ở đây để tránh làm mất bàn đang có khách
            
            return ResponseEntity.ok(tableRepository.save(table));
        } else {
            // Nếu không tìm thấy
            return ResponseEntity.badRequest().body("Lỗi: Không tìm thấy bàn!");
        }
    }

    // 2. API Xóa bàn
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTable(@PathVariable Integer id) {
        if (!tableRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Lỗi: Bàn không tồn tại!");
        }
        tableRepository.deleteById(id);
        return ResponseEntity.ok("Xóa bàn thành công!");
    }
}