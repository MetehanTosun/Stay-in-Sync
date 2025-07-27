package de.unistuttgart.stayinsync.core.configuration.edc;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class EDCDataAddress extends PanacheEntity {

    public String jsonLDType;

    public String type;

    public String baseURL;

    public boolean proxyPath;

    public boolean proxyQueryParams;

}
