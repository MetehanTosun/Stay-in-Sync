package java.de.unistuttgart.stayinsync.pollingnode.execution.service;

import java.de.unistuttgart.stayinsync.pollingnode.execution.ressource.RestClient;
import io.quarkus.logging.Log;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;


import java.util.Map;
import java.util.Optional;


@ApplicationScoped
public class PollingJobPollingService {
    private final RestClient restClient;


    public PollingJobPollingService(final RestClient restClient){
        super();
        this.restClient = restClient;
    }

    /**
     * Returns a Map with the fields and their values, created from the polled JsonObject of the SourceSystem.
     * @param apiAddress used to poll from the correct SourceSystem
     * @return a Map with the fields as keys and the values as their elements. If not successful empty Map.
     */
    public Map<String,Object> pollAndMapData(final String apiAddress) {
        return fetchJsonData(apiAddress)
                .map(this::convertJsonObjectToFieldValueMapping)
                .orElse(Map.of());
    }

    /**
     * Polls data of given Api and returns it as an Optional JsonObject
     */
    protected Optional<JsonObject> fetchJsonData(final String apiAddress) {
        try {
            JsonObject result = restClient.getJsonOfApi(apiAddress)
                    .subscribe().asCompletionStage()
                    .toCompletableFuture()
                    .get();

            Log.infof("JsonObject polled successfully: %s", apiAddress);
            return Optional.of(result);

        } catch (Exception e) {
            Log.errorf(e, "Error during poll of %s: %s", apiAddress, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Converts JsonObject to Map<String,Object>. Every field is put as key to its value as the element.
     */
    protected Map<String, Object> convertJsonObjectToFieldValueMapping(final JsonObject jsonObject) {
        if (jsonObject == null || jsonObject.isEmpty()) {
            Log.warn("JsonObject was empty and therefore an empty List was returned.");
            return Map.of();
        }
        Log.infof("JsonObject was created successfully", jsonObject);
        return jsonObject.getMap();
    }
}
