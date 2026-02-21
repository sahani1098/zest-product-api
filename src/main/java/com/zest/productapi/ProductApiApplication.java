package com.zest.productapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ProductApiApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(ProductApiApplication.class, args);

        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");

        System.out.println("\n");
        System.out.println("========================================");
        System.out.println("  Application is Running Successfully!");
        System.out.println("========================================");
        System.out.println("  Live Link:  http://localhost:" + port + "/swagger-ui.html");
        System.out.println("  API Docs: http://localhost:" + port + "/api-docs");
        System.out.println("========================================");
        System.out.println("\n");
    }
}