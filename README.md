# recipe-scanner
Reads files containing recipes such as PDFs and images, and sends it to the food-history-api

## Requirements

- **Java 21**
- **Tesseract OCR** installed on the host machine
  - macOS: `brew install tesseract`
  - Ubuntu/Debian: `sudo apt install tesseract-ocr`
  - Windows: [installer](https://github.com/UB-Mannheim/tesseract/wiki)
  - Language data must be installed for any language you intend to scan (English is `tesseract-ocr-eng` on Debian-based systems)
- **Google API key** — set as an environment variable:
  ```bash
  export GOOGLE_API_KEY=...
  ```
  Obtain a free-tier key at [aistudio.google.com](https://aistudio.google.com)

## Configuration

Set the downstream API base URL and OCR language in `src/main/resources/application.yaml`:

```yaml
food-history-api:
  base-url: "<food-history-api base URL>"

ocr:
  language: "eng"
```

`food-history-api` must be running and accessible. See that project for setup instructions.

`ocr.language` must match a Tesseract language code installed on your machine. Run `tesseract --list-langs` to see available languages.

## Upload limits

Multipart uploads are capped at 50MB by default. Adjust in `src/main/resources/application.yaml`:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```
