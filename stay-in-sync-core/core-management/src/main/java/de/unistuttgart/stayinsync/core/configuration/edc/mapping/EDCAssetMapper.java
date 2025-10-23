package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Asset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EdcInstance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * MapStruct-Mapper zur Konvertierung zwischen EDCAsset-Entities und EDCAssetDto-Objekten.
 * 
 * Verwendet MapStruct um die Konvertierung zwischen Entity- und DTO-Objekten zu automatisieren.
 * Verwendet zusätzlich EDCDataAddressMapper und EDCPropertyMapper für die Konvertierung
 * der entsprechenden Unter-Objekte.
 */
@Mapper
public interface EDCAssetMapper {

    /**
     * Singleton-Instanz des Mappers.
     */
    EDCAssetMapper INSTANCE = Mappers.getMapper(EDCAssetMapper.class);

    /**
     * Konvertiert ein EDCAsset-Entity in ein EDCAssetDto.
     * 
     * @param asset Das zu konvertierende Entity
     * @return Das erzeugte DTO
     */
    @Mapping(source = "targetEDC", target = "targetEDCId")
    @Mapping(target = "context", expression = "java(getDefaultContext())")
    @Mapping(source = "properties", target = "properties", qualifiedByName = "propertyToMap")
    @Mapping(target = "jsonLDType", constant = "Asset")
    @Mapping(source = "properties.name", target = "name")
    EDCAssetDto assetToAssetDto(Asset asset);

    /**
     * Konvertiert ein EDCAssetDto in ein EDCAsset-Entity.
     * 
     * @param assetDto Das zu konvertierende DTO
     * @return Das erzeugte Entity
     */
    @Mapping(source = "targetEDCId", target = "targetEDC")
    @Mapping(source = "properties", target = "properties", qualifiedByName = "mapToProperty")
    @Mapping(target = "targetSystemEndpoint", ignore = true)
    Asset assetDtoToAsset(EDCAssetDto assetDto);

    /**
     * Erzeugt den Standard-Kontext für EDC-Assets.
     * 
     * @return Eine Map mit dem Standard-EDC-Kontext
     */
    default Map<String, String> getDefaultContext() {
        return new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/"));
    }

    /**
     * Hilfsmethode zum Mapping einer Long ID zu einer EDCInstance.
     * Lädt die EDCInstance aus der Datenbank anhand der ID.
     * 
     * @param targetEDCId Die ID der zu ladenden EDCInstance
     * @return Die gefundene EDCInstance oder null, wenn keine gefunden wurde
     */
    default EdcInstance map(Long targetEDCId) {
        if (targetEDCId == null) {
            return null;
        }
        return EdcInstance.findById(targetEDCId);
    }

    /**
     * Hilfsmethode zum Mapping einer EDCInstance zu einer Long.
     * Extrahiert die ID aus der EDCInstance.
     * 
     * @param targetEDC Die EDCInstance, aus der die ID extrahiert werden soll
     * @return Die ID der EDCInstance oder null, wenn targetEDC null ist
     */
    default Long map(EdcInstance targetEDC) {
        if (targetEDC == null) {
            return null;
        }
        return targetEDC.id;
    }
    
        /**
     * Hilfsmethode zum Konvertieren eines Object-Typs in einen String.
     * Wird für die Typumwandlung von queryParams benötigt.
     * 
     * @param obj Das zu konvertierende Objekt
     * @return Der String-Wert des Objekts oder null
     */
    default String map(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof String) {
            return (String) obj;
        }
        
        try {
            if (obj instanceof Map) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.writeValueAsString(obj);
            }
        } catch (Exception e) {
            System.err.println("Error converting object to string: " + e.getMessage());
        }
        
        return obj.toString();
    }
    
    /**
     * Konvertiert eine Map<String, String> zu einem JSON-String.
     * 
     * @param value Die Map, die konvertiert werden soll
     * @return Der JSON-String
     */
    default String map(Map<String, String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            System.err.println("Error converting map to string: " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Konvertiert einen JSON-String zu einer Map<String, String>.
     * 
     * @param value Der JSON-String, der konvertiert werden soll
     * @return Die Map mit den Schlüssel-Wert-Paaren
     */
    default Map<String, String> mapStringToMap(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(value, 
                objectMapper.getTypeFactory().constructMapType(
                    HashMap.class, String.class, String.class));
        } catch (Exception e) {
            System.err.println("Error converting string to map: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Helfermethode: mappt eine einzelne EDCProperty zu einer Map von Properties.
     * MapStruct benötigt diese Methode, weil das Entity ein einzelnes EDCProperty
     * und das DTO eine Map von Properties verwendet.
     */
    @Named("propertyToMap")
    default Map<String, Object> mapPropertyToMap(de.unistuttgart.stayinsync.core.configuration.edc.entities.Property properties) {
        if (properties == null) return new HashMap<>();
        
        EDCPropertyDto dto = EDCPropertyMapper.INSTANCE.toDto(properties);
        return dto.properties();
    }

    /**
     * Helfermethode: mappt eine Map von Properties auf ein einzelnes EDCProperty.
     */
    @Named("mapToProperty")
    default de.unistuttgart.stayinsync.core.configuration.edc.entities.Property mapMapToProperty(Map<String, Object> propertiesMap) {
        if (propertiesMap == null || propertiesMap.isEmpty()) return null;
        
        EDCPropertyDto dto = new EDCPropertyDto(null, propertiesMap);
        return EDCPropertyMapper.INSTANCE.fromDto(dto);
    }

    /**
     * Wandelt einen String wie "key1=value1&key2=value2" in eine Map<String, String> um.
     * Diese Methode ist für URL-Parameter-formatierte Strings gedacht.
     */
    @Named("urlParamStringToMap")
    default Map<String, String> stringToMap(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        for (String pair : value.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    /**
     * Wandelt eine Map<String, String> in einen String wie "key1=value1&key2=value2" um.
     * Diese Methode ist für URL-Parameter-formatierte Strings gedacht.
     */
    @Named("mapToUrlParamString")
    default String mapToString(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse(null);
    }
}
    

