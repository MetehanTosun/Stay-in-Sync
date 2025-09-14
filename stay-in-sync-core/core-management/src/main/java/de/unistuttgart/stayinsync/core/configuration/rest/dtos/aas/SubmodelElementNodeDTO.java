package de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SubmodelElementNode", description = "Snapshot node representing a SubmodelElement from the Core database (lite).")
public record SubmodelElementNodeDTO(
        @Schema(description = "AAS modelType of the element") String type,
        @Schema(description = "idShort of the element") String idShort,
        @Schema(description = "Slash-separated path of idShorts from the submodel root") String idShortPath,
        @Schema(description = "Whether the element has children (collections, lists, entity)") boolean hasChildren,
        @Schema(description = "Value type for Property/Range/etc.") String valueType,
        @Schema(description = "Semantic ID of the element, if present") String semanticId,
        @Schema(description = "True if the element is a ReferenceElement") Boolean isReference,
        @Schema(description = "Reference target type, if ReferenceElement") String referenceTargetType,
        @Schema(description = "Serialized reference keys JSON, if ReferenceElement") String referenceKeys,
        @Schema(description = "Target Submodel ID if a reference points to a submodel") String targetSubmodelId,
        @Schema(description = "Value list element type for SubmodelElementList") String typeValueListElement,
        @Schema(description = "Order relevant flag for SubmodelElementList") Boolean orderRelevant,
        @Schema(description = "Operation input variables (subset)") List<VariableDTO> inputVariables,
        @Schema(description = "Operation output variables (subset)") List<VariableDTO> outputVariables,
        @Schema(description = "Primary key of the Core entity (aas_element_lite.id)") Long coreEntityId
) {
    public record VariableDTO(String idShort, String valueType) { }
}


