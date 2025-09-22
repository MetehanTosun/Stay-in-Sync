package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.core.transport.domain.TargetApiRequestConfigurationActionRole;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
public class TargetSystemApiRequestConfigurationAction extends PanacheEntity {
    @ManyToOne
    public TargetSystemApiRequestConfiguration targetSystemApiRequestConfiguration;

    @ManyToOne
    public TargetSystemEndpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TargetApiRequestConfigurationActionRole actionRole;

    /** Sets the position inside the execution order of a synchronisation in order to sequentially work through
     * the required ApiRequestConfigurations
     */
    @Column(nullable = false)
    public int executionOrder;

    // TODO: Evaluate key-mappings for workflow steps and dependencies (map string string or jsonb)
}
