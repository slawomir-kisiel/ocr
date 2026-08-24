# Ujednolicony model detectorow dla identification i anchorow

| Pole          | Wartosc |
|---------------|---------|
| Dokument      | `28-unified-detector-model.md` |
| Status        | Draft |
| Powiazane     | `08-category-configuration.md`, `10-extension-api.md`, `13-javafx-configurator.md`, `24-remaining-implementation-backlog.md` |

## 1. Cel

Celem zmiany jest ujednolicenie sposobu, w jaki system korzysta z tekstu OCR,
kodow QR i kodow kreskowych podczas identyfikacji kategorii oraz wykrywania
anchorow.

Obecnie konfiguracja jest niespojna:

- identification ma `type` oraz opcjonalny `detector`,
- `type` steruje logika `TEXT`, `QR`, `BARCODE`,
- anchor nie ma `type`, ale ma `detector`,
- dla anchorow tekstowych istnieje specjalny fallback oparty o parametr
  detectora `text`,
- QR i barcode zwracaja geometrie oraz wspolny `message`, ale nie zwracaja
  struktury zgodnej z modelem OCR.

Docelowo mechanizmem wspolnym powinien byc `detector`, a wynikiem detectora
powinna byc tresc reprezentowana w strukturze zgodnej z OCR: `page`, `area`,
`paragraph`, `line`, `word`.

## 2. Decyzja kierunkowa

Nalezy usunac semantyczne znaczenie pola `type` z warunkow identyfikacji i
oprzec identyfikacje oraz anchory na tym samym modelu:

- detector wybiera zrodlo tresci i geometrii,
- matcher porownuje oczekiwana wartosc z trescia zwrocona przez detector,
- `searchRegion` ogranicza obszar dzialania detectora,
- identification i anchor uzywaja tego samego modelu `detector` +
  `expectedText` + `matcher`,
- wynik detectora jest przeszukiwany tak samo niezaleznie od tego, czy pochodzi
  z OCR, QR czy barcode.

W efekcie anchor oparty o slowo OCR, kod QR albo kod kreskowy powinien byc
wykrywany przez ten sam przeplyw.

## 3. Docelowy model wyniku detectora

Detector powinien zwracac tresc w strukturze zgodnej z modelem OCR.

Minimalna postac wyniku:

- status detekcji,
- tekst calosciowy,
- struktura OCR-like:
  - page,
  - areas,
  - paragraphs,
  - lines,
  - words,
- opcjonalny komunikat diagnostyczny.

W obecnym modelu domenowym najblizszym typem jest `OcrText`, ktory zawiera:

- `value`,
- `hocr`,
- `areas`,
- dostep do `paragraphs()`, `lines()` i `words()`.

Rekomendowana zmiana kontraktu `DetectionResult`:

```java
public record DetectionResult(
    DetectionStatus status,
    OcrText text,
    String message
) {
}
```

Pole `message` powinno sluzyc do diagnostyki, a nie jako podstawowy payload
biznesowy. Geometria powinna wynikac ze struktury OCR-like.

## 4. Mapowanie QR i barcode na OCR-like

Dla kodu QR detector powinien zwracac jeden logiczny element OCR:

- jedna `area` z koordynatami wykrytego kodu QR,
- w niej jeden `paragraph` z tymi samymi koordynatami,
- w nim jedna `line` z tymi samymi koordynatami,
- w niej jeden `word`:
  - `text` = pelna tresc QR,
  - `boundingBox` = koordynaty kodu QR,
  - `confidence` = wynik detekcji, jezeli jest dostepny, albo `1.0`.

Tresc QR nie powinna byc dzielona na slowa wedlug spacji, separatorow ani
formatu payloadu. Jeden kod QR daje jedno `word`.

Dla barcode zasada jest identyczna:

- jedna `area`,
- jeden `paragraph`,
- jedna `line`,
- jedno `word`,
- `text` = zdekodowana wartosc, na przyklad numer lub identyfikator.

## 5. OCR jako detector

Aby model byl naprawde jednolity, OCR tekstowy rowniez powinien byc traktowany
jako detector.

Docelowo powinien istniec detector, na przyklad:

- `ocr`,
- albo `text`,

ktory zwraca `OcrText` z silnika OCR dla wskazanego obrazu albo regionu.

Dzieki temu:

- identification nie musi miec `type = TEXT`,
- anchor tekstowy nie wymaga specjalnego fallbacku,
- UI moze zawsze pokazywac pole `Detector`,
- logika wyszukiwania tekstu dziala na wyniku detectora.

## 6. Konfiguracja kategorii

### 6.1. Identification

Docelowy warunek identyfikacji:

```json
{
  "page": 1,
  "detector": {
    "id": "ocr",
    "parameters": {}
  },
  "matcher": {
    "id": "contains",
    "parameters": {}
  },
  "expectedText": "INVOICE",
  "searchRegion": {
    "x": 10,
    "y": 20,
    "width": 200,
    "height": 50
  }
}
```

Przyklad dla QR:

```json
{
  "page": 1,
  "detector": {
    "id": "qr",
    "parameters": {}
  },
  "expectedText": "FORM-A",
  "searchRegion": {
    "x": 400,
    "y": 20,
    "width": 120,
    "height": 120
  }
}
```

Przyklad dla barcode:

```json
{
  "page": 1,
  "detector": {
    "id": "barcode",
    "parameters": {}
  },
  "expectedText": "5901234123457"
}
```

Pole `type` powinno zostac usuniete z konfiguracji identification. Nowy format
nie musi obslugiwac starego zapisu.

### 6.2. Anchor

Anchor powinien byc definiowany analogicznie do warunku identification:

- `page`,
- `detector`,
- `expectedText`,
- `matcher`,
- `searchRegion`,
- `referenceFeature`,
- `required`.

Oznacza to, ze anchor nie jest tylko wykryta geometria z detectora. Jest
warunkiem detekcji tresci, ktory dodatkowo dostarcza geometrie do normalizacji
dokumentu.

Przyklad dla QR:

```json
{
  "id": "document-code",
  "page": 1,
  "detector": {
    "id": "qr",
    "parameters": {}
  },
  "expectedText": "FORM-A",
  "matcher": {
    "id": "contains",
    "parameters": {}
  },
  "required": true,
  "searchRegion": {
    "x": 400,
    "y": 20,
    "width": 120,
    "height": 120
  },
  "referenceFeature": {
    "x": 410,
    "y": 30,
    "width": 100,
    "height": 100
  }
}
```

Dla anchorow tekstowych nie nalezy kodowac tekstu jako parametru detectora
`text`. Oczekiwana wartosc anchoru powinna byc jawna w polu `expectedText`, a
sposob dopasowania powinien wynikac z `matcher`.

Przyklad dla anchoru tekstowego:

```json
{
  "id": "title",
  "page": 1,
  "detector": {
    "id": "ocr",
    "parameters": {}
  },
  "expectedText": "INVOICE",
  "matcher": {
    "id": "contains",
    "parameters": {}
  },
  "required": true,
  "searchRegion": {
    "x": 20,
    "y": 20,
    "width": 300,
    "height": 80
  },
  "referenceFeature": {
    "x": 20,
    "y": 20,
    "width": 120,
    "height": 30
  }
}
```

## 7. Logika core

### 7.1. Wspolny przeplyw

Identification i anchor powinny korzystac z tego samego przebiegu:

1. Wybrac detector z konfiguracji.
2. Przyciac obraz do `searchRegion`, jezeli region jest podany.
3. Uruchomic detector.
4. Otrzymac `OcrText`.
5. Przeszukac `words`, `lines`, `paragraphs` albo `areas` w zaleznosci od
   potrzeb.
6. Porownac tekst z `expectedText` przez matcher albo domyslne porownanie.
7. Zwrocic wynik wraz z geometria znalezionego elementu.

### 7.2. Identification

Warunek identyfikacji jest spelniony, jezeli:

- detector zwrocil status `DETECTED`,
- przy pustym `expectedText` samo wykrycie jest wystarczajace,
- przy podanym `expectedText` tresc detectora pasuje do wartosci oczekiwanej.

Domyslne porownanie moze pozostac zgodne z obecna logika `contains` po
normalizacji tekstu. Jezeli podano matcher, matcher ma pierwszenstwo.

### 7.3. Anchor

Anchor jest wykryty, jezeli:

- detector zwrocil status `DETECTED`,
- przy pustym `expectedText` mozna uzyc pierwszego wykrytego elementu,
- przy podanym `expectedText` nalezy znalezc pasujace `word`, `line`,
  `paragraph` albo `area`,
- jezeli podano `matcher`, dopasowanie tekstu anchoru musi przejsc przez ten
  matcher,
- jezeli `matcher` nie jest podany, stosowane jest domyslne porownanie zgodne
  z identification.

Geometria `ReferenceFeature` powinna pochodzic z pasujacego elementu OCR-like.
Dla QR i barcode bedzie to geometria kodu, poniewaz `area`, `paragraph`,
`line` i `word` maja te same koordynaty.

## 8. JavaFX Configurator

### 8.1. Identification UI

Panel identification powinien:

- usunac pole `Type` z glownego workflow,
- zawsze pokazywac `Detector`,
- prezentowac detector jako select-list / `ComboBox`,
- filtrowac liste do rozszerzen typu `DETECTOR`,
- domyslnie wybierac `ocr` albo pierwszy dostepny detector tekstowy,
- pokazywac parametry wybranego detectora inline,
- pozostawic `Matcher`, `Expected Text`, `Page` i `Search Region`.

Dialog wyboru rozszerzenia moze pozostac dla miejsc, gdzie lista jest dluga
albo potrzebne jest wyszukiwanie, ale dla detectorow w identification lepsza
jest zwykla lista wyboru.

### 8.2. Anchor UI

Panel anchor powinien uzywac analogicznego wyboru detectora:

- `Detector` jako select-list,
- `Expected Text`,
- `Matcher`,
- `Search Region`,
- `Reference Feature`,
- `Required`.

### 8.3. Viewer i diagnostyka

Poniewaz QR i barcode beda mapowane do OCR-like struktury, viewer moze
prezentowac je tym samym mechanizmem co OCR:

- areas,
- paragraphs,
- lines,
- words.

W diagnostyce warto pokazywac:

- id detectora,
- status detectora,
- tekst calosciowy,
- znaleziony element,
- geometrie znalezionego elementu,
- matcher i wynik matchera.

## 9. Plan wdrozenia

Zmiane nalezy wprowadzic etapami.

### 9.1. Etap 1: rozszerzenie kontraktu detectora

- Dodac OCR-like wynik do `DetectionResult`.
- Dostosowac QR i barcode detectory.
- Usunac biznesowe uzycie `message` jako payloadu detectora.
- Usunac `geometries`, jezeli ich rola zostanie przejeta przez OCR-like
  strukture wyniku.
- Dodac testy jednostkowe mapowania QR/barcode do `OcrText`.

### 9.2. Etap 2: wspolna logika odczytu wyniku detectora

- Wydzielic wspolny serwis/pomocnik uruchamiania detectora.
- Zwracac wynik zawierajacy `OcrText` i geometrie elementow.
- Przepiac identification QR/BARCODE na nowy wynik.
- Przepiac anchor QR/BARCODE na nowy wynik.

### 9.3. Etap 3: OCR jako detector

- Dodac detector `ocr` albo `text`.
- Usunac fallback anchorow oparty o parametr `detector.parameters.text`.
- Dodac `expectedText` i `matcher` do modelu anchoru.
- Przepiac anchor tekstowy na zwykly detector OCR z dopasowaniem przez matcher.

### 9.4. Etap 4: konfiguracja bez `type`

- Usunac `IdentificationCondition.type`.
- Usunac `ConditionDto.type`.
- Usunac zaleznosci core od `type`.
- Usunac walidacje i domyslne wartosci oparte o `type`.
- Zaktualizowac wszystkie konfiguracje testowe i przykladowe do nowego formatu.
- Zaktualizowac DTO, runtime config, walidacje i test fixtures.

### 9.5. Etap 5: UI

- Zastapic wybor typu warunku select-lista detectora.
- Usunac automatyczne mapowanie `type -> detector`.
- Pokazywac detector zawsze.
- Przeniesc wybor detectora z dialogu do `ComboBox`.
- Zaktualizowac testy JavaFX draft/viewmodel.

## 10. Kompatybilnosc

Zmiana jest breaking change. Nie utrzymujemy kompatybilnosci wstecznej dla
konfiguracji opartych o `type`.

Konsekwencje:

- loader nie dopelnia `detector.id` na podstawie `type`,
- konfiguracje bez jawnego `detector` sa niepoprawne,
- konfiguracje z `type` powinny zostac zaktualizowane w repo razem ze zmiana
  modelu,
- zapis z konfiguratora emituje tylko nowy format.

## 11. Ryzyka i decyzje otwarte

### 11.1. Czy `DetectionResult` powinien zalezec od domenowego `OcrText`

Najprostsza implementacja to uzycie `OcrText` bezposrednio w
`extension-api`. Wymaga to jednak akceptacji zaleznosci API rozszerzen od
modelu OCR domeny.

Alternatywa to osobny typ w `extension-api`, na przyklad `DetectedText`,
ktory ma taka sama strukture jak OCR, ale nie importuje domenowego `OcrText`.
To ogranicza coupling, ale zwieksza liczbe mapowan.

Rekomendacja: uzyc `OcrText`, jezeli `extension-api` juz akceptuje zaleznosc
od typow domenowych takich jak `Region`. W obecnym kodzie taka zaleznosc juz
istnieje.

### 11.2. Jak wybierac geometrie anchoru przy wielu dopasowaniach

Dla wielu pasujacych elementow nalezy ustalic regule:

- pierwszy element w kolejnosci czytania,
- najlepszy score,
- najblizszy `referenceFeature`,
- albo jawna strategia w konfiguracji.

Rekomendacja na pierwszy etap: pierwszy pasujacy element w wyniku detectora,
z zachowaniem kolejnosci zwroconej przez detector.

### 11.3. Domyslny matcher dla anchoru

Anchor powinien miec taki sam model dopasowania jak identification, czyli
`expectedText` oraz opcjonalny `matcher`.

Otwarte pozostaje tylko to, jaki matcher powinien byc domyslny, gdy pole
`matcher` nie jest podane. Rekomendacja na pierwszy etap: uzyc tego samego
domyslnego porownania co identification, czyli `contains` po normalizacji
tekstu.

## 12. Kryteria akceptacji

Zmiana jest zakonczona, jezeli:

- identification nie wymaga `type` do rozrozniania OCR, QR i barcode,
- `type` nie istnieje w DTO ani runtime modelu identification,
- anchor tekstowy, QR i barcode uzywaja tego samego przeplywu detectora,
- anchor ma `expectedText` i `matcher` analogicznie do identification,
- QR detector zwraca payload jako jedno `word` z geometria kodu,
- barcode detector zwraca payload jako jedno `word` z geometria kodu,
- UI identification zawsze pokazuje detector jako select-list,
- UI anchor pokazuje detector w tym samym modelu,
- loader wymaga jawnego `detector` i nie interpretuje starego pola `type`,
- testy pokrywaja:
  - identification OCR,
  - identification QR,
  - identification barcode,
  - anchor OCR,
  - anchor QR,
  - anchor barcode,
  - walidacje braku jawnego detectora.
