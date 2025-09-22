package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.unistuttgart.stayinsync.core.transport.domain.ApiAuthType;
import jakarta.validation.constraints.NotNull;

public record CreateSourceSystemDTO(Long id, @NotNull String name, @NotNull String apiUrl, String description,
                                    @NotNull String apiType,
                                    String aasId,
                                    ApiAuthType apiAuthType,
                                    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "authType")
                                    @JsonSubTypes({
                                            @JsonSubTypes.Type(value = BasicAuthDTO.class, name = "BASIC"),
                                            @JsonSubTypes.Type(value = ApiKeyAuthDTO.class, name = "API_KEY")
                                    })
                                    ApiAuthConfigurationDTO authConfig,
                                    String openApiSpec) {

    public CreateSourceSystemDTO(Long id, String name, String apiUrl, String description,
                                 String apiType,
                                 ApiAuthType apiAuthType,
                                 ApiAuthConfigurationDTO authConfig,
                                 String openApiSpec) {
        this(id, name, apiUrl, description, apiType, null, apiAuthType, authConfig, openApiSpec);
    }
}