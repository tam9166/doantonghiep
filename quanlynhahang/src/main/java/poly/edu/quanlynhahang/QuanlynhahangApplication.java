package poly.edu.quanlynhahang;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class QuanlynhahangApplication {

    // Ép múi giờ và encoding ngay khi ứng dụng khởi động
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(QuanlynhahangApplication.class, args);
    }
}