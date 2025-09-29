package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO zur Erstellung einer Policy im EDC.
 * Diese Klasse repräsentiert die Datenstruktur, die zum Anlegen einer Policy 
 * im Eclipse Dataspace Connector (EDC) benötigt wird.
 * Basierend auf dem EDC PolicyDefinitionRequestDto Format.
 */
public class CreateEDCPolicyDTO {

    @JsonProperty("@id")
    private String id = "policy-" + UUID.randomUUID().toString();

    @JsonProperty("@context")
    private Map<String, String> context = new HashMap<>();
    {
        context.put("odrl", "http://www.w3.org/ns/odrl/2/");
    }

    @JsonProperty("@type")
    private String type = "PolicyDefinitionRequestDto";

    @JsonProperty("policy")
    private PolicyDTO policy = new PolicyDTO();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PolicyDTO getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyDTO policy) {
        this.policy = policy;
    }
    
    /**
     * Hilfsmethode, um auf die Constraint zuzugreifen.
     * Die Methode navigiert durch die verschachtelte Struktur.
     */
    public ConstraintDTO getConstraint() {
        if (policy != null && 
            policy.getPermission() != null && 
            policy.getPermission().size() > 0 && 
            policy.getPermission().get(0).getConstraint() != null) {
                
            return policy.getPermission().get(0).getConstraint();
        }
        return null;
    }

    /**
     * DTO für die Policy, die im EDC verwendet wird.
     */
    public static class PolicyDTO {
        @JsonProperty("@type")
        private String type = "odrl:Set";
        
        @JsonProperty("odrl:permission")
        private List<PermissionDTO> permission = new ArrayList<>();
        
        public PolicyDTO() {
            // Ein Standard-Permission-Objekt erstellen und zur Liste hinzufügen
            permission.add(new PermissionDTO());
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<PermissionDTO> getPermission() {
            return permission;
        }

        public void setPermission(List<PermissionDTO> permission) {
            this.permission = permission;
        }
    }

    /**
     * DTO für die Berechtigungen einer Policy.
     */
    public static class PermissionDTO {
        @JsonProperty("odrl:action")
        private String action = "USE";

        @JsonProperty("odrl:constraint")
        private ConstraintDTO constraint = new ConstraintDTO();

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
     * Implementiert die odrl:or Logik mit einem einzelnen Constraint.
     */
    public static class ConstraintDTO {
        @JsonProperty("@type")
        private String type = "LogicalConstraint";
        
        @JsonProperty("odrl:or")
        private List<InnerConstraintDTO> or = new ArrayList<>();
        
        public ConstraintDTO() {
            // Eine Standard-InnerConstraint erstellen und zur Liste hinzufügen
            or.add(new InnerConstraintDTO());
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
        
        public List<InnerConstraintDTO> getOr() {
            return or;
        }
        
        public void setOr(List<InnerConstraintDTO> or) {
            this.or = or;
        }
        
        /**
         * Hilfsmethode, um den rechten Operanden des ersten Constraints zu setzen
         */
        public void setRightOperand(String rightOperand) {
            if (!or.isEmpty()) {
                or.get(0).setRightOperand(rightOperand);
            }
        }
        
        /**
         * Hilfsmethode, um den rechten Operanden des ersten Constraints zu bekommen
         */
        public String getRightOperand() {
            if (!or.isEmpty()) {
                return or.get(0).getRightOperand();
            }
            return "";
        }
        
        /**
         * Hilfsmethode, um den linken Operanden des ersten Constraints zu setzen
         */
        public void setLeftOperand(String leftOperand) {
            if (!or.isEmpty()) {
                or.get(0).setLeftOperand(leftOperand);
            }
        }
        
        /**
         * Hilfsmethode, um den linken Operanden des ersten Constraints zu bekommen
         */
        public String getLeftOperand() {
            if (!or.isEmpty()) {
                InnerConstraintDTO inner = or.get(0);
                if (inner.leftOperand instanceof String) {
                    return (String)inner.leftOperand;
                } else if (inner.leftOperand instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>)inner.leftOperand;
                    return map.containsKey("@id") ? map.get("@id").toString() : "BusinessPartnerNumber";
                }
            }
            return "BusinessPartnerNumber";
        }
        
        /**
         * Hilfsmethode, um den Operator des ersten Constraints zu bekommen
         */
        public OperatorDTO getOperator() {
            if (!or.isEmpty()) {
                return or.get(0).getOperator();
            }
            return new OperatorDTO();
        }
    }
    
    /**
     * DTO für einen einzelnen Constraint innerhalb des odrl:or.
     */
    public static class InnerConstraintDTO {
        @JsonProperty("@type")
        private String type = "Constraint";
        
        @JsonProperty("odrl:leftOperand")
        private Object leftOperand = new HashMap<String, String>() {{ 
            put("@id", "BusinessPartnerNumber"); 
        }};
        
        @JsonProperty("odrl:operator")
        private OperatorDTO operator = new OperatorDTO();
        
        @JsonProperty("odrl:rightOperand")
        private String rightOperand = "";
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Object getLeftOperand() {
            return leftOperand;
        }
        
        public void setLeftOperand(String leftOperand) {
            if (leftOperand != null && !leftOperand.isEmpty()) {
                this.leftOperand = new HashMap<String, String>() {{ 
                    put("@id", leftOperand); 
                }};
            } else {
                this.leftOperand = new HashMap<String, String>() {{ 
                    put("@id", "BusinessPartnerNumber"); 
                }};
            }
        }
        
        public OperatorDTO getOperator() {
            return operator;
        }
        
        public void setOperator(OperatorDTO operator) {
            this.operator = operator;
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
        private String id = "odrl:eq";

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}