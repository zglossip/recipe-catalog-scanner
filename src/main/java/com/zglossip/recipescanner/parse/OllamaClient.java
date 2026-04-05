package com.zglossip.recipescanner.parse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zglossip.recipescanner.config.OllamaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OllamaClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaClient.class);

	private static final String RECIPE_SYSTEM = """
			You are a recipe extraction assistant. Extract all recipes from OCR-scanned text and map each one to the following structure:

			- name: the full name of the recipe.
			- servingAmount: the number of servings as an integer (e.g. 4).
			- servingName: the label for a single serving. Use "serving" by default, but use a more specific term if the recipe implies one (e.g. "cookie", "slice", "piece").
			- ingredients: a list of ingredients, each with:
			  - name: ONLY the core ingredient name, stripped of quantity, unit, and preparation notes (e.g. "all-purpose flour", "sweet potato", "black pepper"). Do NOT include preparation instructions or descriptors in the name.
			  - quantity: the numeric amount (e.g. 2.5). Omit if not specified. For fractional amounts like 1/4, use the decimal equivalent (0.25).
			  - uom: the unit of measure (e.g. "cups", "tsp", "oz", "pinch", "clove"). Omit if not specified. Informal units like "pinch" or "dash" are valid uom values.
			  - notes: any preparation or clarifying notes separated from the ingredient name (e.g. "sifted", "at room temperature", "finely chopped", "peeled and diced", "freshly ground"). Omit if not specified.
			- instructions: an ordered list of steps, each as a plain string.

			Ingredient parsing rules:
			- "1 large sweet potato, peeled and cut into 1/2-inch dice" → name: "sweet potato", quantity: 1, notes: "large, peeled and cut into 1/2-inch dice"
			- "pinch of freshly ground black pepper" → name: "black pepper", uom: "pinch", notes: "freshly ground"
			- "2 cups chicken broth" → name: "chicken broth", quantity: 2, uom: "cups"
			- "3 cloves garlic, minced" → name: "garlic", quantity: 3, uom: "cloves", notes: "minced"
			""";

	private static final RecipesResultSchema RECIPE_SCHEMA = new RecipesResultSchema();

	private final RestClient restClient;
	private final String model;
	private final int numCtx;
	private final ObjectMapper objectMapper;

	public OllamaClient(RestClient restClient, OllamaProperties ollamaProperties, ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.model = ollamaProperties.model();
		this.numCtx = ollamaProperties.numCtx();
		this.objectMapper = objectMapper;
	}

	private static final int CHUNK_SIZE = 30_000;
	private static final int CHUNK_OVERLAP = 15_000;

	public ParsedRecipesResult generateRecipes(String text) {
		List<String> chunks = chunk(text);
		LOGGER.info("Processing text in {} chunk(s) totalChars={}", chunks.size(), text.length());
		List<ParsedRecipe> allRecipes = new ArrayList<>();
		for (int i = 0; i < chunks.size(); i++) {
			LOGGER.info("Processing chunk {}/{} chars={}", i + 1, chunks.size(), chunks.get(i).length());
			allRecipes.addAll(generateChunk(chunks.get(i)).recipes());
			LOGGER.info("Chunk {}/{} complete recipiesSoFar={}", i + 1, chunks.size(), allRecipes.size());
		}
		List<ParsedRecipe> deduped = deduplicate(allRecipes);
		LOGGER.info("Parsing complete totalRecipes={} beforeDedup={}", deduped.size(), allRecipes.size());
		return new ParsedRecipesResult(deduped);
	}

	private List<ParsedRecipe> deduplicate(List<ParsedRecipe> recipes) {
		List<ParsedRecipe> deduped = new ArrayList<>();
		for (ParsedRecipe recipe : recipes) {
			int existingIndex = -1;
			for (int i = 0; i < deduped.size(); i++) {
				if (deduped.get(i).name() != null && deduped.get(i).name().equalsIgnoreCase(recipe.name())) {
					existingIndex = i;
					break;
				}
			}
			if (existingIndex == -1) {
				deduped.add(recipe);
			} else if (recipeLength(recipe) > recipeLength(deduped.get(existingIndex))) {
				deduped.set(existingIndex, recipe);
			}
		}
		return deduped;
	}

	private int recipeLength(ParsedRecipe recipe) {
		int length = recipe.name() != null ? recipe.name().length() : 0;
		if (recipe.ingredients() != null) {
			for (var ingredient : recipe.ingredients()) {
				if (ingredient.name() != null) length += ingredient.name().length();
				if (ingredient.uom() != null) length += ingredient.uom().length();
				if (ingredient.notes() != null) length += ingredient.notes().length();
			}
		}
		if (recipe.instructions() != null) {
			for (String step : recipe.instructions()) {
				if (step != null) length += step.length();
			}
		}
		return length;
	}

	private ParsedRecipesResult generateChunk(String text) {
		OllamaRequest request = new OllamaRequest(
				model,
				List.of(
						new OllamaMessage("system", RECIPE_SYSTEM),
						new OllamaMessage("user", "Extract all recipes from this OCR text:\n\n" + text)
				),
				RECIPE_SCHEMA,
				false,
				Map.of("num_ctx", numCtx)
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
		String content = response.message().content();
		LOGGER.debug("Raw Ollama response: {}", content);
		try {
			return objectMapper.readValue(content, ParsedRecipesResult.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to parse Ollama response as JSON", e);
		}
	}

	private List<String> chunk(String text) {
		List<String> chunks = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			int end = Math.min(start + CHUNK_SIZE, text.length());
			if (end < text.length()) {
				int newline = text.lastIndexOf('\n', end);
				if (newline > start) {
					end = newline + 1;
				}
			}
			chunks.add(text.substring(start, end));
			if (end >= text.length()) {
				break;
			}
			start = end - CHUNK_OVERLAP;
		}
		return chunks;
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