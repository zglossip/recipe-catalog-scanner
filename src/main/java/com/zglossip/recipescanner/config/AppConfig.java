package com.zglossip.recipescanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

	@Bean
	public Client geminiClient() {
		return new Client();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}