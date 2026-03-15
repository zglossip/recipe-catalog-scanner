package com.zglossip.recipescanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class RecipeScannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecipeScannerApplication.class, args);
	}

}
