package com.zglossip.recipecatalog.scanner.domain;

import java.util.List;

public record InstructionList(Long recipeId, List<String> instructions) {
}
