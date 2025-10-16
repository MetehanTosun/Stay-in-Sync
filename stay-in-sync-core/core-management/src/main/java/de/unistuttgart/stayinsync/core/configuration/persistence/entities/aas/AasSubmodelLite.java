package de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "aas_submodel_lite",
        uniqueConstraints = @UniqueConstraint(name = "uk_submodel_source_submodelId", columnNames = {"source_system_id", "submodel_id"})
)
public class AasSubmodelLite extends PanacheEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_system_id", nullable = false)
    public SourceSystem sourceSystem;

    @Column(name = "submodel_id", nullable = false)
    public String submodelId;

    @Column(name = "submodel_id_short")
    public String submodelIdShort;

    @Column(name = "semantic_id")
    public String semanticId;

    @Column(name = "kind")
    public String kind;
}


