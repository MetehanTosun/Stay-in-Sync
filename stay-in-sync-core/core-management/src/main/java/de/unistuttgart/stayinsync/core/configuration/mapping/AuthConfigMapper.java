package de.unistuttgart.stayinsync.core.configuration.mapping;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.ApiKeyAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.BasicAuthConfig;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.SyncSystemAuthConfig;
import de.unistuttgart.stayinsync.transport.dto.ApiKeyAuthConfigMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.BasicAuthConfigMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncSystemAuthConfigMessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface AuthConfigMapper {

    @SubclassMapping(source = ApiKeyAuthConfig.class, target = ApiKeyAuthConfigMessageDTO.class)
    @SubclassMapping(source = BasicAuthConfig.class, target = BasicAuthConfigMessageDTO.class)
    SyncSystemAuthConfigMessageDTO toDto(SyncSystemAuthConfig entity);

    BasicAuthConfigMessageDTO toBasicDto(BasicAuthConfig entity);

    ApiKeyAuthConfigMessageDTO toApiKeyDto(ApiKeyAuthConfig entity);
}
