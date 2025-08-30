package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.dtoedc.EDCContractDefinitionDto;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCAsset;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCContractDefinition;
import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EDCContractDefinitionService {

    /**
     * Returns a ContractDefinition found in the database with the id.
     * @param id used to find the contractDefinition
     * @return found contractDefinition
     */
    public Optional<EDCContractDefinition> findById(final UUID id) {
        return EDCContractDefinition.findByIdOptional(id);
    }

    /**
     * Returns a list with all contractDefinition saved in the database.
     * @return List with all contractDefinitions
     */
    public List<EDCContractDefinition> listAll() {
        return EDCContractDefinition.listAll();
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
        return findById(id).map(existing -> {
            // nur die zu ändernden Felder übernehmen
            existing.contractDefinitionId = updatedContractDefinition.contractDefinitionId;
            existing.asset              = EDCAsset.findById(updatedContractDefinition.asset != null ? updatedContractDefinition.asset.id : null);
            existing.accessPolicy       = EDCPolicy.findById(updatedContractDefinition.accessPolicy != null ? updatedContractDefinition.accessPolicy.id : null);
            existing.contractPolicy     = EDCPolicy.findById(updatedContractDefinition.contractPolicy != null ? updatedContractDefinition.contractPolicy.id : null);
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

