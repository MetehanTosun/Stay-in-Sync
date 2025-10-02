package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.ApiKeyAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.BasicAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.SyncSystemAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiAuthConfigurationDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ApiKeyAuthDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.BasicAuthDTO;
import de.unistuttgart.stayinsync.transport.dto.ApiAuthConfigurationMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface AuthConfigMapper {
    @Named("mapAuthConfigToEntity")
    default SyncSystemAuthConfig mapToEntity(ApiAuthConfigurationDTO dto) {
        if (dto == null) {
            return null;
        }
        return switch (dto.getAuthType()) {
            case BASIC -> mapBasicAuth((BasicAuthDTO) dto);
            case API_KEY -> mapApiKeyAuth((ApiKeyAuthDTO) dto);
        };
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "syncSystem", ignore = true)
    @Mapping(target = "authType", constant = "BASIC")
    BasicAuthConfig mapBasicAuth(BasicAuthDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "syncSystem", ignore = true)
    @Mapping(target = "authType", constant = "API_KEY")
    ApiKeyAuthConfig mapApiKeyAuth(ApiKeyAuthDTO dto);

    @Named("mapAuthConfigToDTO")
    default ApiAuthConfigurationMessageDTO mapToDTO(SyncSystemAuthConfig entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof ApiKeyAuthConfig apiKeyAuth) {
            return new ApiAuthConfigurationMessageDTO("123", "Authorization"); // TODO: TEMPORARY FIX THIS!!!
        }
        if (entity instanceof BasicAuthConfig basicAuth) {
            return new ApiAuthConfigurationMessageDTO("123", "Authorization"); // TODO: TEMPORARY FIX THIS!!!
        }
        return null;
    }
}
