package de.unistuttgart.stayinsync.core.configuration.edc;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class EDCDataAddress extends PanacheEntity {

    public String jsonLDType;

    public String type;

    public String baseURL;

    public boolean proxyPath;

    public boolean proxyQueryParams;

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
