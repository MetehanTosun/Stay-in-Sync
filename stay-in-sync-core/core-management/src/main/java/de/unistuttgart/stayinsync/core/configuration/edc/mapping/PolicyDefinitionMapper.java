package de.unistuttgart.stayinsync.core.configuration.edc.mapping;



import de.unistuttgart.stayinsync.core.configuration.edc.dto.ContextDto;
import de.unistuttgart.stayinsync.core.configuration.edc.dto.PolicyDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.PolicyDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;



/**
 * Mapstruct PolicyDefinition Mapper that converts entities to dtos and dtos to entities.
 */
@Mapper(uses = {PolicyMapper.class})
public interface PolicyDefinitionMapper {

    PolicyDefinitionMapper mapper = Mappers.getMapper(PolicyDefinitionMapper.class);

    /**
     * Konvertiert eine EDCPolicy-Entität in ein EDCPolicyDto.
     *
     * @param policy Die zu konvertierende Entität
     * @return Das erzeugte DTO oder null, wenn die Eingabe null ist
     */
    @Mapping(target = "context", expression = "java(getDefaultContext())")
    @Mapping(target = "type", constant = "PolicyDefinition")
    PolicyDefinitionDto entityToDto(PolicyDefinition policy);

    /**
     * Konvertiert ein EDCPolicyDto in eine EDCPolicy-Entität.
     *
     * @param policyDto Das zu konvertierende DTO
     * @return Die erzeugte Entität oder null, wenn die Eingabe null ist
     */
    @Mapping(target = "targetEdc", ignore = true) 
    PolicyDefinition dtoToEntity(PolicyDefinitionDto policyDto);

    default ContextDto getDefaultContext() {
        return new ContextDto();
    }


}
