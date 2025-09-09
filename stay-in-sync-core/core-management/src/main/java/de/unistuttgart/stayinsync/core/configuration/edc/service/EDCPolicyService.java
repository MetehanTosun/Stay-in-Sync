package de.unistuttgart.stayinsync.core.configuration.edc.service;

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

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;


/**
 * EDCPolicyService interacts with the EDC database and offers methods for the important CRUD-Operations:
 * GET, GET ALL, POST, PUT, DELETE.
 */
@ApplicationScoped
public class EDCPolicyService {

    private static final Logger LOG = Logger.getLogger(EDCPolicyService.class);
    
    @PersistenceContext
    EntityManager entityManager;

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
     * @param policy to be persisted.
     * @return the created EDCPolicy.
     */
    @Transactional
    public EDCPolicy create(final EDCPolicy policy) {
        policy.persist();
        return policy;
    }

    /**
     * Searches for database entry with id. The returned object is linked to the database.
     * Database is updated according to changes after the program flow moves out of this method/transaction.
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
        policyLinkedToDatabase.setPolicyId(updatedPolicy.getPolicyId());
        policyLinkedToDatabase.setPolicyJson(updatedPolicy.getPolicyJson());
        return Optional.of(policyLinkedToDatabase);
    }

    /**
     * Removes the policy from the database
     * @param id used to find policy to be deleted in database
     * @return the deleted policy
     */
    @Transactional
    public boolean delete(final UUID id) {
        return EDCPolicy.deleteById(id);
    }
}
