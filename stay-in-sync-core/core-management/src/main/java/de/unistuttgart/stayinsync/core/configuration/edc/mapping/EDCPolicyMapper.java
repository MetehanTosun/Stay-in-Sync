package de.unistuttgart.stayinsync.core.configuration.edc.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCPolicyDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import org.jboss.logging.Logger;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Map;
import java.util.Optional;

/**
 * Mapper-Klasse für die Konvertierung zwischen EDCPolicy-Entitäten und DTOs.
 * 
 * Diese Klasse stellt Methoden bereit, um zwischen den Datenbank-Entitäten (EDCPolicy)
 * und den Data Transfer Objects (EDCPolicyDto) zu konvertieren. Die Hauptaufgabe ist die
 * Serialisierung und Deserialisierung der Policy-Struktur zwischen JSON-String und Map-Objekt.
 */
@Mapper(uses = EDCInstanceMapper.class)
public interface EDCPolicyMapper {
    EDCPolicyMapper policyMapper = Mappers.getMapper(EDCPolicyMapper.class);

    EDCPolicyDto policyToPolicyDto(EDCPolicy policy);

    EDCPolicy policyDtoToPolicy(EDCPolicyDto policyDto);

}
