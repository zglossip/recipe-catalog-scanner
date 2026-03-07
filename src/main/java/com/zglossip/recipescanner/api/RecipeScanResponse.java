package com.zglossip.recipescanner.api;

import com.zglossip.recipescanner.domain.ScannedRecipe;

import java.util.List;

public record RecipeScanResponse(List<ScannedRecipe> recipes, String scanned, String message) {
}
