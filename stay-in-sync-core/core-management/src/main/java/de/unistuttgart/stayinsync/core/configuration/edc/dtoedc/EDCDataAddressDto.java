package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class EDCDataAddressDto {

    @JsonIgnore
    private UUID id;

    @JsonProperty("jsonLDType")
    private String jsonLDType;

    @NotBlank
    private String type;

    // Unterstützt beide Feldnamen: baseURL und base_url
    @JsonProperty("baseURL")
    @NotBlank
    private String baseURL;

    @JsonProperty("proxyPath")
    private Boolean proxyPath;

    @JsonProperty("proxyQueryParams")
    private Boolean proxyQueryParams;

    public UUID getId() {
        return id;
    }

    public EDCDataAddressDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getJsonLDType() {
        return jsonLDType != null ? jsonLDType : "DataAddress";
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

    // Zusätzlicher Getter für base_url, JSON-Serialisierung berücksichtigt diesen Wert
    @JsonProperty("base_url")
    public String getBaseUrl() {
        return baseURL;
    }

    public EDCDataAddressDto setBaseURL(String baseURL) {
        this.baseURL = baseURL;
        return this;
    }

    // Zusätzlicher Setter für base_url, JSON-Deserialisierung kann diesen Wert verwenden
    @JsonProperty("base_url")
    public EDCDataAddressDto setBaseUrl(String baseUrl) {
        this.baseURL = baseUrl;
        return this;
    }

    public Boolean getProxyPath() {
        return proxyPath != null ? proxyPath : Boolean.TRUE;
    }

    public EDCDataAddressDto setProxyPath(Boolean proxyPath) {
        this.proxyPath = proxyPath;
        return this;
    }

    public Boolean getProxyQueryParams() {
        return proxyQueryParams != null ? proxyQueryParams : Boolean.TRUE;
    }

    public EDCDataAddressDto setProxyQueryParams(Boolean proxyQueryParams) {
        this.proxyQueryParams = proxyQueryParams;
        return this;
    }
    
    @Override
    public String toString() {
        return "EDCDataAddressDto{" +
                "id=" + id +
                ", jsonLDType='" + jsonLDType + '\'' +
                ", type='" + type + '\'' +
                ", baseURL='" + baseURL + '\'' +
                ", proxyPath=" + proxyPath +
                ", proxyQueryParams=" + proxyQueryParams +
                '}';
    }
}
