package com.zglossip.recipecatalog.scanner.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Ingredient(String name, Double quantity, String uom, String notes) {
}
