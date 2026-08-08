# Zadania JavaFX Configurator

| Pole          | Wartość                                                                                          |
| ------------- | ------------------------------------------------------------------------------------------------ |
| ID dokumentu  | DOC-018                                                                                          |
| Tytuł         | Zadania dla aplikacji JavaFX Configurator                                                        |
| Wersja        | 0.1                                                                                              |
| Status        | Draft                                                                                            |
| Typ           | Task Backlog                                                                                     |
| Źródło prawdy | `docs/13-javafx-configurator.md`, `docs/17-implementation-plan.md`, aktualny kod modułu `javafx` |

## 1. Cel dokumentu

Celem dokumentu jest opisanie zadań, które pozostały do wykonania, aby aplikacja JavaFX Configurator zrealizowała wszystkie funkcjonalności zaprojektowane w dokumentacji projektu.

Dokument nie zastępuje specyfikacji `13-javafx-configurator.md`. Jest wykonawczym backlogiem prac dla modułu:

```text
javafx
```

## 2. Aktualny stan

Aktualna implementacja zawiera pierwszy działający shell configuratora:

- entry point JavaFX,
- toolbar,
- configuration tree w formie uproszczonej,
- panel szczegółów,
- panel walidacji,
- status bar,
- otwieranie i zapis category JSON,
- tworzenie prostego draftu kategorii,
- otwieranie dokumentu referencyjnego przez istniejący `DocumentReader`,
- render pierwszej strony,
- scroll w podglądzie dokumentu,
- `Fit Page`,
- zoom przyciskami,
- zoom przez `Ctrl+scroll`,
- zoom przez gest touchpada,
- uruchomienie OCR strony poza JavaFX Application Thread,
- overlay bounding boxów OCR words,
- kliknięcie rozpoznanego słowa i pokazanie szczegółów,
- `ConfigurationSession` z podstawowym cache stron i OCR,
- `PreviewRunGuard`,
- `CoordinateMapper`,
- `DraftValidationService`.

Aktualna implementacja nie jest jeszcze pełnym configuratorem kategorii. Najważniejsze braki to edycja struktury kategorii z UI, region selection, edytory identification/anchors/fields, preview pipeline, trace viewer, test category oraz eksport diagnostyczny.

## 3. Priorytety

Priorytety w tym dokumencie:

| Priorytet | Znaczenie                                                                          |
| --------- | ---------------------------------------------------------------------------------- |
| P0        | Konieczne, aby UI realizowało założony MVP configuratora                           |
| P1        | Konieczne dla pełnej funkcjonalności zaprojektowanej w `13-javafx-configurator.md` |
| P2        | Ulepszenia po MVP lub hardening                                                    |

## 4. Zasady implementacji

Każde zadanie powinno zachować zasady architektury UI:

- UI nie implementuje alternatywnego pipeline'u OCR.
- UI używa tego samego Core co CLI.
- PDFBox, Tess4J i ZXing są ukryte za use case'ami lub adapterami.
- Logika edycji i preview trafia do ViewModel/use case, nie do samego widoku.
- Ciężkie operacje działają poza JavaFX Application Thread.
- Zapis JSON jest deterministyczny i UTF-8.
- Semantycznie błędny draft może zostać zapisany, ale UI musi pokazać błędy.
- Zmiany konfiguracji invalidują tylko wymagane cache downstream.

## 5. P0 - Edycja Modelu Kategorii

### FX-001 Category Editor

Zaimplementować edytor właściwości kategorii.

Zakres:

- edycja `id`,
- edycja `displayName`,
- edycja `description`,
- edycja `version`,
- edycja page policy,
- edycja OCR defaults kategorii,
- dirty state po każdej zmianie,
- walidacja po zmianie lub na żądanie.

Kryteria akceptacji:

- użytkownik może utworzyć nową kategorię bez ręcznej edycji JSON,
- zmiany są widoczne w configuration tree i details panel,
- zapis JSON zawiera zmienione wartości,
- testy ViewModel pokrywają dirty state i walidację.

### FX-002 Configuration Tree Model

Zastąpić uproszczone drzewo modelem odzwierciedlającym strukturę category JSON.

Zakres:

- category root,
- identification groups,
- conditions,
- anchors,
- geometry,
- fields,
- field image processors,
- field OCR,
- field transformers,
- field validators,
- output settings.

Kryteria akceptacji:

- kliknięcie elementu drzewa otwiera właściwy edytor,
- drzewo aktualizuje się po dodaniu/usunięciu/przeniesieniu elementów,
- multi-page element przełącza viewer na właściwą stronę.

### FX-003 Draft Mutation API

Wydzielić operacje modyfikacji draftu z widoku do osobnej warstwy.

Zakres:

- `CategoryDraftEditor`,
- immutable replace DTO,
- operacje add/remove/move dla list,
- spójna invalidacja cache,
- jedna ścieżka ustawiania dirty state.

Kryteria akceptacji:

- widok nie składa ręcznie złożonych DTO,
- wszystkie mutacje są testowalne bez JavaFX,
- błędne operacje zwracają jasny błąd lub problem walidacji.

### FX-004 Properties Details Forms

Zastąpić tekstowy panel properties/details edytowalnymi formularzami dla aktualnie wybranego elementu konfiguracji.

Zakres:

- formularz właściwości dla category root,
- formularze właściwości dla identification groups i conditions,
- formularze właściwości dla anchors,
- formularze właściwości dla geometry,
- formularze właściwości dla fields,
- formularze właściwości dla field OCR, output i pipeline steps,
- każda właściwość prezentowana jako kontrolka UI dobrana do typu danych,
- każda kontrolka ma widoczną etykietę,
- każda kontrolka ma tooltip z krótkim opisem znaczenia pola,
- zmiany w kontrolkach przechodzą wyłącznie przez `CategoryDraftEditor`/ViewModel,
- błędy walidacji są prezentowane przy odpowiednich kontrolkach oraz w panelu walidacji.

Kryteria akceptacji:

- panel details nie pokazuje już złożonego modelu jako bloku tekstu,
- użytkownik może edytować podstawowe właściwości bez ręcznej edycji JSON,
- etykiety i tooltipy są dostępne dla wszystkich edytowalnych pól,
- testy ViewModel/form mapping pokrywają zapis zmian do draftu.

## 6. P0 - Document Viewer

### FX-010 Region Selection

Dodać zaznaczanie regionu myszą.

Zakres:

- tryb `Select`,
- tryb `Pan`,
- tryb `Draw Region`,
- kontrolki wyboru trybu jako ikony w pionowym toolbarze w lewym górnym rogu panelu podglądu,
- skróty klawiaturowe dla trybów wyboru,
- rysowanie prostokąta na overlay,
- normalizacja regionu niezależnie od kierunku przeciągania,
- zapis regionu w koordynatach obrazu/reference,
- pokazanie aktualnego regionu w panelu properties.

Kryteria akceptacji:

- zaznaczenie działa przy dowolnym zoomie,
- wynikowy `Region` nie zależy od aktualnej skali viewera,
- są testy coordinate mapping i region normalization.

### FX-011 Viewer Layers

Dodać niezależnie włączane warstwy overlay.

Zakres:

- OCR words,
- anchors,
- field regions,
- current selection,
- diagnostics.

Kryteria akceptacji:

- warstwy można włączać/wyłączać z menu lub toolbaru,
- overlay nie przesuwa się względem obrazu przy zoomie i scrollu,
- elementy overlay mają typ i selection state.

### FX-012 Fit Width i 100%

Uzupełnić kontrolki zoomu.

Zakres:

- `Fit Width`,
- `Fit Page`,
- `100%`,
- `Zoom In`,
- `Zoom Out`,
- kontrolki zoomu jako ikony w pionowym toolbarze w lewym górnym rogu panelu podglądu,
- wspólny pionowy toolbar dla zoomu i trybów zaznaczania,
- skróty klawiaturowe dla `Zoom In`, `Zoom Out`, `Fit Page`, `Fit Width` i `100%`,
- zachowanie zoom center przy `Ctrl+scroll`,
- stabilny status `Zoom N%`.

Kryteria akceptacji:

- `Fit Width` mieści dokument w poziomie,
- `Fit Page` mieści całą stronę,
- `100%` pokazuje rzeczywisty rozmiar renderu,
- wszystkie akcje zoomu są dostępne z ikon oraz skrótów klawiaturowych,
- zwykły scroll nadal przewija dokument.

### FX-013 Multi-page Rendering

Rozszerzyć obsługę dokumentów wielostronicowych.

Zakres:

- render strony na żądanie,
- cache ograniczony rozmiarem,
- liczba stron w status bar,
- previous/next page,
- kontrolki previous/next page w prawym dolnym rogu panelu podglądu dokumentu,
- pole tekstowe między previous/next z aktualnym numerem strony,
- przejście do wpisanej strony po `Enter`, jeśli strona istnieje,
- walidacja wpisanego numeru strony bez zmiany aktualnej strony przy wartości spoza zakresu,
- zabezpieczenie przed wyjściem poza zakres,
- automatyczne przełączanie strony po wyborze elementu konfiguracji.

Kryteria akceptacji:

- aplikacja nie renderuje wszystkich stron od razu,
- cache nie rośnie bez limitu,
- nawigacja stron jest dostępna bez opuszczania panelu podglądu,
- wpisanie istniejącego numeru strony i `Enter` przełącza viewer na tę stronę,
- wpisanie niepoprawnego lub nieistniejącego numeru strony pokazuje walidację i nie zmienia strony,
- testy pokrywają page navigation i cache miss/hit.

## 7. P0 - OCR i OCR Explorer

### FX-020 OCR Result Explorer

Dodać panel listy wyników OCR.

Zakres:

- lista words,
- tekst,
- confidence,
- bounds,
- page,
- filtrowanie po tekście,
- kliknięcie słowa zaznacza bounding box na viewerze.

Kryteria akceptacji:

- użytkownik może znaleźć rozpoznany tekst bez klikania po obrazie,
- wybór w panelu i wybór na overlay są zsynchronizowane.

### FX-021 OCR Overlay Modes

Dodać tryby overlay OCR.

Zakres:

- `OFF`,
- `WORDS`,
- `LINES`,
- `BLOCKS`.

Kryteria akceptacji:

- `WORDS` działa na obecnym modelu,
- `LINES` i `BLOCKS` są zasilane z modelu OCR, gdy Core zacznie je dostarczać,
- brak danych dla trybu pokazuje pustą warstwę bez błędu.

### FX-022 OCR Actions

Dodać akcje na elemencie OCR.

Zakres:

- `Use as Identification Condition`,
- `Use as Anchor`,
- `Copy text`,
- `Copy bounds`.

Kryteria akceptacji:

- akcje są dostępne z context menu na bounding boxie i z OCR explorer,
- utworzone condition/anchor używa tekstu i bounds klikniętego elementu.

## 8. P0 - Identification Editor

### FX-030 Identification Groups Editor

Zaimplementować edytor grup identyfikacji.

Zakres:

- dodaj grupę OR,
- usuń grupę,
- dodaj condition AND,
- usuń condition,
- przenieś condition,
- edycja typu condition.

Kryteria akceptacji:

- UI odzwierciedla model `(A AND B) OR (C AND D)`,
- pusta konfiguracja pokazuje błąd walidacji,
- zapis JSON zachowuje strukturę grup.

### FX-031 Condition Editor

Zaimplementować edytor pojedynczego condition.

Zakres:

- `type`,
- `page`,
- `expectedText`,
- `searchRegion`,
- matcher,
- detector,
- parameters.

Kryteria akceptacji:

- dla TEXT widoczne są pola tekstowe i matcher,
- dla QR/BARCODE widoczne są ustawienia detector/matcher,
- region może pochodzić z aktualnego zaznaczenia viewera.

### FX-032 Test Identification

Dodać use case i widok testowania identyfikacji.

Zakres:

- uruchomienie OCR, jeśli brak cache,
- wykonanie identyfikacji Core,
- pokazanie statusu,
- pokazanie matched category,
- pokazanie grup i condition results.

Kryteria akceptacji:

- test działa poza FX thread,
- wynik starszego testu nie nadpisuje nowszego,
- błędy są widoczne w diagnostics panel.

## 9. P0 - Anchor Editor

### FX-040 Anchor CRUD

Zaimplementować tworzenie i edycję anchorów.

Zakres:

- `id`,
- `page`,
- `detector`,
- `searchRegion`,
- `required`,
- `referenceFeature.bounds`,
- add/remove.

Kryteria akceptacji:

- anchor można utworzyć z zaznaczenia regionu,
- anchor można utworzyć z klikniętego OCR word,
- anchor jest widoczny jako overlay.

### FX-041 Text Anchor Workflow

Dodać workflow tworzenia text anchor z OCR.

Zakres:

- context action `Use as Anchor`,
- ustawienie detector `text`,
- parametr `text`,
- reference bounds z OCR bounding box,
- search region opcjonalnie z aktualnego zaznaczenia.

Kryteria akceptacji:

- utworzony anchor jest walidowany,
- test anchor znajduje reference feature na bieżącym dokumencie.

### FX-042 QR Anchor Workflow

Dodać workflow QR anchor.

Zakres:

- wybór detector `qr`,
- search region,
- uruchomienie ZXing przez Core/adapter,
- pokazanie payload,
- pokazanie result points/bounds,
- akceptacja reference feature.

Kryteria akceptacji:

- UI nie zależy od typów ZXing,
- wynik QR jest cache'owany w sesji,
- błędy wykrycia QR trafiają do diagnostics panel.

## 10. P0 - Geometry Editor i Preview

### FX-050 Geometry Editor

Zaimplementować edytor geometrii.

Zakres:

- `referenceWidth`,
- `referenceHeight`,
- strategy type,
- lista anchor IDs,
- automatyczne pobranie reference dimensions z dokumentu,
- ostrzeżenie przy zmianie reference dimensions po zdefiniowaniu regionów.

Kryteria akceptacji:

- użytkownik może skonfigurować strategię bez ręcznej edycji JSON,
- wybrane anchor IDs istnieją,
- walidacja pokazuje brakujące anchor references.

### FX-051 Test Geometry

Dodać preview geometrii.

Zakres:

- wykrycie anchorów,
- uruchomienie `GeometryNormalizationService`,
- pokazanie `GeometryStatus`,
- pokazanie użytych anchorów,
- pokazanie transformacji,
- overlay reference i detected bounds,
- overlay resolved field regions.

Kryteria akceptacji:

- wynik geometrii jest cache'owany,
- zmiana anchor/geometry invaliduje cache geometrii i pól,
- preview działa poza FX thread.

## 11. P0 - Field Editor

### FX-060 Field CRUD

Zaimplementować tworzenie, edycję i usuwanie pól.

Zakres:

- `id`,
- `displayName`,
- `page`,
- `region`,
- `required`,
- output `exported`,
- output `columnName`,
- add/remove,
- kopiowanie pola,
- region z aktualnego zaznaczenia.

Kryteria akceptacji:

- field region jest widoczny na viewerze,
- wybór pola przełącza viewer na jego stronę,
- zapis JSON zawiera wszystkie pola.

### FX-061 Field OCR Options

Dodać edycję OCR options pola.

Zakres:

- language,
- datapath,
- pokazanie effective values z kategorii/profilu,
- walidacja pustych i błędnych wartości.

Kryteria akceptacji:

- puste wartości oznaczają dziedziczenie,
- UI pokazuje źródło wartości effective.

### FX-062 Output Settings

Dodać pełny edytor output pola.

Zakres:

- exported,
- columnName,
- walidacja duplicate output columns,
- podgląd kolumn w CSV schema.

Kryteria akceptacji:

- zmiana output nie uruchamia ponownie OCR,
- walidacja duplikatów działa przed zapisem.

## 12. P0 - Pipeline Editor

### FX-070 Extension Picker

Dodać wybór extension z registry.

Zakres:

- lista extension,
- filtrowanie po `ExtensionType`,
- displayName,
- id,
- description,
- version,
- unresolved extension placeholder.

Kryteria akceptacji:

- użytkownik może wybrać matcher/detector/processor/transformer/validator,
- brak extension po otwarciu JSON nie blokuje edycji całego draftu.

### FX-071 Dynamic Extension Form

Zaimplementować formularze parametrów z `ExtensionDescriptor`.

Zakres:

- STRING -> `TextField`,
- INTEGER -> `Spinner` lub numeric field,
- DECIMAL -> numeric field,
- BOOLEAN -> `CheckBox`,
- ENUM -> `ComboBox`,
- REGEX -> `TextField` z walidacją,
- wymagane parametry,
- wartości domyślne, jeśli descriptor je dostarcza.

Kryteria akceptacji:

- formularz nie jest kodowany osobno dla każdego pluginu,
- błędy parametrów są widoczne przy polach,
- zapis JSON zachowuje typy parametrów.

### FX-072 Pipeline Step Operations

Dodać operacje na krokach pipeline.

Zakres:

- add,
- remove,
- move up,
- move down,
- duplicate,
- enable/disable, jeśli model konfiguracji to wspiera.

Kryteria akceptacji:

- kolejność kroków w UI odpowiada kolejności w JSON,
- zmiana kroku invaliduje tylko właściwe cache downstream.

## 13. P0 - Preview Field i Trace

### FX-080 PreviewFieldUseCase

Dodać use case preview pojedynczego pola.

Zakres:

- wymuszenie `TraceMode.FULL`,
- resolve region,
- crop,
- image processors,
- OCR pola,
- transformers,
- validators,
- wynik `FieldResult`,
- `ProcessingTrace`.

Kryteria akceptacji:

- preview używa tego samego Core co batch/CLI,
- nie przechowuje ciężkich obrazów poza ostatnim preview,
- działa poza FX thread.

### FX-081 TraceImageStore

Dodać magazyn obrazów trace po stronie configuratora.

Zakres:

- zapis obrazów etapów preview,
- identyfikatory obrazów,
- lifecycle ostatniego preview,
- zwalnianie poprzednich obrazów,
- brak zależności Domain od obrazów.

Kryteria akceptacji:

- nowy preview usuwa stare obrazy,
- trace entries mogą wskazywać obrazy przez `TraceImageRef`,
- eksport obrazu używa tego store.

### FX-082 Trace Viewer

Dodać pełny trace viewer.

Zakres:

- lista etapów,
- stage,
- status,
- duration,
- field,
- anchor,
- page,
- issues,
- context,
- input/output image side-by-side.

Kryteria akceptacji:

- użytkownik widzi każdy etap pipeline pola,
- kliknięcie etapu pokazuje szczegóły,
- brak obrazu nie powoduje błędu UI.

### FX-083 Field Result Panel

Dodać panel wyniku pola.

Zakres:

- raw OCR,
- value after transformers,
- validation status,
- issues,
- final `FieldResult.status`.

Kryteria akceptacji:

- wynik preview pola jest czytelny bez otwierania CSV,
- błędy walidacji są linkowane do konfiguracji validatorów.

## 14. P0 - Test Category

### FX-090 TestCategoryUseCase

Dodać test całej kategorii na otwartym dokumencie.

Zakres:

- OCR strony,
- identification,
- anchors,
- geometry,
- all fields,
- document status,
- issues,
- trace.

Kryteria akceptacji:

- test używa Core `DocumentProcessor` albo równoważnej aplikacyjnej ścieżki bez duplikowania logiki,
- działa poza FX thread,
- wynik starszego testu nie nadpisuje nowszego.

### FX-091 Test Category Results View

Dodać widok wyników testu kategorii.

Zakres:

- category id,
- identification status,
- geometry status,
- field results table,
- errors,
- warnings,
- trace link.

Kryteria akceptacji:

- użytkownik widzi, dlaczego dokument przeszedł lub nie przeszedł,
- field results nie wypływają do logów.

## 15. P1 - Diagnostics i Export

### FX-100 Diagnostic Export

Dodać eksport diagnostyczny.

Zakres:

- export selected trace image,
- export all latest preview trace images,
- export trace metadata JSON,
- opcjonalnie diagnostic bundle ZIP.

Kryteria akceptacji:

- eksport nie blokuje FX thread,
- format jest deterministyczny,
- nazwy plików zawierają stage/order bez danych wrażliwych.

### FX-101 Validation Navigation

Rozszerzyć panel walidacji.

Zakres:

- severity,
- code,
- path,
- message,
- kliknięcie problemu zaznacza element w tree,
- przewinięcie do odpowiedniego edytora.

Kryteria akceptacji:

- walidacja jest użyteczna jako lista zadań naprawczych,
- unresolved extension jest prezentowany jako edytowalny problem.

### FX-102 Error Presentation

Ujednolicić prezentację błędów.

Zakres:

- błędy składni JSON,
- błędy semantyczne,
- błędy OCR,
- błędy PDF,
- błędy pluginów,
- błędy zapisu.

Kryteria akceptacji:

- użytkownik dostaje konkretny komunikat i kontekst,
- stack trace nie jest domyślnie pokazywany w UI,
- szczegóły techniczne mogą trafić do logów.

## 16. P1 - Settings i Preferences

### FX-110 Settings Dialog

Dodać okno ustawień runtime.

Zakres:

- Tesseract datapath,
- default OCR language,
- default PDF DPI,
- last opened directory,
- cache limits.

Kryteria akceptacji:

- ustawienia UI nie trafiają do category JSON,
- ustawienia są zapisywane w `java.util.prefs.Preferences`,
- zmiana OCR settings invaliduje OCR cache.

### FX-111 Loaded Extensions Dialog

Dodać widok załadowanych extension.

Zakres:

- type,
- id,
- displayName,
- description,
- version,
- provider.

Kryteria akceptacji:

- użytkownik może sprawdzić, czy plugin jest dostępny,
- lista jest filtrowalna po typie extension.

## 17. P1 - Application Shell

### FX-120 Menu Bar

Dodać pełne menu aplikacji.

Zakres:

- File,
- View,
- Run,
- Tools,
- Help,
- akcja `Open document`,
- akcja `Open configuration`,
- listy ostatnio otwieranych dokumentów i konfiguracji dostępne także z menu.

Kryteria akceptacji:

- akcje z toolbaru są dostępne także z menu,
- skróty klawiaturowe są widoczne w menu.

### FX-120A Recent Files Split Buttons

Dodać menu ostatnio otwieranych plików do przycisków `Open document` oraz `Open configuration`.

Zakres:

- przy `Open document` dodać po prawej stronie strzałkę w dół rozwijającą ostatnio otwierane dokumenty,
- przy `Open configuration` dodać po prawej stronie strzałkę w dół rozwijającą ostatnio otwierane konfiguracje,
- rozdzielić historię dokumentów od historii konfiguracji,
- zapisywać historię w preferencjach aplikacji,
- ograniczyć liczbę wpisów w historii,
- usuwać lub oznaczać wpisy wskazujące na nieistniejące pliki,
- wybór wpisu z historii uruchamia tę samą ścieżkę co ręczne otwarcie pliku,
- otwarcie konfiguracji z historii respektuje unsaved changes guard.

Kryteria akceptacji:

- przyciski zachowują podstawową akcję kliknięcia i dodatkowe menu pod strzałką,
- historia dokumentów nie miesza się z historią konfiguracji,
- ostatnio użyty plik trafia na początek odpowiedniej listy,
- błędny wpis z historii daje czytelny komunikat i nie zmienia stanu aplikacji.

### FX-120B Last File Chooser Directories

Zapamiętać ostatnio wybrane foldery używane przez okna odczytu i zapisu plików.

Zakres:

- osobny ostatni folder dla odczytu konfiguracji kategorii,
- osobny ostatni folder dla zapisu konfiguracji kategorii,
- osobny ostatni folder dla odczytu dokumentu,
- osobny ostatni folder dla zapisu/eksportu dokumentu lub artefaktów dokumentu, gdy taka akcja zostanie dodana,
- inicjalizacja `FileChooser.initialDirectory` na podstawie zapamiętanego folderu,
- aktualizacja zapamiętanego folderu po udanym wyborze pliku,
- zapis folderów w `java.util.prefs.Preferences`,
- ignorowanie zapamiętanego folderu, jeśli już nie istnieje lub nie jest katalogiem.

Kryteria akceptacji:

- `Open configuration` startuje w ostatnim folderze użytym do otwarcia konfiguracji,
- `Save`/`Save As` konfiguracji startuje w ostatnim folderze użytym do zapisu konfiguracji,
- `Open document` startuje w ostatnim folderze użytym do otwarcia dokumentu,
- foldery konfiguracji i dokumentów nie nadpisują się nawzajem,
- nieistniejący folder nie powoduje błędu UI i aplikacja używa domyślnej lokalizacji systemowej.

### FX-121 Keyboard Shortcuts

Uzupełnić skróty klawiaturowe.

Zakres:

- `Ctrl+S` save,
- `Ctrl+Shift+S` save as,
- `Ctrl+O` open configuration,
- `Ctrl+N` new category,
- `Ctrl+Plus` zoom in,
- `Ctrl+Minus` zoom out,
- `Ctrl+0` fit page lub 100% zgodnie z decyzją UX,
- skrót dla `Fit Page`,
- skrót dla `Fit Width`,
- skrót dla `100%`,
- skróty dla previous/next page,
- skrót fokusujący pole numeru strony,
- skróty dla trybów `Select`, `Pan` i `Draw Region`,
- `F5` test category,
- `Esc` cancel current selection/preview.

Kryteria akceptacji:

- skróty nie konfliktują z edycją pól tekstowych,
- zachowanie jest opisane w menu/tooltips.

### FX-122 Unsaved Changes Guard

Dodać ochronę przed utratą zmian.

Zakres:

- zamknięcie aplikacji,
- open another configuration,
- new category,
- open recent.

Kryteria akceptacji:

- użytkownik dostaje wybór `Save`, `Discard`, `Cancel`,
- `Cancel` przerywa akcję,
- zapis błędnego draftu jest dozwolony.

## 18. P1 - Packaging i Uruchamianie

### FX-130 Packaging Configurator

Doprecyzować packaging artefaktu JavaFX.

Zakres:

- finalna nazwa artefaktu `configurator.jar`,
- manifest,
- zależności runtime,
- uruchamianie przez `javafx:run`,
- uruchamianie poza Mavenem,
- dokumentacja wymagań JavaFX runtime.

Kryteria akceptacji:

- użytkownik może uruchomić aplikację z przygotowanego artefaktu,
- README zawiera komendy dla Windows i Linux.

### FX-131 JavaFX Smoke Test

Dodać smoke test aplikacji JavaFX.

Zakres:

- uruchomienie aplikacji w trybie testowym,
- utworzenie stage,
- weryfikacja podstawowych kontrolek,
- zamknięcie bez wycieków wątków.

Kryteria akceptacji:

- test nie wymaga ręcznej interakcji,
- test może być pominięty w środowisku headless, jeśli JavaFX nie ma dostępnego glass toolkit.

## 19. P2 - Ergonomia i Hardening

### FX-140 Raw JSON View

Dodać opcjonalny podgląd surowego JSON.

Zakres:

- read-only JSON,
- odświeżanie po zmianach,
- kopiowanie do clipboard,
- diff względem ostatniego zapisu.

### FX-141 Undo/Redo

Dodać historię zmian draftu.

Zakres:

- command stack,
- undo,
- redo,
- dirty state zależny od historii,
- limit historii.

### FX-142 OCR Snapping

Dodać przyciąganie regionów do OCR bounds.

Zakres:

- snap do najbliższego word,
- snap do grupy zaznaczonych words,
- toggle snapping.

### FX-143 Page Thumbnails

Dodać miniatury stron.

Zakres:

- lazy thumbnails,
- wybór strony,
- oznaczanie stron z elementami konfiguracji.

### FX-144 Performance Telemetry

Dodać diagnostykę wydajności preview.

Zakres:

- czas OCR,
- czas image processing,
- czas transformers,
- czas validators,
- cache hit/miss.

## 20. Testy Do Dodania

Minimalny zestaw testów przed uznaniem UI za kompletne:

- ViewModel dirty state dla każdej operacji edycji,
- walidacja category editor,
- walidacja identification editor,
- walidacja anchor editor,
- walidacja field editor,
- extension form generation,
- extension parameter validation,
- region selection przy różnych zoomach,
- OCR overlay selection,
- preview run race protection dla OCR, field preview i test category,
- cache invalidation graph,
- save/load roundtrip JSON,
- save invalid draft,
- bad JSON presentation,
- missing extension presentation,
- document viewer page navigation,
- fit page/fit width/100%,
- trace viewer mapping,
- diagnostic export.

## 21. Sugerowana Kolejność Implementacji

Rekomendowana kolejność prac:

1. `FX-003 Draft Mutation API`
2. `FX-001 Category Editor`
3. `FX-004 Properties Details Forms`
4. `FX-002 Configuration Tree Model`
5. `FX-012 Fit Width i 100%`
6. `FX-013 Multi-page Rendering`
7. `FX-010 Region Selection`
8. `FX-030 Identification Groups Editor`
9. `FX-031 Condition Editor`
10. `FX-040 Anchor CRUD`
11. `FX-050 Geometry Editor`
12. `FX-060 Field CRUD`
13. `FX-070 Extension Picker`
14. `FX-071 Dynamic Extension Form`
15. `FX-072 Pipeline Step Operations`
16. `FX-080 PreviewFieldUseCase`
17. `FX-081 TraceImageStore`
18. `FX-082 Trace Viewer`
19. `FX-090 TestCategoryUseCase`
20. `FX-100 Diagnostic Export`
21. `FX-120 Menu Bar`
22. `FX-120A Recent Files Split Buttons`
23. `FX-120B Last File Chooser Directories`
24. `FX-122 Unsaved Changes Guard`
25. `FX-130 Packaging Configurator`

## 22. Kryteria Ukończenia Całości UI

Aplikacja UI JavaFX realizuje zaprojektowane funkcjonalności, gdy użytkownik może:

1. otworzyć dokument referencyjny,
2. wykonać OCR strony,
3. zobaczyć i wykorzystać wyniki OCR,
4. utworzyć pełną category configuration bez ręcznej edycji JSON,
5. skonfigurować identification,
6. skonfigurować text i QR anchors,
7. skonfigurować geometrię,
8. utworzyć pola i regiony,
9. skonfigurować image processors, OCR, transformers, validators i output,
10. korzystać z formularzy generowanych z `ExtensionDescriptor`,
11. uruchomić preview pola,
12. zobaczyć trace każdego etapu,
13. uruchomić test całej kategorii,
14. zobaczyć błędy i ostrzeżenia,
15. zapisać również nieukończony draft,
16. ponownie otworzyć zapisany JSON,
17. wyeksportować diagnostykę,
18. wykonać ciężkie operacje bez blokowania UI,
19. uruchomić konfigurację zapisaną przez UI w CLI.

## 23. Znane Ryzyka

| Ryzyko                                         | Mitigacja                                                    |
| ---------------------------------------------- | ------------------------------------------------------------ |
| JavaFX stanie się miejscem duplikacji pipeline | Wszystkie preview przez Core/use case                        |
| Trace FULL będzie trzymał zbyt dużo obrazów    | Trzymać tylko ostatni preview, dodać limit store             |
| Dynamic forms będą zbyt ogólne                 | Zacząć od typów z obecnego Extension API                     |
| Edycja DTO stanie się chaotyczna               | Wydzielić `CategoryDraftEditor` i testy roundtrip            |
| Regiony będą błędne przy zoomie/scrollu        | Rozbudować testy `CoordinateMapper` i region selection       |
| Brak pluginu zablokuje edycję                  | Wprowadzić unresolved extension placeholder                  |
| UI zacznie blokować na OCR                     | Wymusić `BackgroundExecutor` dla PDF/OCR/preview/test/export |

## 24. Zadania Zależne Od Core

Część funkcji UI wymaga dalszego dopracowania Core:

- pełniejsze `ProcessingTrace` z czasami etapów i obrazami,
- trace image references niezależne od Domain,
- publiczny use case preview field albo stabilne API `FieldProcessingService`,
- identyfikacja/test category z bogatymi wynikami per condition,
- QR detector z payload i bounds w neutralnym modelu,
- OCR model z lines/blocks, jeśli overlay ma obsłużyć więcej niż words,
- standardowe extensions z pełnymi descriptorami parametrów,
- jawny model unresolved extension/config problem, jeśli konfigurator ma otwierać błędne drafty semantyczne.
