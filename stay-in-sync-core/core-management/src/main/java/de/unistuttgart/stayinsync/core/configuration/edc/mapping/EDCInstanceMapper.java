package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCInstanceDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EdcInstance;
import org.mapstruct.*;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Mapper für die Konvertierung zwischen EDCInstance-Entitäten und DTOs.
 * Implementiert mit MapStruct für automatische Konvertierung.
 */
@Mapper(componentModel = "cdi")
@Singleton
public interface EDCInstanceMapper {
    
    /**
     * Konvertiert eine EDCInstance-Entität in ein DTO.
     * 
     * @param entity Die zu konvertierende Entität
     * @return Das erzeugte DTO oder null, wenn die Entität null ist
     */
    EDCInstanceDto toDto(EdcInstance entity);
    
    /**
     * Konvertiert eine Liste von EDCInstance-Entitäten in DTOs
     *
     * @param entities Die Liste von Entitäten
     * @return Liste von DTOs
     */
    List<EDCInstanceDto> toDtoList(List<EdcInstance> entities);
    
    /**
     * Konvertiert ein DTO in eine EDCInstance-Entität.
     * 
     * @param dto Das zu konvertierende DTO
     * @return Die erzeugte Entität oder null, wenn das DTO null ist
     */
    EdcInstance fromDto(EDCInstanceDto dto);
    
    /**
     * Aktualisiert ein existierendes EDCInstance-Objekt mit Werten aus dem DTO.
     * Alle anderen Felder bleiben unberührt.
     *
     * @param dto Die Quelldaten als DTO
     * @param entity Das zu aktualisierende Ziel-Entity
     */
    @InheritConfiguration(name = "fromDto")
    void updateEntityFromDto(EDCInstanceDto dto, @MappingTarget EdcInstance entity);
    
    /**
     * Nach dem Mapping führt diese Methode zusätzliche Operationen durch.
     * Sucht nach einer existierenden Entität mit der ID aus dem DTO, falls vorhanden.
     * 
     * @param dto Die Quell-DTO
     * @param entity Die neu erstellte Entität
     * @return Die finale Entität (entweder die existierende oder die neue)
     */
    @AfterMapping
    default EdcInstance handleExistingEntity(EDCInstanceDto dto, @MappingTarget EdcInstance entity) {
        if (dto.id() != null) {
            EdcInstance existingEntity = EdcInstance.findById(dto.id());
            
            if (existingEntity != null) {
                // Kopiere alle Felder von der neuen Entity zur existierenden
                existingEntity.name = entity.name;
                existingEntity.controlPlaneManagementUrl = entity.controlPlaneManagementUrl;
                existingEntity.protocolVersion = entity.protocolVersion;
                existingEntity.description = entity.description;
                existingEntity.bpn = entity.bpn;
                existingEntity.apiKey = entity.apiKey;
                existingEntity.edcAssetEndpoint = entity.edcAssetEndpoint;
                existingEntity.edcPolicyEndpoint = entity.edcPolicyEndpoint;
                existingEntity.edcContractDefinitionEndpoint = entity.edcContractDefinitionEndpoint;
                
                return existingEntity;
            }
        }
        
        return entity;
    }
}