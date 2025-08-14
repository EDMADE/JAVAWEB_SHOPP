package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootTest
@EnableScheduling
class SpringBootReactShopApplicationTests {
	 public static void main(String[] args) {
	        SpringApplication.run(SpringBootReactShopApplication.class, args);
	    }

	@Test
	void contextLoads() {
	}

}
