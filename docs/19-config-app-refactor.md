# Refaktor ConfiguratorApplication

| Pole           | Wartość                                                                 |
| -------------- | ----------------------------------------------------------------------- |
| ID dokumentu   | DOC-019                                                                 |
| Tytuł          | Refaktor `ConfiguratorApplication`                                      |
| Wersja         | 0.1                                                                     |
| Status         | Draft                                                                   |
| Typ            | Refactoring Plan                                                        |
| Źródło prawdy  | Aktualny kod `javafx`, `docs/13-javafx-configurator.md`, `docs/18-javafx-tasks.md` |

## 1. Cel dokumentu

Celem dokumentu jest rozpisanie etapowego refaktoru klasy:

```text
javafx/src/main/java/pl/sk/ocr/configurator/ConfiguratorApplication.java
```

Klasa pełni obecnie zbyt wiele ról jednocześnie: shell aplikacji, budowę drzewa konfiguracji, panel właściwości, viewer dokumentu, overlay regionów, mutacje formularzy, skróty klawiaturowe i obsługę statusu.

Refaktor ma zmniejszyć rozmiar i odpowiedzialność `ConfiguratorApplication`, bez zmiany zachowania użytkowego aplikacji.

## 2. Zasady refaktoru

- Refaktor wykonywać krok po kroku.
- Po każdym kroku uruchomić `mvn -pl javafx test`.
- Nie zmieniać modelu DTO ani formatu JSON w ramach samego refaktoru.
- Nie przenosić logiki pipeline OCR do UI.
- Mutacje konfiguracji nadal przechodzą przez `CategoryEditorViewModel` i `CategoryDraftEditor`.
- `ConfiguratorApplication` pozostaje entry pointem JavaFX i kompozytorem głównych paneli.
- Każdy wydzielony panel ma mieć jasny kontrakt `refresh` i `commit`, aby zapis zawsze obejmował cały aktualny draft kategorii.
- Wydzielane klasy powinny być możliwie package-private, dopóki nie będzie potrzeby publicznego API.
- Nie wykonywać dużego jednorazowego przepisywania viewer/overlay razem z formularzami.

## 3. Docelowy podział odpowiedzialności

Docelowo `ConfiguratorApplication` powinien odpowiadać głównie za:

- inicjalizację usług,
- utworzenie `Stage` i głównego layoutu,
- konfigurację toolbaru i globalnych skrótów,
- połączenie paneli ze wspólnym `CategoryEditorViewModel`,
- zapis/odczyt konfiguracji i dokumentu,
- status bar.

Pozostałe odpowiedzialności powinny zostać wydzielone.

## 4. Wspólny kontrakt panelu właściwości

Wydzielane panele właściwości powinny implementować wspólny kontrakt:

```java
interface DetailsPanel {
    Node view();
    void refresh();
    void commit();
}
```

Znaczenie metod:

- `view()` zwraca główny node panelu.
- `refresh()` odświeża kontrolki na podstawie aktualnego draftu i aktualnego zaznaczenia.
- `commit()` zapisuje wartości z kontrolek do draftu przez ViewModel.

`commit()` musi być bezpieczny do wywołania przed `Save`, nawet jeśli panel nie ma aktywnego elementu.

## 5. Proponowane klasy

### 5.1. `PropertiesPanel`

Router paneli właściwości.

Odpowiedzialności:

- trzyma instancje paneli szczegółowych,
- wybiera aktywny panel na podstawie `TreeNodeType`,
- wywołuje `refresh()` na aktywnym panelu,
- udostępnia `commitActivePanel()`,
- utrzymuje wspólny `ScrollPane` panelu właściwości.

### 5.2. `CategoryPropertiesPanel`

Panel właściwości root category.

Zakres:

- `id`,
- `displayName`,
- `description`,
- `version`,
- `Page Policy`,
- OCR defaults.

### 5.3. `IdentificationPropertiesPanel`

Panel identyfikacji.

Zakres:

- root identification,
- identification group,
- condition,
- add/remove/move group,
- add/remove/move condition,
- edycja condition,
- `condition.searchRegion`.

### 5.4. `AnchorPropertiesPanel`

Panel anchorów.

Zakres:

- lista anchorów,
- add/remove/move anchor,
- `id`,
- `page`,
- `detector.id`,
- `required`,
- `searchRegion`,
- `referenceFeature.bounds`.

### 5.5. `GeometryPropertiesPanel`

Panel geometrii.

Zakres:

- `referenceWidth`,
- `referenceHeight`,
- pobranie wymiarów z dokumentu,
- ostrzeżenie przy zmianie reference dimensions po zdefiniowaniu regionów,
- `strategy.type`,
- wybór anchor IDs strategii.

### 5.6. `FieldPropertiesPanel`

Panel pól.

Zakres początkowy:

- lista pól,
- `field.region`.

Zakres późniejszy:

- field OCR,
- output,
- image processors,
- transformers,
- validators.

### 5.7. `DocumentViewerPanel`

Panel podglądu dokumentu.

Zakres:

- `ImageView`,
- `ScrollPane`,
- zoom toolbar,
- page navigator,
- render strony,
- overlay OCR,
- overlay regionów,
- tryby `Select`, `Pan`, `Draw Region`,
- move/resize aktywnych ramek.

### 5.8. `ConfigurationTreePanel`

Panel drzewa konfiguracji.

Zakres:

- budowa drzewa,
- zachowanie expanded nodes,
- focus po add/remove/move,
- selection model,
- przełączanie strony po wyborze elementu.

## 6. Zadania refaktoru

### RF-001 Wspólne helpery UI

Wydzielić helpery używane przez formularze.

Zakres:

- `section`,
- `titledPane`,
- `addFormRow`,
- `installTooltip`,
- `setVisibleManaged`,
- konfiguracja koloru etykiet i output labels,
- tworzenie numeric spinnerów regionów.

Proponowana klasa:

```text
pl.sk.ocr.configurator.ui.FormControls
```

Kryteria akceptacji:

- wygląd formularzy nie zmienia się,
- wszystkie etykiety pozostają czytelne,
- `ConfiguratorApplication` nie zawiera już helperów formularzy.

### RF-002 GeometryPropertiesPanel

Wydzielić panel geometrii jako pierwszy, ponieważ ma mało zależności z overlay.

Zakres:

- przenieść kontrolki geometrii,
- przenieść `geometryDetailsForm`,
- przenieść `applyGeometry`,
- przenieść wybór anchorów strategii,
- przenieść ostrzeżenie reference dimensions.

Kryteria akceptacji:

- edycja geometrii działa jak przed refaktorem,
- `Save` zapisuje całą kategorię,
- `commit()` panelu geometrii jest wywoływany przed zapisem, gdy panel jest aktywny.

### RF-003 AnchorPropertiesPanel

Wydzielić panel anchorów.

Zakres:

- formularz anchorów,
- add/remove/move anchor,
- `searchRegion`,
- `referenceFeature.bounds`,
- aktywacja rysowania regionów przez callback do viewer/region service.

Kryteria akceptacji:

- add wybiera nowy anchor i rozwija parent,
- remove wybiera parent,
- move zachowuje zaznaczenie przeniesionego anchora,
- rysowanie regionów nadal działa.

### RF-004 IdentificationPropertiesPanel

Wydzielić panel identyfikacji.

Zakres:

- root identification,
- group,
- condition,
- condition search region,
- add/remove/move group i condition.

Kryteria akceptacji:

- po add/remove/move zachowany jest kontekst drzewa,
- edycja condition działa jak przed refaktorem,
- rysowanie i resize regionu condition nadal działa.

### RF-005 CategoryPropertiesPanel

Wydzielić panel kategorii.

Zakres:

- metadata,
- page policy,
- OCR defaults.

Kryteria akceptacji:

- warunkowe pokazywanie pól page policy działa jak przed refaktorem,
- `Ctrl+S` zapisuje ostatnie zmiany z aktywnych pól.

### RF-006 FieldPropertiesPanel

Wydzielić panel pól.

Zakres:

- lista fields,
- field region,
- aktywacja rysowania regionu pola.

Kryteria akceptacji:

- field region można edytować z formularza,
- field region można narysować na viewerze,
- późniejszy rozwój field OCR/output/pipeline nie wymaga zmian w `ConfiguratorApplication`.

### RF-007 PropertiesPanel Router

Wydzielić router aktywnego panelu właściwości.

Zakres:

- wybór panelu po `TreeNodeType`,
- `refreshActivePanel`,
- `commitActivePanel`,
- wspólny `ScrollPane`,
- wspólna sekcja `detailsInfo`.

Kryteria akceptacji:

- `ConfiguratorApplication` nie zawiera switcha budującego wszystkie formularze,
- save wywołuje `commitActivePanel`,
- panel właściwości nadal ma pionowy scroll i brak scrolla poziomego.

### RF-008 DocumentViewerPanel

Wydzielić viewer dokumentu po ustabilizowaniu paneli formularzy.

Zakres:

- toolbar zoom/mode,
- page navigation,
- render strony,
- zoom,
- scroll,
- overlay OCR,
- overlay regionów,
- hit-test ramek,
- move/resize regionów.

Kryteria akceptacji:

- `Fit Page`, `Fit Width`, `100%`, zoom i page navigation działają jak przed refaktorem,
- `Ctrl+scroll` zachowuje punkt pod kursorem,
- region drawing i resize działają dla condition oraz anchorów,
- `Pan` zachowuje dotychczasowy kursor i przeciąganie dokumentu.

### RF-009 ConfigurationTreePanel

Wydzielić drzewo konfiguracji.

Zakres:

- budowa drzewa,
- node IDs,
- expanded state,
- pending selection,
- selection callbacks,
- page switching.

Kryteria akceptacji:

- rozwinięte węzły pozostają rozwinięte po mutacjach,
- po add zaznaczany jest nowy element,
- po remove zaznaczany jest parent,
- wybór elementu konfiguracji przełącza viewer na właściwą stronę.

## 7. Kolejność realizacji

Rekomendowana kolejność:

1. `RF-001 Wspólne helpery UI`
2. `RF-002 GeometryPropertiesPanel`
3. `RF-003 AnchorPropertiesPanel`
4. `RF-004 IdentificationPropertiesPanel`
5. `RF-005 CategoryPropertiesPanel`
6. `RF-006 FieldPropertiesPanel`
7. `RF-007 PropertiesPanel Router`
8. `RF-008 DocumentViewerPanel`
9. `RF-009 ConfigurationTreePanel`

## 8. Ryzyka

| Ryzyko                                      | Mitigacja                                                |
| ------------------------------------------- | -------------------------------------------------------- |
| Refaktor zmieni zachowanie zapisu            | Po każdym kroku sprawdzać `Save` i `Ctrl+S`              |
| Region edit target stanie się rozproszony    | Użyć jawnych callbacków albo małego serwisu koordynującego |
| Viewer będzie zależny od formularzy          | Viewer powinien znać typ targetu, nie szczegóły formularza |
| Zniknie zachowanie zaznaczenia drzewa        | Wydzielić reguły selection/focus razem z tree panelem     |
| Zbyt duży refaktor naraz                     | Realizować jeden panel na raz                            |

## 9. Kryteria zakończenia refaktoru

Refaktor można uznać za zakończony, gdy:

- `ConfiguratorApplication` jest entry pointem i kompozytorem paneli, a nie implementacją wszystkich formularzy,
- każdy panel właściwości ma własną klasę,
- viewer dokumentu ma własną klasę,
- drzewo konfiguracji ma własną klasę,
- save zapisuje cały draft kategorii niezależnie od aktywnego panelu,
- wszystkie obecne funkcjonalności UI działają jak przed refaktorem,
- `mvn -pl javafx test` przechodzi po każdym etapie.
