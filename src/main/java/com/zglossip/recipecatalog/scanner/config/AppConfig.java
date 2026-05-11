package com.zglossip.recipecatalog.scanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AppConfig {

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper().findAndRegisterModules();
	}

	@Bean
	@Qualifier("ollamaRestClient")
	public RestClient ollamaRestClient(OllamaProperties ollamaProperties) {
		return buildRestClient(ollamaProperties.baseUrl(), Duration.ofSeconds(5), Duration.ofSeconds(120));
	}

	@Bean
	@Qualifier("recipeCatalogApiRestClient")
	public RestClient recipeCatalogApiRestClient(RecipeCatalogApiProperties recipeCatalogApiProperties) {
		return buildRestClient(recipeCatalogApiProperties.baseUrl(), Duration.ofSeconds(5), Duration.ofSeconds(30));
	}

	private RestClient buildRestClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
	}

}