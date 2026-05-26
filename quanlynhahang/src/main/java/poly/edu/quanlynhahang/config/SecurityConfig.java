package poly.edu.quanlynhahang.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
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
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
            // Mở hoàn toàn cho auth, error, và đơn hàng (không cần prefix ROLE_)
            .requestMatchers("/api/auth/**", "/error").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/tables/**", "/api/posts/**", "/api/reviews/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/applications").permitAll()
            .requestMatchers(HttpMethod.PUT, "/api/posts/*/like").permitAll()
            .requestMatchers("/api/chatbot/**", "/ws/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/orders/guest-booking").permitAll()

            // ✅ Cho phép Waiter gọi PUT /api/tables/{id}/status (nút Khách Về)
            .requestMatchers(HttpMethod.PUT, "/api/tables/**").hasAnyAuthority("ROLE_WAITER", "ROLE_ADMIN", "ROLE_MANAGER")

            // ✅ FIX: Dùng hasAnyAuthority với đúng tên = "ROLE_KITCHEN", "ROLE_WAITER", v.v.
            // Vì CustomUserDetails.getAuthorities() tạo SimpleGrantedAuthority("ROLE_KITCHEN")
            // hasAuthority so sánh CHÍNH XÁC chuỗi, không thêm prefix
            .requestMatchers("/api/kitchen/**").hasAnyAuthority("ROLE_KITCHEN", "ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers("/api/waiter/**").hasAnyAuthority("ROLE_WAITER", "ROLE_ADMIN", "ROLE_MANAGER")

            // ✅ MỞ RỘNG: Cho phép Bếp và Phục vụ cũng lấy được đơn hàng qua /api/admin/orders
            .requestMatchers("/api/admin/orders/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER")

            // ✅ Cho phép Bếp quản lý nguyên liệu + công thức và báo hết món
            .requestMatchers("/api/admin/ingredients/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN")
            .requestMatchers("/api/admin/recipes/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN")
            .requestMatchers(HttpMethod.PUT, "/api/admin/products/*/toggle-available").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN")

            // Các API admin khác chỉ cho Admin/Manager
            .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

            .anyRequest().authenticated()
        );

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}