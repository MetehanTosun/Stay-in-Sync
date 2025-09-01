package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EDCContractDefinitionService {

    private static final Logger LOG = Logger.getLogger(EDCContractDefinitionService.class);
    
    @PersistenceContext
    EntityManager entityManager;

    /**
     * Returns all contract definitions that are associated with a specific EDC instance.
     *
     * @param edcId the UUID of the EDC instance
     * @return List with all contract definitions for that EDC instance
     */
    public List<EDCContractDefinition> listAllByEdcId(final UUID edcId) {
        LOG.info("Fetching contract definitions for EDC: " + edcId);
        EDCInstance edcInstance = EDCInstance.findById(edcId);
        if (edcInstance == null) {
            LOG.warn("No EDC instance found with id " + edcId);
            return new ArrayList<>();
        }
        
        TypedQuery<EDCContractDefinition> query = entityManager.createQuery(
                "SELECT c FROM EDCContractDefinition c WHERE c.edcInstance.id = :edcId", 
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
     * @param edcId the UUID of the EDC instance
     * @return found contract definition or empty if not found
     */
    public Optional<EDCContractDefinition> findByIdAndEdcId(final UUID id, final UUID edcId) {
        LOG.info("Fetching contract definition " + id + " for EDC: " + edcId);
        TypedQuery<EDCContractDefinition> query = entityManager.createQuery(
                "SELECT c FROM EDCContractDefinition c WHERE c.id = :id AND c.edcInstance.id = :edcId", 
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
    @Transactional
    public EDCContractDefinition create(final EDCContractDefinition contractDefinition) {
        contractDefinition.persist();
        return contractDefinition;
    }

    /**
     * Searches for database entry with id. The returned object is linked to the database.
     * Database is updated according to changes after the program flow moves out of this method/transaction.
     * @param id to find the database entry
     * @param updatedContractDefinition contains the updated data
     * @return Optional with the updated EDCPolicy or an empty Optional if nothing was found.
     */
    @Transactional
    public Optional<EDCContractDefinition> update(final UUID id, final EDCContractDefinition updatedContractDefinition) {
        LOG.info("Updating contract definition: " + id);
        return findByIdAndEdcId(id, updatedContractDefinition.edcInstance.id).map(existing -> {
            // nur die zu ändernden Felder übernehmen
            existing.contractDefinitionId = updatedContractDefinition.contractDefinitionId;
            existing.asset              = EDCAsset.findById(updatedContractDefinition.asset != null ? updatedContractDefinition.asset.id : null);
            existing.accessPolicy       = EDCPolicy.findById(updatedContractDefinition.accessPolicy != null ? updatedContractDefinition.accessPolicy.id : null);
            existing.contractPolicy     = EDCPolicy.findById(updatedContractDefinition.contractPolicy != null ? updatedContractDefinition.contractPolicy.id : null);
            existing.edcInstance        = updatedContractDefinition.edcInstance;
            return existing;
        });
    }

    /**
     * Removes the policy from the database
     * @param id used to find policy to be deleted in database
     * @return the deleted policy
     */
    @Transactional
    public boolean delete(UUID id) {
        return EDCContractDefinition.deleteById(id);
    }
}

