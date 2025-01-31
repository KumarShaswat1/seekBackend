package com.example.TicketApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TicketManagementApplication {
	public static void main(String[] args) {
		SpringApplication.run(TicketManagementApplication.class, args);
	}
}
