package com.zglossip.recipecatalog.scanner.client;

import com.zglossip.recipecatalog.scanner.domain.Ingredient;
import com.zglossip.recipecatalog.scanner.domain.ScannedRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class RecipeCatalogApiClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCatalogApiClient.class);

	private final RestClient restClient;

	public RecipeCatalogApiClient(@Qualifier("recipeCatalogApiRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public Boolean send(ScannedRecipe scannedRecipe) {
		try {
			RecipeResponse response = restClient.post()
					.uri("/recipe")
					.contentType(MediaType.APPLICATION_JSON)
					.body(new RecipeRequest(scannedRecipe))
					.retrieve()
					.body(RecipeResponse.class);
			LOGGER.info("Recipe submitted successfully id={}", response != null ? response.id() : null);
			return Boolean.TRUE;
		} catch (RestClientResponseException e) {
			LOGGER.error("Failed to submit recipe status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
			return Boolean.FALSE;
		} catch (RestClientException e) {
			LOGGER.error("Failed to submit recipe: transport error", e);
			return Boolean.FALSE;
		}
	}

	private record RecipeRequest(
			String name,
			List<String> courseTypes,
			List<String> cuisineTypes,
			List<String> tags,
			Integer servingAmount,
			String servingName,
			String source,
			List<Ingredient> ingredients,
			List<String> instructions
	) {
		RecipeRequest(ScannedRecipe scannedRecipe) {
			this(
					scannedRecipe.recipe().name(),
					scannedRecipe.recipe().courseTypes(),
					scannedRecipe.recipe().cuisineTypes(),
					scannedRecipe.recipe().tags(),
					// TODO: API and DB should be updated to accept nullable servingAmount (Phase 2.5 / migration framework needed first)
					scannedRecipe.recipe().servingAmount() != null ? scannedRecipe.recipe().servingAmount() : 0,
					scannedRecipe.recipe().servingName(),
					scannedRecipe.recipe().source(),
					scannedRecipe.ingredients().stream()
							// TODO: API and DB should be updated to accept nullable quantity (Phase 2.5 / migration framework needed first)
							.map(i -> i.quantity() != null ? i : new Ingredient(i.name(), 0.0, i.uom(), i.notes()))
							.toList(),
					scannedRecipe.instructions()
			);
		}
	}

	private record RecipeResponse(Integer id) {}

}
