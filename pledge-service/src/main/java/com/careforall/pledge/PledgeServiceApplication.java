package com.careforall.pledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PledgeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PledgeServiceApplication.class, args);
    }
}
