package de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "aas_snapshot")
public class AasSnapshot extends PanacheEntity {

    @ManyToOne(optional = false, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "source_system_id", nullable = false)
    public SourceSystem sourceSystem;

    @Column(name = "snapshot_ts", nullable = false)
    public OffsetDateTime snapshotTs;

    @Column(name = "etag")
    public String etag;

    @Column(name = "hash")
    public String hash;
}


