package de.unistuttgart.stayinsync.core.configuration.rest.dtos.typegeneration;

import java.util.List;

public record GetTypeDefinitionsResponseDTO(
        List<TypeLibraryDTO> libraries
) {
}
