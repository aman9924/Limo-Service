package com.honklimo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HonkLimoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HonkLimoApplication.class, args);
    }
}
