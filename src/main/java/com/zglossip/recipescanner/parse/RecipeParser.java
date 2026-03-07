package com.zglossip.recipescanner.parse;

import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zglossip.recipescanner.domain.Recipe;
import com.zglossip.recipescanner.domain.ScannedRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RecipeParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecipeParser.class);

	private static final Schema RECIPE_SCHEMA = Schema.builder()
			.type(Type.Known.OBJECT)
			.properties(ImmutableMap.of(
					"recipes", Schema.builder()
							.type(Type.Known.ARRAY)
							.items(Schema.builder()
									.type(Type.Known.OBJECT)
									.properties(ImmutableMap.of(
											"name",          Schema.builder().type(Type.Known.STRING).build(),
											"servingAmount", Schema.builder().type(Type.Known.INTEGER).build(),
											"servingName",   Schema.builder().type(Type.Known.STRING).build(),
											"ingredients",   Schema.builder()
													.type(Type.Known.ARRAY)
													.items(Schema.builder()
															.type(Type.Known.OBJECT)
															.properties(ImmutableMap.of(
																	"name",     Schema.builder().type(Type.Known.STRING).build(),
																	"quantity", Schema.builder().type(Type.Known.NUMBER).build(),
																	"uom",      Schema.builder().type(Type.Known.STRING).build(),
																	"notes",    Schema.builder().type(Type.Known.STRING).build()
															))
															.required(List.of("name")).build())
													.build(),
											"instructions",  Schema.builder()
													.type(Type.Known.ARRAY)
													.items(Schema.builder().type(Type.Known.STRING).build())
													.build()
									))
									.required(List.of("name")).build())
							.build()
			))
			.required(List.of("recipes"))
			.build();

	private static final GenerateContentConfig CONFIG = GenerateContentConfig.builder()
			.responseMimeType("application/json")
			.candidateCount(1)
			.responseSchema(RECIPE_SCHEMA)
			.systemInstruction(Content.fromParts(Part.fromText("""
					You are a recipe extraction assistant. Extract all recipes from OCR-scanned text and map them to the following structure:

					- name: the full name of the recipe.
					- servingAmount: the number of servings as an integer (e.g. 4).
					- servingName: the label for a single serving. Use "serving" by default, but use a more specific term if the recipe implies one (e.g. "cookie", "slice", "piece").
					- ingredients: a list of ingredients, each with:
					  - name: the ingredient name (e.g. "all-purpose flour").
					  - quantity: the numeric amount (e.g. 2.5). Omit if not specified.
					  - uom: the unit of measure (e.g. "cups", "tsp", "oz"). Omit if not specified.
					  - notes: any preparation or clarifying notes (e.g. "sifted", "at room temperature", "finely chopped"). Omit if not specified.
					- instructions: an ordered list of steps, each as a plain string.
					""")))
			.build();

	private final Client client;
	private final ObjectMapper objectMapper;

	public RecipeParser(Client client, ObjectMapper objectMapper) {
		this.client = client;
		this.objectMapper = objectMapper;
	}

	public List<ScannedRecipe> parse(String text) {
		try {
			GenerateContentResponse response = client.models.generateContent(
					"gemini-2.0-flash",
					"Extract all recipes from this OCR text:\n\n" + text,
					CONFIG
			);
			ParsedRecipesResult result = objectMapper.readValue(response.text(), ParsedRecipesResult.class);
			return result.recipes().stream()
					.map(p -> new ScannedRecipe(
							new Recipe(
									p.name(),
									List.of(),
									List.of(),
									List.of(),
									p.servingAmount(),
									p.servingName(),
									"",
									Instant.now()
							),
							p.ingredients() != null ? p.ingredients() : List.of(),
							p.instructions() != null ? p.instructions() : List.of()
					))
					.toList();
		} catch (Exception e) {
			LOGGER.warn("Failed to parse recipes", e);
			return List.of();
		}
	}
}
