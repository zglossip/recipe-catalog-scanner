package com.zglossip.recipescanner.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OllamaClient {

	private static final String RECIPE_SYSTEM = """
			You are a recipe extraction assistant. Extract the recipe from OCR-scanned text and map it to the following structure:

			- name: the full name of the recipe.
			- servingAmount: the number of servings as an integer (e.g. 4).
			- servingName: the label for a single serving. Use "serving" by default, but use a more specific term if the recipe implies one (e.g. "cookie", "slice", "piece").
			- ingredients: a list of ingredients, each with:
			  - name: the ingredient name (e.g. "all-purpose flour").
			  - quantity: the numeric amount (e.g. 2.5). Omit if not specified.
			  - uom: the unit of measure (e.g. "cups", "tsp", "oz"). Omit if not specified.
			  - notes: any preparation or clarifying notes (e.g. "sifted", "at room temperature", "finely chopped"). Omit if not specified.
			- instructions: an ordered list of steps, each as a plain string.
			""";

	private static final RecipesResultSchema RECIPE_SCHEMA = new RecipesResultSchema();

	private final RestClient restClient;
	private final String model;
	private final ObjectMapper objectMapper;

	public OllamaClient(RestClient restClient, @Value("${ollama.model}") String model, ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.model = model;
		this.objectMapper = objectMapper;
	}

	public ParsedRecipesResult generateRecipes(String text) throws Exception {
		OllamaRequest request = new OllamaRequest(
				model,
				List.of(
						new OllamaMessage("system", RECIPE_SYSTEM),
						new OllamaMessage("user", "Extract the recipe from this OCR text:\n\n" + text)
				),
				RECIPE_SCHEMA,
				false
		);
		OllamaResponse response = restClient.post()
				.uri("/api/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(OllamaResponse.class);
		if (response == null || response.message() == null) {
			throw new IllegalStateException("Received null response from Ollama");
		}
		return objectMapper.readValue(response.message().content(), ParsedRecipesResult.class);
	}

	private record StringSchema(String type) {
		StringSchema() { this("string"); }
	}

	private record NumberSchema(String type) {
		NumberSchema() { this("number"); }
	}

	private record StringArraySchema(String type, StringSchema items) {
		StringArraySchema() { this("array", new StringSchema()); }
	}

	private record IngredientProperties(StringSchema name, NumberSchema quantity, StringSchema uom, StringSchema notes) {
		IngredientProperties() { this(new StringSchema(), new NumberSchema(), new StringSchema(), new StringSchema()); }
	}

	private record IngredientSchema(String type, IngredientProperties properties, List<String> required) {
		IngredientSchema() { this("object", new IngredientProperties(), List.of("name")); }
	}

	private record IngredientArraySchema(String type, IngredientSchema items) {
		IngredientArraySchema() { this("array", new IngredientSchema()); }
	}

	private record RecipeProperties(StringSchema name, NumberSchema servingAmount, StringSchema servingName, IngredientArraySchema ingredients, StringArraySchema instructions) {
		RecipeProperties() { this(new StringSchema(), new NumberSchema(), new StringSchema(), new IngredientArraySchema(), new StringArraySchema()); }
	}

	private record RecipeObjectSchema(String type, RecipeProperties properties, List<String> required) {
		RecipeObjectSchema() { this("object", new RecipeProperties(), List.of("name")); }
	}

	private record RecipeArraySchema(String type, RecipeObjectSchema items) {
		RecipeArraySchema() { this("array", new RecipeObjectSchema()); }
	}

	private record RecipesResultProperties(RecipeArraySchema recipes) {
		RecipesResultProperties() { this(new RecipeArraySchema()); }
	}

	private record RecipesResultSchema(String type, RecipesResultProperties properties, List<String> required) {
		RecipesResultSchema() { this("object", new RecipesResultProperties(), List.of("recipes")); }
	}


}