package com.zglossip.recipecatalog.scanner.api;

import com.zglossip.recipecatalog.scanner.domain.ScannedRecipe;
import com.zglossip.recipecatalog.scanner.service.RecipeScanService;
import com.zglossip.recipecatalog.scanner.validation.UploadedFileValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/recipes")
public class RecipeScanController {
	private final RecipeScanService recipeScanService;
	private final UploadedFileValidator uploadedFileValidator;

	public RecipeScanController(
			RecipeScanService recipeScanService,
			UploadedFileValidator uploadedFileValidator
	) {
		this.recipeScanService = recipeScanService;
		this.uploadedFileValidator = uploadedFileValidator;
	}

	@PostMapping(path = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<RecipeScanResponse> scan(@RequestPart("file") MultipartFile file) {
		uploadedFileValidator.validateForScan(file);
		return ResponseEntity.ok(recipeScanService.scan(file));
	}

	@PostMapping(path = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Boolean> submit(@RequestBody List<ScannedRecipe> submission) {
		return ResponseEntity.ok(recipeScanService.submit(submission));
	}
}
