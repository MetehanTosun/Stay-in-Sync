package de.unistuttgart.stayinsync.core.configuration.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TransformationRule;
import de.unistuttgart.graphengine.dto.transformationrule.TransformationRuleDTO;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public abstract class TransformationRuleMapper {

    @Inject
    ObjectMapper objectMapper;

    public TransformationRuleDTO mapToDto(TransformationRule entity) {
        if (entity == null) {
            Log.warn("MAPPER LOG: Source TransformationRule entity is null. Returning null.");
            return null;
        }

        if (objectMapper == null) {
            Log.error("MAPPER LOG: CRITICAL FAILURE - ObjectMapper has not been injected! Returning null.");
            return new TransformationRuleDTO(null);
        }

        String json = entity.graph.graphDefinitionJson;
        if (json == null || json.isBlank()) {
            Log.warnf("MAPPER LOG: graphJsonString for Rule ID %d is empty. Returning DTO with null graph.", entity.id);
            return new TransformationRuleDTO(null);
        }

        try {
            GraphDTO graphDTO = objectMapper.readValue(json, GraphDTO.class);

            if (graphDTO != null) {
                Log.infof("MAPPER LOG: SUCCESS! Deserialized GraphDTO. Node count: %d", graphDTO.getNodes() != null ? graphDTO.getNodes().size() : 0);
            } else {
                Log.error("MAPPER LOG: Deserialization returned null for non-empty JSON. This is unexpected.");
            }

            return new TransformationRuleDTO(graphDTO);

        } catch (JsonProcessingException e) {
            Log.errorf(e, "MAPPER LOG: FATAL JSON PARSING ERROR for Rule ID %d. Returning DTO with null graph.", entity.id);
            return new TransformationRuleDTO(null);
        }
    }
}