package de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;

import java.util.List;
import java.util.Set;

@Entity
public class AasTargetApiRequestConfiguration extends PanacheEntity {
    @Column(nullable = false)
    public String alias;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    public TargetSystem targetSystem;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    public AasSubmodelLite submodel;

    @ManyToMany(mappedBy = "aasTargetApiRequestConfigurations")
    public Set<Transformation> transformations = new HashSet<>();

    @OneToMany(mappedBy = "aasTargetApiRequestConfiguration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    public List<de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasTargetApiRequestConfigurationAction> actions = new ArrayList<>();
}
