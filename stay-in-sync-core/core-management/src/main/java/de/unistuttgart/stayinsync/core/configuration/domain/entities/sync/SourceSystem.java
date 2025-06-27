package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;

import java.util.Set;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystem extends SyncSystem {
    
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