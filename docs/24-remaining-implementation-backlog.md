# Pozostały backlog implementacyjny

| Pole          | Wartość |
|---------------|---------|
| Dokument      | `24-remaining-implementation-backlog.md` |
| Status        | Draft |
| Powiązane     | `18-javafx-tasks.md`, `17-implementation-plan.md`, `13-javafx-configurator.md` |

## 1. Cel

Celem dokumentu jest zebranie pozostałych obszarów implementacyjnych po aktualnym etapie prac nad CLI, JavaFX Configuratorem, workspace preprocessingiem, dokumentami wzorcowymi, diagnostyką i packagingiem.

Dokument nie zastępuje szczegółowego backlogu w `18-javafx-tasks.md`. Jest krótką listą priorytetów do wyboru kolejnego etapu implementacji.

## 2. Najważniejsze obszary do implementacji

### 2.1. FX-011 Viewer Layers

Dodać przełączalne warstwy overlay w podglądzie dokumentu:

- OCR:
  - words,
  - lines,
  - paragraphs,
  - areas,
- anchors,
- field regions,
- current selection,
- diagnostics.

Warstwa OCR powinna być rozwijana w UI i umożliwiać niezależne włączanie poziomów `words`, `lines`, `paragraphs` i `areas`. Każdy poziom OCR może być zaznaczany odrębnie.

Warstwy powinny być niezależnie włączane i wyłączane, a ich położenie musi pozostawać spójne przy zoomie, scrollu i zmianie strony.

### 2.2. FX-020 / FX-021 / FX-022 OCR Explorer

Dodać panel eksploracji wyników OCR:

- lista słów, linii i bloków z HOCR,
- filtrowanie po tekście,
- confidence,
- bounds,
- page,
- synchronizacja wyboru z overlay na viewerze.

Dodatkowo należy dodać akcje na elemencie OCR:

- `Use as Identification Condition`,
- `Use as Anchor`,
- `Copy text`.

### 2.3. FX-032 Test Identification

Dodać osobne testowanie identyfikacji kategorii.

Zakres:

- uruchomienie samego etapu identification,
- wynik dla grup i warunków,
- pokazanie rozpoznanego tekstu użytego przez condition,
- wskazanie powodów braku identyfikacji kategorii.

### 2.4. FX-041 / FX-042 Anchor Workflows

Dokończyć workflow anchorów:

- tekstowy anchor,
- QR anchor,
- wybór regionu referencyjnego,
- wybór search region,
- test anchor,
- prezentacja wyniku detekcji na viewerze.

QR anchor pozostaje istotnym elementem P1 ze względu na stabilizację geometrii dokumentu.

### 2.5. FX-051 Test Geometry

Dodać osobne testowanie geometrii:

- znalezione anchors,
- brakujące anchors,
- transformacja,
- `GeometryStatus`,
- wizualizacja wyniku na viewerze.

### 2.6. FX-061 / FX-062 Field OCR Options i Output Settings

Zweryfikować i uzupełnić UI pól względem modelu konfiguracji:

- opcje OCR pola,
- ustawienia output,
- flagi eksportu,
- walidacja wymaganych wartości,
- tooltipy i etykiety.

### 2.7. FX-101 / FX-102 Validation Navigation i Error Presentation

Rozwinąć obsługę walidacji i błędów:

- przejście z problemu walidacji do właściwego elementu drzewa,
- zaznaczenie problematycznego pola,
- czytelne komunikaty błędów,
- spójny format błędów operacji asynchronicznych,
- rozróżnienie błędów użytkownika i błędów technicznych.

### 2.8. FX-121 Keyboard Shortcuts

Wykonać audyt i uzupełnić skróty klawiaturowe.

Zakres powinien objąć co najmniej:

- zapis,
- test category,
- test all documents,
- preview field,
- validate,
- tryby viewera,
- zoom,
- nawigację stron,
- akcje edycji elementów konfiguracji.

### 2.9. FX-131 JavaFX Smoke Test

Dodać smoke test uruchomienia artefaktu JavaFX albo opisać ręczną procedurę testową dla paczki:

- build `javafx-*-all.jar`,
- uruchomienie aplikacji,
- sprawdzenie ładowania ikon,
- sprawdzenie ładowania rozszerzeń,
- sprawdzenie startu bez brakujących zależności JavaFX.

Test automatyczny może być pomijany w środowisku headless, jeśli JavaFX nie ma dostępnego toolkit.

## 3. Obszary P2 / po MVP

### 3.1. FX-140 Raw JSON View

Dodać opcjonalny podgląd surowego JSON:

- read-only JSON,
- odświeżanie po zmianach,
- kopiowanie do clipboard,
- diff względem ostatniego zapisu.

### 3.2. FX-141 Undo / Redo

Dodać historię zmian draftu:

- command stack,
- undo,
- redo,
- dirty state zależny od historii,
- limit historii.

### 3.3. FX-142 OCR Snapping

Dodać przyciąganie regionów do OCR bounds:

- snap do najbliższego word,
- snap do grupy zaznaczonych words,
- toggle snapping.

### 3.4. FX-143 Page Thumbnails

Dodać miniatury stron:

- lazy thumbnails,
- wybór strony,
- oznaczanie stron z elementami konfiguracji.

### 3.5. FX-144 Performance Telemetry

Dodać diagnostykę wydajności preview:

- czas OCR,
- czas image processing,
- czas transformers,
- czas validators,
- cache hit / miss.

### 3.6. Rozszerzenia poza UI

Po MVP można rozważyć:

- plugin JAR hot-loading poza classpath,
- dodatkowe formaty kodów kreskowych,
- dodatkowe tryby CLI,
- recursive scanning,
- quiet mode,
- JSONL output,
- zaawansowane image processors.

## 4. Rekomendowana kolejność

Najbardziej sensowny kolejny etap:

1. `FX-011 Viewer Layers`
2. `FX-020 / FX-021 / FX-022 OCR Explorer`
3. `FX-032 Test Identification`
4. `FX-041 / FX-042 Anchor Workflows`
5. `FX-051 Test Geometry`

Uzasadnienie: warstwy viewera i eksplorator OCR odblokowują diagnostykę oraz przyspieszają konfigurację identification, anchorów i geometrii.
