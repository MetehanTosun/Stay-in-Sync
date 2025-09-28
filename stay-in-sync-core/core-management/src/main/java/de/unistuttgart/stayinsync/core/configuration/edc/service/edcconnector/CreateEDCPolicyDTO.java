package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * DTO zur Erstellung einer Policy im EDC.
 * Diese Klasse repräsentiert die Datenstruktur, die zum Anlegen einer Policy 
 * im Eclipse Dataspace Connector (EDC) benötigt wird.
 */
public class CreateEDCPolicyDTO {

    @JsonProperty("@id")
    String id = "policy-" + UUID.randomUUID().toString();

    @JsonProperty("@context")
    ContextDTO context = new ContextDTO();

    @JsonProperty("@type")
    String type = "Policy";

    @JsonProperty("odrl:permission")
    PermissionDTO permission = new PermissionDTO();

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PermissionDTO getPermission() {
        return permission;
    }

    public void setPermission(PermissionDTO permission) {
        this.permission = permission;
    }

    /**
     * DTO für die Berechtigungen einer Policy.
     */
    public static class PermissionDTO {
        @JsonProperty("odrl:action")
        String action = "USE";

        @JsonProperty("odrl:constraint")
        ConstraintDTO constraint = new ConstraintDTO();

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public ConstraintDTO getConstraint() {
            return constraint;
        }

        public void setConstraint(ConstraintDTO constraint) {
            this.constraint = constraint;
        }
    }

    /**
     * DTO für die Einschränkungen einer Policy.
     */
    public static class ConstraintDTO {
        @JsonProperty("@type")
        String type = "LogicalConstraint";

        @JsonProperty("odrl:operator")
        OperatorDTO operator = new OperatorDTO();

        @JsonProperty("odrl:leftOperand")
        String leftOperand = "BusinessPartnerNumber";

        @JsonProperty("odrl:rightOperand")
        String rightOperand = "";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public OperatorDTO getOperator() {
            return operator;
        }

        public void setOperator(OperatorDTO operator) {
            this.operator = operator;
        }

        public String getLeftOperand() {
            return leftOperand;
        }

        public void setLeftOperand(String leftOperand) {
            this.leftOperand = leftOperand;
        }

        public String getRightOperand() {
            return rightOperand;
        }

        public void setRightOperand(String rightOperand) {
            this.rightOperand = rightOperand;
        }
    }

    /**
     * DTO für den Operator einer Constraint.
     */
    public static class OperatorDTO {
        @JsonProperty("@id")
        String id = "odrl:eq";

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}