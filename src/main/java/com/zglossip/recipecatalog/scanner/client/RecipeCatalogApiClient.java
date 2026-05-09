package com.zglossip.recipecatalog.scanner.client;

import com.zglossip.recipecatalog.scanner.domain.IngredientList;
import com.zglossip.recipecatalog.scanner.domain.InstructionList;
import com.zglossip.recipecatalog.scanner.domain.ScannedRecipe;
import org.springframework.stereotype.Component;

@Component
public class RecipeCatalogApiClient {

	public Boolean send(ScannedRecipe scannedRecipe) {
		// TODO: POST /recipe → deserialize response to get assigned id
		// TODO: PUT /recipe/{id}/ingredients with new IngredientList(id, scannedRecipe.ingredients())
		// TODO: PUT /recipe/{id}/instructions with new InstructionList(id, scannedRecipe.instructions())
		// TODO: Return false if any step fails
		return Boolean.FALSE;
	}
}
