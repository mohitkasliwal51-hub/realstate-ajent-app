package com.bhartiyasaas.stayfile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StayFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(StayFileApplication.class, args);
    }
}

