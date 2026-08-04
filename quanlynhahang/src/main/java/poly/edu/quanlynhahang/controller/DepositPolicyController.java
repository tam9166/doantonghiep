package poly.edu.quanlynhahang.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.entity.DepositPolicy;
import poly.edu.quanlynhahang.service.DepositPolicyService;

import java.util.List;

@RestController
public class DepositPolicyController {
    private final DepositPolicyService depositPolicyService;

    public DepositPolicyController(DepositPolicyService depositPolicyService) {
        this.depositPolicyService = depositPolicyService;
    }

    @GetMapping("/api/admin/deposit-policies")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public List<DepositPolicy> list() {
        return depositPolicyService.findAll();
    }

    @PostMapping("/api/admin/deposit-policies")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public DepositPolicy create(@RequestBody DepositPolicy request) {
        return depositPolicyService.save(request);
    }

    @PutMapping("/api/admin/deposit-policies/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public DepositPolicy update(@PathVariable Long id, @RequestBody DepositPolicy request) {
        return depositPolicyService.update(id, request);
    }

    @DeleteMapping("/api/admin/deposit-policies/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public void delete(@PathVariable Long id) {
        depositPolicyService.deactivate(id);
    }
}
