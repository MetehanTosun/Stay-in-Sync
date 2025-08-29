package de.unistuttgart.stayinsync.core.configuration.edc.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCProperty;

//TODO Add documentation explaining the use of this Service (what is a EDCProperty)
@ApplicationScoped
public class EDCPropertyService {


    /**
     * Returns an edcProperty found in the database with the id.
     * @param  id used to find the policy
     * @return found policy
     */
    public Optional<EDCProperty> findById(final UUID id) {
        return EDCProperty.findByIdOptional(id);
    }

    /**
     * Returns a list with all edcProperties saved in the database.
     * @return List with all edcProperties
     */
    public List<EDCProperty> listAll() {
        return EDCProperty.listAll();
    }

    /**
     * Moves edcProperty into the database and returns it to the caller.
     * @param edcProperty to be persisted.
     * @return the created edcProperty.
     */
    @Transactional
    public EDCProperty create(final EDCProperty edcProperty) {
        edcProperty.persist();
        return edcProperty;
    }

    /**
     * Searches for database entry with id. The returned object is linked to the database.
     * Database is updated according to changes after the program flow moves out of this method/transaction.
     * @param id to find the database entry
     * @param updatedEdcProperty contains the updated data
     * @return Optional with the updated EDCProperty or an empty Optional if nothing was found.
     */
    @Transactional
    public Optional<EDCProperty> update(final UUID id, final EDCProperty updatedEdcProperty) {
        return findById(id)
            .map(existing -> {
                existing.description = updatedEdcProperty.description;
                return existing;
            });
    }

    /**
     * Removes the edcProperty from the database
     * @param id used to find edcProperty to be deleted in database
     * @return the deleted edcProperty
     */
    @Transactional
    public boolean delete(final UUID id) {
        return EDCProperty.deleteById(id);
    }
}
