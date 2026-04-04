# recipe-scanner
Reads files containing recipes such as PDFs and images, and sends it to the recipe-catalog-api

## Requirements

- **Java 21**
- **Tesseract OCR** installed on the host machine
  - macOS: `brew install tesseract`
  - Ubuntu/Debian: `sudo apt install tesseract-ocr`
  - Windows: [installer](https://github.com/UB-Mannheim/tesseract/wiki)
  - Language data must be installed for any language you intend to scan (English is `tesseract-ocr-eng` on Debian-based systems)
- **Ollama** running and accessible with a model loaded

## Configuration

The following properties must be supplied at runtime — they are not included in the packaged jar.

| Property | Description |
|---|---|
| `recipe-catalog-api.base-url` | Base URL of the recipe-catalog-api |
| `ollama.base-url` | Base URL of the Ollama instance |
| `ollama.model` | Ollama model to use for recipe extraction |

`recipe-catalog-api` must be running and accessible. See that project for setup instructions.

`ocr.language` in `application.yaml` must match a Tesseract language code installed on your machine. Run `tesseract --list-langs` to see available languages.

### Running locally (development)

Create `src/main/resources/application-dev.yaml` (gitignored):

```yaml
recipe-catalog-api:
  base-url: "http://<host>:5000"

ollama:
  base-url: "http://<host>:11434"
  model: "<model>"
```

Then run with the `dev` profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Or set **Active profiles** to `dev` in your IDE run configuration.

### Deploying

Place an `application.yaml` alongside the jar with the required properties, or supply them as environment variables:

```bash
RECIPE_CATALOG_API_BASE_URL=http://... OLLAMA_BASE_URL=http://... OLLAMA_MODEL=... java -jar recipe-scanner.jar
```

## Upload limits

Multipart uploads are capped at 50MB by default. Adjust in `src/main/resources/application.yaml`:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```
