package com.zglossip.recipecatalog.scanner.api;

import com.zglossip.recipecatalog.scanner.domain.ScannedRecipe;

import java.util.List;

public record RecipeScanResponse(List<ScannedRecipe> recipes, String scanned, String message) {
}
