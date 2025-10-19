package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;

public record EDCContractDefinitionDto(
    Long id,
    
    @NotBlank
    String contractDefinitionId,
    
    @NotBlank
    String assetId,
    
    String rawJson,
    
    // Policy IDs (Long for database ID, String for direct policy ID)
    Long accessPolicyId,      // ID des EDCAccessPolicy–Datensatzes
    String accessPolicyIdStr, // String version of policy ID from frontend
    
    Long contractPolicyId,    // ID der ContractPolicy (ebenfalls EDCAccessPolicy)
    String contractPolicyIdStr // String version of policy ID from frontend
) {
    // Constructor with builder pattern methods for backward compatibility
    public static class Builder {
        private Long id;
        private String contractDefinitionId;
        private String assetId;
        private String rawJson;
        private Long accessPolicyId;
        private String accessPolicyIdStr;
        private Long contractPolicyId;
        private String contractPolicyIdStr;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder contractDefinitionId(String contractDefinitionId) {
            this.contractDefinitionId = contractDefinitionId;
            return this;
        }

        public Builder assetId(String assetId) {
            this.assetId = assetId;
            return this;
        }

        public Builder rawJson(String rawJson) {
            this.rawJson = rawJson;
            return this;
        }

        public Builder accessPolicyId(Long accessPolicyId) {
            this.accessPolicyId = accessPolicyId;
            return this;
        }

        public Builder accessPolicyIdStr(String accessPolicyIdStr) {
            this.accessPolicyIdStr = accessPolicyIdStr;
            return this;
        }

        public Builder contractPolicyId(Long contractPolicyId) {
            this.contractPolicyId = contractPolicyId;
            return this;
        }

        public Builder contractPolicyIdStr(String contractPolicyIdStr) {
            this.contractPolicyIdStr = contractPolicyIdStr;
            return this;
        }

        public EDCContractDefinitionDto build() {
            return new EDCContractDefinitionDto(
                id,
                contractDefinitionId,
                assetId,
                rawJson,
                accessPolicyId,
                accessPolicyIdStr,
                contractPolicyId,
                contractPolicyIdStr
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
