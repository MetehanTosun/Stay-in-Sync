package de.unistuttgart.stayinsync.core.configuration.edc.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCDataAddress;

//TODO Add documentation explaining the use of this Service (what is a dataAddress)
@ApplicationScoped
public class EDCDataAddressService {

    /**
     * Returns an EdcDataAddress found in the database with the id.
     * @param id used to find the edcDataAddress
     * @return found edcDataAddress
     */
    public Optional<EDCDataAddress> findById(Long id) {
        return Optional.ofNullable(EDCDataAddress.findById(id));
    }

    /**
     * Returns a list with all EdcDataAddresses saved in the database.
     * @return List with all edcDataAddress
     */
    public List<EDCDataAddress> listAll() {
        return EDCDataAddress.listAll();
    }

    /**
     * Moves EDCDataAddress into the database and returns it to the caller.
     * @param dataAddress to be persisted.
     * @return the created edcDataAddress.
     */
    public EDCDataAddress create(final EDCDataAddress dataAddress) {
        dataAddress.persist();
        return dataAddress;
    }




}
