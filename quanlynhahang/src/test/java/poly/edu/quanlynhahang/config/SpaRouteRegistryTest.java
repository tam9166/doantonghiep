package poly.edu.quanlynhahang.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SpaRouteRegistryTest {
    @Test
    void registryIncludesRefreshSensitiveCustomerAndAdminRoutes() {
        Set<String> routes = Set.copyOf(Arrays.asList(SpaRouteRegistry.ROUTES));

        assertTrue(routes.contains("/change-password"));
        assertTrue(routes.contains("/staff/profile"));
        assertTrue(routes.contains("/admin/kitchen-proposals"));
        assertTrue(routes.contains("/dat-su-kien"));
        assertTrue(routes.contains("/admin/ai-knowledge"));
        assertTrue(routes.contains("/admin/deposit-policies"));
        assertTrue(routes.contains("/admin/activity-log"));
        assertTrue(routes.contains("/admin/reservation-cancellations"));
        assertTrue(routes.contains("/admin/products"));
        assertTrue(routes.contains("/admin/expired-food"));
        assertTrue(routes.contains("/kitchen/inventory"));
    }

    @Test
    void registryCannotAccidentallyPermitApiOrAssetPatterns() {
        assertTrue(Arrays.stream(SpaRouteRegistry.ROUTES).allMatch(route -> route.startsWith("/")));
        assertFalse(Arrays.stream(SpaRouteRegistry.ROUTES).anyMatch(route -> route.startsWith("/api/")));
        assertFalse(Arrays.stream(SpaRouteRegistry.ROUTES).anyMatch(route -> route.startsWith("/ws/")));
        assertFalse(Arrays.stream(SpaRouteRegistry.ROUTES).anyMatch(route -> route.startsWith("/actuator/")));
        assertFalse(Arrays.stream(SpaRouteRegistry.ROUTES).anyMatch(route -> route.startsWith("/assets/")));
        assertFalse(Arrays.stream(SpaRouteRegistry.ROUTES).anyMatch(route -> route.contains("*")));
        assertFalse(Arrays.asList(SpaRouteRegistry.ROUTES).contains("/index.html"),
                "Forwarding index.html to itself would create a dispatch loop");
    }
}
