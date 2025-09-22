package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.unistuttgart.stayinsync.core.transport.domain.ApiAuthType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "authType", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasicAuthDTO.class, name = "BASIC"),
        @JsonSubTypes.Type(value = ApiKeyAuthDTO.class, name = "API_KEY")
})
public interface ApiAuthConfigurationDTO {
    
    ApiAuthType getAuthType();
}
