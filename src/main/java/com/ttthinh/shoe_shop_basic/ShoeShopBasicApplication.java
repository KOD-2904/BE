package com.ttthinh.shoe_shop_basic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShoeShopBasicApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoeShopBasicApplication.class, args);
		System.out.println("Hello World!");
	}

}
