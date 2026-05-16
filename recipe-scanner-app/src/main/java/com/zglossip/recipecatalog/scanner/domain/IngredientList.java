package com.zglossip.recipecatalog.scanner.domain;

import java.util.List;

public record IngredientList(Long recipeId, List<Ingredient> ingredients) {
}
