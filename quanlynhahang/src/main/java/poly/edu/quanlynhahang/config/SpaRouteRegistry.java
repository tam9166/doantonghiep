package poly.edu.quanlynhahang.config;

/**
 * Single source of truth for browser routes handled by Vue Router.
 *
 * <p>These paths serve only the SPA shell. API authorization remains enforced
 * independently by {@link SecurityConfig} and by method security.</p>
 */
public final class SpaRouteRegistry {
    private SpaRouteRegistry() {
    }

    public static final String[] ROUTES = {
        "/",
        "/login", "/register", "/staff-login", "/change-password",
        "/menu", "/history", "/profile",
        "/reservation", "/dat-su-kien", "/reservation-lookup", "/dine-in",
        "/staff", "/staff/profile", "/waiter", "/kitchen", "/cashier",
        "/admin", "/admin/orders", "/admin/reservations", "/admin/reservation-cancellations", "/admin/reservation-reviews",
        "/admin/customer-history", "/admin/deposit-policies", "/admin/analytics",
        "/admin/ai-knowledge", "/admin/categories", "/admin/tables",
        "/admin/table-areas", "/admin/staff", "/admin/kitchen-proposals", "/admin/posts", "/admin/ingredients",
        "/admin/activity-log", "/admin/popular-items", "/admin/purchase-suggestions",
        "/admin/vouchers"
    };
}
