package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.quanlynhahang.dto.HotMenuItemResponse;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;

import java.util.List;

@Service
public class HotMenuItemService {
    private final OrderDetailRepository repository;

    public HotMenuItemService(OrderDetailRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<HotMenuItemResponse> getHotMenuItems(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 50));
        return repository.findHotMenuItems(limit).stream().map(HotMenuItemResponse::from).toList();
    }
}
