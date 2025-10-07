package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dto.ContextDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.AssetDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.Asset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapstruct Asset Mapper that converts the
 */
@Mapper(uses = {AssetPropertiesMapper.class, AssetDataAddressMapper.class})
public interface AssetMapper {

    AssetMapper mapper = Mappers.getMapper(AssetMapper.class);

    /**
     * Konvertiert eine Asset-Entity zu einem AssetDto.
     * Der context wird automatisch mit Standard-Werten gesetzt.
     * targetEDC wird ignoriert (nur intern relevant).
     * thirdPartyChanges wird mitgeliefert (wird durch @JsonView bei EDC-Kommunikation ausgeblendet).
     *
     * @param entity Die zu konvertierende Asset-Entity
     * @return Das AssetDto für UI oder EDC-Kommunikation
     */
    @Mapping(target = "context", expression = "java(getDefaultContext())")
    @Mapping(target = "type", constant = "Asset")
    @Mapping(source = "assetId", target = "id")
    @Mapping(source = "thirdPartyChanges", target = "thirdPartyChanges")
    @Mapping(source = "properties", target = "properties")
    @Mapping(source = "dataAddress", target = "dataAddress")
    AssetDto entityToDto(Asset entity);

    /**
     * Konvertiert ein AssetDto zu einer Asset-Entity.
     * targetEDC muss separat gesetzt werden (wird nicht aus DTO übernommen).
     * context wird ignoriert (nicht in der Entity gespeichert).
     * Die @id aus dem DTO wird als assetId übernommen.
     *
     * @param dto Das zu konvertierende AssetDto
     * @return Die Asset-Entity (targetEDC muss noch gesetzt werden!)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "targetEDC", ignore = true) // Muss vom Caller gesetzt werden
    @Mapping(target = "type", constant = "Asset")
    @Mapping(source = "id", target = "assetId")
    @Mapping(source = "thirdPartyChanges", target = "thirdPartyChanges", defaultValue = "false")
    @Mapping(source = "properties", target = "properties")
    @Mapping(source = "dataAddress", target = "dataAddress")
    Asset dtoToEntity(AssetDto dto);


    /**
     * Erzeugt den Standard-Kontext für EDC-Assets (Tractus-X).
     *
     * @return Das Standard-AssetContextDto mit allen erforderlichen Namespaces
     */
    default ContextDto getDefaultContext() {
        return new ContextDto();
    }
}