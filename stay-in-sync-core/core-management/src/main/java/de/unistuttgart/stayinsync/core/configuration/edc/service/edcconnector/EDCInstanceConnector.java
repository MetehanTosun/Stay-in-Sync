package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.ConnectionToEdcFailedException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.RequestBuildingException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.RequestExecutionException;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.ResponseSubscriptionException;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import jakarta.enterprise.context.ApplicationScoped;



@ApplicationScoped
public class EDCInstanceConnector {

    EDCRequestBuilder requestBuilder;

    EDCRestClient restClient;


    /**
     * This method is used to test the connection to the referenced Edc using the details of the edcInstance.
     *
     * @param edcInstance contains details to connect to referenced edc
     * @throws ConnectionToEdcFailedException if the connection fails due to a specific reason
     */
    public void tryConnectingInstanceToExistingEdc(final EDCInstance edcInstance) throws ConnectionToEdcFailedException {
        final String managementUrl = edcInstance.getControlPlaneManagementUrl();

        Log.debugf("Trying to connect to Control Plane Management URL");

        try {

            final HttpRequest<Buffer> assetRequest = requestBuilder.buildRequest(edcInstance, "GET", edcInstance.getEdcAssetEndpoint());
            final HttpRequest<Buffer> policyRequest =requestBuilder.buildRequest(edcInstance, "GET", edcInstance.getEdcPolicyEndpoint());
            final HttpRequest<Buffer> contractDefinitionRequest =requestBuilder.buildRequest(edcInstance, "GET", edcInstance.getEdcContractDefinitionEndpoint());

            validateEDCEndpoint("Assets", assetRequest, createEmptyQueryBody());
            validateEDCEndpoint("Policies", policyRequest, createEmptyQueryBody());
            validateEDCEndpoint("ContractDefinitions", contractDefinitionRequest, createEmptyQueryBody());

            Log.debugf("Successfully connected to EDC with the Management URL");

        } catch(RequestBuildingException e) {
            final String exceptionMessage = "No valid HttpRequest could be built with the provided Details.";
            Log.debugf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        } catch(RequestExecutionException | ResponseSubscriptionException e) {
            final String exceptionMessage = "Failed to execute validation requests against EDC Management API.";
            Log.debugf(exceptionMessage, e);
            throw new ConnectionToEdcFailedException(exceptionMessage, e);
        }
    }

    /**
     * Validiert einen EDC-Endpoint durch Ausführung einer Test-Query
     *
     * @param endpointName Name des Endpoints für Logging
     * @param request Der zu testende Request
     * @param queryBody Query-Body für den Test
     * @throws RequestExecutionException Bei Ausführungsfehlern
     * @throws ResponseSubscriptionException Bei Response-Fehlern
     * @throws ConnectionToEdcFailedException Bei EDC-spezifischen Validierungsfehlern
     */
    private void validateEDCEndpoint(String endpointName, HttpRequest<Buffer> request, String queryBody) throws RequestExecutionException, ResponseSubscriptionException, ConnectionToEdcFailedException {

        try {
            Log.debugf("Validating EDC %s endpoint...", endpointName);

            JsonObject response = restClient.executeEdcCall(request, queryBody);

            if (!isValidEDCResponse(response, endpointName)) {
                throw new ConnectionToEdcFailedException(String.format("Invalid EDC response format for %s endpoint", endpointName));
            }
            Log.debugf("Successfully validated EDC %s endpoint", endpointName);

        } catch (RequestExecutionException | ResponseSubscriptionException e) {
            // Spezifische HTTP-Fehlerbehandlung
            if (isAuthenticationError(e)) {
                throw new ConnectionToEdcFailedException(String.format("Authentication failed for %s endpoint: %s", endpointName, e.getMessage()), e);
            } else if (isNotFoundError(e)) {
                throw new ConnectionToEdcFailedException(String.format("EDC %s endpoint not found - possibly not a valid EDC instance", endpointName), e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Checks if Response has format typical for an Edc. This format even applies, if the edc has no entities.
     */
    private boolean isValidEDCResponse(JsonObject response, String endpointName) {
        if (response == null) {
            return false;
        }

        return switch (endpointName.toLowerCase()) {
            case "assets", "policies", "contractdefinitions" -> response.containsKey("entities") ||
                    response.containsKey("@context") ||
                    response.isEmpty();
            default -> true;
        };
    }

    private String createEmptyQueryBody() {
        return new JsonObject()
                .put("offset", 0)
                .put("limit", 1)
                .encode();
    }


    private boolean isAuthenticationError(Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("401") ||
                message.contains("403") ||
                message.contains("unauthorized") ||
                message.contains("forbidden");
    }

    private boolean isNotFoundError(Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("404") ||
                message.contains("not found");
    }
}