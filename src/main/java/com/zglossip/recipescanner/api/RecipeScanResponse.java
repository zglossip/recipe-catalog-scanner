package com.zglossip.recipescanner.api;

import com.zglossip.recipescanner.domain.Recipe;

import java.util.List;

public record RecipeScanResponse(List<Recipe> recipes, String scanned, String message) {
}
