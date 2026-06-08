package com.pac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserManagementApp {

	public static void main(String[] args) {
		System.out.println("DATABASE USERNAME=" + System.getenv("DATABASE_USER"));
		System.out.println("DATABASE PASSWORD=" + System.getenv("DATABASE_PASSWORD"));
		SpringApplication.run(UserManagementApp.class, args);
	}

}
