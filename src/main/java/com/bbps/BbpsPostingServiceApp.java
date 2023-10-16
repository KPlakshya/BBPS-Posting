package com.bbps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
@EnableJpaRepositories
public class BbpsPostingServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(BbpsPostingServiceApp.class, args);
    }
}