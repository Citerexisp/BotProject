package com.example.KarinaProject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.example.KarinaProject")
public class KarinaProjectApplication {



	public static void main(String[] args) {
		SpringApplication.run(KarinaProjectApplication.class, args);
	}
}
