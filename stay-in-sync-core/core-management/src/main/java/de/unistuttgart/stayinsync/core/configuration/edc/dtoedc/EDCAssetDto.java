package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public class EDCAssetDto {

    @Null(groups = Create.class)
    @NotNull(groups = Update.class)
    private Long id;

    @NotBlank
    private String assetId;

    private Long dataAddressId;
    private Long propertiesId;

    private Set<@NotNull Long> accessPolicyIds;

    private Long targetSystemEndpointId;
    private Long targetEDCId;

    // Marker‑Interfaces
    public interface Create {}
    public interface Update {}

    // Getter & Fluent‑Setter
    public Long getId() { return id; }
    public EDCAssetDto setId(Long id) { this.id = id; return this; }

    public String getAssetId() { return assetId; }
    public EDCAssetDto setAssetId(String assetId) { this.assetId = assetId; return this; }

    public Long getDataAddressId() { return dataAddressId; }
    public EDCAssetDto setDataAddressId(Long dataAddressId) { this.dataAddressId = dataAddressId; return this; }

    public Long getPropertiesId() { return propertiesId; }
    public EDCAssetDto setPropertiesId(Long propertiesId) { this.propertiesId = propertiesId; return this; }

    public Set<Long> getAccessPolicyIds() { return accessPolicyIds; }
    public EDCAssetDto setAccessPolicyIds(Set<Long> accessPolicyIds) { this.accessPolicyIds = accessPolicyIds; return this; }

    public Long getTargetSystemEndpointId() { return targetSystemEndpointId; }
    public EDCAssetDto setTargetSystemEndpointId(Long targetSystemEndpointId) { 
        this.targetSystemEndpointId = targetSystemEndpointId; 
        return this; 
    }

    public Long getTargetEDCId() { return targetEDCId; }
    public EDCAssetDto setTargetEDCId(Long targetEDCId) { this.targetEDCId = targetEDCId; return this; }
}
