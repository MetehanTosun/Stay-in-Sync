package de.unistuttgart.stayinsync.core.configuration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.MappingConstants;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemForm;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.CreateSourceSystemJsonDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface SourceSystemMapper {

    /** JSON-POST: CreateSourceSystemJsonDTO → Entity */
    @Mappings({
        @Mapping(source = "name",            target = "name"),
        @Mapping(source = "description",     target = "description"),
        @Mapping(source = "type",            target = "type"),
        @Mapping(source = "apiUrl",          target = "apiUrl"),
        @Mapping(source = "authType",        target = "authType"),
        @Mapping(source = "username",        target = "username"),
        @Mapping(source = "password",        target = "password"),
        @Mapping(source = "apiKey",          target = "apiKey"),
        @Mapping(source = "openApiSpecUrl",  target = "openApiSpecUrl"),
        // raw spec content (openApi) wird nachträglich im Resource-Layer gesetzt
        @Mapping(target = "openApi",         ignore = true)
    })
    SourceSystem toEntity(CreateSourceSystemJsonDTO dto);

    /** Multipart-Form: CreateSourceSystemForm → Entity */
    @Mappings({
        @Mapping(source = "name",            target = "name"),
        @Mapping(source = "description",     target = "description"),
        @Mapping(source = "type",            target = "type"),
        @Mapping(source = "apiUrl",          target = "apiUrl"),
        @Mapping(source = "authType",        target = "authType"),
        @Mapping(source = "username",        target = "username"),
        @Mapping(source = "password",        target = "password"),
        @Mapping(source = "apiKey",          target = "apiKey"),
        @Mapping(source = "openApiSpecUrl",  target = "openApiSpecUrl"),
        @Mapping(target = "openApi",         ignore = true)
    })
    SourceSystem toEntity(CreateSourceSystemForm form);

    /** Update: SourceSystemDto → Entity */
    @Mappings({
        @Mapping(source = "id",              target = "id"),
        @Mapping(source = "name",            target = "name"),
        @Mapping(source = "description",     target = "description"),
        @Mapping(source = "type",            target = "type"),
        @Mapping(source = "apiUrl",          target = "apiUrl"),
        @Mapping(source = "authType",        target = "authType"),
        @Mapping(source = "username",        target = "username"),
        @Mapping(source = "password",        target = "password"),
        @Mapping(source = "apiKey",          target = "apiKey"),
        @Mapping(source = "openApiSpecUrl",  target = "openApiSpecUrl"),
        @Mapping(source = "openApiSpec",     target = "openApi")
    })
    SourceSystem toEntity(SourceSystemDto dto);

    /** Entity → REST-DTO */
    @Mappings({
        @Mapping(source = "openApi",         target = "openApiSpec"),
        @Mapping(source = "openApiSpecUrl",  target = "openApiSpecUrl")
        // alle anderen Felder (id, name, description, type, apiUrl, authType, username, password, apiKey) mappen per Name
    })
    SourceSystemDto toDto(SourceSystem entity);
}
