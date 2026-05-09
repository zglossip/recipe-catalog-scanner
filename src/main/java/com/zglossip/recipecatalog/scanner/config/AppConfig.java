package com.zglossip.recipecatalog.scanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper().findAndRegisterModules();
	}

	@Bean
	public RestClient restClient(OllamaProperties ollamaProperties) {
		return RestClient.builder().baseUrl(ollamaProperties.baseUrl()).build();
	}

}