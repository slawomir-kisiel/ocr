# Dokumenty wzorcowe kategorii

## 1. Cel

Celem zmiany jest powiązanie dokumentów wzorcowych z definicją kategorii, a nie z bieżącą, niezależną sesją JavaFX Configurator.

Dokumenty wzorcowe są używane wyłącznie na etapie konfiguracji i testowania kategorii w UI. Nie są częścią runtime przetwarzania CLI poza tym, że konfiguracja kategorii powstała na ich podstawie.

Zmiana ma umożliwić:

- wskazanie listy dokumentów wzorcowych w ramach jednej kategorii,
- zapis lokalizacji dokumentów wzorcowych w definicji kategorii,
- przechowywanie ścieżek względnych względem pliku kategorii,
- wybór bieżącego dokumentu, dla którego prowadzona jest konfiguracja,
- testowanie tej samej kategorii na wielu wariantach dokumentu, np. ciemnym, przechylonym, o innym kontraście albo jakości skanu,
- opisanie, po co dany dokument został dodany.

## 2. Zakres

W zakresie:

- rozszerzenie modelu `CategoryDto`,
- zapis i odczyt listy dokumentów wzorcowych,
- JavaFX UI do zarządzania dokumentami kategorii,
- automatyczne otwieranie bieżącego dokumentu po wyborze kategorii,
- izolacja cache renderowania, OCR, trace i overlay per dokument,
- uruchamianie `Test Category` dla jednego albo wielu dokumentów wzorcowych.

Poza zakresem:

- wykorzystywanie dokumentów wzorcowych przez CLI w normalnym batch processing,
- kopiowanie plików dokumentów do repozytorium projektu,
- synchronizacja dokumentów z zewnętrznym storage,
- wersjonowanie binarnych dokumentów w Git.

## 3. Decyzja

Dokumenty wzorcowe zapisujemy w definicji kategorii.

Ścieżki zapisujemy względem pliku kategorii, ponieważ dokumenty są semantycznie związane z kategorią, a nie z profilem. Dzięki temu ta sama kategoria może być używana w wielu profilach bez utraty informacji o dokumentach używanych do jej konfiguracji.

Przykładowa struktura repozytorium:

```text
profiles/
  default.json
  customer-a.json
categories/
  invoice.json
  voucher.json
samples/
  invoice/
    clean.pdf
    dark-skewed.pdf
    low-contrast.pdf
```

Przykładowa ścieżka w `categories/invoice.json`:

```json
"path": "../samples/invoice/dark-skewed.pdf"
```

## 4. Model konfiguracji kategorii

Do `CategoryDto` dodajemy opcjonalną sekcję `referenceDocuments`.

Proponowany JSON:

```json
{
  "schemaVersion": "1.0",
  "id": "invoice",
  "version": "1.0",
  "displayName": "Invoice",
  "referenceDocuments": {
    "active": "dark-skewed",
    "documents": [
      {
        "id": "clean",
        "path": "../samples/invoice/clean.pdf",
        "displayName": "Clean scan",
        "description": "Poprawny dokument bazowy, dobra jakość skanu."
      },
      {
        "id": "dark-skewed",
        "path": "../samples/invoice/dark-skewed.pdf",
        "displayName": "Dark skewed scan",
        "description": "Ciemny i lekko przechylony dokument do sprawdzenia preprocessingu i anchorów."
      }
    ]
  }
}
```

### 4.1. Pola

| Pole | Wymagane | Znaczenie |
| ---- | -------- | --------- |
| `referenceDocuments.active` | Nie | `id` ostatnio aktywnego dokumentu wzorcowego dla tej kategorii. |
| `referenceDocuments.documents` | Nie | Lista dokumentów wzorcowych. |
| `documents[].id` | Tak | Stabilny identyfikator dokumentu w ramach kategorii. |
| `documents[].path` | Tak | Ścieżka do pliku dokumentu, względna względem pliku kategorii albo bezwzględna. |
| `documents[].displayName` | Nie | Krótka nazwa prezentowana w UI, np. w zakładce dokumentu. |
| `documents[].description` | Nie | Opis celu dokumentu testowego. |

## 5. Reguły walidacji

Walidator kategorii powinien sprawdzać:

- `documents[].id` nie jest puste,
- `documents[].id` jest unikalne w ramach kategorii,
- `documents[].path` nie jest puste,
- `referenceDocuments.active`, jeśli podane, wskazuje istniejący dokument,
- ścieżka może wskazywać nieistniejący lokalnie plik, ale wtedy UI pokazuje ostrzeżenie zamiast blokować edycję.

Brak lokalnego pliku nie powinien powodować błędu walidacji kategorii, ponieważ definicja może być współdzielona między maszynami, a dokumenty wzorcowe mogą nie być obecne lokalnie.

## 6. JavaFX UX

### 6.1. Lista dokumentów

W obszarze podglądu dokumentu dodajemy pasek dokumentów wzorcowych, działający podobnie do zakładek.

Pasek powinien:

- prezentować dokumenty z aktualnie wybranej kategorii,
- pozwalać wybrać aktywny dokument,
- pozwalać dodać dokument,
- pozwalać usunąć dokument po potwierdzeniu,
- pozwalać zmienić opis/nazwę dokumentu,
- przewijać się poziomo, gdy dokumentów jest więcej niż mieści się w oknie.

Paginacja stron dokumentu powinna być powiązana z aktywnym dokumentem.

### 6.2. Dodawanie dokumentu

Operacja `Add reference document`:

1. Otwiera file chooser.
2. Domyślnie startuje z ostatnio użytego folderu dokumentów wzorcowych.
3. Tworzy wpis w `referenceDocuments.documents`.
4. Zapisuje ścieżkę względną względem pliku kategorii, jeśli kategoria ma znaną ścieżkę.
5. Ustawia nowy dokument jako aktywny.
6. Otwiera dokument i czyści cache zależny od poprzedniego dokumentu.

Jeśli kategoria nie ma jeszcze ścieżki pliku, UI powinien zapisać ścieżkę tymczasowo jako bezwzględną albo wymusić zapis kategorii przed dodaniem dokumentu. Rekomendowane jest wymuszenie zapisu kategorii, bo dzięki temu od razu można zapisać poprawną ścieżkę względną.

### 6.3. Wybór dokumentu

Po wybraniu dokumentu:

- dokument jest otwierany w viewerze,
- przywracana jest ostatnia aktywna strona tego dokumentu, jeśli UI ją pamięta w sesji,
- cache renderowania, preprocessing, OCR i trace przełączają się na kontekst tego dokumentu,
- overlay regionów pozostaje ten sam, ponieważ regiony należą do kategorii, ale jest rysowany na obrazie aktywnego dokumentu.

### 6.4. Brak pliku

Jeśli plik dokumentu nie istnieje lokalnie:

- zakładka dokumentu pozostaje widoczna,
- viewer pokazuje pusty stan albo czytelny komunikat,
- użytkownik może wskazać nową lokalizację pliku,
- po wskazaniu lokalizacji zapisujemy nową ścieżkę względną względem pliku kategorii.

## 7. Testowanie kategorii

`Test Category` powinien otrzymać dwa tryby:

- `Test current reference document`,
- `Test all reference documents`.

Wynik testu powinien zawierać identyfikator dokumentu wzorcowego:

```json
{
  "referenceDocumentId": "dark-skewed",
  "referenceDocumentPath": "../samples/invoice/dark-skewed.pdf",
  "status": "FAILED",
  "issues": []
}
```

Widok wyników powinien umożliwiać filtrowanie albo grupowanie po dokumencie wzorcowym.

## 8. Cache i trace

Cache musi być separowany co najmniej po:

- `categoryId`,
- `referenceDocumentId`,
- `pageNumber`,
- konfiguracji workspace preprocessing,
- konfiguracji OCR.

Nie wolno mieszać:

- obrazów stron między dokumentami,
- HOCR między dokumentami,
- ramek diagnostycznych wynikających z trace,
- wyników `Preview Field`,
- wyników `Test Category`.

## 9. Relacja z workspace preprocessing

Dokument wzorcowy jest najpierw renderowany, a następnie przechodzi przez workspace preprocessing profilu.

Regiony kategorii nadal są definiowane względem obrazu po workspace preprocessing, zgodnie z decyzją z `21-workspace-preprocessing.md`.

Konsekwencja: jeśli dokumenty wzorcowe różnią się kontrastem albo przechyłem, powinny być dobrym materiałem do dopracowania preprocessingu profilu i odporności anchorów, ale nie powinny tworzyć oddzielnych układów współrzędnych dla tej samej kategorii.

## 10. Migracja

Kategorie bez `referenceDocuments` pozostają poprawne.

Po otwarciu starej kategorii:

- lista dokumentów wzorcowych jest pusta,
- UI może nadal pozwalać ręcznie otworzyć dokument bez zapisu w kategorii,
- po dodaniu dokumentu i zapisie kategorii sekcja `referenceDocuments` zostaje dopisana.

## 11. Zadania implementacyjne

### CRD-001 DTO i JSON

- Dodać `CategoryReferenceDocumentsDto`.
- Dodać `CategoryReferenceDocumentDto`.
- Rozszerzyć `CategoryDto` o `referenceDocuments`.
- Zachować kompatybilność z kategoriami bez tej sekcji.

### CRD-002 Walidacja

- Dodać walidację unikalności `documents[].id`.
- Dodać walidację pustych pól `id` i `path`.
- Dodać walidację `active`.
- Nie walidować istnienia pliku jako błędu krytycznego.

### CRD-003 Ścieżki względne

- Dodać helper zapisu ścieżki dokumentu względem pliku kategorii.
- Dodać helper odczytu ścieżki dokumentu względem pliku kategorii.
- Obsłużyć ścieżki bezwzględne.

### CRD-004 UI listy dokumentów

- Dodać pasek zakładek dokumentów nad viewerem.
- Dodać operacje dodaj, usuń, zmień opis, relink brakującego pliku.
- Zapamiętywać `referenceDocuments.active` po zmianie dokumentu.

### CRD-005 Integracja z viewerem

- Po wyborze kategorii otworzyć aktywny dokument wzorcowy, jeśli istnieje.
- Po wyborze dokumentu przełączyć viewer i cache.
- Obsłużyć brak pliku bez blokowania edycji kategorii.

### CRD-006 Test Category

- Dodać test bieżącego dokumentu.
- Dodać test wszystkich dokumentów wzorcowych.
- Rozszerzyć wynik o `referenceDocumentId` i `referenceDocumentPath`.

### CRD-007 Trace i diagnostyka

- Oznaczać trace identyfikatorem dokumentu wzorcowego.
- Eksport diagnostyczny powinien rozdzielać artefakty per dokument.

## 12. Dodatkowe rekomendacje

Dodałbym jeszcze:

- `displayName`, bo samo `id` albo nazwa pliku zwykle nie wystarczą w UI,
- `description`, żeby zapisać cel dokumentu testowego,
- `active`, żeby po ponownym otwarciu kategorii wrócić do właściwego dokumentu,
- status brakującego pliku jako ostrzeżenie UI, nie błąd konfiguracji,
- test wszystkich dokumentów wzorcowych, bo pojedynczy dokument szybko przestanie wystarczać do oceny odporności kategorii.

Nie dodawałbym na tym etapie oddzielnej konfiguracji preprocessingu per dokument. To mogłoby ukrywać problemy kategorii. Preprocessing powinien pozostać na poziomie workspace/profilu, a dokumenty wzorcowe powinny sprawdzać, czy ta konfiguracja działa na różnych wariantach wejścia.
