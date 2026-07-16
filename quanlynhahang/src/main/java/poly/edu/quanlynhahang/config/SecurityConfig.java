package poly.edu.quanlynhahang.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import poly.edu.quanlynhahang.security.AuthTokenFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    private static final String[] SPA_ROUTES = {
        "/",
        "/index.html",
        "/favicon.ico",
        "/login",
        "/register",
        "/staff-login",
        "/menu",
        "/history",
        "/profile",
        "/admin",
        "/admin/orders",
        "/admin/analytics",
        "/reservation",
        "/reservation-lookup",
        "/admin/categories",
        "/admin/tables",
        "/admin/table-areas",
        "/admin/staff",
        "/admin/posts",
        "/dine-in",
        "/kitchen",
        "/waiter",
        "/staff",
        "/admin/ingredients",
        "/admin/vouchers",
        "/admin/reservations",
        "/admin/reservation-reviews",
        "/admin/customer-history",
        "/cashier"
    };

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With",
                "X-Idempotency-Key", "X-Webhook-Signature", "X-Webhook-Timestamp", "X-Captcha-Token",
                "X-Payment-Capability", "X-Reservation-Capability"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(SPA_ROUTES).permitAll()
            .requestMatchers(HttpMethod.GET, "/admin/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
            // Mở hoàn toàn cho auth, error, và đơn hàng (không cần prefix ROLE_)
            .requestMatchers("/api/auth/**", "/error").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/areas/admin").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/tables/**", "/api/areas/**", "/api/posts/**", "/api/reviews/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/categories", "/api/categories/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers(HttpMethod.GET, "/api/vouchers/admin").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers(HttpMethod.POST, "/api/vouchers/admin/create").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers(HttpMethod.GET, "/api/menu-items/preorder").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/reservations/quote").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/reservations/table-suggestions").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/reservations/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/reservation-waitlist").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/reservation-waitlist/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/reservation-reviews/public", "/api/reservation-reviews/mine/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/reservation-reviews").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/payments/qr", "/api/payments/*/regenerate").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/payments/*").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/webhooks/payments/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/applications", "/api/applications/upload").permitAll()
            .requestMatchers(HttpMethod.PUT, "/api/posts/*/like").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/chatbot/chat").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/orders/guest-booking").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/orders/checkout").permitAll()

            .requestMatchers(HttpMethod.PUT, "/api/orders/*/add-items")
                .hasAnyRole("WAITER", "CASHIER", "MANAGER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/orders/merge-tables", "/api/orders/split-table")
                .hasAnyRole("WAITER", "CASHIER", "MANAGER", "ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/orders/details/*/status")
                .hasAnyRole("KITCHEN", "MANAGER", "ADMIN")

            // ✅ Cho phép user đã đăng nhập gọi các API đặt hàng khác
            .requestMatchers("/api/orders/**").authenticated()

            // ✅ Cho phép Waiter và Cashier gọi PUT /api/tables/{id}/status (nút Khách Về / Dọn bàn)
            .requestMatchers(HttpMethod.PUT, "/api/tables/**").hasAnyAuthority("ROLE_WAITER", "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER")

            // ✅ FIX: Dùng hasAnyAuthority với đúng tên = "ROLE_KITCHEN", "ROLE_WAITER", v.v.
            // Vì CustomUserDetails.getAuthorities() tạo SimpleGrantedAuthority("ROLE_KITCHEN")
            // hasAuthority so sánh CHÍNH XÁC chuỗi, không thêm prefix
            .requestMatchers("/api/kitchen/**").hasAnyAuthority("ROLE_KITCHEN", "ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers("/api/waiter/**").hasAnyAuthority("ROLE_WAITER", "ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers("/api/staff/ai/kitchen").hasAnyAuthority("ROLE_KITCHEN", "ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers("/api/staff/ai/waiter").hasAnyAuthority("ROLE_WAITER", "ROLE_ADMIN", "ROLE_MANAGER")

            // ✅ MỞ RỘNG: Cho phép Bếp, Phục vụ, Thu ngân cũng lấy được đơn hàng qua /api/admin/orders
            .requestMatchers("/api/admin/orders", "/api/admin/orders/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER", "ROLE_CASHIER")

            // ✅ Cho phép Thu ngân
            .requestMatchers("/api/cashier/**").hasAnyAuthority("ROLE_CASHIER", "ROLE_ADMIN", "ROLE_MANAGER")

            // ✅ Các API chung cho nhân sự (Chấm công, Xem lịch làm, Phân khu vực phục vụ)
            .requestMatchers("/api/timekeeping/**", "/api/schedules/**", "/api/staff/me").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER", "ROLE_CASHIER")
            .requestMatchers("/api/service-zones/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER", "ROLE_CASHIER")

            // ✅ Cho phép Bếp quản lý nguyên liệu + công thức và báo hết món
            .requestMatchers("/api/admin/ingredients", "/api/admin/ingredients/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN")
            .requestMatchers("/api/admin/recipes", "/api/admin/recipes/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN")
            .requestMatchers("/api/admin/import-invoices", "/api/admin/import-invoices/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN")
            .requestMatchers(HttpMethod.PUT, "/api/admin/products/*/toggle-available").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN")

            // ✅ Các API mới: Nhật ký, Thông báo, Món hay dùng, Đề xuất mua hàng
            .requestMatchers("/api/admin/activity-logs", "/api/admin/activity-logs/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers("/api/admin/notifications", "/api/admin/notifications/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER", "ROLE_CASHIER")
            .requestMatchers("/api/admin/popular-items", "/api/admin/popular-items/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN")
            .requestMatchers("/api/admin/purchase-suggestions", "/api/admin/purchase-suggestions/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers("/api/admin/ai/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

            // Các API admin khác chỉ cho Admin/Manager
            .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

            .anyRequest().authenticated()
        );

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
