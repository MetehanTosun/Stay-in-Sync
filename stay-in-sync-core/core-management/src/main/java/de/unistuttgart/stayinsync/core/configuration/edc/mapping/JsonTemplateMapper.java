package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.JsonTemplateDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.JsonTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct-Mapper zur Konvertierung zwischen Template-Entities und TemplateDto-Objekten.
 *
 * Verwendet MapStruct um die Konvertierung zwischen Entity- und DTO-Objekten zu automatisieren.
 */
@Mapper(componentModel = "cdi")
public interface JsonTemplateMapper {

    /**
     * Singleton-Instanz des Mappers.
     */
    JsonTemplateMapper INSTANCE = Mappers.getMapper(JsonTemplateMapper.class);

    /**
     * Konvertiert ein Template-Entity in ein TemplateDto.
     *
     * @param template Das zu konvertierende Entity
     * @return Das erzeugte DTO
     */
    JsonTemplateDto templateToTemplateDto(JsonTemplate template);

    /**
     * Konvertiert ein TemplateDto in ein Template-Entity.
     *
     * @param templateDto Das zu konvertierende DTO
     * @return Das erzeugte Entity
     */
    JsonTemplate templateDtoToTemplate(JsonTemplateDto templateDto);
}
