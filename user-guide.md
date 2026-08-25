# Instrukcja użytkownika

| Pole | Wartość |
| ---- | ------- |
| ID dokumentu | DOC-025 |
| Tytuł | Instrukcja użytkownika |
| Wersja | 0.1 |
| Status | Draft |
| Typ | User Guide |
| Źródło prawdy | Aktualna implementacja projektu |

## 1. Cel dokumentu

Dokument opisuje praktyczne użycie dwóch aplikacji projektu OCR:

- konfiguratora JavaFX, używanego do przygotowania profilu, kategorii, pól, identyfikacji, geometrii i diagnostyki,
- aplikacji CLI, używanej do wsadowego przetwarzania dokumentów na podstawie profilu przygotowanego w konfiguratorze.

Konfigurator jest narzędziem projektowym. CLI jest narzędziem uruchomieniowym.

## 2. Konfigurator JavaFX

### 2.1. Uruchomienie

W trybie developerskim:

```bash
mvn -pl javafx javafx:run
```

Po zbudowaniu paczki uruchomieniowej konfigurator może być uruchamiany z pliku JAR modułu `javafx`.

### 2.2. Główne obszary okna

Aplikacja składa się z kilku stałych obszarów:

| Obszar | Przeznaczenie |
| ------ | ------------- |
| Menu główne | Operacje plikowe, widok, uruchamianie testów, narzędzia i pomoc |
| Pasek operacji | Najczęściej używane akcje, między innymi otwarcie profilu, zapis, podgląd pola i test kategorii |
| Lewy panel | Praca z profilem, preprocessingiem i kategoriami |
| Środkowy panel | Podgląd dokumentu wzorcowego |
| Prawy panel | Właściwości, eksplorator OCR oraz diagnostyka |
| Pasek dokumentów i stron | Wybór dokumentu wzorcowego kategorii oraz strony dokumentu |
| Pasek statusu | Informacje o bieżącej operacji i jej wyniku |

### 2.3. Menu File

Menu `File` służy do pracy z profilem i ustawieniami aplikacji.

| Operacja | Opis |
| -------- | ---- |
| `New Profile` | Tworzy nowy profil roboczy |
| `Open Profile` | Otwiera plik profilu JSON |
| `Open Recent Profile` | Otwiera jeden z ostatnio używanych profili |
| `Save Profile` | Zapisuje cały bieżący profil i powiązane definicje kategorii |
| `Save Profile As` | Zapisuje profil pod nową ścieżką |
| `Export Profile Package...` | Eksportuje profil, kategorie i opcjonalnie dokumenty wzorcowe do jednej paczki ZIP |
| `Import Profile Package...` | Importuje paczkę ZIP do wskazanego katalogu i otwiera rozpakowany profil |
| `Settings` | Otwiera ustawienia aplikacji |

Zapis można wywołać skrótem `Ctrl+S`.

Profil jest głównym plikiem roboczym. Kategorie są przypisane do profilu przez ścieżki do plików kategorii. Jeżeli kategoria korzysta z dokumentów wzorcowych, ich ścieżki są zapisane w definicji kategorii względem pliku kategorii.

Eksport paczki profilu tworzy przenośny plik ZIP. W paczce znajduje się `profile.json`, podfolder `categories/` z definicjami kategorii oraz opcjonalnie podfolder `documents/` z dokumentami wzorcowymi. Ścieżki w JSON są przepisywane do struktury paczki, niezależnie od tego, gdzie pliki znajdowały się w katalogach roboczych konfiguratora.

### 2.4. Menu View

Menu `View` steruje sposobem prezentacji podglądu dokumentu.

| Operacja | Skrót | Opis |
| -------- | ----- | ---- |
| `Zoom In` | `Ctrl++` | Powiększa dokument |
| `Zoom Out` | `Ctrl+-` | Pomniejsza dokument |
| `Fit Page` | `Ctrl+F` | Dopasowuje całą stronę do obszaru podglądu |
| `Fit Width` | `Ctrl+Shift+W` | Dopasowuje stronę do szerokości podglądu |
| `100%` | `Ctrl+0` | Ustawia rzeczywisty rozmiar obrazu |
| `Layers` | - | Włącza i wyłącza warstwy podglądu |

Warstwy pozwalają pokazywać:

- aktywną ramkę zaznaczenia,
- regiony pól,
- anchor,
- diagnostykę,
- OCR words,
- OCR lines,
- OCR paragraphs,
- OCR areas.

Te same warstwy są dostępne z pionowego toolbaru po lewej stronie podglądu dokumentu.

### 2.5. Menu Run

Menu `Run` uruchamia operacje diagnostyczne.

| Operacja | Skrót | Opis |
| -------- | ----- | ---- |
| `Preview Field` | - | Uruchamia przetwarzanie bieżącego pola dla aktualnego dokumentu wzorcowego |
| `Test Category` | `F5` | Testuje identyfikację i ekstrakcję kategorii |

`Preview Field` jest używane podczas strojenia regionu, OCR, transformerów i walidatorów konkretnego pola.

`Test Category` sprawdza kategorię na dokumencie wzorcowym. Jeżeli kategoria ma wiele dokumentów wzorcowych, aplikacja obsługuje także testowanie wszystkich dokumentów i zbiorczy eksport diagnostyczny.

### 2.6. Menu Tools

Menu `Tools` zawiera operacje pomocnicze.

| Operacja | Opis |
| -------- | ---- |
| `Extensions` | Pokazuje dostępne rozszerzenia: detectory, matchery, image processory, transformery i walidatory |

Okno rozszerzeń pozwala sprawdzić, czy biblioteka rozszerzeń została poprawnie załadowana przez `ServiceLoader`.

### 2.7. Lewy panel: Preprocessing

Zakładka `Preprocessing` definiuje pipeline przygotowania obrazu na poziomie profilu.

Pipeline preprocessingu jest wykonywany przed identyfikacją kategorii, OCR i ekstrakcją pól. Regiony konfigurowane w aplikacji odnoszą się do obrazu po preprocessingu.

Dostępne operacje:

| Ikona | Znaczenie |
| ----- | --------- |
| `plus` | Dodanie kroku preprocessingu |
| `edit` | Wybór rozszerzenia image processor |
| `copy` | Duplikowanie kroku |
| `angle-up` | Przesunięcie kroku wyżej |
| `angle-down` | Przesunięcie kroku niżej |
| `debug` | Podgląd działania wybranego kroku |
| `eraser` | Usunięcie kroku |

Debug kroku pokazuje obraz wejściowy i wynikowy obok siebie. Zoom i przesuwanie działają wspólnie dla obu obrazów, co ułatwia porównanie szczegółów.

### 2.8. Lewy panel: Categories

Zakładka `Categories` służy do pracy z kategoriami dokumentów w ramach profilu.

Nad drzewem kategorii znajduje się lista kategorii przypisanych do profilu oraz przyciski:

| Operacja | Opis |
| -------- | ---- |
| `Nowa` | Tworzy nową kategorię i pozwala wskazać jej docelowy plik |
| `Otwórz` | Dodaje istniejącą kategorię do profilu |
| `Usuń` | Usuwa kategorię z profilu po potwierdzeniu |

Drzewo kategorii pokazuje między innymi:

- właściwości kategorii,
- grupy identyfikacji,
- warunki identyfikacji,
- anchor,
- geometrię,
- pola,
- kroki pipeline pól,
- walidatory,
- transformery.

Po dodaniu elementu aplikacja ustawia fokus na nowo utworzonym elemencie i rozwija jego parent. Po usunięciu elementu fokus wraca na parent usuniętego elementu.

### 2.9. Dokumenty wzorcowe kategorii

Każda kategoria może mieć wiele dokumentów wzorcowych. Dokumenty wzorcowe służą wyłącznie do konfiguracji i testowania kategorii.

Dla dokumentu wzorcowego można określić:

- ścieżkę do pliku,
- opis, na przykład `ciemny, przechylony`,
- bieżący dokument używany do konfiguracji.

Ścieżka dokumentu wzorcowego jest zapisywana względem pliku kategorii. Dzięki temu kategorię można przenieść razem z dokumentami lub wersjonować w repozytorium.

### 2.10. Podgląd dokumentu

Środkowy panel pokazuje aktualny dokument wzorcowy i stronę.

Dostępne tryby:

| Tryb | Skrót | Opis |
| ---- | ----- | ---- |
| `Select` | `S` | Zaznaczanie i edycja istniejących ramek |
| `Pan` | `P` | Przesuwanie dokumentu |
| `Draw Region` | `R` | Rysowanie nowego regionu |

Zoom można zmieniać:

- ikonami toolbaru,
- skrótami klawiaturowymi,
- `Ctrl+scroll`,
- gestem touchpada.

Przy `Ctrl+scroll` punktem odniesienia jest pozycja kursora nad obrazem.

W trybie `Select` ramkę można:

- przesuwać po najechaniu kursorem do wnętrza ramki,
- zmieniać rozmiar przez przeciąganie krawędzi,
- zmieniać rozmiar przez przeciąganie narożników.

### 2.11. Panel Properties

Zakładka `Properties` w prawym panelu pokazuje formularz właściwości aktualnie wybranego elementu drzewa.

Przykładowe sekcje:

| Sekcja | Opis |
| ------ | ---- |
| `Category` | ID, nazwa, opis, wersja |
| `Page Policy` | Zakres stron, do których pasuje kategoria |
| `OCR` | Język i lokalizacja danych Tesseract |
| `Identification` | Grupy i warunki identyfikacji |
| `Search Region` | Region szukania tekstu albo anchor |
| `Reference Feature` | Referencyjna ramka anchor |
| `Geometry` | Parametry geometrii kategorii |
| `Field` | Definicja pola |
| `Pipeline` | Image processory, OCR, transformery i walidatory pola |

Pola koordynatów `X`, `Y`, `W`, `H` są numeryczne. Puste wartości regionu oznaczają brak ograniczenia regionem, jeżeli dany mechanizm to obsługuje, na przykład szukanie na całej stronie.

Przy regionach dostępne są ikony:

| Ikona | Opis |
| ----- | ---- |
| `mode-draw-region` | Rysowanie regionu na podglądzie |
| `eraser` | Czyszczenie regionu |
| `lock-open` / `lock` | Włączenie lub wyłączenie symetrycznej zmiany rozmiaru |

Symetryczna zmiana rozmiaru powoduje, że zmiana jednej krawędzi lub wymiaru zachowuje środek ramki albo dodaje równy margines po obu stronach.

### 2.12. Panel OCR

Zakładka `OCR` pozwala przeglądać wynik OCR aktualnego dokumentu.

Można wybrać poziom:

- area,
- paragraph,
- line,
- word.

Tabela pozwala filtrować i kopiować tekst. Wybrany element OCR może zostać użyty jako podstawa warunku identyfikacji albo anchor.

### 2.13. Panel Validation/Trace

Zakładka `Validation/Trace` pokazuje wynik ostatniej operacji diagnostycznej.

Panel zawiera:

- walidację konfiguracji,
- wynik przetwarzania pola,
- wynik testu kategorii,
- trace etapów przetwarzania,
- listę obrazów diagnostycznych,
- wynik OCR i HOCR, jeżeli zostały zapisane w trace.

Dwuklik na obrazie diagnostycznym otwiera podgląd pary obrazów wejście-wyjście dla danego etapu, jeżeli taka para jest dostępna.

Dostępne przyciski eksportu:

| Operacja | Opis |
| -------- | ---- |
| `Export` | Zapisuje ZIP z diagnostyką bieżącego wyniku |
| `Export All` | Zapisuje ZIP z diagnostyką wszystkich dokumentów wzorcowych |
| menu `ellipsis` | Dodatkowe eksporty: wybrany obraz, wszystkie obrazy, metadane |

### 2.14. Ustawienia aplikacji

Okno `Settings` przechowuje preferencje użytkownika.

Typowe ustawienia:

- domyślne DPI renderowania PDF,
- limit cache,
- ostatnio używane katalogi,
- ustawienia używane przy otwieraniu i zapisie plików.

DPI jest wybierane z listy standardowych wartości:

- `72`,
- `100`,
- `150`,
- `200`,
- `300`,
- `Custom`.

Pole `Custom DPI` jest widoczne tylko po wybraniu wartości `Custom`.

## 3. Podstawowe przypadki użycia konfiguratora

### 3.1. Utworzenie nowego profilu

1. Wybierz `File -> New Profile`.
2. Uzupełnij podstawowe dane profilu.
3. Dodaj albo utwórz kategorię w zakładce `Categories`.
4. Zapisz profil skrótem `Ctrl+S`.

### 3.2. Dodanie istniejącej kategorii do profilu

1. Otwórz profil przez `Open Profile`.
2. Przejdź do zakładki `Categories`.
3. Kliknij `Otwórz`.
4. Wskaż plik kategorii JSON.
5. Zapisz profil.

Profil zapisze względną ścieżkę do pliku kategorii, jeżeli jest to możliwe.

### 3.3. Dodanie dokumentu wzorcowego do kategorii

1. Wybierz kategorię.
2. Użyj przycisku dodania dokumentu wzorcowego na pasku dokumentów.
3. Wskaż plik PDF lub obraz.
4. Dodaj opis dokumentu, na przykład `niski kontrast`.
5. Wybierz dokument z listy i konfiguruj kategorię względem niego.

### 3.4. Konfiguracja preprocessingu profilu

1. Przejdź do zakładki `Preprocessing`.
2. Dodaj krok ikoną `plus`.
3. Wybierz image processor ikoną `edit`.
4. Uzupełnij parametry rozszerzenia.
5. Użyj `debug`, aby porównać obraz przed i po transformacji.
6. Zapisz profil.

Do usuwania widocznych ramek tabel można dodać image processor `im-remove-table-frames`. Processor wykrywa tabele liniowe i zamalowuje linie wierszy oraz kolumn kolorem tła z sąsiedztwa. Najczęściej dostrajane parametry to `frameThickness`, `sampleRadius`, `lineGapTolerance`, `lineMergeTolerance`, `minLineCoverage` i `minLineLengthRatio`.

### 3.5. Konfiguracja identyfikacji kategorii

1. W drzewie kategorii wybierz sekcję identyfikacji.
2. Dodaj grupę identyfikacji.
3. Dodaj warunek.
4. Ustaw `Page`, `Detector`, `Matcher` i `Expected Text`.
5. W sekcji `Search Region` narysuj region na podglądzie.
6. Uruchom `Test Category`.

Dla większych regionów tekstowych zwykle wygodniejszy jest matcher `contains` niż dokładne dopasowanie tekstu.

Dla warunków tekstowych detector OCR jest źródłem treści. Dla warunków `QR` i `BARCODE` należy wybrać odpowiednio detector `qr` albo `barcode`. `Expected Text` może być puste, jeżeli samo wykrycie kodu wystarcza do identyfikacji kategorii. Jeżeli wartość jest podana, porównywana jest z payloadem odczytanym z kodu, a `Search Region` ogranicza obszar dekodowania.

Parametry detectora i matchera są prezentowane bezpośrednio pod kontrolką, której dotyczą. Dzięki temu widać, czy dana opcja konfiguruje odczyt treści, czy sposób porównania. Gdy dla danego typu treści dostępny jest tylko jeden detector, pole wyboru detectora może być ukryte, ale jego wartość nadal wynika z konfiguracji warunku.

Warunek identyfikacji działa według wspólnego modelu `Detector -> Matcher -> Expected Text`:

1. `Detector` wybiera źródło treści, na przykład OCR tekstowy, QR albo barcode.
2. `Search Region` ogranicza obszar działania detectora. Pusty region oznacza wyszukiwanie na całej stronie, jeżeli detector to obsługuje.
3. Detector zwraca tekst oraz, jeśli jest dostępna, geometrię znalezionego elementu.
4. `Matcher` porównuje tekst zwrócony przez detector z `Expected Text`.
5. Jeżeli `Expected Text` jest puste, samo skuteczne wykrycie treści może wystarczyć do spełnienia warunku.

Dla tekstu OCR dopasowanie anchorów i warunków korzysta z wyrazów OCR. Jeśli oczekiwany tekst ma więcej niż jeden wyraz, system szuka najkrótszej sekwencji kolejnych wyrazów spełniającej matcher i jako geometrię przyjmuje najmniejszy prostokąt obejmujący te wyrazy.

### 3.6. Konfiguracja anchor i geometrii

1. Dodaj anchor w sekcji `Anchors`.
2. Ustaw `Detector`, `Matcher` i `Expected Text`.
3. Narysuj `Search Region`, czyli obszar, w którym anchor ma być szukany.
4. Narysuj `Reference Feature`, czyli wzorcowe położenie cechy anchor.
5. Przejdź do `Geometry` i wybierz strategię geometrii.
6. Uruchom `Test Category` i sprawdź warstwy anchor oraz diagnostykę.

Anchor może korzystać z detectora `text`, `qr` albo `barcode`. Dla `qr` i `barcode` pozycja `Reference Feature` jest ustalana na podstawie geometrii kodu zwróconej przez ZXing, a puste współrzędne `Search Region` oznaczają szukanie na całej stronie.

Anchor jest bardzo podobny do warunku identyfikacji, ale oprócz dopasowania treści dostarcza też geometrię do normalizacji dokumentu.

Znaczenie pól anchor:

| Pole | Opis |
| ---- | ---- |
| `Detector` | Mechanizm wyszukujący treść i geometrię, na przykład `text`, `qr`, `barcode` |
| `Expected Text` | Oczekiwana treść anchoru; może być pusta, jeśli wystarczy samo wykrycie elementu |
| `Matcher` | Sposób porównania treści, na przykład `contains` albo dokładne dopasowanie |
| `Search Region` | Obszar, w którym anchor ma być szukany w dokumencie testowym lub produkcyjnym |
| `Reference Feature` | Położenie tego samego obiektu w dokumencie wzorcowym |
| `Required` | Określa, czy brak anchoru powinien blokować wyliczenie geometrii |

Ważne rozróżnienie:

- `Search Region` to miejsce szukania anchoru.
- `Reference Feature` to wzorcowy prostokąt obiektu w dokumencie referencyjnym.
- `Detected Bounds` to faktycznie znaleziony prostokąt obiektu w testowanym dokumencie.

W `Validation/Trace` tabela `Geometry trace` pokazuje wszystkie anchory używane przez geometrię. Kolumna `Used` oznacza, które anchory albo punkty kontrolne weszły do finalnego obliczenia transformacji. Po zaznaczeniu wiersza na podglądzie dokumentu pokazywane są dwie ramki: obszar dopasowania oraz obszar szukania. Przycisk lupki w kolumnie `OCR` pokazuje tekst OCR z obszaru szukania, co pomaga diagnozować brak dopasowania.

#### 3.6.1. Punkty kontrolne geometrii

Geometria nie używa bezpośrednio całego prostokąta anchoru tekstowego do skalowania. Zamiast tego tworzy punkty kontrolne:

| Typ anchoru | Punkty kontrolne |
| ----------- | ---------------- |
| `text` | `TOP_LEFT`, czyli lewy górny punkt dopasowanego tekstu |
| `qr` | `TOP_LEFT` i `BOTTOM_RIGHT`, jeśli kod ma wystarczająco stabilny rozmiar |
| `barcode` | `TOP_LEFT` i `BOTTOM_RIGHT`, jeśli kod ma wystarczająco stabilny rozmiar |

Dla tekstu używany jest tylko lewy górny punkt, ponieważ szerokość i wysokość tekstu OCR mogą się zmieniać przez czcionkę, jakość skanu i granice słów. Dla QR i barcode prostokąt kodu jest zwykle stabilniejszy, więc pojedynczy kod może dostarczyć dwa punkty i pozwolić na skalowanie.

#### 3.6.2. Strategie geometrii

Strategia geometrii określa, jak z punktów kontrolnych wyliczana jest transformacja regionów pól.

| Strategia | Minimalna liczba punktów | Co wylicza |
| --------- | ------------------------ | ---------- |
| `NONE` | 0 | Brak transformacji |
| `ANCHOR_TRANSLATION` | 1 | Tylko przesunięcie `dx/dy` |
| `TWO_POINT_SCALE_TRANSLATE` | 2 | Skalę `scaleX/scaleY` i przesunięcie `dx/dy` |
| `AFFINE` | 3 | Transformację afiniczną `a,b,c,d,tx,ty` |
| `ROBUST_AFFINE` | 4 lub więcej zalecane | Transformację afiniczną z odrzucaniem punktów odstających |

Jeżeli strategia nie ma wystarczającej liczby punktów, system używa najlepszego dostępnego uproszczenia albo zgłasza problem, jeśli brakuje wymaganej kotwicy.

#### 3.6.3. `NONE`

Strategia `NONE` wyłącza normalizację geometrii. Regiony pól są używane dokładnie tak, jak zostały skonfigurowane względem obrazu po preprocessingu.

Ta strategia jest właściwa, gdy:

- dokumenty są zawsze wyrównane tak samo,
- pola nie przesuwają się między skanami,
- nie chcesz używać anchorów do przeliczania regionów.

#### 3.6.4. `ANCHOR_TRANSLATION`

Strategia `ANCHOR_TRANSLATION` wylicza tylko przesunięcie dokumentu.

Działanie:

1. System wykrywa anchory wskazane w konfiguracji geometrii.
2. Z każdego wykrytego anchoru tworzy punkty kontrolne.
3. Dla każdego punktu liczy różnicę pomiędzy pozycją wykrytą i referencyjną.
4. Finalne `dx` i `dy` są średnią tych różnic.
5. `scaleX` i `scaleY` pozostają równe `1.0`.

W praktyce oznacza to, że wszystkie regiony pól są przesuwane o ten sam wektor. Nie zmienia się ich rozmiar.

Ta strategia jest dobrym wyborem, gdy:

- skany są przesunięte, ale nie są istotnie skalowane,
- format dokumentu jest stały,
- masz jedną albo kilka stabilnych kotwic tekstowych.

#### 3.6.5. `TWO_POINT_SCALE_TRANSLATE`

Strategia `TWO_POINT_SCALE_TRANSLATE` wylicza skalowanie i przesunięcie bez rotacji.

Działanie:

1. System buduje punkty kontrolne ze wszystkich wykrytych anchorów.
2. Jeżeli dostępny jest jeden punkt, strategia zachowuje się jak translacja.
3. Jeżeli dostępne są co najmniej dwa punkty, wybierana jest para o największym dystansie euklidesowym w dokumencie referencyjnym.
4. Z tej pary liczona jest skala `scaleX`, `scaleY` oraz przesunięcie `dx`, `dy`.
5. Regiony pól są skalowane i przesuwane.

Para o największym dystansie jest wybierana dlatego, że błąd kilku pikseli ma wtedy mniejszy wpływ na wynikową skalę.

Ta strategia jest dobrym wyborem, gdy:

- dokument może być lekko większy albo mniejszy,
- nie występuje istotny obrót,
- masz dwie odległe kotwice tekstowe albo jeden stabilny QR/barcode.

Historyczna wartość strategii `ANCHORS` jest traktowana jak alias `TWO_POINT_SCALE_TRANSLATE`.

#### 3.6.6. `AFFINE`

Strategia `AFFINE` wylicza transformację afiniczną:

```text
x' = a*x + b*y + tx
y' = c*x + d*y + ty
```

Pozwala to modelować:

- przesunięcie,
- skalowanie,
- obrót,
- pochylenie / shear.

Działanie:

1. System wymaga co najmniej trzech punktów kontrolnych.
2. Transformacja jest dopasowywana metodą najmniejszych kwadratów do wszystkich dostępnych punktów.
3. Region pola jest przeliczany przez transformację narożników.
4. Wynikiem dla pola nadal jest prostokąt osiowy obejmujący przetransformowane narożniki.

Ta strategia jest dobrym wyborem, gdy:

- skany mogą być lekko obrócone albo pochylone,
- masz co najmniej trzy stabilne punkty kontrolne,
- chcesz dokładniejszego dopasowania niż samo skalowanie i przesunięcie.

#### 3.6.7. `ROBUST_AFFINE`

Strategia `ROBUST_AFFINE` jest wariantem `AFFINE` odpornym na pojedyncze błędnie wykryte kotwice.

Działanie:

1. System buduje modele affine z różnych trójek punktów kontrolnych.
2. Dla każdego modelu sprawdza, które punkty są zgodne z modelem.
3. Punkty odstające są odrzucane.
4. Finalna transformacja jest ponownie dopasowywana na punktach zaakceptowanych.
5. Przy remisie preferowana jest transformacja mniej zniekształcona względem dokumentu referencyjnego.

Ta strategia jest dobrym wyborem, gdy:

- masz więcej niż trzy anchory,
- część anchorów może czasem zostać błędnie rozpoznana,
- zależy Ci na większej odporności diagnostycznej.

`ROBUST_AFFINE` nie zastępuje poprawnej konfiguracji anchorów. Jeżeli większość anchorów jest błędna albo bardzo blisko siebie, wynik nadal może być niestabilny.

#### 3.6.8. Diagnostyka transformacji

Po `Test Category` zakładka `Validation/Trace` pokazuje pod tabelą `Geometry trace` podsumowanie transformacji:

- `dx`, `dy` - przesunięcie,
- `scaleX`, `scaleY` - skala wynikowa,
- `affine[a,b,c,d]` - współczynniki macierzy affine.

W trace dostępne są też:

- `usedControlPoints`,
- liczba punktów kontrolnych,
- wybrana para punktów dla `TWO_POINT_SCALE_TRANSLATE`,
- wszystkie wykryte i brakujące anchory.

### 3.7. Konfiguracja pola

1. Dodaj pole w sekcji `Fields`.
2. Ustaw ID, nazwę, typ i region pola.
3. Wybierz OCR, image processory, transformery i walidatory.
4. Użyj `Preview Field`, aby sprawdzić wynik tylko dla tego pola.
5. Sprawdź zakładkę `Validation/Trace`.
6. Zapisz kategorię i profil.

### 3.8. Diagnostyka problemu z OCR

1. Uruchom `Preview Field` albo `Test Category`.
2. Przejdź do `Validation/Trace`.
3. Sprawdź wynik walidacji i trace.
4. Otwórz obrazy diagnostyczne dwuklikiem.
5. Sprawdź HOCR i poziomy OCR w zakładce `OCR`.
6. Wyeksportuj paczkę `Export`, jeżeli problem wymaga analizy poza aplikacją.

### 3.9. Test wszystkich dokumentów wzorcowych

1. Wybierz kategorię z wieloma dokumentami wzorcowymi.
2. Uruchom test kategorii dla wszystkich dokumentów.
3. Sprawdź tabelę wyników.
4. Użyj `Export All`, aby zapisać zbiorczą diagnostykę ZIP.

### 3.10. Eksport paczki profilu

1. Otwórz albo przygotuj profil w konfiguratorze.
2. Upewnij się, że lista kategorii zawiera wszystkie kategorie, które mają trafić do paczki.
3. Wybierz `File -> Export Profile Package...`.
4. Wskaż docelowy plik ZIP.
5. Wybierz, czy paczka ma zawierać dokumenty wzorcowe.
6. Po eksporcie przekaż ZIP do innej instancji konfiguratora albo użyj go jako wartości `--profile` w CLI.

Struktura ZIP:

```text
profile.json
categories/
  <category-id>.json
documents/
  <category-id>/
    <reference-document-file>
```

Folder `documents/` jest obecny tylko przy eksporcie z dokumentami wzorcowymi. Jeżeli dokument lokalny nie jest dostępny, zostanie pominięty, a konfigurator pokaże informację o liczbie brakujących dokumentów.

### 3.11. Import paczki profilu

1. Wybierz `File -> Import Profile Package...`.
2. Wskaż plik ZIP.
3. Wskaż katalog docelowy, do którego paczka ma zostać rozpakowana.
4. Konfigurator rozpakowuje paczkę i otwiera `profile.json`.
5. Po imporcie możesz pracować z profilem tak samo jak z profilem utworzonym lokalnie.

Import nie modyfikuje oryginalnej paczki ZIP. Rozpakowany projekt jest normalną strukturą plików z profilem, kategoriami i opcjonalnymi dokumentami wzorcowymi.

## 4. CLI

### 4.1. Przeznaczenie

CLI przetwarza dokumenty wsadowo na podstawie profilu JSON albo paczki ZIP wyeksportowanej z konfiguratora.

CLI nie służy do edycji konfiguracji. Konfigurację profilu i kategorii należy przygotować w konfiguratorze albo ręcznie w plikach JSON zgodnych z dokumentacją projektu.

### 4.2. Uruchomienie

Po zbudowaniu paczki:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar --profile config/profiles/default.json
```

Albo z paczką profilu:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar --profile packages/default-profile.zip
```

W zależności od sposobu pakowania nazwa pliku JAR może się różnić.

Pomoc:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar --help
```

Wersja:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar --version
```

### 4.3. Składnia

```text
sk-ocr --profile <plik-profilu> [opcje]
```

`--profile` jest wymagany przy normalnym uruchomieniu przetwarzania.

### 4.4. Parametry CLI

| Parametr | Typ | Wymagany | Opis |
| -------- | --- | -------- | ---- |
| `--profile` | path | Tak | Ścieżka do pliku profilu JSON albo paczki profilu ZIP |
| `--input` | path | Nie | Nadpisuje katalog wejściowy z profilu |
| `--success` | path | Nie | Nadpisuje katalog dokumentów przetworzonych poprawnie |
| `--error` | path | Nie | Nadpisuje katalog dokumentów z błędem |
| `--workers` | integer | Nie | Nadpisuje liczbę równoległych workerów; wartość musi być większa lub równa `1` |
| `--mode` | enum | Nie | Nadpisuje tryb przetwarzania; dozwolone wartości: `FULL`, `CLASSIFY_ONLY` |
| `--output` | path | Nie | Nadpisuje ścieżkę pliku CSV z wynikiem |
| `--summary-json` | path | Nie | Zapisuje techniczne podsumowanie batcha w JSON |
| `--trace` | enum | Nie | Nadpisuje tryb trace; dozwolone wartości: `OFF`, `BASIC`, `FULL` |
| `--ocr-datapath` | path | Nie | Nadpisuje lokalizację danych Tesseract, czyli katalog `tessdata` |
| `--ocr-language` | string | Nie | Nadpisuje język OCR, na przykład `pol` albo `eng` |
| `--log-level` | enum | Nie | Nadpisuje poziom logowania; dozwolone wartości: `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE` |
| `--help` | flag | Nie | Pokazuje pomoc CLI |
| `--version` | flag | Nie | Pokazuje wersję aplikacji |

### 4.5. Override profilu

Parametry CLI mają pierwszeństwo przed wartościami zapisanymi w profilu.

Przykład: jeżeli profil ma katalog wejściowy `./input`, ale wywołanie zawiera:

```bash
--input C:\ocr\inbox
```

to podczas tego uruchomienia użyty zostanie katalog `C:\ocr\inbox`. Plik profilu nie jest przez to modyfikowany.

Jeżeli `--profile` wskazuje plik ZIP, CLI najpierw rozpakowuje paczkę do katalogu tymczasowego, a następnie uruchamia standardowe ładowanie profilu z rozpakowanego `profile.json`. Override działa tak samo dla profilu JSON i paczki ZIP.

Tryb `--mode CLASSIFY_ONLY` kończy przetwarzanie po identyfikacji kategorii. CLI nadal zapisuje CSV, ale zawiera on tylko kolumny techniczne, między innymi nazwę pliku, `categoryId`, status i kody błędów. Geometria, anchor i ekstrakcja pól nie są wtedy uruchamiane.

### 4.6. Trace

Parametr `--trace` steruje ilością danych diagnostycznych zbieranych podczas przetwarzania.

| Wartość | Znaczenie |
| ------- | --------- |
| `OFF` | Bez diagnostyki trace |
| `BASIC` | Podstawowe informacje diagnostyczne |
| `FULL` | Pełna diagnostyka, w tym szczegółowe artefakty przetwarzania, jeśli profil i implementacja je zapisują |

Tryb `FULL` może zwiększyć zużycie dysku i czas przetwarzania.

### 4.7. Tesseract

Dla OCR wymagane są dane językowe Tesseract.

Na Windows typowa lokalizacja to:

```text
C:\Program Files\Tesseract-OCR\tessdata
```

Można ją wskazać w profilu albo przez CLI:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar ^
  --profile config\profiles\default.json ^
  --ocr-datapath "C:\Program Files\Tesseract-OCR\tessdata" ^
  --ocr-language pol
```

Jeżeli Tesseract nie znajdzie pliku językowego, pojawi się błąd podobny do:

```text
Error opening data file ./pol.traineddata
Failed loading language 'pol'
Tesseract couldn't load any languages
```

W takim przypadku należy sprawdzić `--ocr-datapath`, `--ocr-language` oraz obecność pliku, na przykład `pol.traineddata`.

### 4.8. Kody wyjścia

CLI zwraca kod wyjścia zależny od wyniku uruchomienia.

| Sytuacja | Znaczenie |
| -------- | --------- |
| Sukces | Batch zakończył się poprawnie |
| Błąd argumentów | Brakuje wymaganego argumentu albo argument jest niepoprawny |
| Błąd konfiguracji | Profil lub kategorie są niepoprawne |
| Błąd środowiska | Brakuje katalogów, uprawnień albo zależności środowiskowych |
| Błąd wykonania | Wystąpił błąd podczas przetwarzania dokumentów |

Dokładne wartości liczbowe kodów są definiowane przez implementację `ExitCodeResolver`.

### 4.9. Przykłady wywołań

Minimalne uruchomienie:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar \
  --profile config/profiles/default.json
```

Uruchomienie z paczki ZIP:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar \
  --profile packages/default-profile.zip
```

Uruchomienie z katalogami produkcyjnymi:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar \
  --profile config/profiles/production.json \
  --input /data/ocr/input \
  --success /data/ocr/success \
  --error /data/ocr/error \
  --output /data/ocr/result.csv
```

Uruchomienie z większą równoległością:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar \
  --profile config/profiles/production.json \
  --workers 8
```

Uruchomienie tylko w trybie kategoryzacji:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar \
  --profile config/profiles/production.json \
  --mode CLASSIFY_ONLY \
  --output /data/ocr/categories.csv
```

Uruchomienie z diagnostyką:

```bash
java -jar cli/target/cli-0.1.0-SNAPSHOT.jar \
  --profile config/profiles/test.json \
  --trace FULL \
  --summary-json target/batch-summary.json \
  --log-level DEBUG
```

Uruchomienie na Windows:

```bat
java -jar cli\target\cli-0.1.0-SNAPSHOT.jar ^
  --profile config\profiles\default.json ^
  --input C:\ocr\input ^
  --success C:\ocr\success ^
  --error C:\ocr\error ^
  --output C:\ocr\result.csv ^
  --ocr-datapath "C:\Program Files\Tesseract-OCR\tessdata" ^
  --ocr-language pol
```

### 4.10. Podstawowe przypadki użycia CLI

#### 4.10.1. Przetworzenie dokumentów z katalogu profilu

1. Przygotuj profil w konfiguratorze.
2. Upewnij się, że profil ma ustawione katalogi `input`, `success`, `error` i plik `output.csv`.
3. Umieść dokumenty w katalogu wejściowym.
4. Uruchom CLI z `--profile`.
5. Sprawdź plik CSV oraz katalogi `success` i `error`.

#### 4.10.2. Jednorazowe przetworzenie innego katalogu

1. Użyj istniejącego profilu.
2. Podaj `--input`, `--success`, `--error` i `--output`.
3. Uruchom batch.
4. Profil pozostanie bez zmian, a override zadziała tylko dla tego uruchomienia.

#### 4.10.3. Uruchomienie diagnostyczne

1. Ustaw `--trace FULL`.
2. Ustaw `--summary-json`.
3. Ustaw `--log-level DEBUG`.
4. Uruchom CLI na małej próbce dokumentów.
5. Przeanalizuj summary JSON, logi i wyniki.

#### 4.10.4. Uruchomienie produkcyjne

1. Użyj sprawdzonego profilu.
2. Ustaw stabilne katalogi wejścia i wyjścia.
3. Ustaw `--workers` odpowiednio do CPU, pamięci i kosztu OCR.
4. Ustaw `--trace OFF` albo `BASIC`, jeżeli pełna diagnostyka nie jest potrzebna.
5. Monitoruj kod wyjścia procesu i plik wynikowy CSV.

#### 4.10.5. Automatyzacja w skrypcie

Przykład PowerShell:

```powershell
$jar = "cli\target\cli-0.1.0-SNAPSHOT.jar"
$profile = "config\profiles\production.json"

java -jar $jar --profile $profile --summary-json "logs\last-batch.json"

if ($LASTEXITCODE -ne 0) {
    Write-Error "OCR batch failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}
```

#### 4.10.6. Uruchomienie z paczki profilu

1. W konfiguratorze wybierz `File -> Export Profile Package...`.
2. Zdecyduj, czy paczka ma zawierać dokumenty wzorcowe. Dla CLI dokumenty wzorcowe nie są wymagane do przetwarzania produkcyjnego, ale mogą być przydatne przy przenoszeniu kompletnego projektu.
3. Przekaż ZIP na maszynę uruchomieniową.
4. Uruchom CLI z `--profile <plik.zip>`.
5. Opcjonalnie użyj `--input`, `--success`, `--error` i `--output`, aby wskazać katalogi właściwe dla środowiska uruchomieniowego.

#### 4.10.7. Sama kategoryzacja dokumentów

1. Przygotuj profil i kategorie z poprawną sekcją identyfikacji.
2. Uruchom CLI z `--mode CLASSIFY_ONLY`.
3. Sprawdź CSV wynikowy, w którym `categoryId` wskazuje rozpoznaną kategorię.
4. Dokumenty z nierozpoznaną albo niejednoznaczną kategorią otrzymają odpowiedni status i kod błędu.

## 5. Typowy przepływ end-to-end

1. Utwórz profil w konfiguratorze.
2. Dodaj kategorie do profilu.
3. Dla każdej kategorii dodaj dokumenty wzorcowe.
4. Skonfiguruj preprocessing profilu.
5. Skonfiguruj identyfikację kategorii.
6. Skonfiguruj anchor i geometrię, jeżeli dokumenty wymagają normalizacji położenia.
7. Skonfiguruj pola i pipeline pól.
8. Uruchom `Preview Field` dla pól problematycznych.
9. Uruchom `Test Category` dla dokumentów wzorcowych.
10. Wyeksportuj diagnostykę, jeżeli wynik wymaga analizy.
11. Zapisz profil i kategorie.
12. Opcjonalnie wyeksportuj paczkę profilu ZIP.
13. Uruchom CLI na katalogu wejściowym, wskazując profil JSON albo paczkę ZIP.

## 6. Najczęstsze problemy

| Problem | Możliwa przyczyna | Rozwiązanie |
| ------- | ----------------- | ----------- |
| Brak rozszerzeń w UI | Rozszerzenia nie są na classpath albo `ServiceLoader` ich nie widzi | Sprawdź zależności Maven i okno `Tools -> Extensions` |
| OCR nie rozpoznaje języka | Brak pliku `*.traineddata` | Ustaw `ocr.datapath` albo `--ocr-datapath` |
| Kategoria nie jest identyfikowana | Zbyt ścisły matcher, zły region, błędny OCR | Sprawdź zakładkę `OCR`, użyj matcher `contains`, uruchom `Test Category` |
| Ramki nie pokrywają się z tekstem | Region był ustawiany względem innego obrazu preprocessingu | Sprawdź pipeline preprocessingu i ponownie ustaw regiony |
| CLI nie startuje | Brak `--profile` | Podaj ścieżkę do profilu |
| CLI nie ładuje paczki ZIP | Paczka nie zawiera `profile.json` albo jest uszkodzona | Wyeksportuj paczkę ponownie z konfiguratora |
| Po imporcie brakuje dokumentów wzorcowych | Paczka była eksportowana bez dokumentów albo pliki źródłowe nie były dostępne | Wyeksportuj paczkę z opcją dołączenia dokumentów |
| `--workers` powoduje błąd | Wartość mniejsza niż `1` | Ustaw `--workers 1` lub więcej |
| Wynik CSV jest w złej lokalizacji | Profil wskazuje inną ścieżkę | Użyj `--output` albo popraw profil |
