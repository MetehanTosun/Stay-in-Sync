package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.DataAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * MapStruct-Mapper zur Konvertierung zwischen EDCDataAddress-Entities und EDCDataAddressDto-Objekten.
 */
@Mapper
public interface EDCDataAddressMapper {

    /**
     * Singleton-Instanz des Mappers.
     */
    EDCDataAddressMapper INSTANCE = Mappers.getMapper(EDCDataAddressMapper.class);

    /**
     * Konvertiert ein EDCDataAddress-Entity in ein EDCDataAddressDto.
     * 
     * @param entity Das zu konvertierende Entity
     * @return Das erzeugte DTO
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "jsonLDType", target = "jsonLDType")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "baseUrl", target = "baseUrl")
    @Mapping(source = "path", target = "path")
    @Mapping(source = "queryParams", target = "queryParams")
    @Mapping(target = "headerParams", expression = "java(convertJsonToHeaderParamsMap(entity.getHeaderParams()))")
    @Mapping(source = "proxyPath", target = "proxyPath")
    @Mapping(source = "proxyQueryParams", target = "proxyQueryParams")
    EDCDataAddressDto toDto(DataAddress entity);

    /**
     * Konvertiert ein EDCDataAddressDto in ein EDCDataAddress-Entity.
     * 
     * @param dto Das zu konvertierende DTO
     * @return Das erzeugte Entity
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "jsonLDType", target = "jsonLDType")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "baseUrl", target = "baseUrl")
    @Mapping(source = "path", target = "path")
    @Mapping(source = "queryParams", target = "queryParams")
    @Mapping(target = "headerParams", expression = "java(convertMapToJson(dto.headerParams()))")
    @Mapping(source = "proxyPath", target = "proxyPath")
    @Mapping(source = "proxyQueryParams", target = "proxyQueryParams")
    DataAddress fromDto(EDCDataAddressDto dto);
    
    /**
     * Konvertiert einen JSON-String in eine Map für Header-Parameter.
     *
     * @param json JSON-String mit Header-Parametern
     * @return Map mit Header-Parametern oder null, wenn der String leer oder ungültig ist
     */
    default Map<String, String> convertJsonToHeaderParamsMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            // Log the error but don't throw an exception that would break the whole mapping
            System.err.println("Error parsing JSON for header parameters: " + e.getMessage());
            return new HashMap<>(); // Return empty map instead of null to avoid NPE
        }
    }
    
    /**
     * Konvertiert eine Map in einen JSON-String.
     *
     * @param map Map mit Parametern
     * @return JSON-String oder null, wenn die Map leer oder ungültig ist
     */
    default String convertMapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            // Log the error but don't throw an exception that would break the whole mapping
            System.err.println("Error converting map to JSON: " + e.getMessage());
            return "{}"; // Return empty JSON object instead of null to avoid issues
        }
    }
}
