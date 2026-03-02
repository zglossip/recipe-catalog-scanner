package com.zglossip.recipescanner.parse;

import com.zglossip.recipescanner.domain.Recipe;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecipeParser {
	public List<Recipe> parse(String text) {
		// TODO: Implement parsing rules to map OCR text into Recipe fields.
		// TODO: Extract tags, courseTypes, and cuisineTypes from parsed sections.
		return Collections.emptyList();
	}
}
