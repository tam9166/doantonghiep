package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import poly.edu.quanlynhahang.entity.Notification;
import poly.edu.quanlynhahang.repository.NotificationRepository;

class NotificationAuthorizationTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final NotificationController controller = controller();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void waiterListAndUnreadCountAreScopedToWaiterAndAllRoles() {
        authenticate("waiter", "ROLE_WAITER");
        when(notifications.findByTargetRoleInOrderByCreatedAtDesc(List.of("ALL", "ROLE_WAITER")))
                .thenReturn(List.of());

        controller.getAll();
        controller.getUnreadCount();

        verify(notifications).findByTargetRoleInOrderByCreatedAtDesc(List.of("ALL", "ROLE_WAITER"));
        verify(notifications).countUnreadByRoles(List.of("ALL", "ROLE_WAITER"));
    }

    @Test
    void waiterCannotMarkAdminNotificationAsRead() {
        authenticate("waiter", "ROLE_WAITER");
        Notification adminOnly = new Notification();
        adminOnly.setId(7L);
        adminOnly.setTargetRole("ROLE_ADMIN");
        when(notifications.findById(7L)).thenReturn(Optional.of(adminOnly));

        assertEquals(HttpStatus.NOT_FOUND, controller.markAsRead(7L).getStatusCode());

        verify(notifications, never()).save(argThat(value -> value.getId().equals(7L)));
    }

    @Test
    void markAllUpdatesOnlyCurrentRoleAndGlobalNotifications() {
        authenticate("cashier", "ROLE_CASHIER");

        controller.markAllAsRead();

        verify(notifications).markAllAsReadByRoles(List.of("ALL", "ROLE_CASHIER"));
        verify(notifications, never()).markAllAsRead();
    }

    private NotificationController controller() {
        NotificationController value = new NotificationController();
        ReflectionTestUtils.setField(value, "notificationRepository", notifications);
        return value;
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(username, null, role));
    }
}
