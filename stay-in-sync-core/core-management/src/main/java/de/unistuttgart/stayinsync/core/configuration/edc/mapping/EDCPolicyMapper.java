package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import io.quarkus.logging.Log;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper-Klasse für die Konvertierung zwischen EDCPolicy-Entitäten und DTOs.
 * 
 * Diese Klasse stellt Methoden bereit, um zwischen den Datenbank-Entitäten (EDCPolicy)
 * und den Data Transfer Objects (EDCPolicyDto) zu konvertieren. Die Hauptaufgabe ist die
 * Serialisierung und Deserialisierung der Policy-Struktur zwischen JSON-String und Map-Objekt.
 */
@Mapper(uses = {EDCInstanceMapper.class})
public interface EDCPolicyMapper {
    
    /**
     * Singleton-Instanz des Mappers.
     */
    EDCPolicyMapper policyMapper = Mappers.getMapper(EDCPolicyMapper.class);
    
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
    @Mapping(source = "edcInstance", target = "edcId")
    @Mapping(source = "policyJson", target = "policy", qualifiedByName = "jsonToMap")
    @Mapping(target = "context", expression = "java(getDefaultContext())")
    EDCPolicyDto policyToPolicyDto(EDCPolicy policy);

    /**
     * Konvertiert ein EDCPolicyDto in eine EDCPolicy-Entität.
     * 
     * @param policyDto Das zu konvertierende DTO
     * @return Die erzeugte Entität oder null, wenn die Eingabe null ist
     */
    @Mapping(source = "edcId", target = "edcInstance")
    @Mapping(source = "policy", target = "policyJson", qualifiedByName = "mapToJson")
    EDCPolicy policyDtoToPolicy(EDCPolicyDto policyDto);

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
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            Log.error("Fehler beim Deserialisieren des Policy-JSON: " + e.getMessage(), e);
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
     * Hilfsmethode zum Mapping einer UUID zu einer EDCInstance.
     * Lädt die EDCInstance aus der Datenbank anhand der UUID.
     * 
     * @param edcId Die UUID der zu ladenden EDCInstance
     * @return Die gefundene EDCInstance oder null, wenn keine gefunden wurde
     */
    default EDCInstance map(UUID edcId) {
        if (edcId == null) {
            return null;
        }
        return EDCInstance.findById(edcId);
    }

    /**
     * Hilfsmethode zum Mapping einer EDCInstance zu einer UUID.
     * Extrahiert die ID aus der EDCInstance.
     * 
     * @param edcInstance Die EDCInstance, aus der die ID extrahiert werden soll
     * @return Die ID der EDCInstance oder null, wenn edcInstance null ist
     */
    default UUID map(EDCInstance edcInstance) {
        if (edcInstance == null) {
            return null;
        }
        return edcInstance.id;
    }
    
    /**
     * Erzeugt den Standard-Kontext für EDC-Policies.
     * 
     * @return Eine Map mit dem Standard-Policy-Kontext
     */
    default Map<String, String> getDefaultContext() {
        return new HashMap<>(Map.of("odrl", "http://www.w3.org/ns/odrl/2/"));
    }
}
