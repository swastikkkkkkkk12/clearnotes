package com.clearnotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ClearNotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClearNotesApplication.class, args);
    }
}
