package poly.edu.quanlynhahang.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

/** The only runtime writer that can make a physical table available again. */
@Service
public class TableLifecycleService {
    private final RestaurantTableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final TableReleaseGuardService releaseGuard;
    private final TableSessionService tableSessionService;

    public TableLifecycleService(RestaurantTableRepository tableRepository,
                                 OrderRepository orderRepository,
                                 TableReleaseGuardService releaseGuard,
                                 TableSessionService tableSessionService) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.releaseGuard = releaseGuard;
        this.tableSessionService = tableSessionService;
    }

    @Transactional
    public RestaurantTable release(Integer tableId) {
        return releaseAll(List.of(tableId)).getFirst();
    }

    @Transactional
    public List<RestaurantTable> releaseAll(Collection<Integer> tableIds) {
        List<Integer> ids = tableIds == null ? List.of() : tableIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        values -> values.stream().sorted().toList()));
        if (ids.isEmpty()) return List.of();

        List<RestaurantTable> tables = tableRepository.findLockedByIdIn(ids);
        if (tables.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn cần giải phóng");
        }
        tables.forEach(table -> releaseGuard.prepareForRelease(table.getId()));
        for (RestaurantTable table : tables) {
            if (Integer.valueOf(0).equals(table.getIsOccupied()) && table.getReservedTime() == null) {
                continue;
            }
            table.setIsOccupied(0);
            table.setReservedTime(null);
            tableRepository.save(table);
            tableSessionService.revokeActiveForTable(table.getId());
        }
        return tables;
    }

    @Transactional
    public boolean releaseIfNoOtherActiveOrder(Integer tableId, Integer excludedOrderId) {
        if (tableId == null) return false;
        tableRepository.findLockedById(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn"));
        if (orderRepository.existsActiveOrderForTableExcludingOrder(tableId, excludedOrderId)) return false;
        release(tableId);
        return true;
    }

    @Transactional
    public RestaurantTable markCleaningAfterPayment(Integer tableId) {
        RestaurantTable table = tableRepository.findLockedById(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn"));
        releaseGuard.prepareForRelease(tableId);
        table.setIsOccupied(3);
        table.setReservedTime("Đã thanh toán - chờ dọn");
        tableSessionService.revokeActiveForTable(tableId);
        return tableRepository.save(table);
    }

    @Transactional
    public void releaseReservationTables(Reservation reservation) {
        if (reservation == null) return;
        List<Integer> ids = reservation.getTableAssignments() != null
                && !reservation.getTableAssignments().isEmpty()
                        ? reservation.getTableAssignments().stream()
                                .map(assignment -> assignment.getTable().getId()).toList()
                        : reservation.getTable() == null ? List.of() : List.of(reservation.getTable().getId());
        releaseAll(ids);
    }
}
