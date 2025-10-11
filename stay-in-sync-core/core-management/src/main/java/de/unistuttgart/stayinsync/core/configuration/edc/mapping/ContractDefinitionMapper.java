package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.ContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.ContractDefinition;
import io.quarkus.logging.Log;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ContractDefinitionMapper {

    ContractDefinitionMapper mapper = Mappers.getMapper(ContractDefinitionMapper.class);

    @Mapping(target = "type", constant = "ContractDefinition")
    @Mapping(target = "context", expression = "java(getDefaultContext())")
    @Mapping(source = "contractDefinitionId", target = "contractDefinitionId")
    @Mapping(source = "accessPolicyId", target = "accessPolicyId")
    @Mapping(source = "contractPolicyId", target = "contractPolicyId")
    @Mapping(source = "assetSelector", target = "assetsSelector", qualifiedByName = "stringToAssetSelectorMap")
    ContractDefinitionDto entityToDto(ContractDefinition entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "entityOutOfSync")
    @Mapping(target = "targetEdc", ignore = true)
    @Mapping(source = "contractDefinitionId", target = "contractDefinitionId")
    @Mapping(source = "accessPolicyId", target = "accessPolicyId")
    @Mapping(source = "contractPolicyId", target = "contractPolicyId")
    @Mapping(source = "assetsSelector", target = "assetSelector", qualifiedByName = "assetSelectorMapToString")
    ContractDefinition dtoToEntity(ContractDefinitionDto dto);


    /**
     * Converts a JSON string to a Map for asset selector.
     * Parses the JSON string representation back to a Map<String, Object>.
     *
     * @param assetSelectorString JSON string representation of asset selector
     * @return Map with asset selector criteria
     */
    @Named("stringToAssetSelectorMap")
    default Map<String, Object> stringToAssetSelectorMap(String assetSelectorString) {
        if (assetSelectorString == null || assetSelectorString.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(
                    assetSelectorString,
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException e) {
            Log.error("Failed to deserialize asset selector from JSON string: " + assetSelectorString, e);
            return new HashMap<>();
        }
    }

    /**
     * Converts a Map to JSON string for database storage.
     * Serializes the asset selector Map to a JSON string.
     *
     * @param assetSelectorMap Map with asset selector criteria
     * @return JSON string representation
     */
    @Named("assetSelectorMapToString")
    default String assetSelectorMapToString(Map<String, Object> assetSelectorMap) {
        if (assetSelectorMap == null || assetSelectorMap.isEmpty()) {
            return "{}";
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(assetSelectorMap);
        } catch (JsonProcessingException e) {
            Log.error("Failed to serialize asset selector to JSON string", e);
            return "{}";
        }
    }
}