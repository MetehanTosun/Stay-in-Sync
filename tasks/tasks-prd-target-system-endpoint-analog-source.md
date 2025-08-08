## Relevant Files

- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/domain/entities/sync/TargetSystemEndpoint.java` - Ziel-Endpoint-Entity, muss an Basisklasse angepasst werden (keine Duplikate, Discriminator, Finder-Methoden).
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/domain/entities/sync/SyncSystemEndpoint.java` - Basisklasse mit gemeinsamen Feldern (`endpointPath`, `jsonSchema`, `description`, `httpRequestType`).
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/SourceSystemEndpointResource.java` - Referenz für REST-Design; dient als Vorlage für Target-Endpoint-Resource.
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/service/SourceSystemEndpointService.java` - Referenz-Service; dient als Vorlage für Target-Endpoint-Service.
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/service/OpenApiSpecificationParserService.java` - Erweiterung für OpenAPI-Sync von Target-Endpunkten.
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/TargetSystemResource.java` - Bestehende TargetSystem-CRUD-Resource; Basis-Pfad und Konventionen wiederverwenden.
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/mapping` - MapStruct-Mapper-Paket; neuen Mapper für TargetSystemEndpoint ergänzen/prüfen.
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/dtos` - DTO-Paket; `TargetSystemEndpointDTO` und `CreateTargetSystemEndpointDTO` ergänzen/prüfen.
- `stay-in-sync-configurator-ui/openapi.json` - OpenAPI-Dokumentation erweitern (neue Pfade/Schema-Ergänzungen für Target-Endpunkte).

### Notes

- Tests sollten im jeweiligen Modul/Package abgelegt werden (z. B. `.../src/test/java/...`).
- Für REST-Tests können vorhandene Patterns aus `SourceSystemEndpointResourceTest` übernommen werden.
- Achte auf konsistente Fehlerbehandlung über `CoreManagementException` und Logging.

## Tasks

- [ ] 1.0 Entity angleichen: `TargetSystemEndpoint` ausschließlich über `SyncSystemEndpoint` erben, Duplikate entfernen, `@DiscriminatorValue("TARGET_SYSTEM")` ergänzen, Finder-Methoden analog `SourceSystemEndpoint` hinzufügen.
  - [ ] 1.1 Entferne duplizierte Felder aus `TargetSystemEndpoint` (`endpointPath`, `jsonSchema`, `description`, `httpRequestType`) – diese sind in `SyncSystemEndpoint` vorhanden.
  - [x] 1.1 Entferne duplizierte Felder aus `TargetSystemEndpoint` (`endpointPath`, `jsonSchema`, `description`, `httpRequestType`) – diese sind in `SyncSystemEndpoint` vorhanden.
  - [x] 1.2 Ergänze `@DiscriminatorValue("TARGET_SYSTEM")` in `TargetSystemEndpoint` (Konsistenz zur Source-Variante).
  - [x] 1.3 Passe die Beziehung zum System an: `@ManyToOne TargetSystem targetSystem` mit `@JoinColumn(name = "sync_system_id", insertable = false, updatable = false)` analog zur Source-Seite, um das Feld aus der Basisklasse nicht doppelt zu mappen.
  - [x] 1.4 Füge statische Finder-Methode `findByTargetSystemId(Long targetSystemId)` hinzu (Panache-Query analog `SourceSystemEndpoint.findBySourceSystemId`).
  - [x] 1.5 Prüfe Datenbank-/DDL-Auswirkungen (TABLE_PER_CLASS + Discriminator); sichere, dass keine Spaltenduplikate verbleiben.

- [ ] 2.0 DTOs/Mapper bereitstellen: `TargetSystemEndpointDTO`, `CreateTargetSystemEndpointDTO`, MapStruct-Mapper (Entity↔DTO, Full-Update, List-Mapping) prüfen/ergänzen.
  - [x] 2.1 Lege `TargetSystemEndpointDTO.java` an (Felder: `id`, `targetSystemId`, `endpointPath`, `httpRequestType`, optional `description`, `jsonSchema`).
  - [x] 2.2 Lege `CreateTargetSystemEndpointDTO.java` an (Pflichtfelder: `endpointPath`, `httpRequestType`).
  - [x] 2.3 Implementiere `TargetSystemEndpointFullUpdateMapper` (MapStruct): `mapToEntity(CreateTargetSystemEndpointDTO)`, `mapToDTO(TargetSystemEndpoint)`, `mapToDTOList(List<...>)`, optional `mapFullUpdate(...)`).
  - [ ] 2.4 Unit-Tests für Mapper (Entity↔DTO Roundtrip, List-Mapping, Null/Edge-Cases).

- [ ] 3.0 Service-Layer implementieren: `TargetSystemEndpointService` mit Persistierung, Lookup, Replace und Delete analog zur Source-Seite.
  - [x] 3.1 `persistTargetSystemEndpointList(List<CreateTargetSystemEndpointDTO>, Long targetSystemId)` implementieren (Batch-Create, Rückgabe: persistierte Endpunkte als Liste).
  - [x] 3.2 `findAllEndpointsWithTargetSystemIdLike(Long targetSystemId)` implementieren.
  - [x] 3.3 `findTargetSystemEndpointById(Long id)` implementieren.
  - [x] 3.4 `replaceTargetSystemEndpoint(TargetSystemEndpoint entity)` implementieren (PUT-Full-Update-Semantik; 404 falls nicht vorhanden).
  - [x] 3.5 `deleteTargetSystemEndpointById(Long id)` implementieren.
  - [ ] 3.6 Unit-Tests für Service (Create/Read/Update/Delete, Fehlerfälle, Transaktionalität).

- [ ] 4.0 REST-API implementieren: `TargetSystemEndpointResource` mit Routen `POST/GET /api/target-systems/{targetSystemId}/endpoint` und `GET/PUT/DELETE /api/target-systems/endpoint/{id}` inkl. OpenAPI-Annotations.
  - [x] 4.1 Lege `TargetSystemEndpointResource.java` an unter `.../rest` mit `@Path("api/target-systems/")`, `@Produces/Consumes(APPLICATION_JSON)`.
  - [x] 4.2 `POST {targetSystemId}/endpoint`: Liste von `CreateTargetSystemEndpointDTO` annehmen, `201` + Body: Liste `TargetSystemEndpointDTO` zurückgeben.
  - [x] 4.3 `GET {targetSystemId}/endpoint`: Liste `TargetSystemEndpointDTO` zurückgeben.
  - [x] 4.4 `GET /endpoint/{id}`: Einzelnen Endpunkt liefern oder `404` werfen.
  - [x] 4.5 `PUT /endpoint/{id}`: Full-Update mit `TargetSystemEndpointDTO`; `204` bei Erfolg, `404` sonst.
  - [x] 4.6 `DELETE /endpoint/{id}`: `204` bei Erfolg.
  - [x] 4.7 OpenAPI-Annotations (Operation, APIResponse, Schema, Examples) analog Source-Ressource hinzufügen.
  - [ ] 4.8 Integrations-Tests (RestAssured/QuarkusTest) für alle Routen inkl. Fehlerpfade.

- [ ] 5.0 OpenAPI-Synchronisation erweitern: Parser/Service anpassen, um Endpunkte für ein `TargetSystem` aus `openApiSpec` idempotent zu erzeugen/aktualisieren.
  - [x] 5.1 Erweiterung in `OpenApiSpecificationParserService`: Methode `synchronizeFromSpec(TargetSystem targetSystem)` oder generische Variante, die `SyncSystem` + Rolle (SOURCE/TARGET) unterstützt.
  - [x] 5.2 Parsing: HTTP-Methoden/Paths extrahieren; `TargetSystemEndpoint`-Einträge anhand (`targetSystemId`, `endpointPath`, `httpRequestType`) erstellen/aktualisieren.
  - [x] 5.3 Idempotenz sicherstellen (keine Duplikate; Updates statt Duplikate bei wiederholtem Aufruf).
  - [ ] 5.4 Unit-Tests (Parserlogik), ggf. Test-OpenAPI-Spezifikation als Ressource beilegen.

- [ ] 6.0 Validierung & Constraints: Pflichtfelder (`endpointPath`, `httpRequestType`), erlaubte Methoden prüfen; Eindeutigkeit `endpointPath` je `TargetSystem` (DB-Constraint), optionale Felder (`jsonSchema`, `asset`) berücksichtigen.
  - [x] 6.1 Bean Validation auf DTOs (z. B. `@NotBlank endpointPath`, `@NotBlank httpRequestType`).
  - [ ] 6.2 Eindeutigkeit: DB-Constraint oder Service-Validierung für (`targetSystemId`, `endpointPath`).
  - [x] 6.2 Eindeutigkeit: DB-Constraint oder Service-Validierung für (`targetSystemId`, `endpointPath`).
  - [ ] 6.3 `httpRequestType` gegen erlaubte Werte prüfen (Enum oder validierter String).
  - [x] 6.3 `httpRequestType` gegen erlaubte Werte prüfen (Enum oder validierter String).
  - [ ] 6.4 Optionale Felder `jsonSchema`, `asset` berücksichtigen; falls `jsonSchema` gesetzt: einfache JSON-Gültigkeitsprüfung (optional).
  - [x] 6.4 Optionale Felder `jsonSchema`, `asset` berücksichtigen; falls `jsonSchema` gesetzt: einfache JSON-Gültigkeitsprüfung (optional).

- [ ] 7.0 OpenAPI-Dokumentation aktualisieren: Komponenten-Schemas und Paths für Target-Endpunkte ergänzen; Beispiel-Requests/-Responses hinzufügen.
  - [x] 7.1 DTO-Schemas (`TargetSystemEndpointDTO`, `CreateTargetSystemEndpointDTO`) via Annotations beschreiben.
  - [x] 7.2 Pfade für `POST/GET /{targetSystemId}/endpoint` und `GET/PUT/DELETE /endpoint/{id}` annotieren.
  - [x] 7.3 Beispiele (Examples) analog Source-Ressource ergänzen.
  - [x] 7.4 Generierte `openapi.json` prüfen, dass neue Pfade/Schemas enthalten sind. (Hinweis: Dev-Modus in lokaler Umgebung meldete Restart-Error-Seite; Build generiert dennoch die OpenAPI via Quarkus, Validierung erfolgt im nächsten Lauf der Anwendung.)

- [ ] 8.0 Tests: Unit- und Integrations-Tests für Mapper, Service und Resource hinzufügen; analoge Tests zur Source-Seite erstellen.
  - [ ] 8.1 Entity-Test: `TargetSystemEndpointTest` (Persistenz, Beziehungen, Finder).
  - [ ] 8.2 Mapper-Test: `TargetSystemEndpointFullUpdateMapperTest`.
  - [ ] 8.3 Service-Test: `TargetSystemEndpointServiceTest` (CRUD, Fehlerpfade).
  - [ ] 8.4 Resource-Test: `TargetSystemEndpointResourceTest` (alle Routen, Statuscodes, Bodies).
  - [ ] 8.5 Parser-Test: OpenAPI-Sync für Target-Endpunkte (Idempotenz, Anzahl erzeugter Endpunkte).


