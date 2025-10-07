package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class CreateEDCAssetDTO {

    @JsonProperty("@id")
    String id = "asset-" + UUID.randomUUID().toString();

    @JsonProperty("@context")
    ContextDTO context = new ContextDTO();

    DataAddressDTO dataAddress = new DataAddressDTO();

    PropertiesDTO properties = new PropertiesDTO();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ContextDTO getContext() {
        return context;
    }

    public void setContext(ContextDTO context) {
        this.context = context;
    }

    public DataAddressDTO getDataAddress() {
        return dataAddress;
    }

    public void setDataAddress(DataAddressDTO dataAddress) {
        this.dataAddress = dataAddress;
    }

    public PropertiesDTO getProperties() {
        return properties;
    }

    public void setProperties(PropertiesDTO properties) {
        this.properties = properties;
    }
}
