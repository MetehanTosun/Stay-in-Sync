package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for {@link de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAccessPolicyPermission}
 */
public record EDCAccessPolicyPermissionDto(
    Long id,
    
    @NotBlank
    String action,
    
    @NotBlank
    String constraintLeftOperand,
    
    @NotBlank
    String constraintOperator,
    
    @NotBlank
    String constraintRightOperand
) {
    /**
     * Builder for creating instances of {@link EDCAccessPolicyPermissionDto}
     * for backward compatibility
     */
    public static class Builder {
        private Long id;
        private String action;
        private String constraintLeftOperand;
        private String constraintOperator;
        private String constraintRightOperand;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder constraintLeftOperand(String constraintLeftOperand) {
            this.constraintLeftOperand = constraintLeftOperand;
            return this;
        }

        public Builder constraintOperator(String constraintOperator) {
            this.constraintOperator = constraintOperator;
            return this;
        }

        public Builder constraintRightOperand(String constraintRightOperand) {
            this.constraintRightOperand = constraintRightOperand;
            return this;
        }

        public EDCAccessPolicyPermissionDto build() {
            return new EDCAccessPolicyPermissionDto(
                id,
                action,
                constraintLeftOperand,
                constraintOperator,
                constraintRightOperand
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
