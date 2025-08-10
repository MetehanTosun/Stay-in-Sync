package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class EDCDataAddressDto {

    private UUID id;

    @NotBlank 
    private String jsonLDType;

    @NotBlank 
    private String type;

    @NotBlank 
    private String baseURL;

    @NotNull  
    private Boolean proxyPath;

    @NotNull  
    private Boolean proxyQueryParams;

    public UUID getId() {
        return id;
    }

    public EDCDataAddressDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getJsonLDType() {
        return jsonLDType;
    }

    public EDCDataAddressDto setJsonLDType(String jsonLDType) {
        this.jsonLDType = jsonLDType;
        return this;
    }

    public String getType() {
        return type;
    }

    public EDCDataAddressDto setType(String type) {
        this.type = type;
        return this;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public EDCDataAddressDto setBaseURL(String baseURL) {
        this.baseURL = baseURL;
        return this;
    }

    public Boolean getProxyPath() {
        return proxyPath;
    }

    public EDCDataAddressDto setProxyPath(Boolean proxyPath) {
        this.proxyPath = proxyPath;
        return this;
    }

    public Boolean getProxyQueryParams() {
        return proxyQueryParams;
    }

    public EDCDataAddressDto setProxyQueryParams(Boolean proxyQueryParams) {
        this.proxyQueryParams = proxyQueryParams;
        return this;
    }
}
