## PRD – TargetSystemEndpoint analog zu SourceSystemEndpoint

### 1) Introduction / Overview
Ziel ist es, `TargetSystemEndpoint` im Backend strukturell und funktional analog zu `SourceSystemEndpoint` umzusetzen. Dabei sollen Duplikate zu Basiseigenschaften vermieden, konsistente REST-APIs bereitgestellt, DTOs/Mapper ergänzt und eine OpenAPI-gestützte Synchronisation ermöglicht werden. Zusätzlich bleiben die für Zielsysteme relevanten Felder (`jsonSchema`, `asset`) erhalten. Dieses PRD richtet sich an Junior-Entwickler und beschreibt klar, was umgesetzt werden muss.

### 2) Goals
- Entity an Basisklasse angleichen: `TargetSystemEndpoint` erbt ausschließlich von `SyncSystemEndpoint` ohne Feldduplikate.
- Parität zu Quelle: Diskriminierungswert und Finder-Methoden analog zu `SourceSystemEndpoint` herstellen.
- Vollständige CRUD-REST-API für `TargetSystemEndpoint` analog zur Source-Variante.
- DTOs/Mapper für Create/Update/Read von `TargetSystemEndpoint` bereitstellen.
- OpenAPI-gestützte Synchronisation der Target-Endpunkte (wie bei Source bereits vorhanden).
- Beibehaltung und Nutzbarkeit von `jsonSchema` und `asset` bei `TargetSystemEndpoint`.

### 3) User Stories
- Als Konfig-Admin möchte ich Target-Endpunkte anlegen/bearbeiten/löschen, damit Transformationsläufe Daten an definierte Ziel-APIs senden können.
- Als Konfig-Admin möchte ich, dass Target-Endpunkte aus einer OpenAPI-Spezifikation erstellt/aktualisiert werden, um Fehler zu vermeiden und Aufwand zu reduzieren.
- Als Entwickler möchte ich eine konsistente API-Struktur wie bei den Source-Endpunkten, um Frontend-Integration und Wartung zu vereinfachen.

### 4) Functional Requirements
1. Domain Model (Entity)
   1. `TargetSystemEndpoint` muss ausschließlich über `SyncSystemEndpoint` die Basisfelder erben; keine Duplikate von `endpointPath`, `jsonSchema`, `description`, `httpRequestType` in `TargetSystemEndpoint` deklarieren.
   2. `TargetSystemEndpoint` behält die Beziehung zu `TargetSystem` (`@ManyToOne targetSystem`) sowie zu EDC (`@OneToOne asset`) und zu spezifischen Collections (`apiQueryParams`, `apiRequestHeaders`, `targetSystemVariable`) bei.
   3. `@DiscriminatorValue("TARGET_SYSTEM")` ergänzen, analog zu `SourceSystemEndpoint`.
   4. Statische Finder-Methoden analog zur Source-Seite bereitstellen, z. B. `findByTargetSystemId(Long targetSystemId)` (und – falls sinnvoll – weitere häufig benötigte Finder wie `findByEndpointId`).

2. DTOs & Mapper
   1. `TargetSystemEndpointDTO` (Read/Update): Felder mindestens `id`, `targetSystemId`, `endpointPath`, `httpRequestType`; optional `description`, `jsonSchema` falls benötigt.
   2. `CreateTargetSystemEndpointDTO` (Create): Pflichtfelder `endpointPath`, `httpRequestType`.
   3. Mapper analog zur Source-Seite (z. B. `TargetSystemEndpointFullUpdateMapper`):
      - `mapToEntity(CreateTargetSystemEndpointDTO)`
      - `mapToDTO(TargetSystemEndpoint)`
      - `mapToDTOList(List<TargetSystemEndpoint>)`
      - Optional: `mapFullUpdate(...)` für PUT-Semantik.

3. REST API (analog SourceSystemEndpointResource)
   1. Basis-Pfad: `/api/target-systems/`
   2. Endpunkte:
      - `POST  /api/target-systems/{targetSystemId}/endpoint` – erstellt eine Liste von Endpunkten (Request: `CreateTargetSystemEndpointDTO[]`), Response: `201 Created` + Body: Liste `TargetSystemEndpointDTO`.
      - `GET   /api/target-systems/{targetSystemId}/endpoint` – listet alle Endpunkte eines Zielsystems (Response: `200 OK` + `TargetSystemEndpointDTO[]`).
      - `GET   /api/target-systems/endpoint/{id}` – holt einen Endpunkt (Response: `200 OK` + `TargetSystemEndpointDTO`, `404` falls nicht vorhanden).
      - `PUT   /api/target-systems/endpoint/{id}` – ersetzt einen Endpunkt (Request: `TargetSystemEndpointDTO`, Response: `204 No Content` oder `404`).
      - `DELETE/api/target-systems/endpoint/{id}` – löscht einen Endpunkt (Response: `204 No Content`).
   3. Fehlerbehandlung und Response-Struktur konsistent zur Source-Seite (z. B. `CoreManagementException`).

4. OpenAPI-gestützte Synchronisation
   1. Erweiterung des bestehenden Mechanismus (z. B. `OpenApiSpecificationParserService`) oder separater Service/Methodenpfad, um für ein `TargetSystem` die in `openApiSpec` hinterlegten Pfade/Methoden zu parsen und `TargetSystemEndpoint`-Einträge idempotent anzulegen/zu aktualisieren.
   2. Unterstützte HTTP-Methoden wie bei Source (GET/POST/PUT/DELETE) validieren (`httpRequestType`).
   3. Synchronisation muss wiederholbar sein (idempotent), ohne Duplikate zu erzeugen.

5. Validierung / Business Rules
   1. `endpointPath`: Pflicht, nicht leer; pro `TargetSystem` eindeutig.
   2. `httpRequestType`: Pflicht; zulässige Werte gemäß Backend-Konvention (z. B. `GET|POST|PUT|DELETE|PATCH`).
   3. `jsonSchema`: optional; falls vorhanden, muss valide JSON-Struktur repräsentieren.
   4. `asset`: optional; wenn gesetzt, muss Beziehung konsistent sein (bestehendes `EDCAsset`).

6. OpenAPI-Dokumentation
   1. Komponenten-Schemas für `TargetSystemEndpoint`, `TargetSystemEndpointDTO`, `CreateTargetSystemEndpointDTO` ergänzen/abrunden.
   2. Pfade unter `/api/target-systems/...` in der OpenAPI-Dokumentation ergänzen.
   3. Beispiel-Requests/-Responses analog zur Source-Seite bereitstellen.

### 5) Non-Goals (Out of Scope)
- Komplexe EDC-spezifische Logik (z. B. Asset-Erstellung/-Provisionierung) über die einfache Relation hinaus.
- Frontend-Implementierung (separat zu planen, nutzt die hier definierte API).
- Erweiterte Authorisierung/Authentifizierung (nur bestehende Patterns weiterverwenden).

### 6) Design Considerations (Optional)
- Persistenz: Panache (Active Record) wie bestehend; Transaktionsgrenzen analog `SourceSystemEndpointService`.
- Mapper: MapStruct wie bestehend (z. B. `componentModel = JAKARTA_CDI`).
- Exceptions: `CoreManagementException` und konsistente Messages/HTTP-Codes.
- Naming & Packages konsistent zu Source-Pendants (`...rest`, `...service`, `...mapping`, `...rest.dtos`).

### 7) Technical Considerations (Optional)
- Datenbank: Keine Migration nötig, wenn Felder nur aus `TargetSystemEndpoint` entfernt werden, die bereits in `SyncSystemEndpoint` existieren (Duplikat-Entfernung). Prüfen, ob DDL (`TABLE_PER_CLASS`) in der Zielumgebung zu Spaltenkonflikten führen könnte; ggf. Migration/Refactor sicherstellen.
- Idempotente Sync-Logik: Vergleiche über (`targetSystemId`, `endpointPath`, `httpRequestType`).
- Uniqueness: DB-Constraint je `targetSystemId` + `endpointPath` empfohlen.

### 8) Success Metrics
- CRUD-Endpunkte für Target-Endpunkte verfügbar und durch Integrationstests abgedeckt (Create/Read/Update/Delete).
- OpenAPI-Sync legt/endaktualisiert Target-Endpunkte korrekt und ohne Duplikate.
- Keine doppelten Felder in `TargetSystemEndpoint` gegenüber `SyncSystemEndpoint` mehr vorhanden.
- OpenAPI-Dokumentation enthält alle neuen Pfade/DTOs.

### 9) Open Questions
1. Sollen Finder-Methoden außer `findByTargetSystemId(Long)` weitere Varianten abdecken (Filter nach `httpRequestType` o. ä.)?
2. Sollten `description`/`jsonSchema` auch in den DTOs zwingend geführt werden oder optional bleiben?
3. Ist eine Validierung für `jsonSchema` (z. B. JSON-Schema Draft-07) gewünscht oder reicht einfache String-Prüfung?
4. Muss der OpenAPI-Sync auch EDC-relevante Metadaten (Asset-Zuordnung) ableiten oder bleibt `asset` rein manuell?


