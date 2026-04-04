package com.zglossip.recipescanner.service;

import com.zglossip.recipescanner.api.RecipeScanResponse;
import com.zglossip.recipescanner.client.RecipeCatalogApiClient;
import com.zglossip.recipescanner.domain.ScannedRecipe;
import com.zglossip.recipescanner.extract.TextExtractor;
import com.zglossip.recipescanner.parse.RecipeParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class RecipeScanService {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecipeScanService.class);
	private final List<TextExtractor> textExtractors;
	private final RecipeParser recipeParser;
	private final RecipeCatalogApiClient foodHistoryApiClient;

	public RecipeScanService(
			List<TextExtractor> textExtractors,
			RecipeParser recipeParser,
			RecipeCatalogApiClient foodHistoryApiClient
	) {
		this.textExtractors = textExtractors;
		this.recipeParser = recipeParser;
		this.foodHistoryApiClient = foodHistoryApiClient;
	}

	public RecipeScanResponse scan(MultipartFile file) {
		LOGGER.info("Scanning recipe file name={} contentType={} sizeBytes={}",
				file.getOriginalFilename(),
				file.getContentType(),
				file.getSize());

		TextExtractor extractor = textExtractors.stream()
				.filter(candidate -> candidate.supports(file))
				.findFirst()
				.orElse(null);

		if (extractor == null) {
			LOGGER.warn("Unsupported file type contentType={}", file.getContentType());
			throw new ResponseStatusException(
					HttpStatus.UNSUPPORTED_MEDIA_TYPE,
					"Unsupported file type."
			);
		}

		LOGGER.info("Selected extractor={}", extractor.getClass().getSimpleName());

		String text = extractor.extract(file);

		if (text == null || text.isBlank()) {
			LOGGER.warn("OCR produced no text filename={} contentType={}",
					file.getOriginalFilename(),
					file.getContentType());
			throw new ResponseStatusException(
					HttpStatus.UNPROCESSABLE_ENTITY,
					"No text could be extracted from the file."
			);
		}
		return new RecipeScanResponse(recipeParser.parse(text), text, "Recipe scanned successfully.");
	}

	public Boolean submit(List<ScannedRecipe> recipes) {
		return recipes.stream()
				.map(foodHistoryApiClient::send)
				.reduce(Boolean.TRUE, (b1, b2) -> b1 && b2);
	}
}
