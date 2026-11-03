# Job Tracker

Aplikacja do zarządzania procesem rekrutacji i ofertami pracy.

## Tech Stack
* Java 25
* Spring Boot 4.x (Data JPA, Web, Validation)
* PostgreSQL
* Flyway (Database Migrations)
* Springdoc OpenAPI (Swagger UI)
* Docker Compose

## Uruchomienie lokalne
1. Uruchom bazę danych:
   ```

   Compose uruchamia PostgreSQL oraz lokalny MinIO. Konsola MinIO jest dostępna pod
   `http://localhost:9001`, a API S3 pod `http://localhost:9000`.

## Pliki dokumentów

Dokument można nadal zarejestrować bez pliku przez `POST /api/documents`. Upload pliku
wykonuje się przez `POST /api/documents/upload` jako `multipart/form-data`:

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@CV.pdf" \
  -F "type=CV" \
  -F "name=CV Java Backend"
```

W bazie przechowywane są wyłącznie metadane i klucz obiektu (`object_key`), a zawartość
pliku trafia do bucketa skonfigurowanego przez `S3_BUCKET`. Plik pobiera się przez
`GET /api/documents/{id}/file`.bash
   docker compose up -d