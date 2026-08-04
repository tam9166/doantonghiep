package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.RestaurantTableUpsertRequest;
import poly.edu.quanlynhahang.dto.RestaurantTableResponse;
import poly.edu.quanlynhahang.dto.PublicRestaurantTableResponse;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

@RestController
@RequestMapping("/api/tables")
public class RestaurantTableController {

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<?> getAllTables() {
        return ResponseEntity.ok(tableRepository.findAll().stream()
                .map(PublicRestaurantTableResponse::from)
                .toList());
    }

    @GetMapping("/seed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> seedTables() {
        if (tableRepository.count() > 20) {
            return ResponseEntity.ok("Đã có đủ bàn, không cần thêm nữa.");
        }

        for (int i = 1; i <= 15; i++) {
            tableRepository.save(new RestaurantTable(null, "Bàn T2-" + String.format("%02d", i),
                    "Tầng 2 (Sảnh tiệc)", 0, false, null, 10, null));
        }

        for (int i = 1; i <= 5; i++) {
            tableRepository.save(new RestaurantTable(null, "VIP T3-" + String.format("%02d", i),
                    "Tầng 3 (Phòng VIP)", 0, false, null, 6, null));
            tableRepository.save(new RestaurantTable(null, "VIP T4-" + String.format("%02d", i),
                    "Tầng 4 (Phòng VIP)", 0, false, null, 6, null));
            tableRepository.save(new RestaurantTable(null, "VIP T5-" + String.format("%02d", i),
                    "Tầng 5 (Phòng VIP)", 0, false, null, 8, null));
        }

        for (int i = 1; i <= 4; i++) {
            tableRepository.save(new RestaurantTable(null, "Rooftop T6-" + String.format("%02d", i),
                    "Tầng 6 (Sân thượng)", 0, false, null, 4, "View phố"));
            tableRepository.save(new RestaurantTable(null, "Rooftop T6-" + String.format("%02d", i + 4),
                    "Tầng 6 (Sân thượng)", 0, false, null, 4, "View sông"));
            tableRepository.save(new RestaurantTable(null, "Rooftop T6-" + String.format("%02d", i + 8),
                    "Tầng 6 (Sân thượng)", 0, false, null, 6, "View sân vườn"));
        }

        return ResponseEntity.ok("Đã tạo tự động các bàn thành công.");
    }

    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(@RequestParam String date, @RequestParam String time) {
        return ResponseEntity.ok(tableRepository.findAll().stream()
                .map(PublicRestaurantTableResponse::from)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public RestaurantTableResponse addTable(@Valid @RequestBody RestaurantTableUpsertRequest request) {
        return RestaurantTableResponse.from(tableRepository.save(request.toNewEntity()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER') or hasRole('WAITER')")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id,
                                          @RequestParam Integer status,
                                          @RequestParam(required = false) String time) {
        return tableRepository.findById(id).map(table -> {
            if ((status == 0 || status == 3)
                    && orderRepository.existsOpenUnpaidOrderForTable(table.getId(), table.getName())) {
                return ResponseEntity.status(409).body(
                        "Bàn còn hóa đơn chưa thanh toán. Thu ngân phải xác nhận thanh toán trước khi chuyển bàn.");
            }
            table.setIsOccupied(status);
            table.setReservedTime(status == 0 ? null : time);
            tableRepository.save(table);

            if (status == 3) {
                List<Order> activeOrders = orderRepository.findAll().stream()
                        .filter(o -> table.getId().equals(o.getTableId()))
                        .filter(o -> Boolean.TRUE.equals(o.getIsPaid()))
                        .filter(o -> o.getStatus() != null && o.getStatus() < 4)
                        .collect(Collectors.toList());
                for (Order order : activeOrders) {
                    order.setStatus(4);
                    orderRepository.save(order);
                }
            }
            return ResponseEntity.ok("Cập nhật thành công.");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy bàn."));
    }

    @PutMapping("/{id}/link/{targetId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER') or hasRole('WAITER')")
    public ResponseEntity<?> linkTable(@PathVariable Integer id, @PathVariable Integer targetId) {
        java.util.Optional<RestaurantTable> sourceOpt = tableRepository.findById(id);
        java.util.Optional<RestaurantTable> targetOpt = tableRepository.findById(targetId);

        if (sourceOpt.isEmpty() || targetOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy bàn.");
        }

        RestaurantTable source = sourceOpt.get();
        RestaurantTable target = targetOpt.get();

        source.setIsOccupied(5);
        source.setReservedTime("[GHÉP VỚI: " + target.getName() + "]");
        tableRepository.save(source);

        return ResponseEntity.ok("Ghép bàn thành công.");
    }

    @PutMapping("/{id}/unlink")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER') or hasRole('WAITER')")
    public ResponseEntity<?> unlinkTable(@PathVariable Integer id) {
        return tableRepository.findById(id).map(table -> {
            table.setIsOccupied(0);
            table.setReservedTime(null);
            tableRepository.save(table);
            return ResponseEntity.ok("Tách bàn thành công.");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy bàn."));
    }
}
