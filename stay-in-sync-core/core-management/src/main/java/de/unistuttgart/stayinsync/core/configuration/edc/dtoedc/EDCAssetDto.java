package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class EDCAssetDto {

    public UUID id;

    @NotBlank
    public String assetId;

    @NotBlank
    public String url;

    @NotBlank
    public String type;

    @NotBlank
    public String contentType;

    public String description;

    @NotNull
    public UUID targetEDCId;

        @NotNull
    private EDCDataAddressDto dataAddress;   // <--- NEU

    private EDCPropertyDto properties; 

    public EDCDataAddressDto getDataAddress() {
        return dataAddress;
    }

    public void setDataAddress(EDCDataAddressDto dataAddress) {
        this.dataAddress = dataAddress;
    }

    public EDCPropertyDto getProperties() {
        return properties;
    }

    public void setProperties(EDCPropertyDto properties) {
        this.properties = properties;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getTargetEDCId() {
        return targetEDCId;
    }

    public void setTargetEDCId(UUID targetEDCId) {
        this.targetEDCId = targetEDCId;
    }
}
