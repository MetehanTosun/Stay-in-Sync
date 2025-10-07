package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.JsonTemplateDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.JsonTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct-Mapper zur Konvertierung zwischen Template-Entities und TemplateDto-Objekten.
 *
 * Verwendet MapStruct um die Konvertierung zwischen Entity- und DTO-Objekten zu automatisieren.
 * Konvertiert automatisch zwischen UUID (Entity) und String (DTO) für die ID.
 */
@Mapper
public interface JsonTemplateMapper {

    /**
     * Singleton-Instanz des Mappers.
     */
    JsonTemplateMapper templateMapper = Mappers.getMapper(JsonTemplateMapper.class);

    /**
     * Konvertiert ein Template-Entity in ein TemplateDto.
     * Konvertiert automatisch die UUID-ID in eine String-ID.
     *
     * @param template Das zu konvertierende Entity
     * @return Das erzeugte DTO
     */
    @Mapping(source = "id", target = "id")
    JsonTemplateDto templateToTemplateDto(JsonTemplate template);

    /**
     * Konvertiert ein TemplateDto in ein Template-Entity.
     * Konvertiert automatisch die String-ID in eine UUID-ID.
     *
     * @param templateDto Das zu konvertierende DTO
     * @return Das erzeugte Entity
     */
    @Mapping(source = "id", target = "id")
    JsonTemplate templateDtoToTemplate(JsonTemplateDto templateDto);

    /**
     * Hilfsmethode zum Mapping einer String-ID zu einer UUID.
     *
     * @param id Die String-ID
     * @return Die UUID oder null, wenn id null oder leer ist
     */
    default java.util.UUID mapStringToUuid(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            // Falls die String-ID kein gültiger UUID-Format hat, null zurückgeben
            return null;
        }
    }

    /**
     * Hilfsmethode zum Mapping einer UUID zu einer String-ID.
     *
     * @param id Die UUID
     * @return Die String-Repräsentation oder null, wenn id null ist
     */
    default String mapUuidToString(java.util.UUID id) {
        if (id == null) {
            return null;
        }
        return id.toString();
    }
}
