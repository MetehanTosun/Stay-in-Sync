package de.unistuttgart.stayinsync.core.configuration.domain.entities.edc;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
public class EDC extends PanacheEntity {

    public String name;

    public String url;

    public String apiKey;

    @OneToMany(mappedBy = "targetEDC")
    public Set<EDCAsset> edcAssets;
}
