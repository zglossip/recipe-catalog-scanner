package com.zglossip.recipescanner.parse;

import com.zglossip.recipescanner.domain.Recipe;
import com.zglossip.recipescanner.domain.ScannedRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class RecipeParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecipeParser.class);

	private static final int SECTION_WINDOW = 10000;

	private final OllamaClient ollamaClient;

	public RecipeParser(OllamaClient ollamaClient) {
		this.ollamaClient = ollamaClient;
	}

	private String extractSection(String text, String recipeName) {
		int idx = text.toLowerCase().indexOf(recipeName.toLowerCase());
		if (idx == -1) {
			LOGGER.warn("Could not find recipe '{}' in text, using full text", recipeName);
			return text;
		}
		int start = Math.max(0, idx - 200);
		int end = Math.min(text.length(), idx + SECTION_WINDOW);
		return text.substring(start, end);
	}

	@Cacheable("recipes")
	public List<ScannedRecipe> parse(String text, List<String> excludedNames) {
		LOGGER.info("Sending OCR text to Ollama ({} chars)", text.length());
		try {
			NamesResult namesResult = ollamaClient.generateNames(text);
			List<String> namesToExtract = namesResult.names().stream()
					.filter(name -> !excludedNames.contains(name))
					.toList();
			LOGGER.info("Found {} recipes, {} excluded, extracting {}", namesResult.names().size(), excludedNames.size(), namesToExtract.size());

			List<ScannedRecipe> recipes = new ArrayList<>();
			for (String name : namesToExtract) {
				try {
					ParsedRecipesResult result = ollamaClient.generateRecipes(extractSection(text, name));
					result.recipes().stream()
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
							.forEach(recipes::add);
				} catch (Exception e) {
					LOGGER.warn("Failed to extract recipe '{}', skipping", name, e);
				}
			}
			return recipes;
		} catch (Exception e) {
			LOGGER.warn("Failed to identify recipes", e);
			return List.of();
		}
	}
}
