package com.zglossip.recipecatalog.scanner.parse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParsedRecipesResult(List<ParsedRecipe> recipes) {
}