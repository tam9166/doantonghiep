package poly.edu.quanlynhahang;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class QuanlynhahangApplication {

    // Ép múi giờ và en8777777777coding ngay khi ứng dụng khởi động
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(QuanlynhahangApplication.class, args);
    }
}
