package poly.edu.quanlynhahang.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import poly.edu.quanlynhahang.dto.KitchenProposalRequest;
import poly.edu.quanlynhahang.dto.KitchenProposalReviewRequest;
import poly.edu.quanlynhahang.entity.KitchenProposal;
import poly.edu.quanlynhahang.service.KitchenProposalService;

@RestController
@RequiredArgsConstructor
public class KitchenProposalController {
    private final KitchenProposalService service;

    @PostMapping("/api/kitchen/proposals")
    @PreAuthorize("hasAuthority('ROLE_KITCHEN')")
    public ResponseEntity<KitchenProposal> submit(@Valid @RequestBody KitchenProposalRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.submit(request, authentication));
    }

    @GetMapping("/api/kitchen/proposals")
    @PreAuthorize("hasAuthority('ROLE_KITCHEN')")
    public ResponseEntity<List<KitchenProposal>> own(Authentication authentication) {
        return ResponseEntity.ok(service.list(authentication, false));
    }

    @GetMapping("/api/admin/kitchen-proposals")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<List<KitchenProposal>> all(Authentication authentication) {
        return ResponseEntity.ok(service.list(authentication, true));
    }

    @PostMapping("/api/admin/kitchen-proposals/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<KitchenProposal> approve(@PathVariable Long id, @RequestBody(required = false) KitchenProposalReviewRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.approve(id, request, authentication));
    }

    @PostMapping("/api/admin/kitchen-proposals/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<KitchenProposal> reject(@PathVariable Long id, @Valid @RequestBody KitchenProposalReviewRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.reject(id, request, authentication));
    }
}
