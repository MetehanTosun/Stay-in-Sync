package de.unistuttgart.stayinsync.core.configuration.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.OperatorMetadataEntity;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.OperatorMetadataDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
public class OperatorMetadata {

    /**
     * Retrieves all operator metadata definitions from the database
     * and maps them to DTOs for client consumption.
     *
     * @return A list of all available operators with their metadata.
     */
    @Transactional(SUPPORTS)
    public List<OperatorMetadataDTO> findAllOperatorMetadata() {
        List<OperatorMetadataEntity> entities = OperatorMetadataEntity.listAll();
        List<OperatorMetadataDTO> dtos = new ArrayList<>();

        for (OperatorMetadataEntity entity : entities) {
            // Convert the JSON string from the entity into a list.
            List<String> inputTypes = fromJsonString(entity.inputTypesJson);

            // Create the new record DTO.
            OperatorMetadataDTO dto = new OperatorMetadataDTO(
                    entity.operatorName,
                    entity.description,
                    entity.category,
                    inputTypes,
                    entity.outputType
            );

            dtos.add(dto);
        }

        return dtos;
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
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // A proper logger should be used here in a production environment.
            return Collections.emptyList();
        }
    }
}