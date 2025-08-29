package de.unistuttgart.stayinsync.core.configuration.edc.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;


/**
 * EDCPolicyService interacts with the EDC database and offers methods for the important CRUD-Operations:
 * GET, GET ALL, POST, PUT, DELETE.
 */
@ApplicationScoped
public class EDCPolicyService {

    /**
     * Returns a policy found in the database with the id.
     * @param id used to find the policy
     * @return found policy
     */
    public Optional<EDCPolicy> findById(final UUID id) {
        return EDCPolicy.findByIdOptional(id);
    }

    /**
     * Returns a list with all Policies saved in the database.
     * @return List with all EDCPolicies
     */
    public List<EDCPolicy> listAll() {
        return EDCPolicy.listAll();
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
        policyLinkedToDatabase.policyId = updatedPolicy.policyId;
        policyLinkedToDatabase.policyJson = updatedPolicy.policyJson;
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
