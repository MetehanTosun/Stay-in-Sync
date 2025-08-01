package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EDCContractDefinitionDto {

    private Long id;

    @NotBlank
    private String contractDefinitionId;

    @NotNull
    private Long assetId;

    @NotNull
    private Long accessPolicyId;      // hier wird die ID des EDCAccessPolicy–Datensatzes erwartet

    @NotNull
    private Long contractPolicyId;    // und hier die ID der ContractPolicy (ebenfalls EDCAccessPolicy)

    // --- Getter & Fluent‑Setter ---

    public Long getId() {
        return id;
    }

    public EDCContractDefinitionDto setId(Long id) {
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

    public Long getAssetId() {
        return assetId;
    }

    public EDCContractDefinitionDto setAssetId(Long assetId) {
        this.assetId = assetId;
        return this;
    }

    public Long getAccessPolicyId() {
        return accessPolicyId;
    }

    public EDCContractDefinitionDto setAccessPolicyId(Long accessPolicyId) {
        this.accessPolicyId = accessPolicyId;
        return this;
    }

    public Long getContractPolicyId() {
        return contractPolicyId;
    }

    public EDCContractDefinitionDto setContractPolicyId(Long contractPolicyId) {
        this.contractPolicyId = contractPolicyId;
        return this;
    }
}
