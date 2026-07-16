package poly.edu.quanlynhahang.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import poly.edu.quanlynhahang.controller.CategoryController;
import poly.edu.quanlynhahang.controller.ChatbotController;
import poly.edu.quanlynhahang.controller.AdminOrderController;
import poly.edu.quanlynhahang.controller.AdminProductController;
import poly.edu.quanlynhahang.controller.OrderController;
import poly.edu.quanlynhahang.controller.VoucherController;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.dto.AiRequest;
import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Voucher;

class EndpointAuthorizationMatrixTest {

    @Test
    void voucherAdminEndpointsOnlyAllowAdminOrManager() throws Exception {
        assertRoles(VoucherController.class.getMethod("getAllVouchers"), "ADMIN", "MANAGER");
        assertRoles(VoucherController.class.getMethod("adminCreateVoucher", Voucher.class), "ADMIN", "MANAGER");
    }

    @Test
    void categoryWritesOnlyAllowAdminOrManager() throws Exception {
        assertRoles(CategoryController.class.getMethod("addCategory", Category.class), "ADMIN", "MANAGER");
        assertRoles(CategoryController.class.getMethod("updateCategory", Integer.class, Category.class),
                "ADMIN", "MANAGER");
        assertRoles(CategoryController.class.getMethod("deleteCategory", Integer.class), "ADMIN", "MANAGER");
    }

    @Test
    void operationalOrderEndpointsExcludeCustomer() throws Exception {
        assertRoles(OrderController.class.getMethod("addItemsToOrder", Integer.class, OrderRequest.class),
                "WAITER", "CASHIER", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("mergeTables", Map.class),
                "WAITER", "CASHIER", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("splitTable", Map.class),
                "WAITER", "CASHIER", "MANAGER", "ADMIN");
        assertRoles(OrderController.class.getMethod("updateOrderDetailStatus", Integer.class, Integer.class),
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
    void internalAiEndpointsUseExplicitRoleWhitelists() throws Exception {
        assertRoles(ChatbotController.class.getMethod("analytics", AiRequest.class), "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("inventory", AiRequest.class), "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("customer", AiRequest.class), "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("suggestKitchenOrder", AiRequest.class),
                "KITCHEN", "ADMIN", "MANAGER");
        assertRoles(ChatbotController.class.getMethod("waiter", AiRequest.class),
                "WAITER", "ADMIN", "MANAGER");
    }

    @Test
    void internalProductCatalogExposesCostOnlyToOperationalRoles() throws Exception {
        assertRoles(AdminProductController.class.getMethod("getProductsForOperations"),
                "ADMIN", "MANAGER", "KITCHEN");
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
