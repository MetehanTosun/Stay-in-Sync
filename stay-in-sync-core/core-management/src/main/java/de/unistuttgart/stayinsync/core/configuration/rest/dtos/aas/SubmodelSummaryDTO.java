package de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SubmodelSummary", description = "Snapshot summary of a Submodel from the Core database (lite).")
public record SubmodelSummaryDTO(
        @Schema(description = "Canonical AAS Submodel ID (IRI/URN)") String id,
        @Schema(description = "AAS idShort of the Submodel, if present") String idShort,
        @Schema(description = "Semantic ID of the Submodel, as provided by AAS") String semanticId,
        @Schema(description = "Submodel kind (Instance or Template)") String kind,
        @Schema(description = "Primary key of the Core entity (aas_submodel_lite.id)") Long coreEntityId
) { }


