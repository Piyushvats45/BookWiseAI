package com.bookwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookWiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookWiseApplication.class, args);
        System.out.println("Running ....");
    }
}
