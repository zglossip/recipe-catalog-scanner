package com.zglossip.recipescanner.parse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record ParsedRecipesResult(List<ParsedRecipe> recipes) {
}