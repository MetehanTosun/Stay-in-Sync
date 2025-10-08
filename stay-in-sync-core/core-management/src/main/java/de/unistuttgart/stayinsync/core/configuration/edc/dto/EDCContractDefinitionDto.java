package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class EDCContractDefinitionDto {

    private UUID id;

    @NotBlank
    private String contractDefinitionId;

    @NotBlank
    private String assetId;

    // Either UUID or String format is required for policy IDs
    private UUID accessPolicyId;      // ID des EDCAccessPolicy–Datensatzes
    private String accessPolicyIdStr; // String version of policy ID from frontend

    private UUID contractPolicyId;    // ID der ContractPolicy (ebenfalls EDCAccessPolicy)
    private String contractPolicyIdStr; // String version of policy ID from frontend

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

    public String getAssetId() {
        return assetId;
    }

    public EDCContractDefinitionDto setAssetId(String assetId) {
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

    public String getAccessPolicyIdStr() {
        return accessPolicyIdStr;
    }

    public EDCContractDefinitionDto setAccessPolicyIdStr(String accessPolicyIdStr) {
        this.accessPolicyIdStr = accessPolicyIdStr;
        return this;
    }

    public UUID getContractPolicyId() {
        return contractPolicyId;
    }

    public EDCContractDefinitionDto setContractPolicyId(UUID contractPolicyId) {
        this.contractPolicyId = contractPolicyId;
        return this;
    }

    public String getContractPolicyIdStr() {
        return contractPolicyIdStr;
    }

    public EDCContractDefinitionDto setContractPolicyIdStr(String contractPolicyIdStr) {
        this.contractPolicyIdStr = contractPolicyIdStr;
        return this;
    }
}
