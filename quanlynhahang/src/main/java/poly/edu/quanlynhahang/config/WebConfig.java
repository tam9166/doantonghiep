package poly.edu.quanlynhahang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    private static final String[] SPA_ROUTES = {
        "/",
        "/login",
        "/register",
        "/staff-login",
        "/menu",
        "/history",
        "/profile",
        "/admin",
        "/admin/orders",
        "/admin/reservations",
        "/admin/reservation-reviews",
        "/admin/customer-history",
        "/admin/deposit-policies",
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
        "/admin/activity-log",
        "/admin/popular-items",
        "/admin/purchase-suggestions",
        "/admin/vouchers",
        "/cashier"
    };

    @Bean
    public HttpMessageConverter<String> responseBodyStringConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Đảm bảo JSON response luôn dùng UTF-8.
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
        converters.add(responseBodyStringConverter());
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        String uploadLocation = Path.of(uploadRoot).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation.endsWith("/") ? uploadLocation : uploadLocation + "/");
    }

    @Override
    public void addViewControllers(org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry) {
        for (String route : SPA_ROUTES) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }
}
