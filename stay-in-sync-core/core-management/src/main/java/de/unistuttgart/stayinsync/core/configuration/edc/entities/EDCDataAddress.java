package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class EDCDataAddress extends UuidEntity {

    @Column(name = "jsonld_type", nullable = false)
    public String jsonLDType;

    @Column(name = "type", nullable = false)
    public String type;

    @Column(name = "base_url", nullable = false)
    public String baseURL;

    @Column(name = "proxy_path", nullable = false)
    public boolean proxyPath;

    @Column(name = "proxy_query_params", nullable = false)
    public boolean proxyQueryParams;

    // Getter/Setter

    public String getJsonLDType() {
        return jsonLDType;
    }

    public void setJsonLDType(String jsonLDType) {
        this.jsonLDType = jsonLDType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public boolean isProxyPath() {
        return proxyPath;
    }

    public void setProxyPath(boolean proxyPath) {
        this.proxyPath = proxyPath;
    }

    public boolean isProxyQueryParams() {
        return proxyQueryParams;
    }

    public void setProxyQueryParams(boolean proxyQueryParams) {
        this.proxyQueryParams = proxyQueryParams;
    }
}
