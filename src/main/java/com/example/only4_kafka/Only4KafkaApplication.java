package com.example.only4_kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Only4KafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(Only4KafkaApplication.class, args);
    }

}
