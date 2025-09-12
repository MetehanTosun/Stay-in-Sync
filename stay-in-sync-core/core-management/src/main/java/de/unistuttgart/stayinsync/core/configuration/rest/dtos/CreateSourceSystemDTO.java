package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.unistuttgart.stayinsync.core.transport.domain.ApiAuthType;
import jakarta.validation.constraints.NotNull;

public record CreateSourceSystemDTO(Long id, @NotNull String name, @NotNull String apiUrl, String description,
                                    @NotNull String apiType,
                                    ApiAuthType apiAuthType,
                                    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "authType")
                                    @JsonSubTypes({
                                            @JsonSubTypes.Type(value = BasicAuthDTO.class, name = "BASIC"),
                                            @JsonSubTypes.Type(value = ApiKeyAuthDTO.class, name = "API_KEY")
                                    })
                                    ApiAuthConfigurationDTO authConfig,
                                    String openApiSpec) {
}