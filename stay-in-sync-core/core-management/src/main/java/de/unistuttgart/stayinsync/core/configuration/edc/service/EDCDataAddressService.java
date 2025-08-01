package de.unistuttgart.stayinsync.core.configuration.edc.service;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCDataAddress;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EDCDataAddressService {

    public EDCDataAddress create(EDCDataAddress e) {
        e.persist();
        return e;
    }

    public List<EDCDataAddress> listAll() {
        return EDCDataAddress.listAll();
    }

    public Optional<EDCDataAddress> findById(Long id) {
        return Optional.ofNullable(EDCDataAddress.findById(id));
    }
}
