package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCAssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCDataAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MapStruct-Mapper zur Konvertierung zwischen EDCAsset-Entities und EDCAssetDto-Objekten.
 * 
 * Verwendet MapStruct um die Konvertierung zwischen Entity- und DTO-Objekten zu automatisieren.
 * Verwendet zusätzlich EDCDataAddressMapper und EDCPropertyMapper für die Konvertierung
 * der entsprechenden Unter-Objekte.
 */
@Mapper(uses = {EDCDataAddressMapper.class, EDCPropertyMapper.class})
public interface EDCAssetMapper {

    /**
     * Singleton-Instanz des Mappers.
     */
    EDCAssetMapper assetMapper = Mappers.getMapper(EDCAssetMapper.class);

    /**
     * Konvertiert ein EDCAsset-Entity in ein EDCAssetDto.
     * 
     * @param asset Das zu konvertierende Entity
     * @return Das erzeugte DTO
     */
    @Mapping(source = "targetEDC", target = "targetEDCId")
    @Mapping(target = "context", expression = "java(getDefaultContext())")
    @Mapping(source = "properties", target = "properties")
    @Mapping(target = "jsonLDType", constant = "Asset")
    @Mapping(source = "properties.name", target = "name")
    EDCAssetDto assetToAssetDto(EDCAsset asset);

    /**
     * Konvertiert ein EDCAssetDto in ein EDCAsset-Entity.
     * 
     * @param assetDto Das zu konvertierende DTO
     * @return Das erzeugte Entity
     */
    @Mapping(source = "targetEDCId", target = "targetEDC")
    @Mapping(source = "properties", target = "properties")
    @Mapping(target = "targetSystemEndpoint", ignore = true)
    EDCAsset assetDtoToAsset(EDCAssetDto assetDto);

    /**
     * Erzeugt den Standard-Kontext für EDC-Assets.
     * 
     * @return Eine Map mit dem Standard-EDC-Kontext
     */
    default Map<String, String> getDefaultContext() {
        return new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/"));
    }

    /**
     * Hilfsmethode zum Mapping einer UUID zu einer EDCInstance.
     * Lädt die EDCInstance aus der Datenbank anhand der UUID.
     * 
     * @param targetEDCId Die UUID der zu ladenden EDCInstance
     * @return Die gefundene EDCInstance oder null, wenn keine gefunden wurde
     */
    default EDCInstance map(UUID targetEDCId) {
        if (targetEDCId == null) {
            return null;
        }
        return EDCInstance.findById(targetEDCId);
    }

    /**
     * Hilfsmethode zum Mapping einer EDCInstance zu einer UUID.
     * Extrahiert die ID aus der EDCInstance.
     * 
     * @param targetEDC Die EDCInstance, aus der die ID extrahiert werden soll
     * @return Die ID der EDCInstance oder null, wenn targetEDC null ist
     */
    default UUID map(EDCInstance targetEDC) {
        if (targetEDC == null) {
            return null;
        }
        return targetEDC.id;
    }

    /**
     * Helfermethode: mappt eine einzelne EDCProperty zu einer Map von Properties.
     * MapStruct benötigt diese Methode, weil das Entity ein einzelnes EDCProperty
     * und das DTO eine Map von Properties verwendet.
     */
    default Map<String, Object> mapPropertyToMap(de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty properties) {
        if (properties == null) return new HashMap<>();
        
        EDCPropertyDto dto = EDCPropertyMapper.toDto(properties);
        return dto.getProperties();
    }

    /**
     * Helfermethode: mappt eine Map von Properties auf ein einzelnes EDCProperty.
     */
    default de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty mapMapToProperty(Map<String, Object> propertiesMap) {
        if (propertiesMap == null || propertiesMap.isEmpty()) return null;
        
        EDCPropertyDto dto = new EDCPropertyDto();
        dto.setProperties(propertiesMap);
        return EDCPropertyMapper.fromDto(dto);
    }
}
