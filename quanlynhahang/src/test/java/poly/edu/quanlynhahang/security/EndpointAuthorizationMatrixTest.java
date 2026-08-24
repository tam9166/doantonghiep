package poly.edu.quanlynhahang.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import poly.edu.quanlynhahang.controller.CategoryController;
import poly.edu.quanlynhahang.controller.ChatbotController;
import poly.edu.quanlynhahang.controller.AdminOrderController;
import poly.edu.quanlynhahang.controller.AdminProductController;
import poly.edu.quanlynhahang.controller.OrderController;
import poly.edu.quanlynhahang.controller.PostController;
import poly.edu.quanlynhahang.controller.VoucherController;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.dto.OrderCancelRequest;
import poly.edu.quanlynhahang.dto.AiRequest;
import poly.edu.quanlynhahang.dto.AssistantQueryRequest;
import poly.edu.quanlynhahang.dto.KitchenDishCancelRequest;
import poly.edu.quanlynhahang.dto.MergeTablesRequest;
import poly.edu.quanlynhahang.dto.SplitTableRequest;
import poly.edu.quanlynhahang.dto.CategoryUpsertRequest;
import poly.edu.quanlynhahang.dto.VoucherUpsertRequest;

class EndpointAuthorizationMatrixTest {

    @Test
    void voucherAdminEndpointsOnlyAllowAdminOrManager() throws Exception {
        assertRoles(VoucherController.class.getMethod("getAllVouchers"), "ADMIN", "MANAGER");
        assertRoles(VoucherController.class.getMethod("adminCreateVoucher", VoucherUpsertRequest.class), "ADMIN", "MANAGER");
    }

    @Test
    void categoryWritesOnlyAllowAdminOrManager() throws Exception {
        assertRoles(CategoryController.class.getMethod("addCategory", CategoryUpsertRequest.class), "ADMIN", "MANAGER");
        assertRoles(CategoryController.class.getMethod("updateCategory", Integer.class, CategoryUpsertRequest.class),
                "ADMIN", "MANAGER");
        assertRoles(CategoryController.class.getMethod("deleteCategory", Integer.class), "ADMIN", "MANAGER");
    }

    @Test
    void operationalOrderEndpointsExcludeCustomer() throws Exception {
        assertRoles(OrderController.class.getMethod("addItemsToOrder", Integer.class, OrderRequest.class, String.class),
                "WAITER", "CASHIER", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("mergeTables", MergeTablesRequest.class),
                "WAITER", "CASHIER", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("splitTable", SplitTableRequest.class),
                "WAITER", "CASHIER", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("updateOrderDetailStatus", Integer.class, Integer.class),
                "KITCHEN", "WAITER", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("startKitchenDish", Integer.class),
                "KITCHEN", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("completeKitchenDish", Integer.class),
                "KITCHEN", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("cancelKitchenDish", Integer.class,
                        KitchenDishCancelRequest.class, Authentication.class),
                "KITCHEN", "MANAGER", "ADMIN");
    }

    @Test
    void manualOrderPaymentActionsOnlyAllowCashierOrManagers() throws Exception {
        assertRoles(AdminOrderController.class.getMethod("payOrder", Integer.class),
                "ADMIN", "MANAGER", "CASHIER");
        assertRoles(AdminOrderController.class.getMethod("confirmManualOrder", Integer.class),
                "ADMIN", "MANAGER", "CASHIER");
        assertRoles(AdminOrderController.class.getMethod("createPaymentQr", Integer.class),
                "ADMIN", "MANAGER", "CASHIER");
        assertRoles(AdminOrderController.class.getMethod(
                        "regeneratePaymentQr", Integer.class, String.class, String.class),
                "ADMIN", "MANAGER", "CASHIER");
    }

    @Test
    void kitchenBoardIsNotExposedToWaiterOrCashier() throws Exception {
        assertRoles(AdminOrderController.class.getMethod("getKitchenBoard"),
                "KITCHEN", "ADMIN", "MANAGER");
    }

    @Test
    void orderOperationalActionsUseLeastPrivilegeRoleMatrix() throws Exception {
        Method kitchenAndWaiterStatus = AdminOrderController.class.getMethod(
                "updateOrderStatus", Integer.class, Integer.class);
        PreAuthorize statusRule = kitchenAndWaiterStatus.getAnnotation(PreAuthorize.class);
        assertNotNull(statusRule);
        assertTrue(statusRule.value().contains("hasRole('KITCHEN') and (#status == 2 or #status == 6)"));
        assertTrue(statusRule.value().contains("hasRole('WAITER') and (#status == 1 or #status == 7)"));
        assertFalse(statusRule.value().contains("CASHIER"));

        assertRoles(AdminOrderController.class.getMethod("updateOrderAddress", Integer.class, String.class),
                "ADMIN", "MANAGER", "WAITER");
        assertRoles(AdminOrderController.class.getMethod("cancelOrder", Integer.class, OrderCancelRequest.class),
                "ADMIN", "MANAGER", "CASHIER");
        assertRoles(AdminOrderController.class.getMethod("cancelOrderWithRefund", Integer.class),
                "ADMIN", "MANAGER", "CASHIER");
        assertRoles(AdminOrderController.class.getMethod("activateScheduledOrders"), "ADMIN", "MANAGER");
    }

    @Test
    void internalAiEndpointsUseExplicitRoleWhitelists() throws Exception {
        assertRoles(ChatbotController.class.getMethod("analytics", AiRequest.class), "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("inventory", AiRequest.class), "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("customer", AiRequest.class), "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("suggestKitchenOrder", AiRequest.class),
                "KITCHEN", "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("waiter", AiRequest.class),
                "WAITER", "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("operations", AiRequest.class),
                "ADMIN", "MANAGER", "KITCHEN", "WAITER", "CASHIER");
        assertRoles(ChatbotController.class.getMethod("staffAssistantQuery", org.springframework.security.core.Authentication.class,
                        AssistantQueryRequest.class),
                "ADMIN", "MANAGER", "KITCHEN", "WAITER", "CASHIER");
    }

    @Test
    void internalProductCatalogExposesCostOnlyToOperationalRoles() throws Exception {
        assertRoles(AdminProductController.class.getMethod("getProductsForOperations"),
                "ADMIN", "MANAGER", "KITCHEN");
    }

    @Test
    void likingAPostRequiresAuthentication() throws Exception {
        Method method = PostController.class.getMethod("likePost", Integer.class);
        PreAuthorize rule = method.getAnnotation(PreAuthorize.class);
        assertNotNull(rule);
        assertTrue(rule.value().contains("isAuthenticated()"));
    }

    @Test
    void dishStatusTransitionsAreLimitedToTheResponsibleRole() throws Exception {
        Method method = OrderController.class.getMethod("updateOrderDetailStatus", Integer.class, Integer.class);
        PreAuthorize rule = method.getAnnotation(PreAuthorize.class);
        assertNotNull(rule);
        assertTrue(rule.value().contains("hasRole('KITCHEN') and #status == 1"));
        assertTrue(rule.value().contains("hasRole('WAITER') and #status == 2"));
        assertFalse(rule.value().contains("CASHIER"));
    }

    private void assertRoles(Method method, String... expectedRoles) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, () -> method + " must have method-level authorization");
        String expression = annotation.value();
        for (String role : expectedRoles) {
            assertTrue(expression.contains("'" + role + "'"), () -> method + " must allow " + role);
        }
        assertFalse(expression.contains("CUSTOMER"), () -> method + " must never allow CUSTOMER");
        assertFalse(expression.contains("USER"), () -> method + " must never allow USER");
    }
}
