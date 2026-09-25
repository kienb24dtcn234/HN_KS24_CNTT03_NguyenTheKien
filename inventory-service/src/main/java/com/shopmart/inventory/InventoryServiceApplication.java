package com.shopmart.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// Câu 1: service tự đăng ký Eureka (tự động khi có eureka-client trên classpath)
// Câu 4: @EnableCaching để bật Spring Cache (Cache-Aside với Redis)
@EnableCaching
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
