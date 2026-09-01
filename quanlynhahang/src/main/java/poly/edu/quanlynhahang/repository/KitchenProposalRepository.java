package poly.edu.quanlynhahang.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.KitchenProposal;

public interface KitchenProposalRepository extends JpaRepository<KitchenProposal, Long> {
    List<KitchenProposal> findAllByOrderByCreatedAtDesc();
    List<KitchenProposal> findByProposedByOrderByCreatedAtDesc(String proposedBy);
}
