package com.buggyshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BuggyShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(BuggyShopApplication.class, args);
    }
}
