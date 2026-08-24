# Backlog rozszerzeń ImageMagick

## 1. Cel

Dokument opisuje metody biblioteki `imagemagick`, które nie mają jeszcze własnych rozszerzeń `ImageProcessor` w module `extensions-imagemagick`.

Rozszerzenia powinny być implementowane jako osobne kroki pipeline, aby można było używać ich niezależnie w preprocessingu profilu oraz w pipeline obrazu pola.

## 2. Już dostępne rozszerzenia

Aktualnie wystawione są:

| ID rozszerzenia | Pokrywana operacja |
| --- | --- |
| `im-profile` | wysokopoziomowy pipeline OCR |
| `im-grayscale` | `ImageMagickLikeOps.grayscale` |
| `im-normalize` | `ImageMagickLikeOps.normalize` |
| `im-contrast-stretch` | `ImageMagickLikeOps.contrastStretch` |
| `im-level` | `ImageMagickLikeOps.level` |
| `im-gamma` | `ImageMagickLikeOps.gamma` |
| `im-sigmoidal-contrast` | `ImageMagickLikeOps.sigmoidalContrast` |
| `im-equalize` | `ImageMagickLikeOps.equalize` |
| `im-clahe` | `ImageMagickLikeOps.clahe` |
| `im-local-contrast` | `ImageMagickLikeOps.localContrast` |
| `im-white-balance` | `ImageMagickLikeOps.grayWorldWhiteBalance` |
| `im-threshold` | `ImageMagickLikeOps.threshold` |
| `im-black-threshold` | `ImageMagickLikeOps.blackThreshold` |
| `im-white-threshold` | `ImageMagickLikeOps.whiteThreshold` |
| `im-range-threshold` | `ImageMagickLikeOps.rangeThreshold` |
| `im-hsv-threshold` | `ImageMagickLikeOps.hsvThreshold` |
| `im-auto-threshold` | `ImageMagickLikeOps.autoThreshold` |
| `im-adaptive-threshold` | `ImageMagickLikeOps.adaptiveThreshold` |
| `im-box-blur` | `ImageMagickLikeOps.boxBlur` |
| `im-gaussian-blur` | `ImageMagickLikeOps.gaussianBlur` |
| `im-sharpen` | `ImageMagickLikeOps.sharpen` |
| `im-unsharp` | `ImageMagickLikeOps.unsharp` |
| `im-bilateral` | `ImageMagickLikeOps.bilateral` |
| `im-kuwahara` | `ImageMagickLikeOps.kuwahara` |
| `im-sobel` | `ImageMagickLikeOps.sobel` |
| `im-deskew` | `ImageMagickLikeOps.deskew` |
| `im-background-correct` | `ImageMagickLikeOps.correctBackground` |
| `im-median` | `ImageMagickLikeOps.median3x3` |
| `im-morphology` | `ImageMagickLikeOps.morphology` |
| `im-remove-table-frames` | `ImageMagickLikeOps.removeTableFrames` |

## 3. Brakujące rozszerzenia

### 3.1. Tonalność i kontrast

Status: zaimplementowane.

### 3.2. Progowanie

Status: zaimplementowane.

### 3.3. Filtry

Status: zaimplementowane.

### 3.4. Geometria

| Proponowane ID | Metoda biblioteki | Priorytet |
| --- | --- | --- |
| `im-rotate` | `rotate` | P2 |
| `im-flip-vertical` | `flipVertical` | P3 |
| `im-flop-horizontal` | `flopHorizontal` | P3 |
| `im-transpose` | `transpose` | P3 |
| `im-transverse` | `transverse` | P3 |
| `im-crop` | `crop` | P2 |
| `im-shave` | `shave` | P3 |
| `im-extent` | `extent` | P3 |
| `im-perspective` | `perspective` | P2 |
| `im-trim` | `trim` | P1 |
| `im-page-contour-crop` | `cropToPageContour` | P1 |

### 3.5. Czyszczenie i kolor

| Proponowane ID | Metoda biblioteki | Priorytet |
| --- | --- | --- |
| `im-remove-small-components` | `removeSmallComponents` | P1 |
| `im-negate` | `negate` | P3 |
| `im-posterize` | `posterize` | P3 |
| `im-strip-metadata` | `stripMetadata` | P3 |

### 3.6. Diagnostyka

| Proponowane ID | Metoda biblioteki | Priorytet |
| --- | --- | --- |
| `im-identify` | `identify` | P3 |
| `im-detect-tables` | `detectTables` | P2 |

Uwaga: `identify` i `detectTables` nie są naturalnymi transformacjami obrazu, bo ich podstawowym wynikiem są dane diagnostyczne. Jeśli zostaną wystawione jako `ImageProcessor`, powinny zwracać kopię obrazu wejściowego i zapisywać wynik w trace. Alternatywnie można je wystawić jako osobne komendy diagnostyczne CLI.

## 4. Rekomendowana kolejność implementacji

Najbardziej użyteczne dla OCR są:

1. `im-threshold`
2. `im-contrast-stretch`
3. `im-level`
4. `im-unsharp`
5. `im-remove-small-components`
6. `im-trim`
7. `im-page-contour-crop`
8. `im-sobel`
9. `im-clahe`
10. `im-local-contrast`
11. `im-white-balance`

## 5. Zasady implementacji

Każde rozszerzenie powinno:

- dziedziczyć po `AbstractImageMagickProcessor`,
- mieć ID z prefiksem `im-`,
- definiować parametry przez `ImageMagickDescriptors`,
- mieć wartości domyślne zgodne z CLI projektu `imagemagick`,
- emitować podstawowy trace z `AbstractImageMagickProcessor`,
- dodawać własne atrybuty trace, jeśli metoda zwraca diagnostykę,
- mieć test rejestracji przez `ServiceLoader`,
- mieć test działania na syntetycznym `BufferedImage`.

## 6. Uwagi UX

Operacje zmieniające geometrię obrazu, takie jak `trim`, `crop`, `rotate`, `perspective` i `page-contour-crop`, powinny być używane ostrożnie na poziomie workspace preprocessing, ponieważ regiony konfiguracji są interpretowane względem obrazu po preprocessingu.

Operacje diagnostyczne, takie jak `im-sobel` i potencjalne `im-detect-tables`, powinny mieć wygodny podgląd w dialogu debugowania kroku image processor.
