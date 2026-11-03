# Job Tracker

REST API do zarządzania procesem rekrutacji: firmami, aplikacjami na stanowiska,
kontaktami, historią statusów oraz dokumentami kandydata.

## Funkcje

- rejestrowanie firm i aplikacji na stanowiska,
- filtrowanie i stronicowanie aplikacji,
- śledzenie zmian statusu rekrutacji oraz powiązanych kontaktów,
- generowanie podsumowań ofert za pomocą modelu zgodnego z OpenAI API,
- rejestrowanie metadanych dokumentów i upload plików,
- przechowywanie plików w obiektowym storage S3-compatible (Silo),
- dokumentacja API dostępna przez Swagger UI.

## Stack technologiczny

- Java 25
- Spring Boot 4.1
- Spring Web MVC, Spring Data JPA i Bean Validation
- PostgreSQL 17
- Flyway
- Silo jako S3-compatible object storage
- Springdoc OpenAPI
- Maven
- Docker Compose

## Wymagania

Do uruchomienia pełnego środowiska potrzebujesz:

- Docker Desktop z Docker Compose,
- opcjonalnie JDK 25 i dostępnego Dockera do uruchamiania testów Testcontainers.

## Szybki start przez Docker Compose

1. Ustaw klucz do dostawcy AI, jeśli chcesz korzystać z generowania podsumowań:

   ```powershell
   $env:GEMINI_API_KEY = "your-api-key"
   ```

   Możesz też zdefiniować zmienne w pliku `.env` w katalogu projektu.

2. Zbuduj obraz aplikacji i uruchom wszystkie usługi:

   ```bash
   docker compose up --build
   ```

   Compose uruchamia aplikację, PostgreSQL oraz Silo. Migracje Flyway są wykonywane
   automatycznie przy starcie aplikacji.

3. Po uruchomieniu:

   | Usługa | Adres |
   | --- | --- |
   | API | http://localhost:8080 |
   | Swagger UI | http://localhost:8080/swagger-ui/index.html |
   | OpenAPI JSON | http://localhost:8080/v3/api-docs |
   | Silo API (S3) | http://localhost:9000 |
   | Silo Console | http://localhost:9001 |
   | PostgreSQL | localhost:5432 |

   Domyślne dane dostępowe do Silo to `minioadmin` / `minioadmin`.

Zatrzymanie usług:

```bash
docker compose down
```

Usunięcie kontenerów wraz z danymi PostgreSQL i Silo:

```bash
docker compose down -v
```

## Konfiguracja

Wartości domyślne działają z dostarczonym `docker-compose.yml`. Można je nadpisać
przez zmienne środowiskowe:

| Zmienna | Domyślna wartość | Opis |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobtracker` | JDBC URL bazy danych |
| `DB_USER` | `jobtracker` | Użytkownik PostgreSQL |
| `DB_PASSWORD` | `jobtracker` | Hasło PostgreSQL |
| `S3_ENDPOINT` | `http://localhost:9000` | Endpoint storage S3 |
| `S3_ACCESS_KEY` | `minioadmin` | Klucz dostępowy storage |
| `S3_SECRET_KEY` | `minioadmin` | Sekret storage |
| `S3_BUCKET` | `job-tracker` | Nazwa bucketa na dokumenty |
| `S3_REGION` | `us-east-1` | Region storage |
| `GEMINI_API_KEY` | brak | Klucz dostawcy AI |
| `GEMINI_MODEL` | `gemini-3.8-flash` | Główny model AI |
| `GEMINI_FALLBACK_MODEL` | `gemini-3.5-flash-lite` | Model używany po błędzie przejściowym (429/5xx) |

W Compose endpoint bazy i storage wskazuje na nazwy usług (`db` i `minio`), dlatego
do uruchamiania aplikacji poza Dockerem należy użyć adresów `localhost`.

## Uruchomienie lokalne bez kontenera aplikacji

Uruchom zależności infrastrukturalne:

```bash
docker compose up -d db minio
```

Następnie uruchom aplikację:

```bash
./mvnw spring-boot:run
```

Na Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Przy uruchomieniu lokalnym aplikacja korzysta z domyślnych wartości z
`application.yml` (`localhost`, `jobtracker`, `minioadmin`).

## API dokumentów

Dokument można utworzyć bez pliku, zapisując wyłącznie jego metadane:

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"name":"CV Java Backend","type":"CV"}'
```

Upload pliku odbywa się przez `multipart/form-data`:

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@CV.pdf" \
  -F "type=CV" \
  -F "name=CV Java Backend"
```

Obsługiwane typy dokumentów to `CV`, `COVER_LETTER` i `OTHER`.

W PostgreSQL przechowywane są metadane oraz klucz obiektu (`object_key`), natomiast
zawartość pliku trafia do bucketa Silo. Plik można pobrać przez:

```text
GET /api/documents/{id}/file
```

Dokumenty można również przypinać do aplikacji:

```text
PUT    /api/applications/{applicationId}/documents/{documentId}
DELETE /api/applications/{applicationId}/documents/{documentId}
GET    /api/applications/{applicationId}/documents
```

## Najważniejsze endpointy

| Zasób | Endpoint bazowy | Dostępne operacje |
| --- | --- | --- |
| Firmy | `/api/companies` | tworzenie, lista, odczyt |
| Aplikacje | `/api/applications` | tworzenie, lista, odczyt, zmiana statusu |
| Historia statusów | `/api/applications/{id}/history` | odczyt historii |
| Kontakty | `/api/contacts` | CRUD |
| Dokumenty | `/api/documents` | CRUD, upload i download plików |
| Dokumenty aplikacji | `/api/applications/{id}/documents` | przypinanie i odpinanie |
| Podsumowania AI | `/api/applications/{id}/summaries` | generowanie, lista, najnowsze |

Pełne requesty, response'y, parametry i kody błędów są dostępne w Swagger UI.

## Testy i budowanie

Uruchomienie testów:

```bash
./mvnw test
```

Na Windows:

```powershell
.\mvnw.cmd test
```

Zbudowanie artefaktu bez uruchamiania testów:

```bash
./mvnw package -DskipTests
```

Dockerfile korzysta z wieloetapowego builda: projekt jest kompilowany w obrazie Maven
z JDK 25, a aplikacja działa w mniejszym obrazie runtime Eclipse Temurin JRE 25.
