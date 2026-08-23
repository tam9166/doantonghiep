package poly.edu.quanlynhahang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Bean
    public HttpMessageConverter<String> responseBodyStringConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Äáº£m báº£o JSON response luÃ´n dÃ¹ng UTF-8.
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
        converters.add(responseBodyStringConverter());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = Path.of(uploadRoot).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation.endsWith("/") ? uploadLocation : uploadLocation + "/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward only explicitly listed SPA routes to index.html
        for (String route : SpaRouteRegistry.ROUTES) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }

    /**
     * Configure path matching for routes containing path variables
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setRemoveSemicolonContent(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }
}
