package com.zglossip.recipescanner.domain;

import java.util.List;

public record ScannedRecipe(Recipe recipe, List<Ingredient> ingredients, List<String> instructions) {
}
