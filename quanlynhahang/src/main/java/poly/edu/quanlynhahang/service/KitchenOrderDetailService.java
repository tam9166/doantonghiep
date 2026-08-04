package poly.edu.quanlynhahang.service;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;

@Service
public class KitchenOrderDetailService {
    private final OrderDetailRepository orderDetailRepository;
    private final ActivityLogService activityLogService;
    private final SimpMessagingTemplate messagingTemplate;

    public KitchenOrderDetailService(OrderDetailRepository orderDetailRepository,
                                     ActivityLogService activityLogService,
                                     SimpMessagingTemplate messagingTemplate) {
        this.orderDetailRepository = orderDetailRepository;
        this.activityLogService = activityLogService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public OrderDetail start(Integer detailId) {
        OrderDetail detail = detail(detailId);
        if (isCancelled(detail) || isReadyOrServed(detail)) {
            throw conflict("Món không còn ở trạng thái chờ chế biến");
        }
        if (detail.getStartedAt() != null) {
            throw conflict("Món đã được bắt đầu chế biến");
        }
        detail.setStartedAt(new Date());
        OrderDetail saved = orderDetailRepository.save(detail);
        publish("DISH_STARTED", saved);
        return saved;
    }

    @Transactional
    public OrderDetail complete(Integer detailId) {
        OrderDetail detail = detail(detailId);
        if (isCancelled(detail) || isReadyOrServed(detail)) {
            throw conflict("Món không còn ở trạng thái có thể hoàn thành");
        }
        if (detail.getStartedAt() == null) {
            throw conflict("Cần bắt đầu chế biến trước khi hoàn thành món");
        }
        detail.setStatus(1);
        detail.setCompletedAt(new Date());
        OrderDetail saved = orderDetailRepository.save(detail);
        publish("DISH_READY", saved);
        return saved;
    }

    @Transactional
    public OrderDetail cancel(Integer detailId, String reason) {
        OrderDetail detail = detail(detailId);
        if (isCancelled(detail) || isReadyOrServed(detail)) {
            throw conflict("Món không còn ở trạng thái có thể hủy");
        }
        detail.setStatus(3);
        detail.setCancelledAt(new Date());
        detail.setCancelReason(reason.trim());
        OrderDetail saved = orderDetailRepository.save(detail);
        publish("DISH_CANCELLED", saved);
        return saved;
    }

    private OrderDetail detail(Integer detailId) {
        return orderDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món trong đơn"));
    }

    private boolean isCancelled(OrderDetail detail) {
        return Integer.valueOf(3).equals(detail.getStatus());
    }

    private boolean isReadyOrServed(OrderDetail detail) {
        return detail.getStatus() != null && detail.getStatus() >= 1;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private void publish(String event, OrderDetail detail) {
        String detailId = String.valueOf(detail.getId());
        activityLogService.log(event, "OrderDetail", detailId, event + " cho món #" + detailId);
        messagingTemplate.convertAndSend("/topic/kitchen", event);
        messagingTemplate.convertAndSend("/topic/waiter", event);
    }
}
