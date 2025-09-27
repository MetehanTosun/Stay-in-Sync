package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataAddressDTO {

    @JsonProperty("@type")
    private String primaryType = "DataAddress";

    @JsonProperty("type")
    private String secondaryType = "HttpData";

    private String baseUrl = "http://dataprovider-submodelserver.tx.test";


    private String proxyPath = "true";

    private String proxyMethod = "true";
    private String proxyQueryParams = "true";
    private String proxyBody = "true";

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    public String getSecondaryType() {
        return secondaryType;
    }

    public void setSecondaryType(String secondaryType) {
        this.secondaryType = secondaryType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getProxyPath() {
        return proxyPath;
    }

    public void setProxyPath(String proxyPath) {
        this.proxyPath = proxyPath;
    }

    public String getProxyMethod() {
        return proxyMethod;
    }

    public void setProxyMethod(String proxyMethod) {
        this.proxyMethod = proxyMethod;
    }

    public String getProxyQueryParams() {
        return proxyQueryParams;
    }

    public void setProxyQueryParams(String proxyQueryParams) {
        this.proxyQueryParams = proxyQueryParams;
    }

    public String getProxyBody() {
        return proxyBody;
    }

    public void setProxyBody(String proxyBody) {
        this.proxyBody = proxyBody;
    }
}
