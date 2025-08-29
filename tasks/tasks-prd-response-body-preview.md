# Task List: Response Body Preview Feature

## Relevant Files

### Backend Files:
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/domain/entities/sync/SyncSystemEndpoint.java` - Hinzufügen des responseBodySchema Attributs (ERSTELLT)
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/dtos/SourceSystemEndpointDTO.java` - Erweiterung um responseBodySchema Feld (ERSTELLT)
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/dtos/CreateSourceSystemEndpointDTO.java` - Erweiterung um responseBodySchema Feld (ERSTELLT)
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/service/OpenApiSpecificationParserService.java` - Erweiterung um Response Body Extraction (ERSTELLT)
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/service/SourceSystemEndpointService.java` - Erweiterung um responseBodySchema Validierung und Persistierung (ERSTELLT)
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/mapping/SourceSystemEndpointFullUpdateMapper.java` - Erweiterung um responseBodySchema Mapping (ERSTELLT)
- `stay-in-sync-core/core-management/src/test/java/de/unistuttgart/stayinsync/service/OpenApiSpecificationParserServiceTest.java` - Unit Tests für Response Body Extraction (ERSTELLT)

### Frontend Files:
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/manage-endpoints/manage-endpoints.component.ts` - Erweiterung um Response Preview Button und Logik (ERSTELLT)
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/manage-endpoints/manage-endpoints.component.html` - UI für Response Preview Button und Modal (ERSTELLT)
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/response-preview-modal/response-preview-modal.component.ts` - Neue Modal Component für Response Preview (ERSTELLT)
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/response-preview-modal/response-preview-modal.component.html` - Modal Template (ERSTELLT)
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/response-preview-modal/response-preview-modal.component.css` - Modal Styling (ERSTELLT)
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/response-preview-modal/response-preview-modal.component.spec.ts` - Unit Tests für Modal Component (ERSTELLT)
- `stay-in-sync-configurator-ui/src/app/features/source-system/models/sourceSystemEndpoint.ts` - Erweiterung um responseBodySchema Feld (ERSTELLT)
- `stay-in-sync-configurator-ui/src/app/features/source-system/models/sourceSystemEndpointDTO.ts` - Erweiterung um responseBodySchema Feld (ERSTELLT)
- `stay-in-sync-configurator-ui/src/app/features/source-system/models/createSourceSystemEndpointDTO.ts` - Erweiterung um responseBodySchema Feld (ERSTELLT)

### Test Files:
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/manage-endpoints/manage-endpoints.component.spec.ts` - Unit Tests für erweiterte Component (ERSTELLT)

### Notes

- Unit tests should typically be placed alongside the code files they are testing (e.g., `MyComponent.tsx` and `MyComponent.test.tsx` in the same directory).
- Use `npx jest [optional/path/to/test/file]` to run tests. Running without a path executes all tests found by the Jest configuration.
- Database migration not required: Project uses Hibernate ORM with `drop-and-create` which automatically creates new fields.
- Response body data is available directly in the frontend like request body data, no separate API endpoint needed.

## Tasks

- [x] 1.0 Backend Response Body Schema Integration
  - [x] 1.1 Add responseBodySchema field to SourceSystemEndpoint entity
  - [x] 1.2 Update SourceSystemEndpointDTO to include responseBodySchema
  - [x] 1.3 Extend OpenApiSpecificationParserService to extract and store response bodies
  - [x] 1.4 Add database migration for new field
  - [x] 1.5 Add unit tests for response body extraction
- [x] 2.0 Frontend Response Preview Modal Component
  - [x] 2.1 Create ResponsePreviewModalComponent class
  - [x] 2.2 Create modal template with Monaco Editor
  - [x] 2.3 Add modal styling and responsive design
  - [x] 2.4 Add unit tests for ResponsePreviewModalComponent
- [x] 3.0 Frontend Integration in Manage Endpoints Component
  - [x] 3.1 Update sourceSystemEndpoint model to include responseBodySchema
  - [x] 3.2 Add Response Preview button to endpoint table (neben Request Body Button)
  - [x] 3.3 Integrate ResponsePreviewModalComponent
  - [x] 3.4 Add loading states and error handling
  - [x] 3.5 Update unit tests for ManageEndpointsComponent 