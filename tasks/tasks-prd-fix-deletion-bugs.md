# Task List: Fix Deletion Bugs for Source Systems and Headers

## Relevant Files

- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/service/SourceSystemService.java` - Contains the backend deletion logic that's causing SQL errors.
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/SourceSystemResource.java` - REST endpoint for source system deletion.
- `stay-in-sync-core/core-management/src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/ApiHeaderResource.java` - REST endpoint for header deletion.
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/source-system-base/source-system-base.component.ts` - Frontend component handling source system deletion.
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/source-system-base/source-system-base.component.html` - UI template with delete buttons.
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/manage-api-headers/manage-api-headers.component.ts` - Frontend component handling header deletion.
- `stay-in-sync-configurator-ui/src/app/features/source-system/components/manage-api-headers/manage-api-headers.component.html` - UI template with header delete buttons.
- `stay-in-sync-configurator-ui/src/app/core/services/http-error.service.ts` - Error handling service that needs improvement.
- `stay-in-sync-configurator-ui/src/app/features/source-system/service/sourceSystemResource.service.ts` - API service for source system operations.
- `stay-in-sync-configurator-ui/src/app/features/source-system/service/apiHeaderResource.service.ts` - API service for header operations.

### Notes

- The backend SQL error is related to the `tse1_0.requestBodySchema` column reference in the deletion query.
- Current error handling only logs to console and doesn't show user-friendly messages.
- No confirmation dialogs exist for deletion operations.
- No loading states are shown during deletion operations.

## Tasks

- [x] 1.0 Fix Backend SQL Deletion Error
  - [x] 1.1 Investigate the SQL grammar exception in SourceSystemService.deleteSourceSystemById method
  - [x] 1.2 Analyze the database schema for the `tse1_0.requestBodySchema` column issue
  - [x] 1.3 Fix the deletion query to properly handle foreign key relationships
  - [x] 1.4 Update the deletion logic to use proper transaction management
  - [x] 1.5 Add proper exception handling and logging for deletion failures
  - [x] 1.6 Test the backend deletion functionality with various scenarios

- [ ] 2.0 Implement Confirmation Dialogs for Deletion
  - [x] 2.1 Create a reusable confirmation dialog component
  - [x] 2.2 Add confirmation dialog to source system deletion in source-system-base component
  - [x] 2.3 Add confirmation dialog to header deletion in manage-api-headers component
  - [x] 2.4 Add confirmation dialog to endpoint deletion in manage-endpoints component
- [x] 2.5 Implement proper dialog styling and user-friendly messaging
- [x] 2.6 Add keyboard shortcuts (Enter/Escape) for dialog interaction

- [x] 3.0 Improve Error Handling and User Feedback
  - [x] 3.1 Enhance HttpErrorService to display user-friendly error messages
  - [x] 3.2 Update error handling in source-system-base component deleteSourceSystem method
  - [x] 3.3 Update error handling in manage-api-headers component deleteHeader method
  - [x] 3.4 Add specific error messages for different types of deletion failures
  - [x] 3.5 Implement toast notifications for error feedback
  - [x] 3.6 Add error logging for debugging purposes

- [ ] 4.0 Add Loading States and Success Messages
  - [ ] 4.1 Add loading state management to source system deletion
  - [ ] 4.2 Add loading state management to header deletion
  - [ ] 4.3 Implement loading spinners during deletion operations
  - [ ] 4.4 Add success messages after successful deletions
  - [ ] 4.5 Disable delete buttons during loading to prevent double-clicks
  - [ ] 4.6 Add visual feedback for successful operations

- [ ] 5.0 Test and Validate Deletion Functionality
  - [ ] 5.1 Create unit tests for the fixed backend deletion logic
  - [ ] 5.2 Test deletion with source systems that have related entities
  - [ ] 5.3 Test deletion with source systems that have no related entities
  - [ ] 5.4 Test header deletion functionality
  - [ ] 5.5 Test confirmation dialog functionality
  - [ ] 5.6 Test error handling scenarios
  - [ ] 5.7 Test loading states and user feedback
  - [ ] 5.8 Perform integration testing of the complete deletion workflow 