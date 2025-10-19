package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPropertyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * MapStruct-Mapper zur Konvertierung zwischen EDCProperty-Entities und EDCPropertyDto-Objekten.
 */
@Mapper
public interface EDCPropertyMapper {

    /**
     * Singleton-Instanz des Mappers.
     */
    EDCPropertyMapper INSTANCE = Mappers.getMapper(EDCPropertyMapper.class);

    /**
     * Konvertiert ein EDCProperty-Entity in ein EDCPropertyDto.
     * 
     * @param entity Das zu konvertierende Entity
     * @return Das erzeugte DTO
     */
    @Mapping(target = "properties", expression = "java(propertyToMap(entity))")
    EDCPropertyDto toDto(EDCProperty entity);

    /**
     * Konvertiert ein EDCPropertyDto in ein EDCProperty-Entity.
     * 
     * @param dto Das zu konvertierende DTO
     * @return Das erzeugte Entity
     */
    @Mapping(source = "id", target = "id")
    @Mapping(target = "name", expression = "java(getNameFromProperties(dto))")
    @Mapping(target = "version", expression = "java(getVersionFromProperties(dto))")
    @Mapping(target = "contentType", expression = "java(getContentTypeFromProperties(dto))")
    @Mapping(target = "description", expression = "java(getDescriptionFromProperties(dto))")
    EDCProperty fromDto(EDCPropertyDto dto);

    /**
     * Konvertiert die Entity-Eigenschaften in eine Map für das DTO.
     */
    default Map<String, Object> propertyToMap(EDCProperty entity) {
        if (entity == null) return new HashMap<>();
        
        Map<String, Object> properties = new HashMap<>();
        
        if (entity.name != null) {
            properties.put(EDCPropertyDto.PROP_NAME, entity.name);
        }
        
        if (entity.version != null) {
            properties.put(EDCPropertyDto.PROP_VERSION, entity.version);
        }
        
        if (entity.contentType != null) {
            properties.put(EDCPropertyDto.PROP_CONTENTTYPE, entity.contentType);
        }
        
        if (entity.description != null) {
            properties.put(EDCPropertyDto.PROP_DESCRIPTION, entity.description);
        }
        
        return properties;
    }

    /**
     * Extrahiert den Namen aus dem DTO.
     */
    default String getNameFromProperties(EDCPropertyDto dto) {
        if (dto == null || dto.properties() == null) return "Default asset";
        String name = (String) dto.properties().get(EDCPropertyDto.PROP_NAME);
        return name != null && !name.trim().isEmpty() ? name : "Default asset";
    }

    /**
     * Extrahiert die Version aus dem DTO.
     */
    default String getVersionFromProperties(EDCPropertyDto dto) {
        if (dto == null || dto.properties() == null) return null;
        Object value = dto.properties().get(EDCPropertyDto.PROP_VERSION);
        return value != null ? value.toString() : null;
    }

    /**
     * Extrahiert den Content-Type aus dem DTO.
     */
    default String getContentTypeFromProperties(EDCPropertyDto dto) {
        if (dto == null || dto.properties() == null) return null;
        Object value = dto.properties().get(EDCPropertyDto.PROP_CONTENTTYPE);
        return value != null ? value.toString() : null;
    }

    /**
     * Extrahiert die Beschreibung aus dem DTO.
     */
    default String getDescriptionFromProperties(EDCPropertyDto dto) {
        if (dto == null || dto.properties() == null) return "Default description";
        String description = (String) dto.properties().get(EDCPropertyDto.PROP_DESCRIPTION);
        return description != null && !description.trim().isEmpty() ? description : "Default description";
    }
}
