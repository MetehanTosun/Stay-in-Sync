package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class ApiRequestConfigurationHeader extends PanacheEntity {

    @ManyToOne
    public ApiRequestConfiguration requestConfiguration;

    @ManyToOne
    public ApiHeader apiHeader;

    public String selectedValue;


}
