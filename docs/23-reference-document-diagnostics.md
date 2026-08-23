# Diagnostyka wielu dokumentów wzorcowych

| Pole          | Wartość |
|---------------|---------|
| Dokument      | `23-reference-document-diagnostics.md` |
| Status        | Draft |
| Powiązane     | `18-javafx-tasks.md`, `21-workspace-preprocessing.md`, `22-category-reference-documents.md` |

## 1. Cel

Celem zmiany jest domknięcie diagnostyki dla operacji `Test All Documents`, czyli testowania kategorii na wszystkich dokumentach wzorcowych przypisanych do kategorii.

Po tej zmianie użytkownik powinien móc:

- uruchomić test kategorii dla wielu dokumentów wzorcowych,
- zobaczyć wynik każdego dokumentu osobno,
- kliknąć dokument i zobaczyć trace właściwy tylko dla niego,
- zobaczyć obrazy preprocessingu, OCR i pipeline'u dla wskazanego dokumentu,
- wyeksportować paczkę diagnostyczną rozdzieloną per dokument wzorcowy.

## 2. Problem

Aktualnie UI posiada:

- `Test Category` dla bieżącego dokumentu,
- `Test All Documents` dla listy dokumentów wzorcowych,
- panel wyników zbiorczych,
- `Trace Viewer`,
- eksport diagnostyczny trace.

Brakującym elementem jest pełne powiązanie wyniku zbiorczego z trace konkretnego dokumentu.

W praktyce użytkownik widzi, że dokument `dark-skewed` nie przeszedł testu, ale nie ma wystarczająco wygodnej ścieżki, aby kliknąć ten dokument i natychmiast zobaczyć jego własne:

- obrazy po workspace preprocessing,
- OCR wejściowy dla kategoryzacji,
- szczegóły identyfikacji,
- trace pól,
- obrazy diagnostyczne użyte w danym przebiegu.

## 3. Zakres funkcjonalny

### RD-DIAG-001 Trace per dokument wzorcowy

Każdy wynik `Test All Documents` powinien przechowywać własny trace.

Model UI powinien zawierać co najmniej:

- `referenceDocumentId`,
- `referenceDocumentPath`,
- `resolvedPath`,
- `DocumentResult`,
- `ProcessingTrace`,
- `TraceImageStore` albo mechanizm dostępu do obrazów trace danego dokumentu.

Jeśli `DocumentResult.trace()` już zawiera trace, model UI nadal musi rozwiązać problem obrazów, ponieważ obrazy są przechowywane poza samym `ProcessingTrace`.

### RD-DIAG-002 Przełączanie Trace Viewer po wyborze dokumentu

Panel wyników `Test All Documents` powinien emitować informację o aktualnie wybranym wyniku dokumentu.

Po wyborze dokumentu:

- `Trace Viewer` pokazuje trace wybranego dokumentu,
- panel `Images` pokazuje obrazy trace wybranego dokumentu,
- podgląd obrazów w dialogu działa dla obrazów tego dokumentu,
- status UI może pokazać aktualnie analizowany dokument.

Nie należy mieszać trace wielu dokumentów w jednym widoku bez jawnego wyboru dokumentu.

### RD-DIAG-003 Trace workspace preprocessing w `Test All Documents`

Operacja `Test All Documents` powinna wykorzystywać traced wariant preprocessingu.

Dla każdego dokumentu i każdej strony/kroku należy zachować:

- obraz wejściowy kroku,
- obraz wyjściowy kroku,
- numer strony,
- kolejność kroku,
- `processorId`,
- zdarzenia `TraceSink`.

Trace powinien używać tych samych zasad co `Apply preprocessing`.

### RD-DIAG-004 Eksport diagnostyczny wyniku zbiorczego

Eksport diagnostyczny powinien obsługiwać wynik `Test All Documents`.

Rekomendowana struktura ZIP:

```text
metadata.json
documents/
  <referenceDocumentId>/
    trace.json
    images/
      001_page-preparation_input_<processorId>.png
      002_page-preparation_output_<processorId>.png
      ...
```

`metadata.json` powinien zawierać indeks dokumentów:

```json
{
  "documents": [
    {
      "referenceDocumentId": "dark-skewed",
      "referenceDocumentPath": "samples/dark-skewed.pdf",
      "status": "FAILED",
      "categoryId": null,
      "issueCodes": ["CATEGORY_NOT_IDENTIFIED"]
    }
  ]
}
```

Nazwy plików nie powinny zawierać danych wrażliwych ani pełnych ścieżek absolutnych.

### RD-DIAG-005 Zachowanie dla błędów

Jeśli dokument wzorcowy nie istnieje albo nie da się go otworzyć:

- wynik dokumentu powinien być widoczny jako `FAILED`,
- issue powinien mieć kod np. `REFERENCE_DOCUMENT_TEST_FAILED`,
- trace może być pusty,
- eksport powinien nadal zawierać wpis w `metadata.json`.

Błąd jednego dokumentu nie może przerywać testowania pozostałych dokumentów.

## 4. Proponowana implementacja

### 4.1 Model wyniku UI

Rozszerzyć aktualny model `CategoryReferenceDocumentTestResult`.

Proponowany kierunek:

```java
public record CategoryReferenceDocumentTestResult(
    String referenceDocumentId,
    String referenceDocumentPath,
    Path resolvedPath,
    DocumentResult result,
    TraceImageStore traceImageStore
) {
}
```

Alternatywnie można wprowadzić osobny wrapper:

```java
public record CategoryReferenceDocumentDiagnosticResult(
    CategoryReferenceDocumentTestResult testResult,
    ProcessingTrace trace,
    TraceImageStore traceImageStore
) {
}
```

Pierwsza opcja jest prostsza, bo `DocumentResult` już zawiera `ProcessingTrace`.

### 4.2 Panel wyników

`CategoryTestResultPanel` powinien dostać callback:

```java
Consumer<CategoryReferenceDocumentTestResult> onResultSelected
```

Po zaznaczeniu wiersza dokumentu panel wywołuje callback, a `ConfiguratorApplication` ustawia aktywny trace w sesji albo w dedykowanym stanie trace viewer.

### 4.3 Trace Viewer

Preferowane są dwa możliwe warianty:

1. `TraceViewerPanel` czyta trace i store z dostawców zależnych od aktualnie wybranego wyniku.
2. `ConfiguratorApplication` aktualizuje `session.latestTrace()` i aktywny `TraceImageStore`.

Rekomendacja: wariant 1.

Powód: `session.traceImageStore()` jest obecnie globalnym store bieżącej sesji. Nadpisywanie go przy wyborze wyniku dokumentu mogłoby mieszać diagnostykę bieżącego preview z diagnostyką zbiorczą.

### 4.4 Preprocessing trace w teście wielu dokumentów

W `Test All Documents` nie używać prostego helpera zwracającego tylko mapę stron.

Zamiast tego:

- dla każdego dokumentu utworzyć osobny `InMemoryTraceImageStore`,
- wykonać `DocumentImagePreprocessingService.prepareWithTrace(...)`,
- zamienić `DocumentImagePreprocessingResult.StepTrace` na `TraceEntry`,
- połączyć trace preprocessingu z trace zwróconym przez `TestCategoryUseCase`.

Kolejność trace entries:

1. `PAGE_PREPARATION` dla workspace preprocessing,
2. `CATEGORY_IDENTIFICATION`,
3. kolejne etapy wynikające z pipeline'u.

### 4.5 Eksport

Rozszerzyć `DiagnosticExportUseCase` albo dodać osobny use case:

```java
ReferenceDocumentDiagnosticExportUseCase
```

Rekomendacja: osobny use case, ponieważ eksport wielu dokumentów ma inną strukturę katalogów i własny plik indeksu.

## 5. Kryteria akceptacji

Zmiana jest gotowa, jeśli:

- po `Test All Documents` tabela pokazuje wszystkie dokumenty wzorcowe,
- kliknięcie dokumentu przełącza `Trace Viewer` na trace tego dokumentu,
- panel `Images` pokazuje obrazy tylko wybranego dokumentu,
- obrazy workspace preprocessing są widoczne dla każdego dokumentu,
- dwuklik miniatury otwiera podgląd obrazu,
- eksport ZIP zawiera osobny katalog dla każdego dokumentu,
- brakujący dokument ma wpis w wynikach i eksporcie,
- błąd jednego dokumentu nie przerywa całej operacji,
- testy pokrywają co najmniej:
  - zachowanie dla dwóch dokumentów,
  - brakujący dokument,
  - trace image store per dokument,
  - strukturę eksportu.

## 6. Kolejność implementacji

Rekomendowana kolejność:

1. Rozszerzyć model wyniku `CategoryReferenceDocumentTestResult` o `TraceImageStore`.
2. Dodać callback wyboru wyniku w `CategoryTestResultPanel`.
3. Przełączyć `TraceViewerPanel` na trace/store aktualnie wybranego wyniku.
4. Dodać workspace preprocessing trace do `Test All Documents`.
5. Dodać eksport ZIP per dokument wzorcowy.
6. Dodać testy jednostkowe i integracyjne UI/app.

## 7. Poza zakresem

Na tym etapie nie implementujemy:

- porównywania trace między dokumentami,
- automatycznej klasyfikacji różnic obrazów,
- batchowego testowania wielu kategorii,
- zapisu historii wszystkich uruchomień testów,
- osobnych ustawień preprocessingu per dokument wzorcowy.
