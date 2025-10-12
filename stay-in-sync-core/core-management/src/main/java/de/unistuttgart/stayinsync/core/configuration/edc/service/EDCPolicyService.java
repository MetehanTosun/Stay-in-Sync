package de.unistuttgart.stayinsync.core.configuration.edc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector.CreateEDCPolicyDTO;
import de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector.EDCClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCContractDefinition;


/**
 * EDCPolicyService interacts with the EDC database and offers methods for the important CRUD-Operations:
 * GET, GET ALL, POST, PUT, DELETE.
 */
@ApplicationScoped
public class EDCPolicyService {

    private static final Logger LOG = Logger.getLogger(EDCPolicyService.class);
    
    @PersistenceContext
    EntityManager entityManager;
    
    @Inject
    ObjectMapper objectMapper;
    
    public EDCClient createClient(String baseUrl) {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUrl))
                .build(EDCClient.class);
    }

    /**
     * Returns all policies that are associated with a specific EDC instance.
     *
     * @param edcId the UUID of the EDC instance
     * @return List with all policies for that EDC instance
     */
    public List<EDCPolicy> listAllByEdcId(final UUID edcId) {
        LOG.info("Fetching policies for EDC: " + edcId);
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            LOG.warn("No EDC instance found with id " + edcId);
            return new ArrayList<>();
        }
        
        TypedQuery<EDCPolicy> query = entityManager.createQuery(
                "SELECT p FROM EDCPolicy p WHERE p.edcInstance.id = :edcId", 
                EDCPolicy.class);
        query.setParameter("edcId", edcId);
        
        List<EDCPolicy> policyList = query.getResultList();
        LOG.info("Found " + policyList.size() + " policies for EDC " + edcId);
        return policyList;
    }
    
    /**
     * Returns a policy found in the database with the id and belonging to the specific EDC instance.
     *
     * @param id used to find the policy
     * @param edcId the UUID of the EDC instance
     * @return found policy or empty if not found
     */
    public Optional<EDCPolicy> findByIdAndEdcId(final UUID id, final UUID edcId) {
        LOG.info("Fetching policy " + id + " for EDC: " + edcId);
        TypedQuery<EDCPolicy> query = entityManager.createQuery(
                "SELECT p FROM EDCPolicy p WHERE p.id = :policyId AND p.edcInstance.id = :edcId", 
                EDCPolicy.class);
        query.setParameter("policyId", id);
        query.setParameter("edcId", edcId);
        
        List<EDCPolicy> results = query.getResultList();
        if (results.isEmpty()) {
            LOG.warn("No policy found with id " + id + " for EDC " + edcId);
            return Optional.empty();
        }
        
        return Optional.of(results.get(0));
    }

    /**
     * Moves policy into the database and returns it to the caller.
     * Also sends the policy to the EDC API if the EDC instance has a valid management URL.
     * 
     * @param policy to be persisted.
     * @return the created EDCPolicy.
     */
    @Transactional
    public EDCPolicy create(final EDCPolicy policy) {
        // Log important information before persisting
        LOG.info("Creating policy with ID: " + policy.id + ", policyId: " + policy.getPolicyId());
        LOG.info("Policy has EDC instance: " + (policy.getEdcInstance() != null));
        if (policy.getEdcInstance() != null) {
            LOG.info("EDC instance ID: " + policy.getEdcInstance().id);
        }
        
        // Persist policy in database
        policy.persist();
        
        // Send to EDC if possible
        LOG.info("Checking if policy has EDC instance: " + (policy.getEdcInstance() != null));
        if (policy.getEdcInstance() != null) {
            try {
                // Log EDC instance details
                LOG.info("EDC instance found: ID=" + policy.getEdcInstance().id + 
                       ", Name=" + policy.getEdcInstance().getName() + 
                       ", URL=" + policy.getEdcInstance().getControlPlaneManagementUrl() +
                       ", PolicyEndpoint=" + policy.getEdcInstance().getEdcPolicyEndpoint());
                       
                // Create EDC client with URL from EDC instance or fallback to hardcoded URL
                String edcUrl = policy.getEdcInstance().getControlPlaneManagementUrl();
                EDCClient client;
                
                if (edcUrl != null && !edcUrl.trim().isEmpty()) {
                    LOG.info("Using EDC URL from database: " + edcUrl);
                    client = createClient(edcUrl);
                } else {
                    LOG.info("Using hardcoded EDC URL");
                    client = createClient("http://dataprovider-controlplane.tx.test/management/v3");
                }
                
                // Create EDC Policy DTO
                CreateEDCPolicyDTO edcPolicyDTO;
                
                // First, ensure we have a valid policy ID
                if (policy.getPolicyId() == null || policy.getPolicyId().trim().isEmpty()) {
                    // Generate a policy ID if none exists
                    String generatedId = "policy-" + System.currentTimeMillis();
                    policy.setPolicyId(generatedId);
                    // Update the stored entity with the new ID
                    policy.persist();
                    LOG.info("Generated policy ID: " + generatedId);
                }
                
                edcPolicyDTO = null;
                
                // Try to create the DTO from policy JSON first
                try {
                    // The full EDC policy JSON should be usable directly
                    String policyJson = policy.getPolicyJson();
                    
                    if (policyJson != null && !policyJson.trim().isEmpty()) {
                        // Replace any placeholder for policy ID with the actual ID
                        policyJson = policyJson.replace("{{POLICY_ID}}", policy.getPolicyId());
                        
                        LOG.info("Attempting to parse policy JSON directly to EDC format");
                        edcPolicyDTO = objectMapper.readValue(policyJson, CreateEDCPolicyDTO.class);
                        LOG.info("Successfully parsed policy JSON to EDC format");
                        
                        // Ensure the ID is set correctly
                        edcPolicyDTO.setId(policy.getPolicyId());
                    } 
                } catch (Exception e) {
                    LOG.warn("Could not parse policy JSON directly to EDC format", e);
                }
                
                // If direct parsing failed, create default DTO and try to extract values
                if (edcPolicyDTO == null) {
                    LOG.info("Creating default policy DTO");
                    edcPolicyDTO = new CreateEDCPolicyDTO();
                    edcPolicyDTO.setId(policy.getPolicyId());
                    
                    // Try to extract values from policy JSON
                    if (policy.getPolicyJson() != null && !policy.getPolicyJson().trim().isEmpty()) {
                        try {
                            // Try to populate the DTO with values from the policy JSON
                            @SuppressWarnings("unchecked")
                            Map<String, Object> policyMap = objectMapper.readValue(policy.getPolicyJson(), Map.class);
                            
                            // If there's a "policy" property in the JSON (as shown in your example), extract it
                            if (policyMap.containsKey("policy") && policyMap.get("policy") instanceof Map<?,?>) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> innerPolicyMap = (Map<String, Object>) policyMap.get("policy");
                                
                                // Process the inner policy map
                                processComplexPolicyStructure(edcPolicyDTO, innerPolicyMap);
                            } else {
                                // Process the top-level map (might be the policy directly)
                                processComplexPolicyStructure(edcPolicyDTO, policyMap);
                            }
                            
                        } catch (Exception e) {
                            LOG.warn("Could not parse policy JSON to extract values, using defaults", e);
                        }
                    } else {
                        // No valid JSON, set defaults
                        LOG.warn("Policy JSON is null or empty, using default values");
                        edcPolicyDTO.getConstraint().setRightOperand("BPNL000000000000");
                    }
                }
                
                // Replace any placeholder for consumer ID if needed
                String rightOperand = edcPolicyDTO.getConstraint().getRightOperand();
                if (rightOperand != null && rightOperand.contains("{{CONSUMER_ID}}")) {
                    // Replace with a default or extract from somewhere
                    String consumerId = "BPNL000000000000"; // Default value
                    edcPolicyDTO.getConstraint().setRightOperand(
                        rightOperand.replace("{{CONSUMER_ID}}", consumerId)
                    );
                    LOG.info("Replaced CONSUMER_ID placeholder with: " + consumerId);
                }
                
                try {
                    LOG.info("Sending policy to EDC: " + policy.getPolicyId());
                    LOG.info("Policy JSON: " + policy.getPolicyJson());
                    
                    // Make sure the DTO is properly prepared
                    edcPolicyDTO = prepareEDCPolicyDTO(edcPolicyDTO);
                    
                    // Log the EDC DTO that we're about to send
                    try {
                        LOG.info("EDC Policy DTO: " + objectMapper.writeValueAsString(edcPolicyDTO));
                    } catch (Exception ex) {
                        LOG.warn("Could not serialize EDC Policy DTO for logging", ex);
                    }
                    
                    // Send to EDC - using a placeholder API Key here
                    LOG.info("Calling EDC API with apiKey: TEST2");
                    RestResponse<JsonObject> response = client.createPolicy("TEST2", policy.getPolicyJson());
                    
                    if (response.getStatus() >= 400) {
                        LOG.error("Error sending policy to EDC: " + response.getStatus() + ", " + response.getEntity());
                    } else {
                        LOG.info("Policy successfully sent to EDC with status: " + response.getStatus());
                        if (response.getEntity() != null) {
                            LOG.info("EDC response: " + response.getEntity());
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error sending policy to EDC", e);
                    e.printStackTrace(); // Print the full stack trace for debugging
                }
            } catch (Exception e) {
                LOG.error("Error creating EDC client", e);
            }
        } else {
            LOG.warn("Cannot send policy to EDC: EDC instance is null");
        }
        
        return policy;
    }

    /**
     * Searches for database entry with id. The returned object is linked to the database.
     * Database is updated according to changes after the program flow moves out of this method/transaction.
     * @param id to find the database entry
     * @param updatedPolicy contains the updated data
     * @return Optional with the updated EDCPolicy or an empty Optional if nothing was found.
     */
    /**
     * Updates a policy in the database and in the EDC if possible.
     * 
     * @param id to find the database entry
     * @param updatedPolicy contains the updated data
     * @return Optional with the updated EDCPolicy or an empty Optional if nothing was found.
     */
    @Transactional
    public Optional<EDCPolicy> update(final UUID id, final EDCPolicy updatedPolicy) {
        final EDCPolicy policyLinkedToDatabase = EDCPolicy.findById(id);
        if (policyLinkedToDatabase == null) {
            return Optional.empty();
        }
        
        // Update database entry
        policyLinkedToDatabase.setPolicyId(updatedPolicy.getPolicyId());
        policyLinkedToDatabase.setPolicyJson(updatedPolicy.getPolicyJson());
        
        // Try to update in EDC as well
        if (policyLinkedToDatabase.getEdcInstance() != null) {
            
            try {
                // First delete the existing policy in EDC if we have a policy ID
                if (policyLinkedToDatabase.getPolicyId() != null && !policyLinkedToDatabase.getPolicyId().trim().isEmpty()) {
                    EDCClient client = createClient("http://dataprovider-controlplane.tx.test/management/v3");
                    
                    try {
                        // Delete existing policy
                        LOG.info("Deleting existing policy in EDC: " + policyLinkedToDatabase.getPolicyId());
                        RestResponse<Void> deleteResponse = client.deletePolicy("TEST2", policyLinkedToDatabase.getPolicyId());
                        
                        if (deleteResponse.getStatus() >= 400 && deleteResponse.getStatus() != 404) {
                            // Log error but continue - 404 is ok as policy might not exist in EDC
                            LOG.warn("Error deleting policy in EDC: " + deleteResponse.getStatus());
                        }
                        
                        // Create the updated policy
                        CreateEDCPolicyDTO edcPolicyDTO;
                        try {
                            // Try to convert from existing policy JSON
                            edcPolicyDTO = objectMapper.readValue(policyLinkedToDatabase.getPolicyJson(), CreateEDCPolicyDTO.class);
                        } catch (Exception e) {
                            // If conversion fails, create new DTO
                            LOG.warn("Could not convert policy JSON to CreateEDCPolicyDTO, creating new one", e);
                            edcPolicyDTO = new CreateEDCPolicyDTO();
                            
                            // Set policy ID
                            if (policyLinkedToDatabase.getPolicyId() != null && !policyLinkedToDatabase.getPolicyId().trim().isEmpty()) {
                                edcPolicyDTO.setId(policyLinkedToDatabase.getPolicyId());
                            }
                            
                            // Make sure the DTO is properly prepared
                            edcPolicyDTO = prepareEDCPolicyDTO(edcPolicyDTO);
                        }
                        
                        // Send to EDC
                        LOG.info("Creating updated policy in EDC: " + policyLinkedToDatabase.getPolicyId());
                        RestResponse<JsonObject> createResponse = client.createPolicy("TEST2", updatedPolicy.getPolicyJson());
                        
                        if (createResponse.getStatus() >= 400) {
                            LOG.error("Error updating policy in EDC: " + createResponse.getStatus() + ", " + createResponse.getEntity());
                        } else {
                            LOG.info("Policy successfully updated in EDC");
                        }
                    } catch (Exception e) {
                        LOG.error("Error updating policy in EDC", e);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error creating EDC client", e);
            }
        } else {
            LOG.warn("Cannot update policy in EDC: EDC instance is null");
        }
        
        return Optional.of(policyLinkedToDatabase);
    }

    /**
     * Removes the policy from the database
     * @param id used to find policy to be deleted in database
     * @return true if deletion was successful, false otherwise
     */
    @Transactional
    public boolean delete(final UUID id) {
        LOG.info("Attempting to delete policy with ID: " + id);
        
        // First, check if the policy exists
        EDCPolicy policyToDelete = EDCPolicy.findById(id);
        if (policyToDelete == null) {
            LOG.warn("Policy with ID " + id + " not found, cannot delete");
            return false;
        }
        
        LOG.info("Found policy to delete: " + policyToDelete.id + " with policyId=" + policyToDelete.getPolicyId());
        
        // Try to delete from EDC first
        if (policyToDelete.getEdcInstance() != null && 
            policyToDelete.getPolicyId() != null && 
            !policyToDelete.getPolicyId().trim().isEmpty()) {
            
            try {
                EDCClient client = createClient("http://dataprovider-controlplane.tx.test/management/v3");
                LOG.info("Deleting policy from EDC: " + policyToDelete.getPolicyId());
                
                try {
                    RestResponse<Void> response = client.deletePolicy("TEST2", policyToDelete.getPolicyId());
                    
                    if (response.getStatus() >= 400 && response.getStatus() != 404) {
                        // Log error but continue with database deletion - 404 is ok as policy might not exist in EDC
                        LOG.warn("Error deleting policy from EDC: " + response.getStatus());
                    } else {
                        LOG.info("Policy successfully deleted from EDC");
                    }
                } catch (Exception e) {
                    LOG.error("Error deleting policy from EDC", e);
                    // Continue with database deletion even if EDC deletion failed
                }
            } catch (Exception e) {
                LOG.error("Error creating EDC client", e);
            }
        } else {
            LOG.warn("Cannot delete policy from EDC: EDC instance or policy ID is missing");
        }
        
        // Next, delete any contract definitions that reference this policy
        try {
            // Find contract definitions where this policy is either the access policy or contract policy
            TypedQuery<EDCContractDefinition> query = entityManager.createQuery(
                "SELECT cd FROM EDCContractDefinition cd WHERE cd.accessPolicy.id = :policyId OR cd.contractPolicy.id = :policyId", 
                EDCContractDefinition.class);
            query.setParameter("policyId", policyToDelete.id);
            List<EDCContractDefinition> contractDefs = query.getResultList();
            
            if (!contractDefs.isEmpty()) {
                LOG.info("Found " + contractDefs.size() + " contract definitions referencing policy " + id + ". Deleting them first.");
                
                for (EDCContractDefinition cd : contractDefs) {
                    LOG.info("Deleting contract definition " + cd.id + " with contractDefinitionId=" + cd.getContractDefinitionId());
                    entityManager.remove(cd);
                }
                
                // Flush to make sure contract definitions are deleted before we delete the policy
                entityManager.flush();
                LOG.info("Successfully deleted " + contractDefs.size() + " contract definitions");
            }
        } catch (Exception e) {
            LOG.error("Error deleting contract definitions for policy " + id, e);
            // Continue with policy deletion attempt anyway
        }
        
        try {
            // Now delete the policy itself
            entityManager.remove(policyToDelete);
            entityManager.flush(); // Force the delete to happen now
            LOG.info("Policy " + id + " deleted via EntityManager");
            return true;
        } catch (Exception e) {
            LOG.error("Error deleting policy " + id + " via EntityManager", e);
            // Fall back to PanacheEntityBase.deleteById
            boolean result = EDCPolicy.deleteById(id);
            LOG.info("Delete operation for policy " + id + " via PanacheEntityBase returned: " + result);
            return result;
        }
    }
    
    /**
     * Helper method to process a complex policy structure and extract values to set in the EDC Policy DTO.
     * This handles the nested structure with permissions, constraints, etc.
     * 
     * @param dto The EDC Policy DTO to populate
     * @param policyMap The policy structure as a Map
     */
    @SuppressWarnings("unchecked")
    private void processComplexPolicyStructure(CreateEDCPolicyDTO dto, Map<String, Object> policyMap) {
        LOG.info("Processing complex policy structure");
        
        // Extract permission array/object
        if (policyMap.containsKey("odrl:permission")) {
            Object permObj = policyMap.get("odrl:permission");
            List<Map<String, Object>> permissions = null;
            
            // Permission can be an array or a single object
            if (permObj instanceof List<?>) {
                permissions = (List<Map<String, Object>>) permObj;
            } else if (permObj instanceof Map<?,?>) {
                permissions = new ArrayList<>();
                permissions.add((Map<String, Object>) permObj);
            }
            
            // Process first permission (we only support one in our DTO)
            if (permissions != null && !permissions.isEmpty()) {
                Map<String, Object> permission = permissions.get(0);
                
                // Set action if available
                if (permission.containsKey("odrl:action")) {
                    String action = String.valueOf(permission.get("odrl:action"));
                    if (dto.getPolicy().getPermission() != null && !dto.getPolicy().getPermission().isEmpty()) {
                        dto.getPolicy().getPermission().get(0).setAction(action);
                    }
                    LOG.info("Set action from JSON: " + action);
                }
                
                // Process constraints
                if (permission.containsKey("odrl:constraint")) {
                    Object constraintObj = permission.get("odrl:constraint");
                    
                    if (constraintObj instanceof Map<?,?>) {
                        Map<String, Object> constraint = (Map<String, Object>) constraintObj;
                        processConstraint(dto, constraint);
                        
                        // Check for logical constraints
                        processLogicalConstraint(dto, constraint);
                    }
                }
            }
        }
    }
    
    /**
     * Helper method to process a constraint structure from the policy JSON.
     * 
     * @param dto The EDC Policy DTO to update
     * @param constraint The constraint map from the policy JSON
     */
    @SuppressWarnings("unchecked")
    private void processConstraint(CreateEDCPolicyDTO dto, Map<String, Object> constraint) {
        // Extract direct right operand if available
        if (constraint.containsKey("odrl:rightOperand")) {
            String rightOperand = String.valueOf(constraint.get("odrl:rightOperand"));
            if (dto.getConstraint() != null) {
                dto.getConstraint().setRightOperand(rightOperand);
                LOG.info("Set rightOperand from constraint: " + rightOperand);
            }
        }
        
        // Extract left operand if available
        if (constraint.containsKey("odrl:leftOperand")) {
            Object leftOperandObj = constraint.get("odrl:leftOperand");
            String leftOperand;
            
            // Handle left operand as object with @id
            if (leftOperandObj instanceof Map<?,?>) {
                Map<String, Object> leftOperandMap = (Map<String, Object>) leftOperandObj;
                leftOperand = leftOperandMap.containsKey("@id") ? 
                    String.valueOf(leftOperandMap.get("@id")) : "BusinessPartnerNumber";
            } else {
                leftOperand = String.valueOf(leftOperandObj);
            }
            
            if (dto.getConstraint() != null) {
                dto.getConstraint().setLeftOperand(leftOperand);
                LOG.info("Set leftOperand from constraint: " + leftOperand);
            }
        }
        
        // Extract operator if available
        if (constraint.containsKey("odrl:operator")) {
            Object operatorObj = constraint.get("odrl:operator");
            
            if (operatorObj instanceof Map<?,?>) {
                Map<String, Object> operatorMap = (Map<String, Object>) operatorObj;
                if (operatorMap.containsKey("@id")) {
                    String operatorId = String.valueOf(operatorMap.get("@id"));
                    if (dto.getConstraint() != null) {
                        dto.getConstraint().getOperator().setId(operatorId);
                        LOG.info("Set operator ID from constraint: " + operatorId);
                    }
                }
            }
        }
    }
    
    /**
     * Helper method to process logical constraints like odrl:or/odrl:and.
     * 
     * @param dto The EDC Policy DTO to update
     * @param constraint The constraint map that might contain logical operators
     */
    @SuppressWarnings("unchecked")
    private void processLogicalConstraint(CreateEDCPolicyDTO dto, Map<String, Object> constraint) {
        // Handle logical OR constraint
        if (constraint.containsKey("odrl:or") && constraint.get("odrl:or") instanceof List<?>) {
            List<Map<String, Object>> orConstraints = (List<Map<String, Object>>) constraint.get("odrl:or");
            
            // Use the first constraint in the OR list if available
            if (!orConstraints.isEmpty()) {
                Map<String, Object> firstConstraint = orConstraints.get(0);
                processConstraint(dto, firstConstraint);
                LOG.info("Processed first constraint from OR list");
            }
        }
        
        // Handle logical AND constraint (similar to OR)
        if (constraint.containsKey("odrl:and") && constraint.get("odrl:and") instanceof List<?>) {
            List<Map<String, Object>> andConstraints = (List<Map<String, Object>>) constraint.get("odrl:and");
            
            // Use the first constraint in the AND list if available
            if (!andConstraints.isEmpty()) {
                Map<String, Object> firstConstraint = andConstraints.get(0);
                processConstraint(dto, firstConstraint);
                LOG.info("Processed first constraint from AND list");
            }
        }
    }
    
    /**
     * Helper method to prepare the Policy DTO structure.
     * Ensures that all necessary objects are initialized in the DTO.
     * 
     * @param dto The EDC Policy DTO to prepare
     * @return The prepared DTO with initialized structure
     */
    private CreateEDCPolicyDTO prepareEDCPolicyDTO(CreateEDCPolicyDTO dto) {
        // Ensure policy structure exists
        if (dto.getPolicy() == null) {
            dto.setPolicy(new CreateEDCPolicyDTO.PolicyDTO());
        }
        
        // Ensure permission exists
        if (dto.getPolicy().getPermission() == null || dto.getPolicy().getPermission().isEmpty()) {
            List<CreateEDCPolicyDTO.PermissionDTO> permissions = new ArrayList<>();
            CreateEDCPolicyDTO.PermissionDTO permission = new CreateEDCPolicyDTO.PermissionDTO();
            permission.setAction("USE");
            permissions.add(permission);
            dto.getPolicy().setPermission(permissions);
        }
        
        // Ensure constraint exists in first permission
        if (!dto.getPolicy().getPermission().isEmpty()) {
            CreateEDCPolicyDTO.PermissionDTO permission = dto.getPolicy().getPermission().get(0);
            if (permission.getConstraint() == null) {
                permission.setConstraint(new CreateEDCPolicyDTO.ConstraintDTO());
            }
            
            // Set default values for constraint if needed
            CreateEDCPolicyDTO.ConstraintDTO constraint = permission.getConstraint();
            constraint.setLeftOperand("BusinessPartnerNumber");
            
            // Ensure operator exists
            if (constraint.getOr() != null && !constraint.getOr().isEmpty()) {
                CreateEDCPolicyDTO.InnerConstraintDTO innerConstraint = constraint.getOr().get(0);
                CreateEDCPolicyDTO.OperatorDTO operator = innerConstraint.getOperator();
                if (operator == null) {
                    operator = new CreateEDCPolicyDTO.OperatorDTO();
                    operator.setId("eq");
                    innerConstraint.setOperator(operator);
                }
            }
        }
        
        return dto;
    }
}
