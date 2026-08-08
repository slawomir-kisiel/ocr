# SK OCR

Configurable OCR processing platform for identifying document categories and extracting validated data from scanned documents.

The project follows the documentation in `docs/`. The first implementation milestone is a Maven multi-module skeleton targeting Java 21.

## Build

```bash
mvn clean verify
```

OCR integration tests that require a local Tesseract installation will use a separate Maven profile:

```bash
mvn verify -Pocr-integration
```
