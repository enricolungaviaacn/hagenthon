package com.hagenthon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class HagenthonApplication {

    @Value("${app.upload.dir:./data/uploads}")
    private String uploadDir;

    public static void main(String[] args) {
        SpringApplication.run(HagenthonApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() throws IOException {
        Files.createDirectories(Paths.get("./data"));
        Files.createDirectories(Paths.get(uploadDir));
    }
}
