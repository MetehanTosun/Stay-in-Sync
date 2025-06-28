package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;

import java.util.Set;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemType;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystem extends SyncSystem {
    
    /**
     * Type of authentication for this source system (e.g., BASIC, API_KEY).
     */
    @Column(name = "auth_type")
    public String authType;

    /**
     * Username for BASIC authentication.
     */
    @Column(name = "auth_username")
    public String username;

    /**
     * Password for BASIC authentication.
     */
    @Column(name = "auth_password")
    public String password;

    /**
     * API key for API_KEY authentication.
     */
    @Column(name = "auth_api_key")
    public String apiKey;

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Type of this source system (AAS or REST_OPENAPI).
     */
    @Column(name = "type")
    public SourceSystemType type;

    public SourceSystemType getType() {
        return type;
    }

    public void setType(SourceSystemType type) {
        this.type = type;
    }

    @OneToMany(mappedBy = "sourceSystem")
    public Set<SourceSystemEndpoint> sourceSystemEndpoint;

    /**
     * The raw OpenAPI specification uploaded by the user.
     */
    @Column(name = "open_api_spec", columnDefinition = "TEXT")
    public String openApi;

    public String getOpenApi() {
        return openApi;
    }

    public void setOpenApi(String openApi) {
        this.openApi = openApi;
    }

    /**
     * URL to the OpenAPI specification of this SourceSystem.
     */
    @Column(name = "openapi_spec_url")
    public String openApiSpecUrl;
   
    public String getOpenApiSpecUrl() {
        return openApiSpecUrl;
    }

    public void setOpenApiSpecUrl(String openApiSpecUrl) {
        this.openApiSpecUrl = openApiSpecUrl;
    }
}