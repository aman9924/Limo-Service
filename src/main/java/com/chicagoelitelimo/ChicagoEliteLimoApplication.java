package com.chicagoelitelimo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChicagoEliteLimoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChicagoEliteLimoApplication.class, args);
    }
}
