package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EdcInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Policy;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper-Klasse für die Konvertierung zwischen EDCPolicy-Entitäten und DTOs.
 * 
 * Diese Klasse stellt Methoden bereit, um zwischen den Datenbank-Entitäten (EDCPolicy)
 * und den Data Transfer Objects (EDCPolicyDto) zu konvertieren. Die Hauptaufgabe ist die
 * Serialisierung und Deserialisierung der Policy-Struktur zwischen JSON-String und Map-Objekt.
 */
@Mapper(componentModel = "cdi", uses = {EDCInstanceMapper.class})
@ApplicationScoped
public interface EDCPolicyMapper {
    
    /**
     * Der ObjectMapper für die JSON-Serialisierung und -Deserialisierung.
     */
    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Konvertiert eine EDCPolicy-Entität in ein EDCPolicyDto.
     * 
     * @param policy Die zu konvertierende Entität
     * @return Das erzeugte DTO oder null, wenn die Eingabe null ist
     */
    @Mapping(source = "edcInstance.id", target = "edcId")
    @Mapping(source = "policyJson", target = "policy", qualifiedByName = "jsonToMap")
    @Mapping(target = "context", expression = "java(getDefaultContext())")
    @Mapping(target = "rawJson", ignore = true)
    EDCPolicyDto policyToPolicyDto(Policy policy);
    
    /**
     * Konvertiert eine Liste von EDCPolicy-Entitäten in DTOs
     *
     * @param entities Die Liste von Entitäten
     * @return Liste von DTOs
     */
    List<EDCPolicyDto> toDtoList(List<Policy> entities);

    /**
     * Konvertiert ein EDCPolicyDto in eine EDCPolicy-Entität.
     * 
     * @param policyDto Das zu konvertierende DTO
     * @return Die erzeugte Entität oder null, wenn die Eingabe null ist
     */
    @Mapping(source = "edcId", target = "edcInstance", qualifiedByName = "idToInstance")
    @Mapping(source = "policy", target = "policyJson", qualifiedByName = "mapToJson")
    Policy policyDtoToPolicy(EDCPolicyDto policyDto);

    /**
     * Konvertiert einen JSON-String in eine Map<String, Object>.
     * Diese Methode wird von MapStruct für die Konvertierung von policyJson zu policy verwendet.
     * 
     * @param json Der zu konvertierende JSON-String
     * @return Die erzeugte Map oder eine leere Map im Fehlerfall
     */
    @Named("jsonToMap")
    default Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            Log.info("Policy JSON is null or empty, returning empty map");
            return new HashMap<>();
        }
        
        try {
            Log.debug("Converting JSON to Map: " + json);
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Log.debug("Successfully converted JSON to Map");
            return result;
        } catch (IOException e) {
            Log.error("Fehler beim Deserialisieren des Policy-JSON: " + e.getMessage() + ", JSON: " + json, e);
            // Im Fehlerfall eine leere Map zurückgeben, um NPEs zu vermeiden
            return new HashMap<>();
        }
    }

    /**
     * Konvertiert eine Map<String, Object> in einen JSON-String.
     * Diese Methode wird von MapStruct für die Konvertierung von policy zu policyJson verwendet.
     * 
     * @param map Die zu konvertierende Map
     * @return Der erzeugte JSON-String oder ein leerer JSON-String im Fehlerfall
     */
    @Named("mapToJson")
    default String mapToJson(Map<String, Object> map) {
        if (map == null) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            Log.error("Fehler beim Serialisieren der Policy-Map: " + e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * Hilfsmethode zum Mapping einer ID zu einer EDCInstance.
     * Lädt die EDCInstance aus der Datenbank anhand der ID.
     * 
     * @param edcId Die ID der zu ladenden EDCInstance
     * @return Die gefundene EDCInstance oder null, wenn keine gefunden wurde
     */
    @Named("idToInstance")
    default EdcInstance idToInstance(Long edcId) {
        if (edcId == null) {
            Log.warn("EDC ID is null when trying to map to EDCInstance");
            return null;
        }
        EdcInstance instance = EdcInstance.findById(edcId);
        if (instance == null) {
            Log.warn("Could not find EDC instance with ID: " + edcId);
        } else {
            Log.info("Found EDC instance with ID: " + instance.id);
        }
        return instance;
    }
    
    /**
     * Erzeugt den Standard-Kontext für EDC-Policies.
     * 
     * @return Eine Map mit dem Standard-Policy-Kontext
     */
    default Map<String, String> getDefaultContext() {
        return new HashMap<>(Map.of("odrl", "http://www.w3.org/ns/odrl/2/"));
    }
    
    /**
     * Nach dem Mapping führt diese Methode zusätzliche Operationen durch.
     * Sucht nach einer existierenden Entität mit der ID aus dem DTO, falls vorhanden.
     * 
     * @param dto Das Quell-DTO
     * @param entity Die neu erstellte Entität
     * @return Die finale Entität (entweder die existierende oder die neue)
     */
    @AfterMapping
    default Policy handleExistingEntity(EDCPolicyDto dto, @MappingTarget Policy entity) {
        if (dto.id() != null) {
            Policy existingEntity = Policy.findById(dto.id());
            
            if (existingEntity != null) {
                // Kopiere alle Felder von der neuen Entity zur existierenden
                existingEntity.policyId = entity.policyId;
                existingEntity.displayName = entity.displayName;
                existingEntity.policyJson = entity.policyJson;
                existingEntity.edcInstance = entity.edcInstance;
                
                return existingEntity;
            }
        }
        
        return entity;
    }
}
