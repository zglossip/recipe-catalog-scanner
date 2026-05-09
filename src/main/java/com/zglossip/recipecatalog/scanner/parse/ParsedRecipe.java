package com.zglossip.recipecatalog.scanner.parse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zglossip.recipecatalog.scanner.domain.Ingredient;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record ParsedRecipe(
		String name,
		Integer servingAmount,
		String servingName,
		List<Ingredient> ingredients,
		List<String> instructions
) {
}
