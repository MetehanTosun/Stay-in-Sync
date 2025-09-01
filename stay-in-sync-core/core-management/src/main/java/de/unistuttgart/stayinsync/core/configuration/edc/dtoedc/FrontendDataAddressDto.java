package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

/**
 * Repräsentiert das DataAddress-Format aus dem Frontend
 */
public class FrontendDataAddressDto {
    
    private String type;
    private String baseUrl;
    
    // Getter und Setter
    
    public String getType() {
        return type;
    }
    
    public FrontendDataAddressDto setType(String type) {
        this.type = type;
        return this;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public FrontendDataAddressDto setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }
}
