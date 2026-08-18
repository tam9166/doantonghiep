package poly.edu.quanlynhahang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.service.HotMenuItemService;

@RestController
@RequestMapping("/api/menu/hot")
public class HotMenuController {
    private final HotMenuItemService service;

    public HotMenuController(HotMenuItemService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> hot(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(service.getHotMenuItems(limit));
    }
}
