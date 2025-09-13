package de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas;

import java.util.List;

public record SubmodelElementNodeDTO(
        String type,
        String idShort,
        String idShortPath,
        boolean hasChildren,
        String valueType,
        String semanticId,
        Boolean isReference,
        String referenceTargetType,
        String referenceKeys,
        String targetSubmodelId,
        String typeValueListElement,
        Boolean orderRelevant,
        List<VariableDTO> inputVariables,
        List<VariableDTO> outputVariables
) {
    public record VariableDTO(String idShort, String valueType) { }
}


