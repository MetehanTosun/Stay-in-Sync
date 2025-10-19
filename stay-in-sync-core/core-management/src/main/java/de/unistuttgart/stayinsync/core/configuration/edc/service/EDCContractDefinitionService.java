package de.unistuttgart.stayinsync.core.configuration.edc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector.CreateEDCContractDefinitionDTO;
import de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector.EDCClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import jakarta.json.JsonObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EDCContractDefinitionService {

    private static final Logger LOG = Logger.getLogger(EDCContractDefinitionService.class);
    
    @PersistenceContext
    EntityManager entityManager;

    /**
     * Returns all contract definitions that are associated with a specific EDC instance.
     *
     * @param edcId the ID of the EDC instance
     * @return List with all contract definitions for that EDC instance
     */
    public List<EDCContractDefinition> listAllByEdcId(final Long edcId) {
        LOG.info("Fetching contract definitions for EDC: " + edcId);
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            LOG.warn("No EDC instance found with id " + edcId);
            return new ArrayList<>();
        }
        
    TypedQuery<EDCContractDefinition> query = entityManager.createQuery(
        "SELECT c FROM EDCContractDefinition c " +
        "LEFT JOIN FETCH c.asset " +
        "LEFT JOIN FETCH c.accessPolicy " +
        "LEFT JOIN FETCH c.contractPolicy " +
        "WHERE c.edcInstance.id = :edcId",
        EDCContractDefinition.class);
        query.setParameter("edcId", edcId);
        
        List<EDCContractDefinition> contractDefinitions = query.getResultList();
        LOG.info("Found " + contractDefinitions.size() + " contract definitions for EDC " + edcId);
        return contractDefinitions;
    }
    
    /**
     * Returns a contract definition found in the database with the id and belonging to the specific EDC instance.
     *
     * @param id used to find the contract definition
     * @param edcId the ID of the EDC instance
     * @return found contract definition or empty if not found
     */
    public Optional<EDCContractDefinition> findByIdAndEdcId(final Long id, final Long edcId) {
        LOG.info("Fetching contract definition " + id + " for EDC: " + edcId);
    TypedQuery<EDCContractDefinition> query = entityManager.createQuery(
        "SELECT c FROM EDCContractDefinition c " +
        "LEFT JOIN FETCH c.asset " +
        "LEFT JOIN FETCH c.accessPolicy " +
        "LEFT JOIN FETCH c.contractPolicy " +
        "WHERE c.id = :id AND c.edcInstance.id = :edcId",
        EDCContractDefinition.class);
        query.setParameter("id", id);
        query.setParameter("edcId", edcId);
        
        List<EDCContractDefinition> results = query.getResultList();
        if (results.isEmpty()) {
            LOG.warn("No contract definition found with id " + id + " for EDC " + edcId);
            return Optional.empty();
        }
        
        return Optional.of(results.get(0));
    }

    /**
     * Moves contractDefinition into the database and returns it to the caller.
     * @param contractDefinition to be persisted.
     * @return the created EDCPolicy.
     */
    @Inject
    ObjectMapper objectMapper;
    
    /**
     * Erstellt einen EDCClient für die Kommunikation mit dem EDC.
     * 
     * @param baseUrl Die Basis-URL des EDC
     * @return Ein konfigurierter EDCClient
     */
    private EDCClient createClient(String baseUrl) {
        try {
            return RestClientBuilder.newBuilder()
                    .baseUri(URI.create(baseUrl))
                    .build(EDCClient.class);
        } catch (Exception e) {
            LOG.error("Error creating EDC client", e);
            return null;
        }
    }
    
    @Transactional
    public EDCContractDefinition create(final EDCContractDefinition contractDefinition) {
        contractDefinition.persist();
        
        // Synchronisiere mit EDC direkt nach Speicherung in der Datenbank
        try {
            syncContractDefinitionWithEDC(contractDefinition);
        } catch (Exception e) {
            LOG.error("Error while syncing contract definition with EDC", e);
            // Wir werfen keine Exception, um den Datenbank-Vorgang nicht zu beeinträchtigen
        }
        
        return contractDefinition;
    }
    
    /**
     * Sendet eine Contract Definition an den EDC.
     * 
     * @param contractDefinition Die Contract Definition, die an den EDC gesendet werden soll
     */
    private void syncContractDefinitionWithEDC(EDCContractDefinition contractDefinition) {
        if (contractDefinition == null) {
            LOG.warn("Cannot sync null contract definition with EDC");
            return;
        }
        
        LOG.info("Attempting to sync contract definition with EDC: " + contractDefinition.contractDefinitionId);
        
        if (contractDefinition.edcInstance == null) {
            LOG.warn("Cannot sync contract definition: EDC instance is null");
            return;
        }
        
        String edcUrl = contractDefinition.edcInstance.edcContractDefinitionEndpoint;
        if (edcUrl == null || edcUrl.isEmpty()) {
            LOG.info("EDC endpoint URL not specified, using default");
            edcUrl = "http://dataprovider-controlplane.tx.test/management/v3";
        }
        
        EDCClient client = createClient(edcUrl);
        if (client != null) {
            try {
                // Erstellen des DTOs für den EDC
                CreateEDCContractDefinitionDTO edcContractDefinitionDTO = new CreateEDCContractDefinitionDTO();
                edcContractDefinitionDTO.setId(contractDefinition.contractDefinitionId);
                
                // Setzen der Asset- und Policy-IDs
                if (contractDefinition.asset != null && contractDefinition.asset.assetId != null) {
                    edcContractDefinitionDTO.setAssetId(contractDefinition.asset.assetId);

                } else {
                    LOG.warn("Asset ID is missing, contract definition may be invalid in EDC");
                }
                
                if (contractDefinition.accessPolicy != null && contractDefinition.accessPolicy.policyId != null) {
                    edcContractDefinitionDTO.setAccessPolicyId(contractDefinition.accessPolicy.policyId);
                } else {
                    LOG.warn("Access policy ID is missing, contract definition may be invalid in EDC");
                }
                
                if (contractDefinition.contractPolicy != null && contractDefinition.contractPolicy.policyId != null) {
                    edcContractDefinitionDTO.setContractPolicyId(contractDefinition.contractPolicy.policyId);
                } else {
                    LOG.warn("Contract policy ID is missing, contract definition may be invalid in EDC");
                }
                
                // Log the EDC DTO that we're about to send
                try {
                    LOG.info("EDC Contract Definition DTO: " + objectMapper.writeValueAsString(edcContractDefinitionDTO));
                } catch (Exception ex) {
                    LOG.warn("Could not serialize EDC Contract Definition DTO for logging", ex);
                }
                
                // Senden der Contract Definition an den EDC
                LOG.info("Sending contract definition to EDC: " + contractDefinition.contractDefinitionId);
                RestResponse<JsonObject> response = client.createContractDefinition("TEST2", contractDefinition.rawJson);
                
                if (response.getStatus() >= 400) {
                    LOG.error("Error creating contract definition in EDC: " + response.getStatus() + ", " + response.getEntity());
                } else {
                    LOG.info("Contract definition successfully created in EDC");
                }
            } catch (Exception e) {
                LOG.error("Error sending contract definition to EDC", e);
            }
        } else {
            LOG.warn("Cannot send contract definition to EDC: EDC client is null");
        }
    }

    /**
     * Searches for database entry with id. The returned object is linked to the database.
     * Database is updated according to changes after the program flow moves out of this method/transaction.
     * @param id to find the database entry
     * @param updatedContractDefinition contains the updated data
     * @return Optional with the updated EDCPolicy or an empty Optional if nothing was found.
     */
    @Transactional
    public Optional<EDCContractDefinition> update(final Long id, final EDCContractDefinition updatedContractDefinition) {
        LOG.info("Updating contract definition: " + id);
        return findByIdAndEdcId(id, updatedContractDefinition.getEdcInstance().id).map(existing -> {
            // nur die zu ändernden Felder übernehmen
            existing.setContractDefinitionId(updatedContractDefinition.getContractDefinitionId());
            existing.setAsset(EDCAsset.findById(updatedContractDefinition.getAsset() != null ? updatedContractDefinition.getAsset().id : null));
            existing.setAccessPolicy(EDCPolicy.findById(updatedContractDefinition.getAccessPolicy() != null ? updatedContractDefinition.getAccessPolicy().id : null));
            existing.setContractPolicy(EDCPolicy.findById(updatedContractDefinition.getContractPolicy() != null ? updatedContractDefinition.getContractPolicy().id : null));
            existing.setEdcInstance(updatedContractDefinition.getEdcInstance());
            
            // Versuche die aktualisierte Contract Definition mit dem EDC zu synchronisieren
            try {
                syncContractDefinitionWithEDC(existing);
            } catch (Exception e) {
                LOG.error("Error while syncing updated contract definition with EDC", e);
                // Wir werfen keine Exception, um den Datenbank-Vorgang nicht zu beeinträchtigen
            }
            
            return existing;
        });
    }

    /**
     * Removes the policy from the database
     * @param id used to find policy to be deleted in database
     * @return the deleted policy
     */
    @Transactional
    public boolean delete(Long id) {
        return EDCContractDefinition.deleteById(id);
    }
}

