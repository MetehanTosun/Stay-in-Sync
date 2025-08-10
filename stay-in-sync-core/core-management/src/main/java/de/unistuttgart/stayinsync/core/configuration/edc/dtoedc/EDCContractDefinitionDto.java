package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class EDCContractDefinitionDto {

    private UUID id;

    @NotBlank
    private String contractDefinitionId;

    @NotNull
    private UUID assetId;

    @NotNull
    private UUID accessPolicyId;      // ID des EDCAccessPolicy–Datensatzes

    @NotNull
    private UUID contractPolicyId;    // ID der ContractPolicy (ebenfalls EDCAccessPolicy)

    // --- Getter & Fluent-Setter ---

    public UUID getId() {
        return id;
    }

    public EDCContractDefinitionDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getContractDefinitionId() {
        return contractDefinitionId;
    }

    public EDCContractDefinitionDto setContractDefinitionId(String contractDefinitionId) {
        this.contractDefinitionId = contractDefinitionId;
        return this;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public EDCContractDefinitionDto setAssetId(UUID assetId) {
        this.assetId = assetId;
        return this;
    }

    public UUID getAccessPolicyId() {
        return accessPolicyId;
    }

    public EDCContractDefinitionDto setAccessPolicyId(UUID accessPolicyId) {
        this.accessPolicyId = accessPolicyId;
        return this;
    }

    public UUID getContractPolicyId() {
        return contractPolicyId;
    }

    public EDCContractDefinitionDto setContractPolicyId(UUID contractPolicyId) {
        this.contractPolicyId = contractPolicyId;
        return this;
    }
}
