# Workspace preprocessing obrazu dokumentu

## 1. Cel

Celem zmiany jest dodanie konfigurowalnego pipeline'u preprocessingu obrazu dokumentu na poziomie profilu/workspace.

Pipeline ma byc wykonywany raz dla strony dokumentu po renderowaniu, ale przed:

- OCR strony,
- identyfikacja kategorii,
- wykrywaniem anchorow,
- konfiguracja i ekstrakcja pol.

Ten dokument jest wsadem do implementacji. Doprecyzowuje kontrakt, model konfiguracji, zmiany w Core i zmiany w JavaFX Configurator.

## 2. Decyzja architektoniczna

Przyjmujemy zasade:

```text
Regiony konfigurujemy wzgledem obrazu po workspace preprocessing.
```

Oznacza to, ze powierzchnia konfiguracji nie jest surowy render strony, lecz obraz przygotowany przez pipeline profilu.

```text
raw render
-> workspace preprocessing
-> prepared page
-> viewer
-> page OCR
-> identification
-> anchors / geometry
-> field crop
-> field image processors
-> field OCR
```

Konsekwencje:

- JavaFX viewer pokazuje domyslnie `PreparedPage`.
- Regiony `condition.searchRegion`, `anchor.searchRegion`, `anchor.referenceFeature.bounds`, `field.region` sa zapisywane w koordynatach `PreparedPage`.
- OCR strony, identyfikacja, anchory, geometria i pola dzialaja na tym samym obrazie.
- Nie implementujemy mapowania koordynatow `RenderedPage -> PreparedPage`.
- Operacje zmieniajace geometrie obrazu, takie jak deskew, trim, auto crop, rotate albo resize, sa dozwolone w workspace preprocessing, bo konfiguracja powstaje juz po ich zastosowaniu.
- Surowy render moze byc dostepny diagnostycznie, ale nie jest powierzchnia edycji regionow.

## 3. Terminologia

| Termin | Znaczenie |
| ------ | --------- |
| `RenderedPage` | Obraz strony bezposrednio po renderowaniu PDF lub odczycie pliku obrazu. |
| `PreparedPage` | Obraz strony po wykonaniu workspace preprocessing. To powierzchnia konfiguracji. |
| `Workspace preprocessing` | Lista krokow `ImageProcessor` zdefiniowana na poziomie profilu. |
| `Field image processors` | Istniejacy pipeline obrazu pola wykonywany po cropie regionu pola. |
| `OCR input` | Dla page OCR jest to `PreparedPage`; dla field OCR jest to wynik cropu i field image processors. |

## 4. Model konfiguracji profilu

Do `ProfileDto` dodajemy opcjonalna sekcje `preprocessing`.

Przyklad:

```json
{
  "schemaVersion": "1.0",
  "id": "default",
  "version": "1.0",
  "categories": {
    "directory": "categories",
    "mode": "EXPLICIT",
    "active": ["invoice"]
  },
  "preprocessing": {
    "imageProcessors": [
      {
        "id": "im-normalize",
        "parameters": {
          "histogramRegion": "AUTO_STABLE_CENTER"
        }
      },
      {
        "id": "im-deskew",
        "parameters": {
          "autoCrop": true
        }
      }
    ]
  },
  "directories": {
    "input": "./input",
    "success": "./success",
    "error": "./error"
  },
  "processing": {
    "workers": 2
  },
  "ocr": {
    "language": "pol"
  },
  "output": {
    "csv": {
      "file": "./result.csv"
    }
  }
}
```

Proponowane DTO:

```java
public record ProfilePreprocessingDto(
    List<ExtensionRefDto> imageProcessors
) {
}
```

Zmiana w `ProfileDto`:

```java
ProfilePreprocessingDto preprocessing
```

Zasady kompatybilnosci:

- brak `preprocessing` oznacza pusty pipeline,
- brak `imageProcessors` oznacza pusta liste,
- zapis przez UI powinien deterministycznie zapisywac `preprocessing.imageProcessors`, jesli lista nie jest pusta,
- CLI musi akceptowac profile bez sekcji `preprocessing`.

## 5. Runtime model

Do runtime konfiguracji profilu dodajemy liste krokow preprocessingu:

```java
public record ProfilePreprocessingConfiguration(
    List<ExtensionRef> imageProcessors
) {
}
```

W `ProfileRuntimeConfiguration` dodajemy:

```java
ProfilePreprocessingConfiguration preprocessing
```

Jesli sekcji brakuje, runtime dostaje pusta liste.

## 6. Core execution

Dodajemy serwis:

```text
core/src/main/java/.../image/DocumentImagePreprocessingService.java
```

Odpowiedzialnosc:

- przyjmuje `ProcessingImage` strony,
- wykonuje liste `ImageProcessor` z profilu,
- zwraca `ProcessingImage` jako `PreparedPage`,
- mapuje bledy extension do kontrolowanych problemow pipeline,
- zapisuje trace, jesli trace jest wlaczony.

Szkic API:

```java
public final class DocumentImagePreprocessingService {
    public ProcessingImage prepare(
        PageNumber page,
        ProcessingImage renderedPage,
        List<ExtensionRef> imageProcessors,
        TraceSink trace
    );
}
```

Etap wykonywany jest bezposrednio po render/load strony.

Docelowy przeplyw `DocumentProcessor`:

```text
documentReader.read(...)
-> rendered pages
-> for required page:
     preparedPage = documentPreprocessing.prepare(renderedPage, profile.preprocessing)
-> page OCR(preparedPage)
-> identification(page OCR)
-> anchors(page OCR / preparedPage)
-> fields(preparedPage)
```

Wazne:

- `FieldProcessingService.extract(...)` powinien dostawac `PreparedPage`, nie `RenderedPage`.
- `TestCategoryUseCase` w JavaFX powinien uzywac tej samej sciezki przygotowania stron co `DocumentProcessor`.
- `RunPageOcrUseCase` powinien OCR-owac aktualna `PreparedPage`.

## 7. Cache

Sesja przetwarzania powinna rozroznic:

- rendered page cache,
- prepared page cache,
- OCR cache.

Minimalny wariant implementacyjny moze przechowywac tylko `preparedPage` jako dotychczasowa `pageCache`, ale nazewnictwo powinno zostac docelowo poprawione, zeby uniknac niejednoznacznosci.

Rekomendowany model:

```java
Map<PageNumber, ProcessingImage> renderedPageCache;
Map<PageNumber, ProcessingImage> preparedPageCache;
Map<PageNumber, OcrText> pageOcrCache;
```

Invalidacja:

- zmiana pipeline workspace preprocessing czysci `preparedPageCache`, `pageOcrCache`, trace, wyniki preview i test category,
- zmiana ustawien OCR czysci tylko `pageOcrCache` i wyniki zalezne,
- zmiana kategorii nie wymusza ponownego preprocessingu dokumentu, o ile pipeline workspace sie nie zmienil.

## 8. Trace i diagnostyka

Dla `TraceMode.FULL` powinny byc dostepne obrazy:

1. `Rendered page`.
2. `After workspace image processor 001 <id>`.
3. `After workspace image processor 002 <id>`.
4. `Prepared page / Page OCR input`.
5. `Page OCR HOCR`.

Dla `TraceMode.BASIC` wystarcza metadane krokow:

- page,
- processor id,
- parametry,
- input size,
- output size,
- duration,
- status.

Eksport diagnostyczny ZIP powinien zawierac obrazy workspace preprocessing obok istniejacych obrazow field pipeline.

## 9. JavaFX UX

Lewy panel powinien zostac podzielony na zakladki:

```text
Preprocessing | Categories
```

### 9.1. Zakladka `Categories`

Zawiera obecny workspace categories UI:

- select-list kategorii profilu,
- przyciski `Nowa`, `Otworz`, `Usun`,
- drzewo konfiguracji aktualnej kategorii.

### 9.2. Zakladka `Preprocessing`

Zawiera edytor pipeline'u workspace preprocessing:

- lista krokow `ImageProcessor`,
- `Add`,
- `Remove`,
- `Move up`,
- `Move down`,
- `Duplicate`,
- wybor extension przez istniejacy `ExtensionPicker`,
- formularz parametrow generowany przez `ExtensionParametersForm`,
- oznaczenie unresolved extension bez blokowania edycji profilu.

Kolejnosc krokow w UI jest kolejnoscia wykonania pipeline.

### 9.3. Viewer

Viewer pokazuje `PreparedPage`.

Dopuszczalny tryb diagnostyczny po MVP:

- `Show raw rendered page`,
- tylko read-only,
- bez mozliwosci rysowania regionow.

Jesli pipeline workspace preprocessing nie jest pusty i dokument jest otwarty, zmiana kroku powinna:

- oznaczyc profil jako dirty,
- wyczyscic cache prepared/OCR,
- przeliczyc aktualna strone po kliknieciu akcji `Apply preprocessing` albo automatycznie po debounced change, jesli wydajnosc na to pozwoli.

Pierwsza implementacja powinna uzyc jawnej akcji:

```text
Apply preprocessing
```

Pozniej mozna dodac automatyczne odswiezanie.

### 9.4. Wspolny debug pojedynczego kroku `ImageProcessor`

Mechanizm podgladu przed/po nie powinien byc projektowany jako funkcja specyficzna wylacznie dla workspace preprocessing.

Nalezy zaprojektowac go jako wspolny komponent debugowania kroku `ImageProcessor`, ktory pozniej bedzie mozna wykorzystac rowniez dla pipeline'u obrazu pola.

Proponowana nazwa komponentu:

```text
ImageProcessorDebugDialog
```

albo, jesli UI bedzie szerzej obejmowac transformacje obrazu:

```text
ImageTransformationDebugDialog
```

Dla kazdego kroku workspace preprocessing oraz docelowo dla kazdego kroku field image processors nalezy dodac operacje:

```text
Debug
```

Operacja powinna byc dostepna jako przycisk z ikona `debug.svg` przy wybranym kroku pipeline.

Celem jest strojenie parametrow transformacji obrazu bez koniecznosci uruchamiania calego testu kategorii.

Po kliknieciu `Debug` aplikacja otwiera modalne albo niemodalne okno dialogowe z porownaniem:

```text
source image | transformed image | parameters
```

Układ:

- po lewej obraz zrodlowy kroku,
- po srodku obraz wynikowy po wykonaniu wybranego kroku,
- po prawej formularz parametrow tego kroku,
- u gory albo z lewej strony kontrolki zoom: `Zoom In`, `Zoom Out`, `Fit Page`, `Fit Width`, `100%`,
- opcjonalnie status z rozmiarem obrazow i czasem wykonania kroku.

Zoom i przewijanie:

- zoom-in/zoom-out musi dzialac jednoczesnie na obrazie zrodlowym i wynikowym,
- `Ctrl+scroll` nad dowolnym z obrazow zmienia zoom obu obrazow,
- punkt pod kursorem powinien pozostac punktem odniesienia dla aktywnego obrazu,
- przewijanie poziome i pionowe powinno byc synchronizowane miedzy oboma podgladami,
- `Fit Page`, `Fit Width` i `100%` ustawiaja ten sam tryb dla obu obrazow.

Parametry:

- panel po prawej uzywa tego samego dynamicznego formularza parametrow co edytor pipeline,
- zmiana parametru oznacza debug preview jako dirty,
- pierwsza implementacja moze wymagac klikniecia `Apply` w oknie debug,
- docelowo mozna dodac automatyczne przeliczenie po debounce,
- `Apply to pipeline` zapisuje aktualne parametry do kroku workspace preprocessing,
- `Cancel` zamyka okno bez zmiany konfiguracji.

Obraz zrodlowy kroku:

- dla workspace preprocessing:
  - dla pierwszego kroku jest to `RenderedPage`,
  - dla kroku N jest to wynik kroku N-1,
- dla field image processors:
  - punktem startowym jest `PreparedPage`,
  - nastepnie wykonywany jest crop regionu pola,
  - dla pierwszego kroku field image processors obrazem zrodlowym jest crop pola,
  - dla kroku N obrazem zrodlowym jest wynik kroku N-1 w pipeline pola,
- obraz wynikowy jest wynikiem wykonania tylko aktualnie debugowanego kroku na jego obrazie zrodlowym.

Wspolny komponent powinien dostawac kontekst wyliczania obrazu zrodlowego, zamiast znac szczegoly workspace albo field pipeline.

Szkic kontraktu:

```java
interface ImageProcessorDebugContext {
    String title();
    ProcessingImage sourceImageForStep(int stepIndex);
    ExtensionRefDto step(int stepIndex);
    void replaceStep(int stepIndex, ExtensionRefDto updated);
}
```

Docelowe implementacje:

```text
WorkspacePreprocessingDebugContext
FieldImageProcessorDebugContext
```

Dzieki temu `WP-007` nie zabetonuje dialogu pod workspace preprocessing, a pozniejsze dodanie debugowania field pipeline bedzie wymagalo glownie implementacji drugiego kontekstu.

Trace:

- debug pojedynczego kroku nie powinien nadpisywac glownego trace ostatniego testu kategorii,
- moze uzywac osobnego `TraceImageStore` scoped do okna debug,
- po zamknieciu okna obrazy debug powinny byc zwolnione.

Kryteria akceptacji:

- uzytkownik moze porownac obraz przed i po kroku transformacji obok siebie,
- zoom i scroll sa synchronizowane,
- parametry mozna zmienic i ponownie przeliczyc wynik,
- zapis parametrow z debug dialogu aktualizuje wybrany krok pipeline,
- zamkniecie bez zapisu nie zmienia profilu,
- bledy wykonania kroku sa pokazane w oknie dialogowym i logowane w konsoli.

## 10. Walidacja

Walidator profilu powinien sprawdzac:

- `preprocessing.imageProcessors` jest lista, jesli sekcja istnieje,
- kazdy krok ma niepusty `id`,
- duplikaty sa dozwolone, bo kolejnosc i wielokrotne wykonanie moga miec sens,
- parametry sa walidowane przez descriptor extension, jesli extension jest dostepny,
- brak extension nie blokuje zapisu, ale powinien byc problemem walidacji UI.

CLI powinno przerwac przetwarzanie, jesli profil wskazuje nieznany `ImageProcessor`, bo runtime nie moze wykonac pipeline.

## 11. Error model

Nowe lub doprecyzowane kody bledow:

| Kod | Stage | Znaczenie |
| --- | ----- | --------- |
| `DOCUMENT_IMAGE_PREPROCESSING_FAILED` | `IMAGE_PROCESSING` albo nowy `DOCUMENT_IMAGE_PREPROCESSING` | Blad kroku workspace preprocessing. |
| `DOCUMENT_IMAGE_PROCESSOR_NOT_FOUND` | `IMAGE_PROCESSING` | Brak extension wskazanego w profilu. |
| `DOCUMENT_IMAGE_PROCESSOR_INVALID_TYPE` | `IMAGE_PROCESSING` | Extension istnieje, ale nie jest `ImageProcessor`. |

Jesli `ProcessingStage` ma zostac rozszerzony, preferowany nowy etap:

```java
DOCUMENT_IMAGE_PREPROCESSING
```

Jesli nie rozszerzamy enum w pierwszym kroku, mozna tymczasowo uzyc `IMAGE_PROCESSING` z kontekstem `scope=DOCUMENT`.

## 12. Implementacja etapami

### WP-001 Model konfiguracji profilu

Zakres:

- dodac `ProfilePreprocessingDto`,
- rozszerzyc `ProfileDto`,
- rozszerzyc mapper konfiguracji,
- rozszerzyc runtime model,
- zaktualizowac walidator profilu,
- dodac fixture i testy read/write.

Kryteria:

- stary profil bez `preprocessing` nadal dziala,
- profil z `preprocessing.imageProcessors` laduje sie do runtime,
- zapis zachowuje UTF-8 i deterministyczny JSON.

### WP-002 Core document preprocessing service

Zakres:

- dodac `DocumentImagePreprocessingService`,
- wykonywac `ImageProcessor` z registry,
- testy kolejnosci wykonania,
- test blednego extension,
- trace podstawowych metadanych.

Kryteria:

- pipeline wykonuje kroki w kolejnosci,
- output kroku N jest inputem kroku N+1,
- pusta lista zwraca obraz bez zmian semantycznych.

### WP-003 Wpiecie w DocumentProcessor

Zakres:

- wykonac preprocessing po renderze strony,
- page OCR uzywa prepared page,
- field extraction uzywa prepared page,
- testy potwierdzaja, ze field crop dostaje obraz po preprocessingu.

Kryteria:

- identyfikacja i pola uzywaja tego samego prepared page,
- preprocessing strony nie wykonuje sie per kategoria.

### WP-004 JavaFX cache i viewer prepared page

Zakres:

- rozdzielic albo jasno nazwac cache rendered/prepared,
- viewer pokazuje prepared page,
- Run OCR uzywa prepared page,
- region selection zapisuje koordynaty prepared page.

Kryteria:

- po zmianie preprocessing regiony sa interpretowane wzgledem nowego prepared page,
- nie ma ukrytego przeliczania regionow z raw render.

### WP-005 JavaFX zakladka Preprocessing

Zakres:

- dodac `TabPane` w lewym panelu: `Preprocessing`, `Categories`,
- edytor krokow `ImageProcessor`,
- dynamiczny formularz parametrow,
- operacje add/remove/move/duplicate,
- dirty state profilu.

Kryteria:

- uzytkownik moze skonfigurowac `im-normalize`, `im-deskew` i inne procesory na poziomie profilu,
- zapis profilu zawiera pipeline,
- otwarcie profilu odtwarza pipeline.

### WP-006 Apply preprocessing i trace UI

Zakres:

- akcja `Apply preprocessing`,
- odswiezenie aktualnej strony w viewerze,
- trace obrazow workspace preprocessing,
- eksport diagnostyczny obrazow i HOCR.

Kryteria:

- uzytkownik widzi efekt pipeline przed rysowaniem regionow,
- trace pokazuje obraz po kazdym kroku.

### WP-007 Debug dialog dla kroku preprocessing

Zakres:

- dodac akcje `Debug` z ikona `debug.svg` przy kroku preprocessing,
- zaprojektowac dialog jako wspolny `ImageProcessorDebugDialog`, a nie jako komponent specyficzny dla workspace,
- wyliczyc obraz zrodlowy dla wybranego kroku,
- wykonac pojedynczy `ImageProcessor` na obrazie zrodlowym,
- pokazac obraz zrodlowy i wynikowy side-by-side,
- dodac wspolny zoom i zsynchronizowany scroll,
- pokazac formularz parametrow po prawej,
- dodac `Apply`, `Apply to pipeline`, `Cancel`,
- izolowac trace i obrazy debug od glownej sesji.

Kryteria:

- debug dziala bez uruchamiania testu kategorii,
- okno pozwala stroic parametry na aktualnej stronie dokumentu,
- `Apply to pipeline` aktualizuje tylko wybrany krok,
- zmiany w oknie debug nie sa zapisywane do profilu przed jawna akceptacja.

### WP-008 Debug dialog dla field image processors

Zakres:

- wykorzystac ten sam `ImageProcessorDebugDialog`,
- dodac `FieldImageProcessorDebugContext`,
- jako obraz bazowy uzyc `PreparedPage`,
- wykonac crop aktualnego pola,
- dla kroku N wykonac poprzednie field image processors,
- pokazac porownanie input/output dla wybranego kroku,
- `Apply to pipeline` aktualizuje wybrany krok w `field.imageProcessors`.

Kryteria:

- mechanizm debugowania field pipeline nie duplikuje UI debugowania workspace preprocessing,
- field debug uzywa tych samych zasad zoom/scroll/parametry/trace,
- obraz wejściowy field debug jest zgodny z normalna sciezka `FieldProcessingService`.

## 13. Migracja i kompatybilnosc

Istniejace category JSON nie wymagaja migracji.

Istniejace profile bez sekcji `preprocessing` pozostaja poprawne.

Po dodaniu preprocessingu do profilu uzytkownik powinien zweryfikowac regiony kategorii, poniewaz sa one interpretowane wzgledem `PreparedPage`. Jesli pipeline zmienia geometrie obrazu, stare regiony przygotowane wzgledem raw render moga wymagac poprawy.

## 14. Otwarte decyzje

1. Czy `ProcessingStage` rozszerzamy o `DOCUMENT_IMAGE_PREPROCESSING`, czy w pierwszym kroku uzywamy `IMAGE_PROCESSING` ze scope `DOCUMENT`.
2. Czy UI ma automatycznie wykonywac preprocessing po kazdej zmianie parametrow, czy tylko po `Apply preprocessing`.
3. Czy zapis profilu ma pomijac pusta sekcje `preprocessing`, czy zapisywac ja jako pusta liste.
4. Czy `ProfileWorkspace` powinien byc przeniesiony z UI do osobnej warstwy app/viewmodel przed implementacja edytora preprocessing.

## 15. Rekomendacja startowa

Rekomendowany pierwszy zakres implementacji:

1. `WP-001 Model konfiguracji profilu`.
2. `WP-002 Core document preprocessing service`.
3. `WP-003 Wpiecie w DocumentProcessor`.

Dopiero po tym nalezy budowac pelny edytor JavaFX, bo UI powinien od poczatku korzystac z docelowego Core, a nie z lokalnej, alternatywnej implementacji preprocessingu.

Debug dialog (`WP-007`) najlepiej implementowac po `WP-005`, kiedy istnieje juz edytor krokow preprocessing i dynamiczny formularz parametrow na poziomie profilu.
