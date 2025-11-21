package com.careforall.mockpg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockPgApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockPgApplication.class, args);
    }
}
