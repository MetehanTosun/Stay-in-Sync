package de.unistuttgart.stayinsync.core.configuration.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.OperatorMetadata;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.OperatorMetadataDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
public class OperatorMetadataService {

    /**
     * Retrieves all operator metadata definitions from the database
     * and maps them to DTOs for client consumption.
     *
     * @return A list of all available operators with their metadata.
     */
    @Transactional(SUPPORTS)
    public List<OperatorMetadataDTO> findAllOperatorMetadata() {
        Log.debug("Getting all operator metadata.");

        try {
            List<OperatorMetadata> entities = OperatorMetadata.listAll();
            List<OperatorMetadataDTO> dtos = new ArrayList<>();

            for (OperatorMetadata entity : entities) {
                List<String> inputTypes = fromJsonString(entity.inputTypesJson);

                OperatorMetadataDTO dto = new OperatorMetadataDTO(
                        entity.operatorName,
                        entity.description,
                        entity.category,
                        inputTypes,
                        entity.outputType
                );
                dtos.add(dto);
            }

            Log.infof("Successfully retrieved and mapped %d operator metadata entries.", dtos.size());
            return dtos;

        } catch (Exception e) {
            Log.errorf(e, "An unexpected error occurred while fetching operator metadata.");
            throw new CoreManagementException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Database Error", "Could not retrieve operator metadata.", e);
        }
    }

    /**
     * A private helper method to safely parse a JSON array string into a List<String>.
     *
     * @param json The JSON string from the database entity.
     * @return A List of strings, or an empty list if parsing fails or the input is null/blank.
     */
    private List<String> fromJsonString(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            Log.warnf(e, "Failed to parse inputTypesJson string: %s. Returning empty list.", json);
            return Collections.emptyList();
        }
    }
}