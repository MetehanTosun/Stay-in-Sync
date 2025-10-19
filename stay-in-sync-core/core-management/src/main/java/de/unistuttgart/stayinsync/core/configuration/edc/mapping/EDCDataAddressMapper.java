package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCDataAddressDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.DataAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

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
    @Mapping(source = "proxyPath", target = "proxyPath")
    @Mapping(source = "proxyQueryParams", target = "proxyQueryParams")
    DataAddress fromDto(EDCDataAddressDto dto);
}
