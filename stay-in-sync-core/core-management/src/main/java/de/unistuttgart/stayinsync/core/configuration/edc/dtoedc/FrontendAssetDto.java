package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/**
 * Repräsentiert das Asset-Format aus dem Frontend
 */
public class FrontendAssetDto {
    
    @JsonProperty("@context")
    private Map<String, String> context;
    
    @JsonProperty("@id")
    private String id;
    
    private Map<String, String> properties;
    
    private FrontendDataAddressDto dataAddress;
    
    // Getter und Setter
    
    public Map<String, String> getContext() {
        return context;
    }
    
    public FrontendAssetDto setContext(Map<String, String> context) {
        this.context = context;
        return this;
    }
    
    public String getId() {
        return id;
    }
    
    public FrontendAssetDto setId(String id) {
        this.id = id;
        return this;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public FrontendAssetDto setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }
    
    public FrontendDataAddressDto getDataAddress() {
        return dataAddress;
    }
    
    public FrontendAssetDto setDataAddress(FrontendDataAddressDto dataAddress) {
        this.dataAddress = dataAddress;
        return this;
    }
    
    /**
     * Konvertiert das Frontend-Format in das Backend-Format
     */
    public EDCAssetDto toBackendDto(UUID targetEDCId) {
        String assetId = this.id != null && !this.id.isEmpty() ? this.id : null;
        String name = this.properties != null ? this.properties.get("asset:prop:name") : null;
        String description = this.properties != null ? this.properties.get("asset:prop:description") : null;
        String contentType = this.properties != null ? this.properties.get("asset:prop:contenttype") : "application/json";
        
        EDCDataAddressDto dataAddressDto = null;
        if (this.dataAddress != null) {
            dataAddressDto = new EDCDataAddressDto()
                    .setJsonLDType("DataAddress")
                    .setType(this.dataAddress.getType())
                    .setBaseURL(this.dataAddress.getBaseUrl())
                    .setProxyPath(true)
                    .setProxyQueryParams(true);
        }
        
        EDCPropertyDto propertyDto = null;
        if (description != null) {
            propertyDto = new EDCPropertyDto()
                    .setDescription(description);
        }
        
        return new EDCAssetDto(
                null, // ID wird automatisch generiert
                assetId != null ? assetId : "asset-" + UUID.randomUUID().toString(),
                this.dataAddress != null ? this.dataAddress.getBaseUrl() : "",
                this.dataAddress != null ? this.dataAddress.getType() : "HttpData",
                contentType != null ? contentType : "application/json",
                description,
                targetEDCId,
                dataAddressDto,
                propertyDto
        );
    }
    
    /**
     * Konvertiert ein Backend-DTO in das Frontend-Format
     */
    public static FrontendAssetDto fromBackendDto(EDCAssetDto backendDto) {
        FrontendAssetDto frontendDto = new FrontendAssetDto();
        
        // Context setzen
        Map<String, String> context = Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/");
        frontendDto.setContext(context);
        
        // ID setzen
        frontendDto.setId(backendDto.assetId());
        
        // Properties setzen
        Map<String, String> properties = Map.of(
                "asset:prop:name", backendDto.assetId(),
                "asset:prop:description", backendDto.description() != null ? backendDto.description() : "",
                "asset:prop:contenttype", backendDto.contentType(),
                "asset:prop:version", "1.0.0"
        );
        frontendDto.setProperties(properties);
        
        // DataAddress setzen
        FrontendDataAddressDto dataAddress = new FrontendDataAddressDto()
                .setType(backendDto.dataAddress().getType())
                .setBaseUrl(backendDto.dataAddress().getBaseURL());
        frontendDto.setDataAddress(dataAddress);
        
        return frontendDto;
    }
}
