# Projekt rozszerzeń transformacji obrazu opartych o `imagemagick`

## 1. Cel

Celem jest wykorzystanie projektu `imagemagick/` jako biblioteki algorytmów preprocessingu obrazu dla projektu OCR i wystawienie tych operacji jako rozszerzeń typu `ImageProcessor`.

Rozszerzenia mają być używane w istniejącym pipeline pól:

```text
field region crop
→ image processors
→ OCR
→ value transformers
→ validators
```

Projekt `imagemagick` nie jest integracją z natywnym ImageMagick. Jest to biblioteka Java oparta o `BufferedImage`, `Graphics2D` i `ImageIO`, inspirowana wybranymi mechanizmami ImageMagick.

## 2. Kontekst techniczny

### 2.1. Aktualny projekt OCR

Projekt OCR posiada już kontrakt rozszerzeń:

- `pl.sk.ocr.extension.api.image.ImageProcessor`,
- `ImageProcessingRequest`,
- `ImageProcessingContext`,
- `ProcessingImage`,
- `TraceSink`.

Pipeline obrazu jest wykonywany w `FieldProcessingService` przed OCR. Każdy krok:

- dostaje `ProcessingImage`,
- dostaje parametry rozszerzenia przez `ExtensionParameters`,
- zwraca nowy `ProcessingImage`,
- może zapisać diagnostykę przez `ImageProcessingContext.trace()`.

Aktualne standardowe procesory obrazu to:

- `remove-boxes`,
- `condense-content`,
- `crop-empty-margins`.

### 2.2. Projekt `imagemagick`

Projekt `imagemagick/` składa się z:

- `image-ocr-preprocess-core` - biblioteka algorytmów,
- `image-ocr-preprocess-cli` - CLI demonstracyjne.

Najważniejsze klasy biblioteki:

- `OcrImagePreprocessor` - wysokopoziomowy pipeline,
- `ImagePreprocessOptions` - konfiguracja pipeline,
- `PreprocessResult` - obraz wynikowy i diagnostyka,
- `ImageMagickLikeOps` - fasada dla pojedynczych operacji,
- `OcrFilters` - grayscale, threshold, median, adaptive threshold, morphology, trim,
- `ImageNormalize` - normalize i contrast stretch,
- `AutoThreshold` / `ThresholdOps` - Otsu, Triangle, Kapur i inne progowania,
- `TonalAdjustments` - level i gamma,
- `ColorAdjustments` - equalize, CLAHE, local contrast, white balance, HSV threshold, negate, posterize,
- `ConvolutionFilters` - blur, Gaussian blur, sharpen, unsharp,
- `AdvancedDenoiseFilters` - bilateral i Kuwahara,
- `GeometryTransforms` - rotate, flip, flop, crop, shave, extent,
- `PerspectiveCorrection` - korekcja perspektywy,
- `BackgroundCorrection` - korekcja nierównego tła,
- `ConnectedComponents` - usuwanie małych komponentów,
- `EdgeDetection` - Sobel,
- `PageContour` - wykrywanie konturu strony.

## 3. Decyzja architektoniczna

### 3.1. Nowy moduł rozszerzeń

Rekomendowane jest dodanie osobnego modułu Maven:

```text
extensions-imagemagick
```

Moduł powinien zależeć od:

- `extension-api`,
- `domain`,
- `image-ocr-preprocess-core`.

Nie należy mieszać tych rozszerzeń z `extensions-standard`, ponieważ:

- liczba operacji jest duża,
- zależność od biblioteki preprocessingu ma osobny cykl życia,
- część operacji jest kosztowna obliczeniowo,
- w przyszłości można będzie wyłączyć ten pakiet bez usuwania podstawowych rozszerzeń.

### 3.2. Rejestracja przez ServiceLoader

Moduł powinien dostarczyć:

```text
pl.sk.ocr.extensions.imagemagick.ImageMagickExtensionProvider
```

oraz plik:

```text
META-INF/services/pl.sk.ocr.extension.api.ExtensionProvider
```

Wpis:

```text
pl.sk.ocr.extensions.imagemagick.ImageMagickExtensionProvider
```

Dzięki temu istniejący `ServiceLoaderExtensionRegistryFactory` automatycznie załaduje rozszerzenia w CLI i JavaFX.

### 3.3. Adapter obrazu

`imagemagick` operuje na `BufferedImage`. OCR API używa `ProcessingImage`.

Potrzebny jest adapter:

```java
final class ImageMagickProcessingImage implements ProcessingImage
```

W praktyce może on być analogiczny do aktualnego `BufferedImageProcessingImage` z `extensions-standard`, ale powinien być publiczny pakietowo w module `extensions-imagemagick`.

Reguły:

- procesory nie modyfikują wejściowego `BufferedImage` w miejscu,
- wynik zawsze jest nowym `ProcessingImage`,
- przed wywołaniem operacji warto normalizować obraz do formatu obsługiwanego przez bibliotekę, np. przez `BufferedImages.toIntArgb(...)`, jeśli dana operacja tego wymaga.

## 4. Model rozszerzeń

### 4.1. Strategia ID

ID powinny być prefiksowane, aby nie kolidować z dotychczasowymi prostymi procesorami:

```text
im-normalize
im-profile
im-auto-threshold
im-adaptive-threshold
```

Prefiks `im-` oznacza operacje z pakietu ImageMagick-like.

### 4.2. Typ rozszerzeń

Wszystkie operacje preprocessingu obrazu są rozszerzeniami:

```text
ExtensionType.IMAGE_PROCESSOR
```

Nie należy używać `ValueTransformer`, ponieważ transformery tekstu działają po OCR, a te operacje mają modyfikować obraz przed OCR.

### 4.3. Klasa bazowa

Warto dodać klasę bazową:

```java
abstract class AbstractImageMagickProcessor implements ImageProcessor
```

Odpowiedzialności:

- walidacja obecności obrazu,
- pobieranie parametrów z `ExtensionParameters`,
- konwersja typów `String`, `Integer`, `Double`, `Boolean`,
- tworzenie `ProcessingImage` z `BufferedImage`,
- zapisywanie trace z podstawową diagnostyką:
  - `inputWidth`,
  - `inputHeight`,
  - `outputWidth`,
  - `outputHeight`,
  - `processorId`,
  - parametry wykonania.

## 5. Zakres rozszerzeń MVP

MVP powinno dostarczyć najpierw niewielki zestaw operacji, które realnie pomagają OCR i są łatwe do strojenia w UI.

### 5.1. `im-profile`

Wysokopoziomowy procesor używający `OcrImagePreprocessor`.

Parametry:

| Nazwa | Typ | Domyślnie | Opis |
| --- | --- | --- | --- |
| `profile` | ENUM | `GOOD_SCAN` | `CUSTOM`, `GOOD_SCAN`, `NOISY_SCAN`, `PHONE_PHOTO`, `COLOR_BACKGROUND` |
| `maxWidth` | INTEGER | `2500` | Maksymalna szerokość, `0` wyłącza limit |
| `maxHeight` | INTEGER | `3500` | Maksymalna wysokość, `0` wyłącza limit |
| `orientation` | ENUM | `ANY` | `ANY`, `PORTRAIT`, `LANDSCAPE` |
| `autoRotate` | BOOLEAN | `false` | Obrót o 90 stopni do preferowanej orientacji |
| `normalize` | BOOLEAN | zgodnie z profilem | Wymuszenie normalizacji |
| `adaptiveThreshold` | BOOLEAN | zgodnie z profilem | Wymuszenie lokalnego progowania |
| `deskew` | BOOLEAN | zgodnie z profilem | Wymuszenie prostowania |
| `trim` | BOOLEAN | zgodnie z profilem | Przycięcie marginesów |

Trace:

- `scaleApplied`,
- `orientationRotated`,
- `deskewAngle`,
- `cropBounds`,
- `normalizeBlackPoint`,
- `normalizeWhitePoint`.

Zastosowanie:

- szybki start w UI,
- typowe scenariusze: dobry skan, zaszumiony skan, zdjęcie telefonem.

### 5.2. `im-normalize`

Procesor oparty o `ImageNormalize.normalize(...)`.

Parametry:

| Nazwa | Typ | Domyślnie | Opis |
| --- | --- | --- | --- |
| `histogramRegion` | ENUM | `FULL` | `FULL`, `CENTER_PERCENT`, `AUTO_STABLE_CENTER` |
| `centerPercent` | INTEGER | `70` | Procent centralnego regionu histogramu |
| `autoStart` | INTEGER | `50` | Start dla `AUTO_STABLE_CENTER` |
| `autoStep` | INTEGER | `10` | Krok rozszerzania regionu |
| `autoMax` | INTEGER | `100` | Maksymalny region |
| `medianJump` | INTEGER | `25` | Próg zmiany mediany |
| `blackRatioJump` | DECIMAL | `0.05` | Próg zmiany udziału czerni |
| `blackThreshold` | INTEGER | `32` | Próg czerni dla analizy histogramu |

Trace:

- `blackPoint`,
- `whitePoint`,
- użyty tryb histogramu.

### 5.3. `im-auto-threshold`

Automatyczna binaryzacja.

Parametry:

| Nazwa | Typ | Domyślnie | Opis |
| --- | --- | --- | --- |
| `method` | ENUM | `OTSU` | `OTSU`, `TRIANGLE`, `KAPUR` |

Zastosowanie:

- dokumenty z wyraźnym podziałem tekst/tło,
- pola, w których OCR lepiej działa na obrazie binarnym.

### 5.4. `im-adaptive-threshold`

Lokalna binaryzacja.

Parametry:

| Nazwa | Typ | Domyślnie | Opis |
| --- | --- | --- | --- |
| `window` | INTEGER | `31` | Nieparzyste, minimum `3` |
| `offset` | INTEGER | `8` | Przesunięcie progu lokalnego |

Zastosowanie:

- zdjęcia telefonem,
- nierówne oświetlenie,
- cienie.

### 5.5. `im-deskew`

Prostowanie przekoszenia.

Parametry:

| Nazwa | Typ | Domyślnie | Opis |
| --- | --- | --- | --- |
| `threshold` | INTEGER | `180` | Próg uznania piksela za tekst |
| `autoCrop` | BOOLEAN | `true` | Przycięcie po obrocie |

Trace:

- `angle`,
- `cropBounds`.

Uwaga:

Procesor jest kosztowniejszy niż proste filtry. Nie powinien być domyślnie dodawany do każdego pola bez potrzeby.

### 5.6. `im-background-correct`

Korekcja nierównego tła.

Parametry:

| Nazwa | Typ | Domyślnie | Opis |
| --- | --- | --- | --- |
| `blurRadius` | INTEGER | `25` | Promień estymacji tła |

Zastosowanie:

- zdjęcia z cieniem,
- kolorowe lub nierówne tło,
- dokumenty fotografowane telefonem.

### 5.7. `im-median`

Filtr medianowy 3x3.

Parametry: brak.

Zastosowanie:

- drobny szum,
- pojedyncze piksele po skanowaniu lub kompresji.

### 5.8. `im-morphology`

Operacje morfologiczne.

Parametry:

| Nazwa | Typ | Domyślnie | Opis |
| --- | --- | --- | --- |
| `operation` | ENUM | `OPEN` | `ERODE`, `DILATE`, `OPEN`, `CLOSE` |

Zastosowanie:

- `OPEN` usuwa drobne czarne artefakty,
- `CLOSE` domyka przerwy w literach.

## 6. Zakres rozszerzeń P2

Po MVP można dodać operacje bardziej specjalistyczne:

- `im-contrast-stretch`,
- `im-level`,
- `im-gamma`,
- `im-equalize`,
- `im-clahe`,
- `im-local-contrast`,
- `im-blur`,
- `im-gaussian-blur`,
- `im-sharpen`,
- `im-unsharp`,
- `im-bilateral`,
- `im-kuwahara`,
- `im-trim`,
- `im-rotate`,
- `im-crop`,
- `im-shave`,
- `im-extent`,
- `im-perspective`,
- `im-page-contour-crop`,
- `im-white-balance`,
- `im-hsv-threshold`,
- `im-negate`,
- `im-posterize`,
- `im-remove-small-components`,
- `im-sobel`.

Nie wszystkie powinny być eksponowane od razu w UI. Część z nich ma sens głównie diagnostyczny albo wymaga zaawansowanego UX, np. `perspective` z wyborem narożników.

## 7. Konfiguracja Maven

### 7.1. Integracja projektu `imagemagick`

Są dwie możliwe ścieżki.

#### Opcja A - moduł wewnątrz głównego reactor build

Przenieść lub podłączyć `image-ocr-preprocess-core` jako moduł głównego parenta OCR.

Zalety:

- jedna komenda Maven buduje wszystko,
- łatwe testy integracyjne,
- brak potrzeby lokalnego `mvn install`.

Wady:

- trzeba ujednolicić wersję Java z 17 do 21 albo potwierdzić zgodność kompilacji modułu 17 w projekcie 21,
- większy reactor build.

#### Opcja B - zależność publikowana lokalnie lub w repozytorium

Zostawić `imagemagick` jako osobny projekt i dodać zależność:

```xml
<dependency>
  <groupId>pl.imagemagick.ocr</groupId>
  <artifactId>image-ocr-preprocess-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

Zalety:

- separacja kodu,
- możliwość wersjonowania biblioteki preprocessingu niezależnie od OCR.

Wady:

- wymaga publikacji artefaktu albo instalacji lokalnej,
- CI musi budować/pobierać dodatkową zależność.

Rekomendacja dla bieżącego repozytorium:

```text
Opcja A na czas aktywnego rozwoju, później Opcja B po ustabilizowaniu API preprocessingu.
```

## 8. Parametry i dynamiczny formularz UI

Obecny JavaFX Configurator używa `ExtensionDescriptor.parameters()` do budowania dynamicznego formularza.

Dla nowych procesorów trzeba:

- używać `ExtensionParameterType.ENUM` dla wartości słownikowych,
- wypełniać `ParameterConstraints.allowedValues`,
- ustawiać sensowne `defaultValue`,
- opisać każdy parametr w `description`, bo UI pokazuje tooltipy.

Warto rozszerzyć helper deskryptorów o metodę:

```java
enumParameter(name, displayName, description, required, allowedValues, defaultValue)
```

Aktualny `StandardDescriptors` ma helpery dla `STRING`, `INTEGER`, `DECIMAL`, `BOOLEAN`, `REGEX`, ale nie ma wygodnego helpera dla `ENUM`.

## 9. Trace i diagnostyka

Rozszerzenia powinny emitować trace przez:

```java
context.trace().add("imagemagick.processor", attributes);
```

Minimalne atrybuty:

- `processorId`,
- `inputWidth`,
- `inputHeight`,
- `outputWidth`,
- `outputHeight`,
- `parameters`.

Dla konkretnych operacji:

- `im-profile`: `scaleApplied`, `orientationRotated`, `deskewAngle`, `cropBounds`, `normalizeBlackPoint`, `normalizeWhitePoint`,
- `im-normalize`: `blackPoint`, `whitePoint`,
- `im-deskew`: `angle`, `cropBounds`,
- `im-auto-threshold`: `method`,
- `im-adaptive-threshold`: `window`, `offset`.

Obrazy po każdym procesorze są już zapisywane w `FieldProcessingService.preview(...)` jako trace image steps, więc rozszerzenie nie musi samo zapisywać obrazów, dopóki kontrakt `TraceSink` nie obsługuje natywnie artefaktów binarnych.

## 10. Błędy i walidacja

Walidacja powinna działać na dwóch poziomach.

### 10.1. Deskryptor parametrów

Zakresy powinny być opisane w `ParameterConstraints`:

- threshold: `0..255`,
- adaptive window: `3..501`, nieparzystość walidowana w procesorze,
- blur radius: `1..200`,
- gamma: np. `0.1..10.0`,
- max width/height: `0..20000`.

### 10.2. Runtime processor

Procesor powinien rzucać `ExtensionException` albo `IllegalArgumentException` z czytelnym komunikatem, gdy:

- brakuje wymaganego parametru,
- parametr ma zły typ,
- wartość enum jest nieznana,
- `adaptiveWindow` jest parzyste,
- obraz jest pusty,
- operacja zwróciłaby obraz o rozmiarze `0x0`.

## 11. Kolejność implementacji

### Etap 1 - przygotowanie modułu

1. Dodać moduł `extensions-imagemagick`.
2. Podłączyć `image-ocr-preprocess-core`.
3. Dodać `ImageMagickExtensionProvider`.
4. Dodać adapter `ImageMagickProcessingImage`.
5. Dodać klasę bazową `AbstractImageMagickProcessor`.
6. Dodać helper `enumParameter`.

### Etap 2 - MVP procesorów

1. `im-profile`.
2. `im-normalize`.
3. `im-auto-threshold`.
4. `im-adaptive-threshold`.
5. `im-deskew`.
6. `im-background-correct`.
7. `im-median`.
8. `im-morphology`.

### Etap 3 - testy i UI

1. Test ServiceLoader, czy rozszerzenia są widoczne w rejestrze.
2. Test każdego procesora na syntetycznym `BufferedImage`.
3. Test walidacji parametrów.
4. Test, że JavaFX dialog rozszerzeń pokazuje nowe ID.
5. Test dynamicznego formularza dla `ENUM`, `BOOLEAN`, `INTEGER`, `DECIMAL`.

### Etap 4 - trace i eksport diagnostyczny

1. Uzupełnić trace attributes dla procesorów.
2. Sprawdzić eksport ZIP po `Preview Field`.
3. Upewnić się, że w paczce diagnostycznej są obrazy:
   - wejście procesora,
   - wynik po każdym procesorze,
   - obraz wejściowy OCR.

### Etap 5 - rozszerzenia P2

Dodać kolejne procesory według realnych scenariuszy OCR, a nie alfabetycznie według klas biblioteki.

Rekomendowana kolejność:

1. `im-trim`,
2. `im-contrast-stretch`,
3. `im-level`,
4. `im-gamma`,
5. `im-clahe`,
6. `im-white-balance`,
7. `im-remove-small-components`,
8. `im-page-contour-crop`,
9. `im-perspective`.

## 12. Przykład konfiguracji pola

Przykład dla zdjęcia telefonem:

```json
{
  "id": "invoice_number",
  "region": { "x": 100, "y": 300, "width": 800, "height": 220 },
  "imageProcessing": [
    {
      "processorId": "im-background-correct",
      "parameters": {
        "blurRadius": 25
      }
    },
    {
      "processorId": "im-normalize",
      "parameters": {
        "histogramRegion": "AUTO_STABLE_CENTER"
      }
    },
    {
      "processorId": "im-adaptive-threshold",
      "parameters": {
        "window": 41,
        "offset": 10
      }
    }
  ]
}
```

Przykład dla dobrego skanu:

```json
{
  "id": "voucher_code",
  "region": { "x": 120, "y": 180, "width": 900, "height": 160 },
  "imageProcessing": [
    {
      "processorId": "im-profile",
      "parameters": {
        "profile": "GOOD_SCAN"
      }
    },
    {
      "processorId": "im-auto-threshold",
      "parameters": {
        "method": "OTSU"
      }
    }
  ]
}
```

## 13. Ryzyka

### 13.1. Koszt obliczeniowy

Operacje takie jak `deskew`, `bilateral`, `kuwahara`, `perspective`, `clahe` mogą być kosztowne.

Rekomendacje:

- stosować je na cropie pola, a nie na całej stronie, jeśli to możliwe,
- dla całostronicowego preprocessingu dodać osobny etap w przyszłym profilu/runtime,
- pokazywać trace timing w przyszłości.

### 13.2. Zmiana geometrii obrazu

Operacje takie jak `deskew`, `trim`, `crop`, `perspective`, `extent` zmieniają rozmiar i układ obrazu.

W pipeline pola jest to akceptowalne, bo po cropie OCR dostaje już obraz pola. W pipeline całej strony wymagałoby to aktualizacji geometrii regionów lub transformacji współrzędnych.

Dlatego MVP powinno używać tych operacji głównie po wycięciu pola.

### 13.3. Parametry zbyt zaawansowane dla UI

Nie należy od razu eksponować wszystkich parametrów `imagemagick`.

Rekomendacja:

- MVP ma mieć mało, ale dobrze opisanych procesorów,
- profile mają być pierwszym wyborem użytkownika,
- szczegółowe operacje są dla strojenia trudnych pól.

### 13.4. Licencja i pochodzenie algorytmów

Projekt `imagemagick` deklaruje implementację własną w Javie, inspirowaną analizą mechanizmów ImageMagick.

Przed dystrybucją należy:

- zachować informacje licencyjne z `imagemagick/LICENSE`,
- sprawdzić wymagania dla zależności,
- jasno opisać, że nie jest to natywne ImageMagick ani wrapper CLI.

## 14. Kryteria akceptacji

Implementację można uznać za gotową dla MVP, jeśli:

- nowy moduł buduje się razem z głównym projektem,
- `ServiceLoaderExtensionRegistryFactory` widzi nowe procesory,
- JavaFX pokazuje procesory w pickerze `IMAGE_PROCESSOR`,
- dynamiczny formularz obsługuje ich parametry,
- `Preview Field` pokazuje obrazy po każdym kroku,
- eksport diagnostyczny ZIP zawiera obrazy wejściowe/wyjściowe pipeline,
- błędne parametry dają czytelny komunikat,
- testy jednostkowe obejmują co najmniej procesory MVP.

## 15. Rekomendowana nazwa commita

```text
Document ImageMagick-based image processor extensions design
```
