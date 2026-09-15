package com.honklimo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class HonkLimoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HonkLimoApplication.class, args);
    }
}

