# Paczka profilu ZIP

| Pole | Wartość |
| ---- | ------- |
| ID dokumentu | DOC-026 |
| Tytuł | Paczka profilu ZIP |
| Wersja | 0.1 |
| Status | Draft |
| Typ | Implementation Design |
| Źródło prawdy | Implementacja `ProjectPackageService` |

## 1. Cel

Celem funkcji jest przenoszenie kompletnego profilu OCR pomiędzy instancjami konfiguratora oraz uruchamianie CLI bez konieczności zachowania źródłowej struktury katalogów.

Paczka ZIP zawiera:

- profil,
- wszystkie kategorie wskazane przez profil,
- opcjonalnie dokumenty wzorcowe kategorii.

## 2. Struktura paczki

```text
profile.json
categories/
  <category-id>.json
documents/
  <category-id>/
    <reference-document-file>
```

`documents/` jest tworzone tylko wtedy, gdy użytkownik wybierze eksport z dokumentami wzorcowymi i pliki dokumentów są lokalnie dostępne.

## 3. Przepisywanie ścieżek

Eksport nie zachowuje źródłowej struktury katalogów konfiguratora.

Podczas eksportu:

- `profile.categories.directory` jest ustawiane na `categories`,
- `profile.categories.mode` jest ustawiane na `EXPLICIT`,
- `profile.categories.files` zawiera ścieżki `categories/<category-id>.json`,
- `profile.categories.active` zawiera ID eksportowanych kategorii,
- pliki kategorii są zapisywane w `categories/`,
- jeżeli eksport obejmuje dokumenty, ścieżki dokumentów wzorcowych są przepisywane na `../documents/<category-id>/<file>`,
- jeżeli eksport nie obejmuje dokumentów, ścieżki dokumentów wzorcowych pozostają bez zmian.

Ścieżki dokumentów są relatywne względem pliku kategorii, dlatego z katalogu `categories/` wskazują do `../documents/...`.

## 4. Eksport w konfiguratorze

Konfigurator dodaje operacje:

- `File -> Export Profile Package...`,
- `File -> Import Profile Package...`.

Przy eksporcie użytkownik wybiera, czy dołączyć dokumenty wzorcowe.

Przed eksportem konfigurator zapisuje aktualne zmiany formularzy do workspace. Eksport nie wymaga zapisu źródłowego profilu na dysku, ale kategorie bez ścieżki są eksportowane na podstawie aktualnego draftu.

## 5. Import w konfiguratorze

Import wymaga:

- wskazania pliku ZIP,
- wskazania katalogu docelowego.

ZIP jest rozpakowywany do katalogu docelowego. Po imporcie konfigurator otwiera `profile.json` z rozpakowanego katalogu.

Rozpakowywanie zabezpiecza przed zip-slip: wpis ZIP nie może wyjść poza katalog docelowy.

## 6. CLI

Parametr `--profile` obsługuje dwa formaty:

- plik JSON profilu,
- paczkę ZIP profilu.

Jeżeli `--profile` wskazuje ZIP, CLI rozpakowuje go do katalogu tymczasowego i uruchamia standardowy loader profilu na rozpakowanym `profile.json`.

Dotychczasowy format JSON pozostaje obsługiwany bez zmian.

## 7. Ograniczenia

- Eksport dokumentów pomija pliki, których nie można odnaleźć lokalnie.
- Przy kolizji nazw dokumentów w ramach kategorii eksport nadaje kolejnym plikom sufiksy.
- ZIP nie zawiera wyników diagnostycznych ani trace. Do tego służą oddzielne eksporty diagnostyczne.

