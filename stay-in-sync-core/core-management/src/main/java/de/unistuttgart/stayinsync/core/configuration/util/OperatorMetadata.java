package de.unistuttgart.stayinsync.core.configuration.util;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.OperatorMetadataEntity;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.OperatorMetadataDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

import static jakarta.transaction.Transactional.TxType.SUPPORTS;

@ApplicationScoped
public class OperatorMetadata {

    /**
     * Retrieves all operator metadata definitions from the database
     * and maps them to DTOs for client consumption.
     *
     * @return A list of all available operators with their metadata.
     */
    @Transactional(SUPPORTS) // A transaction is not required for a read-only operation.
    public List<OperatorMetadataDTO> findAllOperatorMetadata() {
        List<OperatorMetadataEntity> entities = OperatorMetadataEntity.listAll();
        List<OperatorMetadataDTO> dtos = new ArrayList<>();

        for (OperatorMetadataEntity entity : entities) {
            OperatorMetadataDTO dto = new OperatorMetadataDTO();
            dto.setOperatorName(entity.operatorName);
            dto.setDescription(entity.description);
            dto.setOutputType(entity.outputType);

            // Use the helper method to convert the JSON string from the entity into a list.
            dto.setInputTypes(OperatorMetadataDTO.fromJsonString(entity.inputTypesJson));

            dtos.add(dto);
        }

        return dtos;
    }

}