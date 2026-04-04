package com.zglossip.recipescanner.parse;

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

	private final OllamaClient ollamaClient;

	public RecipeParser(OllamaClient ollamaClient) {
		this.ollamaClient = ollamaClient;
	}

	public List<ScannedRecipe> parse(String text) {
		LOGGER.info("Parsing OCR text ({} chars)", text.length());
		ParsedRecipesResult result = ollamaClient.generateRecipes(text);
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
	}
}