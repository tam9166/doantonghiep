package poly.edu.quanlynhahang.controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.service.EncodingHealthService;
@RestController
@RequestMapping("/api/admin/system")
public class AdminSystemHealthController {
    private final EncodingHealthService encodingHealthService;
    public AdminSystemHealthController(EncodingHealthService encodingHealthService) { this.encodingHealthService = encodingHealthService; }
    @GetMapping("/encoding-health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public Object encodingHealth() { return encodingHealthService.inspectPaymentAccountHolders(); }
}
