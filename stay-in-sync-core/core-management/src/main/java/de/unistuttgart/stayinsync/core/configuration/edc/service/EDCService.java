package de.unistuttgart.stayinsync.core.configuration.edc.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;

@ApplicationScoped
public class EDCService {

    /**
     * Returns a edcInstance found in the database with the id.
     * @param id used to find the edcInstance
     * @return found edcInstance
     */
    public Optional<EDCInstance> findById(final UUID id) {
        return Optional.ofNullable(EDCInstance.findById(id));
    }

    /**
     * Returns a list with all edcInstance saved in the database.
     * @return List with all edcInstances
     */
    public List<EDCInstance> listAll() {
        return EDCInstance.listAll();
    }

    /**
     * Moves edcInstance into the database and returns it to the caller.
     * @param edcInstance to be persisted.
     * @return the created edcInstance.
     */
    public EDCInstance create(final EDCInstance edcInstance) {
        edcInstance.persist();
        return edcInstance;
    }

    /**
     * Searches for database entry with id. The returned object is linked to the database.
     * Database is updated according to changes after the program flow moves out of this method/transaction.
     * @param id to find the database entry
     * @param updatedEdcInstance contains the updated data
     * @return Optional with the updated edcInstance or an empty Optional if nothing was found.
     */
    public Optional<EDCInstance> update(final UUID id, final EDCInstance updatedEdcInstance) {
        return findById(id).map(existing -> {
            existing.name            = updatedEdcInstance.name;
            existing.url             = updatedEdcInstance.url;
            existing.apiKey          = updatedEdcInstance.apiKey;
            existing.protocolVersion = updatedEdcInstance.protocolVersion;
            existing.description     = updatedEdcInstance.description;
            existing.bpn             = updatedEdcInstance.bpn;
            return existing;
        });
    }

    /**
     * Removes the edcInstance from the database
     * @param id used to find edcInstance to be deleted in database
     * @return the deleted edcInstance
     */
    public boolean delete(final UUID id) {
        return EDCInstance.deleteById(id);
    }
}
